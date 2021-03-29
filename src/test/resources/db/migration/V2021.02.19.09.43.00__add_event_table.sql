create table aggregate_in_stream
(
    aggregate_id      uuid       not null,
    aggregate_name    varchar    not null,
    stream_id         uuid       not null,
    PRIMARY KEY (aggregate_id, aggregate_name, stream_id),
    CONSTRAINT aggregate UNIQUE (aggregate_id, aggregate_name) -- there should be only one stream for aggregate
);

create table event
(
    id                  uuid      not null,
    aggregate_id        uuid      not null, -- make it possible to put many aggregates into one stream
    aggregate_name      varchar   not null, -- make it possible to have same aggregate id for many aggregates of different purpose
    stream_id           uuid      not null, -- make it possible to split events on streams
    payload             jsonb     not null,
    order_of_occurrence bigserial not null,
    event_name          varchar   not null,
    event_version       int  not null,
    PRIMARY KEY (id),
    FOREIGN KEY (aggregate_id, aggregate_name, stream_id) REFERENCES aggregate_in_stream (aggregate_id, aggregate_name, stream_id)
);
