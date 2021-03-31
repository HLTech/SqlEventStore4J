package com.hltech.store;

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

    private static final String SAVE_EVENT_WITHOUT_OPTIMISTIC_LOCKING_QUERY =
            "insert into event(id, aggregate_id, aggregate_name, aggregate_version, stream_id, payload, event_name, event_version) "
            + "SELECT "
            + "   ?::uuid, "
            + "   ais.aggregate_id, "
            + "   ais.aggregate_name, "
            + "   (select coalesce(max(aggregate_version), 0) + 1 from event where aggregate_id = ais.aggregate_id and aggregate_name = ais.aggregate_name), "
            + "   ais.stream_id, "
            + "   ?::JSONB, "
            + "   ?, "
            + "   ? "
            + "FROM aggregate_in_stream ais "
            + "WHERE ais.aggregate_id = ? "
            + "AND ais.aggregate_name = ?";
    private static final String SAVE_EVENT_WITH_OPTIMISTIC_LOCKING_QUERY =
            "insert into event(id, aggregate_id, aggregate_name, aggregate_version, stream_id, payload, event_name, event_version) "
            + "SELECT "
            + "   ?::uuid, "
            + "   ais.aggregate_id, "
            + "   ais.aggregate_name, "
            + "   ("
            + "      SELECT CASE WHEN coalesce(MAX(aggregate_version), 0) = ? THEN ? + 1 END "
            + "      FROM event "
            + "      WHERE aggregate_id = ais.aggregate_id "
            + "      AND aggregate_name = ais.aggregate_name"
            + "   ), "
            + "   ais.stream_id, "
            + "   ?::JSONB, "
            + "   ?, "
            + "   ? "
            + "FROM aggregate_in_stream ais "
            + "WHERE ais.aggregate_id = ? "
            + "AND ais.aggregate_name = ?";
    private static final String SAVE_STREAM_QUERY = "insert into aggregate_in_stream(aggregate_id, aggregate_name, stream_id) "
            + "VALUES(?::uuid, ?, ?) ON CONFLICT DO NOTHING";
    public static final String FIND_ALL_BY_AGGREGATE_NAME_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "WHERE aggregate_name = ? "
            + "ORDER BY order_of_occurrence ASC";
    private static final String FIND_ALL_BY_AGGREGATE_ID_AND_AGGREGATE_NAME_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "WHERE aggregate_id = ?::UUID "
            + "AND aggregate_name = ? "
            + "ORDER BY order_of_occurrence ASC";
    private static final String FIND_ALL_TO_EVENT_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "WHERE aggregate_id = ?::UUID "
            + "AND aggregate_name = ? "
            + "and order_of_occurrence <= (select order_of_occurrence from event where id = ?::UUID) "
            + "ORDER BY order_of_occurrence ASC";

    private final Function<E, UUID> eventIdExtractor;
    private final Function<E, UUID> aggregateIdExtractor;
    private final EventTypeMapper<E> eventTypeMapper;
    private final EventBodyMapper<E> eventBodyMapper;
    private final DataSource dataSource;

    @Override
    public void save(
            E event,
            String aggregateName
    ) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(SAVE_EVENT_WITHOUT_OPTIMISTIC_LOCKING_QUERY)
        ) {
            pst.setObject(1, eventIdExtractor.apply(event));
            pst.setObject(2, eventBodyMapper.eventToString(event));
            pst.setObject(3, eventTypeMapper.toName((Class<? extends E>) event.getClass()));
            pst.setObject(4, eventTypeMapper.toVersion((Class<? extends E>) event.getClass()));
            pst.setObject(5, aggregateIdExtractor.apply(event));
            pst.setString(6, aggregateName);
            if (pst.executeUpdate() == 0) {
                throw new StreamNotExistException(aggregateIdExtractor.apply(event), aggregateName);
            }
        } catch (SQLException ex) {
            if (isOptimisticLockingRelated(ex)) {
                save(event, aggregateName);
            } else {
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
    }

    @Override
    public void save(
            E event,
            String aggregateName,
            int expectedAggregateVersion
    ) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(SAVE_EVENT_WITH_OPTIMISTIC_LOCKING_QUERY)
        ) {
            pst.setObject(1, eventIdExtractor.apply(event));
            pst.setObject(2, expectedAggregateVersion);
            pst.setObject(3, expectedAggregateVersion);
            pst.setObject(4, eventBodyMapper.eventToString(event));
            pst.setObject(5, eventTypeMapper.toName((Class<? extends E>) event.getClass()));
            pst.setObject(6, eventTypeMapper.toVersion((Class<? extends E>) event.getClass()));
            pst.setObject(7, aggregateIdExtractor.apply(event));
            pst.setString(8, aggregateName);

            if (pst.executeUpdate() == 0) {
                throw new StreamNotExistException(aggregateIdExtractor.apply(event), aggregateName);
            }
        } catch (SQLException ex) {
            if (isOptimisticLockingRelated(ex)) {
                throw new OptimisticLockingException(aggregateIdExtractor.apply(event), aggregateName, expectedAggregateVersion);
            } else {
                throw new EventStoreException(
                        String.format(
                                "Could not save event to database with aggregateId %s, aggregateName %s and expectedVersion %s",
                                aggregateIdExtractor.apply(event),
                                aggregateName,
                                expectedAggregateVersion
                        ),
                        ex
                );
            }
        }
    }

    @Override
    public void ensureStreamExist(UUID aggregateId, String aggregateName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(SAVE_STREAM_QUERY)
        ) {
            pst.setObject(1, aggregateId);
            pst.setObject(2, aggregateName);
            pst.setObject(3, randomUUID());
            pst.executeUpdate();
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not create stream for aggregateId %s and aggregateName %s", aggregateId, aggregateName),
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

    private List<E> extractEventsFromResultSet(ResultSet rs) throws SQLException {
        List<E> result = new ArrayList<>();

        while (rs.next()) {
            Class<? extends E> eventType = eventTypeMapper.toType(
                    rs.getString("event_name"),
                    rs.getInt("event_version")
            );
            E event = eventBodyMapper.stringToEvent(rs.getObject("payload").toString(), eventType);
            result.add(event);
        }
        return result;
    }

    private boolean isOptimisticLockingRelated(Exception ex) {
        return isAggregateVersionUniqueConstraintViolated(ex)
                || isAggregateVersionNotNullConstraintViolated(ex);
    }

    private boolean isAggregateVersionUniqueConstraintViolated(Exception ex) {
        return ex.getMessage().contains("ERROR: duplicate key value violates unique constraint \"aggregate_version_uq\"");
    }

    private boolean isAggregateVersionNotNullConstraintViolated(Exception ex) {
        return ex.getMessage().contains("ERROR: null value in column \"aggregate_version\" violates not-null constraint");
    }

}
