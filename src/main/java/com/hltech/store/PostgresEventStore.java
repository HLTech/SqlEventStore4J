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

    private static final String PAYLOAD_COLUMN = "payload";
    private static final String EVENT_NAME_COLUMN = "event_name";
    private static final String EVENT_VERSION_COLUMN = "event_version";

    private final Function<E, UUID> eventIdExtractor;
    private final Function<E, UUID> aggregateIdExtractor;
    private final EventTypeMapper<E> eventTypeMapper;
    private final EventMapper<E> eventMapper;
    private final DataSource dataSource;

    @Override
    public void save(
            E event,
            String streamType
    ) {
        String query = "insert into event(id, aggregate_id, stream_type, payload, event_name, event_version) "
                + "VALUES(?::uuid, ?::uuid, ?, ?::JSONB, ?, ?)";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, eventIdExtractor.apply(event));
            pst.setObject(2, aggregateIdExtractor.apply(event));
            pst.setString(3, streamType);
            pst.setObject(4, eventMapper.eventToString(event));
            pst.setObject(5, eventTypeMapper.toName((Class<? extends E>) event.getClass()));
            pst.setObject(6, eventTypeMapper.toVersion((Class<? extends E>) event.getClass()));

            pst.executeUpdate();
        } catch (SQLException ex) {
            log.error(
                    String.format("Could not save event to database with aggregateId %s  in stream %s", aggregateIdExtractor.apply(event), streamType),
                    ex
            );
        }
    }

    @Override
    public List<E> findAll(UUID aggregateId) {
        String query = "SELECT payload, event_name, event_version FROM event where aggregate_id = ?::UUID ORDER BY order_of_occurrence ASC";

        List<E> result = new ArrayList<>();

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, aggregateId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Class<? extends E> eventType = eventTypeMapper.toType(
                        rs.getString(EVENT_NAME_COLUMN),
                        rs.getShort(EVENT_VERSION_COLUMN)
                );
                E event = eventMapper.stringToEvent(rs.getString(PAYLOAD_COLUMN), eventType);
                result.add(event);
            }
        } catch (SQLException ex) {
            log.error(String.format("Could not find events for aggregate %s", aggregateId), ex);
        }
        return result;
    }

    @Override
    public Map<UUID, List<E>> findAll(String streamType) {
        String query = "SELECT payload, event_name, event_version FROM event where stream_type = ? ORDER BY order_of_occurrence ASC";

        List<E> result = new ArrayList<>();

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, streamType);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Class<? extends E> eventType = eventTypeMapper.toType(
                        rs.getString(EVENT_NAME_COLUMN),
                        rs.getShort(EVENT_VERSION_COLUMN)
                );
                E event = eventMapper.stringToEvent(rs.getObject(PAYLOAD_COLUMN).toString(), eventType);
                result.add(event);
            }
        } catch (SQLException ex) {
            log.error(String.format("Could not find events for stream %s", streamType), ex);
        }
        return result.stream().collect(groupingBy(aggregateIdExtractor));
    }

    @Override
    public List<E> findAll(UUID aggregateId, String streamType) {
        String query = "SELECT payload, event_name, event_version FROM event where aggregate_id = ?::UUID and stream_type = ? ORDER BY order_of_occurrence ASC";

        List<E> result = new ArrayList<>();

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, aggregateId);
            pst.setObject(2, streamType);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Class<? extends E> eventType = eventTypeMapper.toType(
                        rs.getString(EVENT_NAME_COLUMN),
                        rs.getShort(EVENT_VERSION_COLUMN)
                );
                E event = eventMapper.stringToEvent(rs.getObject(PAYLOAD_COLUMN).toString(), eventType);
                result.add(event);
            }
        } catch (SQLException ex) {
            log.error(String.format("Could not find events for aggregate %s and stream %s", aggregateId, streamType), ex);
        }
        return result;
    }

    @Override
    public List<E> findAllToEvent(E toEvent, String streamType) {
        String query = "SELECT payload, event_name, event_version FROM event where aggregate_id = ?::UUID and stream_type = ? "
                + " and order_of_occurrence <= (select order_of_occurrence from event where id = ?::UUID)"
                + " ORDER BY order_of_occurrence ASC";

        List<E> result = new ArrayList<>();

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, aggregateIdExtractor.apply(toEvent));
            pst.setObject(2, streamType);
            pst.setObject(3, eventIdExtractor.apply(toEvent));
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Class<? extends E> eventType = eventTypeMapper.toType(
                        rs.getString(EVENT_NAME_COLUMN),
                        rs.getShort(EVENT_VERSION_COLUMN)
                );
                E event = eventMapper.stringToEvent(rs.getObject(PAYLOAD_COLUMN).toString(), eventType);
                result.add(event);
            }
        } catch (SQLException ex) {
            log.error(String.format("Could not find events to event id %s for aggregate %s and stream %s",
                    eventIdExtractor.apply(toEvent), aggregateIdExtractor.apply(toEvent), streamType), ex);
        }
        return result;
    }

}
