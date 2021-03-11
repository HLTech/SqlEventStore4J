package com.hltech.store;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class PostgresEventStore implements EventStore {

    private static final String PAYLOAD_COLUMN = "payload";
    private static final String EVENT_NAME_COLUMN = "event_name";
    private static final String EVENT_VERSION_COLUMN = "event_version";

    private final EventTypeMapper eventTypeMapper;
    private final EventMapper eventMapper;
    private final DataSource dataSource;

    public PostgresEventStore(
            EventTypeMapper eventTypeMapper,
            EventMapper eventMapper,
            DataSource dataSource
    ) {
        this.eventTypeMapper = eventTypeMapper;
        this.eventMapper = eventMapper;
        this.dataSource = dataSource;
    }

    @Override
    public void save(
            Event event,
            String streamType
    ) {
        String query = "insert into event(id, aggregate_id, stream_type, payload, event_name, event_version) "
                + "VALUES(?::uuid, ?::uuid, ?, ?::JSONB, ?, ?)";

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, event.getId());
            pst.setObject(2, event.getAggregateId());
            pst.setString(3, streamType);
            pst.setObject(4, eventMapper.eventToString(event));
            pst.setObject(5, eventTypeMapper.toName(event.getClass()));
            pst.setObject(6, eventTypeMapper.toVersion(event.getClass()));

            pst.executeUpdate();
        } catch (SQLException ex) {
            log.error(
                    String.format("Could not save event to database with aggregateId %s  in stream %s", event.getAggregateId(), streamType),
                    ex
            );
        }
    }

    @Override
    public List<Event> findAll(UUID aggregateId) {
        String query = "SELECT payload, event_name, event_version FROM event where aggregate_id = ?::UUID ORDER BY order_of_occurrence ASC";

        List<Event> result = new ArrayList<>();

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, aggregateId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Class<? extends Event> eventType = eventTypeMapper.toType(
                        rs.getString(EVENT_NAME_COLUMN),
                        rs.getShort(EVENT_VERSION_COLUMN)
                );
                Event event = eventMapper.stringToEvent(rs.getString(PAYLOAD_COLUMN), eventType);
                result.add(event);
            }
        } catch (SQLException ex) {
            log.error(String.format("Could not find events for aggregate %s", aggregateId), ex);
        }
        return result;
    }

    @Override
    public List<Event> findAll(String streamType) {
        String query = "SELECT payload, event_name, event_version FROM event where stream_type = ? ORDER BY order_of_occurrence ASC";

        List<Event> result = new ArrayList<>();

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, streamType);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Class<? extends Event> eventType = eventTypeMapper.toType(
                        rs.getString(EVENT_NAME_COLUMN),
                        rs.getShort(EVENT_VERSION_COLUMN)
                );
                Event event = eventMapper.stringToEvent(rs.getObject(PAYLOAD_COLUMN).toString(), eventType);
                result.add(event);
            }
        } catch (SQLException ex) {
            log.error(String.format("Could not find events for stream %s", streamType), ex);
        }
        return result;
    }

    @Override
    public List<Event> findAll(UUID aggregateId, String streamType) {
        String query = "SELECT payload, event_name, event_version FROM event where aggregate_id = ?::UUID and stream_type = ? ORDER BY order_of_occurrence ASC";

        List<Event> result = new ArrayList<>();

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, aggregateId);
            pst.setObject(2, streamType);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Class<? extends Event> eventType = eventTypeMapper.toType(
                        rs.getString(EVENT_NAME_COLUMN),
                        rs.getShort(EVENT_VERSION_COLUMN)
                );
                Event event = eventMapper.stringToEvent(rs.getObject(PAYLOAD_COLUMN).toString(), eventType);
                result.add(event);
            }
        } catch (SQLException ex) {
            log.error(String.format("Could not find events for aggregate %s and stream %s", aggregateId, streamType), ex);
        }
        return result;
    }

    @Override
    public List<Event> findAllToEvent(Event toEvent, String streamType) {
        String query = "SELECT payload, event_name, event_version FROM event where aggregate_id = ?::UUID and stream_type = ? "
                + " and order_of_occurrence <= (select order_of_occurrence from event where id = ?::UUID)"
                + " ORDER BY order_of_occurrence ASC";

        List<Event> result = new ArrayList<>();

        try (
                Connection con = dataSource.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {
            pst.setObject(1, toEvent.getAggregateId());
            pst.setObject(2, streamType);
            pst.setObject(3, toEvent.getId());
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Class<? extends Event> eventType = eventTypeMapper.toType(
                        rs.getString(EVENT_NAME_COLUMN),
                        rs.getShort(EVENT_VERSION_COLUMN)
                );
                Event event = eventMapper.stringToEvent(rs.getObject(PAYLOAD_COLUMN).toString(), eventType);
                result.add(event);
            }
        } catch (SQLException ex) {
            log.error(String.format("Could not find events to event id %s for aggregate %s and stream %s",
                    toEvent.getId(), toEvent.getAggregateId(), streamType), ex);
        }
        return result;
    }

}
