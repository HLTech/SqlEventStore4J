package com.hltech.store;

import com.hltech.store.versioning.EventVersionPolicy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@RequiredArgsConstructor
public class OracleEventStore<E> implements EventStore<E> {

    private static final String SAVE_EVENT_QUERY =
            "INSERT INTO event(id, aggregate_version, stream_id, payload, event_name, event_version) "
            + "VALUES (?, ?, ?, ?, ?, ?) ";

    private static final String ENSURE_STREAM_EXIST_QUERY =
            "INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(aggregate_in_stream(aggregate_id, aggregate_name)) */ "
            + "INTO aggregate_in_stream(aggregate_id, aggregate_name, aggregate_version, stream_id) "
            + "VALUES(?, ?, 0, ?)";

    private static final String LOCK_STREAM =
            "SELECT stream_id, aggregate_id, aggregate_name, aggregate_version "
            + "FROM aggregate_in_stream "
            + "WHERE aggregate_id = ? "
            + "AND aggregate_name = ? "
            + "FOR UPDATE";

    private static final String INCREMENT_AGGREGATE_VERSION =
            "UPDATE aggregate_in_stream "
            + "SET aggregate_version = ? "
            + "WHERE aggregate_id = ? "
            + "AND aggregate_name = ? ";

    public static final String FIND_ALL_BY_AGGREGATE_NAME_QUERY =
            "SELECT e.payload, e.event_name, e.event_version "
            + "FROM aggregate_in_stream ais "
            + "JOIN event e ON e.stream_id = ais.stream_id "
            + "WHERE ais.aggregate_name = ? "
            + "ORDER BY e.order_of_occurrence ASC";

    private static final String FIND_ALL_BY_AGGREGATE_ID_AND_AGGREGATE_NAME_QUERY =
            "SELECT e.payload, e.event_name, e.event_version "
            + "FROM aggregate_in_stream ais "
            + "JOIN event e ON e.stream_id = ais.stream_id "
            + "WHERE ais.aggregate_id = ? "
            + "AND ais.aggregate_name = ? "
            + "ORDER BY e.order_of_occurrence ASC";
    private static final String FIND_ALL_TO_EVENT_QUERY =
            "SELECT e.payload, e.event_name, e.event_version "
            + "FROM aggregate_in_stream ais "
            + "JOIN event e ON e.stream_id = ais.stream_id "
            + "WHERE ais.aggregate_id = ? "
            + "AND ais.aggregate_name = ? "
            + "AND e.order_of_occurrence <= (SELECT order_of_occurrence FROM event WHERE id = ?) "
            + "ORDER BY e.order_of_occurrence ASC";

    private final Function<E, UUID> eventIdExtractor;
    private final Function<E, UUID> aggregateIdExtractor;
    private final EventVersionPolicy<E> eventVersionPolicy;
    private final EventBodyMapper<E> eventBodyMapper;
    private final DataSource dataSource;

    @Override
    public void save(
            E event,
            String aggregateName
    ) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            AggregateInStream aggregateInStream = lockStream(connection, aggregateIdExtractor.apply(event), aggregateName);
            saveEvent(connection, event, aggregateInStream);
            incrementAggregateVersion(connection, aggregateInStream);
            connection.commit();
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format(
                            "Could not save event to database with aggregateId %s and aggregateName %s",
                            aggregateIdExtractor.apply(event),
                            aggregateName
                    ),
                    ex
            );
        }
    }

    @Override
    public void save(
            E event,
            String aggregateName,
            int expectedAggregateVersion
    ) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            AggregateInStream aggregateInStream = lockStream(connection, aggregateIdExtractor.apply(event), aggregateName);
            if (aggregateInStream.getAggregateVersion() != expectedAggregateVersion) {
                throw new OptimisticLockingException(aggregateIdExtractor.apply(event), aggregateName, expectedAggregateVersion);
            }
            saveEvent(connection, event, aggregateInStream);
            incrementAggregateVersion(connection, aggregateInStream);
            connection.commit();
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format(
                            "Could not save event to database with aggregateId %s and aggregateName %s",
                            aggregateIdExtractor.apply(event),
                            aggregateName
                    ),
                    ex
            );
        }
    }

    @Override
    public Map<UUID, List<E>> findAll(String aggregateName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_AGGREGATE_NAME_QUERY)
        ) {
            pst.setObject(1, aggregateName);
            ResultSet rs = pst.executeQuery();

            List<E> result = extractEventsFromResultSet(rs);
            return result.stream().collect(groupingBy(aggregateIdExtractor));
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events for stream %s", aggregateName), ex
            );
        }
    }

    @Override
    public List<E> findAll(UUID aggregateId, String aggregateName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_AGGREGATE_ID_AND_AGGREGATE_NAME_QUERY)
        ) {
            pst.setObject(1, uuidToDatabaseUUID(aggregateId));
            pst.setObject(2, aggregateName);
            ResultSet rs = pst.executeQuery();

            return extractEventsFromResultSet(rs);
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events for aggregate %s and stream %s", aggregateId, aggregateName), ex
            );
        }
    }

    @Override
    public List<E> findAllToEvent(E toEvent, String aggregateName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_TO_EVENT_QUERY)
        ) {
            pst.setObject(1, uuidToDatabaseUUID(aggregateIdExtractor.apply(toEvent)));
            pst.setObject(2, aggregateName);
            pst.setObject(3, uuidToDatabaseUUID(eventIdExtractor.apply(toEvent)));
            ResultSet rs = pst.executeQuery();

            return extractEventsFromResultSet(rs);
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events to event id %s for aggregate %s and stream %s",
                            eventIdExtractor.apply(toEvent), aggregateIdExtractor.apply(toEvent), aggregateName),
                    ex
            );
        }
    }

    private void saveEvent(
            Connection connection,
            E event,
            AggregateInStream aggregateInStream
    ) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(SAVE_EVENT_QUERY)) {
            pst.setObject(1, uuidToDatabaseUUID(eventIdExtractor.apply(event)));
            pst.setObject(2, aggregateInStream.getAggregateVersion() + 1);
            pst.setObject(3, uuidToDatabaseUUID(aggregateInStream.getStreamId()));
            pst.setBlob(4, new ByteArrayInputStream(eventBodyMapper.eventToString(event).getBytes(UTF_8)));
            pst.setObject(5, eventVersionPolicy.toName((Class<? extends E>) event.getClass()));
            pst.setObject(6, eventVersionPolicy.toVersion((Class<? extends E>) event.getClass()));
            pst.executeUpdate();
        }
    }

    private AggregateInStream lockStream(
            Connection connection,
            UUID aggregateId,
            String aggregateName
    ) throws SQLException {
        try (
                PreparedStatement pst = connection.prepareStatement(LOCK_STREAM)
        ) {
            pst.setObject(1, uuidToDatabaseUUID(aggregateId));
            pst.setString(2, aggregateName);
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) {
                ensureStreamExist(connection, aggregateId, aggregateName);
                return lockStream(connection, aggregateId, aggregateName);
            }
            return new AggregateInStream(
                    aggregateId,
                    aggregateName,
                    rs.getInt("aggregate_version"),
                    UUID.fromString(rs.getString("stream_id"))
            );

        }
    }

    private void ensureStreamExist(
            Connection connection,
            UUID aggregateId,
            String aggregateName
    ) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(ENSURE_STREAM_EXIST_QUERY)) {
            pst.setObject(1, uuidToDatabaseUUID(aggregateId));
            pst.setObject(2, aggregateName);
            pst.setObject(3, uuidToDatabaseUUID(randomUUID()));
            pst.executeUpdate();
        }
    }

    private void incrementAggregateVersion(
            Connection connection,
            AggregateInStream aggregateInStream
    ) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(INCREMENT_AGGREGATE_VERSION)) {
            pst.setObject(1, aggregateInStream.getAggregateVersion() + 1);
            pst.setObject(2, uuidToDatabaseUUID(aggregateInStream.getAggregateId()));
            pst.setObject(3, aggregateInStream.getAggregateName());
            pst.executeUpdate();
        }
    }

    private List<E> extractEventsFromResultSet(ResultSet rs) throws SQLException {
        List<E> result = new ArrayList<>();

        while (rs.next()) {
            Class<? extends E> eventType = eventVersionPolicy.toType(
                    rs.getString("event_name"),
                    rs.getInt("event_version")
            );

            Blob blobedPayload = rs.getBlob("payload");
            byte[] buffedPayload = blobedPayload.getBytes(1, (int) blobedPayload.length());
            E event = eventBodyMapper.stringToEvent(new String(buffedPayload, UTF_8), eventType);
            result.add(event);
        }
        return result;
    }

    private Object uuidToDatabaseUUID(UUID uuid) {
        return String.valueOf(uuid);
    }

    @RequiredArgsConstructor
    @Getter
    private static class AggregateInStream {

        final UUID aggregateId;
        final String aggregateName;
        final int aggregateVersion;
        final UUID streamId;

    }

}
