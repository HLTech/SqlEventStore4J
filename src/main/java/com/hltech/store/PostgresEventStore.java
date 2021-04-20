package com.hltech.store;

import com.hltech.store.versioning.EventVersioningStrategy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@RequiredArgsConstructor
public class PostgresEventStore<E> implements EventStore<E> {

    private static final String SAVE_EVENT_QUERY =
            "INSERT INTO event(id, aggregate_version, stream_id, payload, event_name, event_version) "
            + "VALUES (?::uuid, ?, ?::uuid, ?::JSONB, ?, ?) ";

    private static final String ENSURE_STREAM_EXIST_QUERY =
            "INSERT INTO aggregate_in_stream(aggregate_id, aggregate_name, aggregate_version, stream_id) "
            + "VALUES(?::uuid, ?, 0, ?) ON CONFLICT DO NOTHING";

    private static final String LOCK_STREAM =
            "SELECT stream_id, aggregate_id, aggregate_name, aggregate_version "
            + "FROM aggregate_in_stream "
            + "WHERE aggregate_id = ?::UUID "
            + "AND aggregate_name = ? "
            + "FOR UPDATE";

    private static final String INCREMENT_AGGREGATE_VERSION =
            "UPDATE aggregate_in_stream "
            + "SET aggregate_version = ? "
            + "WHERE aggregate_id = ?::UUID "
            + "AND aggregate_name = ? ";

    public static final String FIND_BY_ID_AND_AGGREGATE_ID_AND_AGGREGATE_NAME_QUERY =
            "SELECT e.payload, e.event_name, e.event_version "
            + "FROM aggregate_in_stream ais "
            + "JOIN event e ON e.stream_id = ais.stream_id "
            + "WHERE e.id = ?::UUID "
            + "AND ais.aggregate_id = ?::UUID "
            + "AND ais.aggregate_name = ?";

    public static final String FIND_ALL_BY_AGGREGATE_NAME_QUERY =
            "SELECT e.payload, e.event_name, e.event_version "
            + "FROM aggregate_in_stream ais "
            + "JOIN event e ON e.stream_id = ais.stream_id "
            + "WHERE ais.aggregate_name = ? "
            + "ORDER BY e.order_of_occurrence ASC";

    private static final String FIND_ALL_BY_AGGREGATE_ID_QUERY =
            "SELECT e.payload, e.event_name, e.event_version "
            + "FROM aggregate_in_stream ais "
            + "JOIN event e ON e.stream_id = ais.stream_id "
            + "WHERE ais.aggregate_id = ?::UUID "
            + "ORDER BY e.order_of_occurrence ASC";

    private static final String FIND_ALL_BY_AGGREGATE_ID_AND_AGGREGATE_NAME_QUERY =
            "SELECT e.payload, e.event_name, e.event_version "
            + "FROM aggregate_in_stream ais "
            + "JOIN event e ON e.stream_id = ais.stream_id "
            + "WHERE ais.aggregate_id = ?::UUID "
            + "AND ais.aggregate_name = ? "
            + "ORDER BY e.order_of_occurrence ASC";

    private static final String FIND_ALL_TO_EVENT_QUERY =
            "SELECT e.payload, e.event_name, e.event_version "
            + "FROM aggregate_in_stream ais "
            + "JOIN event e ON e.stream_id = ais.stream_id "
            + "WHERE ais.aggregate_id = ?::UUID "
            + "AND ais.aggregate_name = ? "
            + "AND e.order_of_occurrence <= (SELECT order_of_occurrence FROM event WHERE id = ?::UUID) "
            + "ORDER BY e.order_of_occurrence ASC";

    private final Function<E, UUID> eventIdExtractor;
    private final Function<E, UUID> aggregateIdExtractor;
    private final EventVersioningStrategy<E> eventVersioningStrategy;
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
    public boolean contains(E event, String aggregateName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_BY_ID_AND_AGGREGATE_ID_AND_AGGREGATE_NAME_QUERY)
        ) {
            pst.setObject(1, eventIdExtractor.apply(event));
            pst.setObject(2, aggregateIdExtractor.apply(event));
            pst.setObject(3, aggregateName);
            ResultSet rs = pst.executeQuery();

            List<E> events = extractEventsFromResultSet(rs);
            if (events.isEmpty()) {
                return false;
            } else {
                return events.get(0).equals(event);
            }
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format(
                            "Could not find event by id %s and aggregate id %s and aggregate name %s",
                            eventIdExtractor.apply(event),
                            aggregateIdExtractor.apply(event),
                            aggregateName
                    ),
                    ex
            );
        }
    }

    @Override
    public Map<UUID, List<E>> findAllGroupByAggregate(String aggregateName) {
        return findAll(aggregateName)
                .stream()
                .collect(groupingBy(aggregateIdExtractor));
    }

    @Override
    public List<E> findAll(String aggregateName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_AGGREGATE_NAME_QUERY)
        ) {
            pst.setObject(1, aggregateName);
            ResultSet rs = pst.executeQuery();
            return extractEventsFromResultSet(rs);
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events for aggregate name %s", aggregateName), ex
            );
        }
    }

    @Override
    public List<E> findAll(UUID aggregateId) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_AGGREGATE_ID_QUERY)
        ) {
            pst.setObject(1, aggregateId);
            ResultSet rs = pst.executeQuery();
            return extractEventsFromResultSet(rs);
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events for aggregate id %s", aggregateId), ex
            );
        }
    }

    @Override
    public List<E> findAll(UUID aggregateId, String aggregateName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_AGGREGATE_ID_AND_AGGREGATE_NAME_QUERY)
        ) {
            pst.setObject(1, aggregateId);
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
            pst.setObject(1, aggregateIdExtractor.apply(toEvent));
            pst.setObject(2, aggregateName);
            pst.setObject(3, eventIdExtractor.apply(toEvent));
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

    @Override
    public EventVersioningStrategy<E> getEventVersioningStrategy() {
        return eventVersioningStrategy;
    }

    private void saveEvent(
            Connection connection,
            E event,
            AggregateInStream aggregateInStream
    ) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(SAVE_EVENT_QUERY)) {
            pst.setObject(1, eventIdExtractor.apply(event));
            pst.setObject(2, aggregateInStream.getAggregateVersion() + 1);
            pst.setObject(3, aggregateInStream.getStreamId());
            pst.setObject(4, eventVersioningStrategy.toJson(event));
            pst.setObject(5, eventVersioningStrategy.toName((Class<? extends E>) event.getClass()));
            pst.setObject(6, eventVersioningStrategy.toVersion((Class<? extends E>) event.getClass()));
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
            pst.setObject(1, aggregateId);
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
                    (UUID) rs.getObject("stream_id")
            );

        }
    }

    private void ensureStreamExist(
            Connection connection,
            UUID aggregateId,
            String aggregateName
    ) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(ENSURE_STREAM_EXIST_QUERY)) {
            pst.setObject(1, aggregateId);
            pst.setObject(2, aggregateName);
            pst.setObject(3, randomUUID());
            pst.executeUpdate();
        }
    }

    private void incrementAggregateVersion(
            Connection connection,
            AggregateInStream aggregateInStream
    ) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(INCREMENT_AGGREGATE_VERSION)) {
            pst.setObject(1, aggregateInStream.getAggregateVersion() + 1);
            pst.setObject(2, aggregateInStream.getAggregateId());
            pst.setObject(3, aggregateInStream.getAggregateName());
            pst.executeUpdate();
        }
    }

    private List<E> extractEventsFromResultSet(ResultSet rs) throws SQLException {
        List<E> result = new ArrayList<>();

        while (rs.next()) {
            E event = eventVersioningStrategy.toEvent(
                    rs.getObject("payload").toString(),
                    rs.getString("event_name"),
                    rs.getInt("event_version")
            );
            result.add(event);
        }
        return result;
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
