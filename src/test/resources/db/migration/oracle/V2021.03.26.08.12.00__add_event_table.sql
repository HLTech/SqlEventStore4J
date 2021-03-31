create table aggregate_in_stream
(
    aggregate_id      varchar2(36)       not null,
    aggregate_name    varchar2(256)    not null,
    stream_id         varchar2(36)       not null,
    PRIMARY KEY (aggregate_id, aggregate_name, stream_id),
    CONSTRAINT aggregate UNIQUE (aggregate_id, aggregate_name) -- there should be only one stream for aggregate
);

create table event
(
        id                  varchar2(36)      not null,
    aggregate_id        varchar2(36)      not null, -- make it possible to put many aggregates into one stream
    aggregate_name      varchar2(256)   not null, -- make it possible to have same aggregate id for many aggregates of different purpose
    stream_id           varchar2(36)      not null, -- make it possible to split events on streams
    payload             blob     not null,
    order_of_occurrence number(38) not null,
    event_name          varchar2(256)   not null,
    event_version       number(38)  not null,
    PRIMARY KEY (id),
    FOREIGN KEY (aggregate_id, aggregate_name, stream_id) REFERENCES aggregate_in_stream (aggregate_id, aggregate_name, stream_id)

);

CREATE SEQUENCE order_of_occurrence_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER order_of_occurrence_seq_tr
    BEFORE INSERT ON event FOR EACH ROW
    WHEN (NEW.order_of_occurrence IS NULL OR NEW.order_of_occurrence = 0)
BEGIN
    SELECT order_of_occurrence_seq.NEXTVAL INTO :NEW.order_of_occurrence FROM dual;
END;
