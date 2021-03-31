create table aggregate_in_stream
(
    aggregate_id      uuid       not null,
    aggregate_name    varchar    not null,
    aggregate_version int        not null,
    stream_id         uuid       not null,
    PRIMARY KEY (stream_id),
    CONSTRAINT aggregate_uq UNIQUE (aggregate_id, aggregate_name) -- there should be only one stream for aggregate
);

create table event
(
    id                  uuid      not null,
    stream_id           uuid      not null,
    aggregate_version   int       not null,
    payload             jsonb     not null,
    order_of_occurrence bigserial not null,
    event_name          varchar   not null,
    event_version       int  not null,
    PRIMARY KEY (id),
    FOREIGN KEY (stream_id) REFERENCES aggregate_in_stream (stream_id)
);
