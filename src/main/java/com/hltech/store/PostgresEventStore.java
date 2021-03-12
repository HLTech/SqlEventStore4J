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

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@RequiredArgsConstructor
public class PostgresEventStore<E> implements EventStore<E> {

    public static final String SAVE_EVENT_QUERY = "insert into event(id, aggregate_id, stream_type, payload, event_name, event_version) "
            + "VALUES(?::uuid, ?::uuid, ?, ?::JSONB, ?, ?)";
    public static final String FIND_ALL_BY_AGGREGATE_ID_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "where aggregate_id = ?::UUID "
            + "ORDER BY order_of_occurrence ASC";
    public static final String FIND_ALL_BY_STREAM_TYPE_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event where stream_type = ? "
            + "ORDER BY order_of_occurrence ASC";
    public static final String FIND_ALL_BY_AGGREGATE_ID_AND_STREAM_TYPE_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "where aggregate_id = ?::UUID and stream_type = ? "
            + "ORDER BY order_of_occurrence ASC";
    public static final String FIND_ALL_FOR_EVENT_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "where aggregate_id = ?::UUID and stream_type = ? "
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
            String streamType
    ) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(SAVE_EVENT_QUERY)
        ) {
            pst.setObject(1, eventIdExtractor.apply(event));
            pst.setObject(2, aggregateIdExtractor.apply(event));
            pst.setString(3, streamType);
            pst.setObject(4, eventBodyMapper.eventToString(event));
            pst.setObject(5, eventTypeMapper.toName((Class<? extends E>) event.getClass()));
            pst.setObject(6, eventTypeMapper.toVersion((Class<? extends E>) event.getClass()));

            pst.executeUpdate();
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not save event to database with aggregateId %s  in stream %s", aggregateIdExtractor.apply(event), streamType),
                    ex
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
                    String.format("Could not find events for aggregate %s", aggregateId), ex
            );
        }
    }

    @Override
    public Map<UUID, List<E>> findAll(String streamType) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_STREAM_TYPE_QUERY)
        ) {
            pst.setObject(1, streamType);
            ResultSet rs = pst.executeQuery();

            List<E> result = extractEventsFromResultSet(rs);
            return result.stream().collect(groupingBy(aggregateIdExtractor));
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events for stream %s", streamType), ex
            );
        }
    }

    @Override
    public List<E> findAll(UUID aggregateId, String streamType) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_AGGREGATE_ID_AND_STREAM_TYPE_QUERY)
        ) {
            pst.setObject(1, aggregateId);
            pst.setObject(2, streamType);
            ResultSet rs = pst.executeQuery();

            return extractEventsFromResultSet(rs);
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events for aggregate %s and stream %s", aggregateId, streamType), ex
            );
        }
    }

    @Override
    public List<E> findAllToEvent(E toEvent, String streamType) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_FOR_EVENT_QUERY)
        ) {
            pst.setObject(1, aggregateIdExtractor.apply(toEvent));
            pst.setObject(2, streamType);
            pst.setObject(3, eventIdExtractor.apply(toEvent));
            ResultSet rs = pst.executeQuery();

            return extractEventsFromResultSet(rs);
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events to event id %s for aggregate %s and stream %s",
                            eventIdExtractor.apply(toEvent), aggregateIdExtractor.apply(toEvent), streamType),
                    ex
            );
        }
    }

    private List<E> extractEventsFromResultSet(ResultSet rs) throws SQLException {
        List<E> result = new ArrayList<>();

        while (rs.next()) {
            Class<? extends E> eventType = eventTypeMapper.toType(
                    rs.getString("event_name"),
                    rs.getShort("event_version")
            );
            E event = eventBodyMapper.stringToEvent(rs.getObject("payload").toString(), eventType);
            result.add(event);
        }
        return result;
    }

}
