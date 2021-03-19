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

    public static final String SAVE_EVENT_QUERY = "insert into event(id, aggregate_id, stream_name, payload, event_name, event_version) "
            + "VALUES(?::uuid, ?::uuid, ?, ?::JSONB, ?, ?)";
    public static final String FIND_ALL_BY_AGGREGATE_ID_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "where aggregate_id = ?::UUID "
            + "ORDER BY order_of_occurrence ASC";
    public static final String FIND_ALL_BY_STREAM_NAME_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event where stream_name = ? "
            + "ORDER BY order_of_occurrence ASC";
    public static final String FIND_ALL_BY_AGGREGATE_ID_AND_STREAM_NAME_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "where aggregate_id = ?::UUID and stream_name = ? "
            + "ORDER BY order_of_occurrence ASC";
    public static final String FIND_ALL_FOR_EVENT_QUERY = "SELECT payload, event_name, event_version "
            + "FROM event "
            + "where aggregate_id = ?::UUID and stream_name = ? "
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
            String streamName
    ) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(SAVE_EVENT_QUERY)
        ) {
            pst.setObject(1, eventIdExtractor.apply(event));
            pst.setObject(2, aggregateIdExtractor.apply(event));
            pst.setString(3, streamName);
            pst.setObject(4, eventBodyMapper.eventToString(event));
            pst.setObject(5, eventTypeMapper.toName((Class<? extends E>) event.getClass()));
            pst.setObject(6, eventTypeMapper.toVersion((Class<? extends E>) event.getClass()));

            pst.executeUpdate();
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not save event to database with aggregateId %s  in stream %s", aggregateIdExtractor.apply(event), streamName),
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
    public Map<UUID, List<E>> findAll(String streamName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_STREAM_NAME_QUERY)
        ) {
            pst.setObject(1, streamName);
            ResultSet rs = pst.executeQuery();

            List<E> result = extractEventsFromResultSet(rs);
            return result.stream().collect(groupingBy(aggregateIdExtractor));
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events for stream %s", streamName), ex
            );
        }
    }

    @Override
    public List<E> findAll(UUID aggregateId, String streamName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_BY_AGGREGATE_ID_AND_STREAM_NAME_QUERY)
        ) {
            pst.setObject(1, aggregateId);
            pst.setObject(2, streamName);
            ResultSet rs = pst.executeQuery();

            return extractEventsFromResultSet(rs);
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events for aggregate %s and stream %s", aggregateId, streamName), ex
            );
        }
    }

    @Override
    public List<E> findAllToEvent(E toEvent, String streamName) {
        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(FIND_ALL_FOR_EVENT_QUERY)
        ) {
            pst.setObject(1, aggregateIdExtractor.apply(toEvent));
            pst.setObject(2, streamName);
            pst.setObject(3, eventIdExtractor.apply(toEvent));
            ResultSet rs = pst.executeQuery();

            return extractEventsFromResultSet(rs);
        } catch (SQLException ex) {
            throw new EventStoreException(
                    String.format("Could not find events to event id %s for aggregate %s and stream %s",
                            eventIdExtractor.apply(toEvent), aggregateIdExtractor.apply(toEvent), streamName),
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

}
