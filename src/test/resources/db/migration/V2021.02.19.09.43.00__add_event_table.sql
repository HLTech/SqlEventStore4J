create table event
(
    id                  uuid      not null
        constraint event_pkey
            primary key,
    aggregate_id        uuid      not null,
    payload             jsonb     not null,
    order_of_occurrence bigserial not null,
    stream_name         varchar   not null,
    event_name          varchar   not null,
    event_version       int  not null
);
