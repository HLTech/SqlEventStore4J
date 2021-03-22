# SQL Event Store For Java

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Table of Contents
1. [**Overview**](#Overview)
2. [**How to add it to project**](#HowToAddItToProject)
3. [**How to use it**](#HowToUseIt)
4. [**Events versioning**](#EventsVersioning)
4. [**Databases**](#Databases)
6. [**Authors**](#Authors)
7. [**License**](#License)

## Overview <a name="Overview"></a>

If you want to use event sourcing together with java and sql database this library is for you.
In additional to its primary goal, which is event persistence, it also:
* helps to deal with DDD aggregates (supports aggregate recreation from events)
* supports hexagonal architecture approach (does not force your events and aggregates to extend library-specific classes)
* supports events versioning
* COMING SOON: supports optimistic locking
* COMING SOON: supports streams versioning
* TODO: supports pessimistic locking
* TODO: supports copy&replace approach

## How to add it to project <a name="HowToAddItToProject"></a>

### Add dependency

If you are using gradle add it to build.gradle:
```groovy
dependencies {
    implementation "com.hltech:sql-event-store-4j:version"
}
```

If you are using maven add it to pom.xml:
```xml
  <dependencies>
    <dependency>
      <groupId>com.hltech</groupId>
      <artifactId>sql-event-store-4j</artifactId>
      <version>version</version>
    </dependency>
  </dependencies>
```

### Migrate database

Use those [scripts](https://github.com/HLTech/SqlEventStore4J/tree/main/src/test/resources/db/migration) to create required tables in you database.

## How to use it <a name="HowToUseIt"></a>

### Assumptions

Let's assume that you have events like those in your code:

```java
class OrderPlaced implements Event {

    private UUID id;
    private UUID aggregateId;
    private String orderNumber;

    // No args and all args constructors and getters here
}

class OrderCancelled implements Event {

    private UUID id;
    private UUID aggregateId;
    private String reason;

    // No args and all args constructors and getters here

}
```

where `Event` is your custom interface that all events implements
```java
interface Event {

    UUID getId();
    UUID getAggregateId();

}
```

### Create event store

To create event store that will store any implementation of `Event` interface, you have to prepare few configuration parameters at first.
Let's go through required parameters for PostgresEventStore:

* `Function<Event, UUID> eventIdExtractor = Event::getId;`

    Event store needs to know how to extract event id from your events. All your events implement `Event` interface so we can use `getId()` method for that.
* `Function<Event, UUID> aggregateIdExtractor = Event::getAggregateId;`

    Event store needs to know how to extract aggregate id from your events. All your events implement `Event` interface so we can use `getAggregateId()` method for that.
* `EventTypeMapper<Event> eventTypeMapper = new SimpleEventTypeMapper<>();`

    EventTypeMapper is needed to map events classes to its names and back again. How to configure it:
    ```
    eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced", 1);
    eventTypeMapper.registerMapping(OrderCancelled.class, "OrderCancelled", 1);
    ```
    Third parameter here is event version. It is detailed described in [events versioning](#EventsVersioning).
    For now, it is important that the event name and version of the event in a pair are unique and point to a single class.

* `EventBodyMapper<Event> eventBodyMapper = new JacksonEventBodyMapper<>(objectMapper);`

    EventBodyMapper is needed to map events instances to its json representation (serialization) and back again (deserialization).
    JacksonEventBodyMapper is based on [jackson-databind](https://github.com/FasterXML/jackson-databind).
    Events serialization/deserialization is delegated to objectMapper passed as a constructor parameter.

* `DataSource dataSource`

    DataSource to be used to connect to the database.

Now you are ready to create event store using its constructor:

```java
EventStore<Event> eventStore =
    new PostgresEventStore(
        eventIdExtractor,
        aggregateIdExtractor,
        eventTypeMapper,
        eventBodyMapper,
        dataSource
    );
```

### Using event store

Event store is ready to use. Its API allows to save and find events in the stream.

```java
UUID aggregateId = UUID.randomUUID();
eventStore.save(new OrderPlaced(UUID.randomUUID(), aggregateId, "PizzaOrder3214"), "OrderStream");
eventStore.save(new OrderCancelled(UUID.randomUUID(), aggregateId, "I'm not hungry anymore"), "OrderStream");
List<Event> events = eventStore.findAll(aggregateId, "OrderStream");
```

You can stop here if it's all you need, but what about aggregates?

### Dealing with aggregates

Let's assume that you have `Order` aggregate in your code. Let's also assume that your events affect that aggregate:

```java
class Order {

    String status;

    Order apply(Event event) {
        if (OrderPlaced.class.equals(event.getClass())) {
            status = "Placed";
        } else if (OrderCancelled.class.equals(event.getClass())) {
            status = "Cancelled";
        }
        return this;
    }

}
```

This is where `AggregateRepository` comes in to help you save aggregate related events in event store and recreate aggregate from events:

```java
class OrderRepository extends AggregateRepository<Order, Event> {

    private static final Supplier<Order> INITIAL_AGGREGATE_STATE_SUPPLIER = Order::new;
    private static final BiFunction<Order, Event, Order> AGGREGATE_EVENT_APPLIER = Order::apply;
    private static final String STREAM_TYPE = "OrderStream";

    public OrderRepository(EventStore<Event> eventStore) {
        super(
                eventStore,
                INITIAL_AGGREGATE_STATE_SUPPLIER,
                AGGREGATE_EVENT_APPLIER,
                STREAM_TYPE
        );
    }

}
```

Let's now create an instance of `OrderRepository` passing previously created event store into it and use them to deal with `Order` aggregate:

```java
OrderRepository repository = new OrderRepository(eventStore);
UUID aggregateId = UUID.randomUUID();
repository.save(new OrderPlaced(UUID.randomUUID(), aggregateId, "PizzaOrder3214"));
repository.save(new OrderCancelled(UUID.randomUUID(), aggregateId, "I'm not hungry anymory"));
Optional<Order> order = repository.find(aggregateId);
```

## Events versioning <a name="EventsVersioning"></a>

Let’s assume that you have an actual version of OrderPlaced event:

```java
class OrderPlaced implements Event {

    private UUID id;
    private UUID aggregateId;
    private String orderNumber;

    // No args and all args constructors and getters here
}
```

but you also have deprecated version of the same event, because some time ago order number was not required:

```java
class OrderPlacedV1 implements OrderPlaced {

    private UUID id;
    private UUID aggregateId;
    private String orderNumber = 'undefined';

    // No args and all args constructors and getters here

}
```

proper configuration for such situation would be:

```java
eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced", 2);
eventTypeMapper.registerMapping(OrderPlacedV1.class, "OrderPlaced", 1);
```

If you will need to add a third version of OrderPlaced event in the future, you should reconfigure mapping for actual event version and add mapping for deprecated:

```java
eventTypeMapper.registerMapping(OrderPlaced.class, "OrderPlaced", 3);
eventTypeMapper.registerMapping(OrderPlacedV2.class, "OrderPlaced", 2);
eventTypeMapper.registerMapping(OrderPlacedV1.class, "OrderPlaced", 1);
```

## Databases <a name="Databases"></a>

Supported databases:
* PostgreSQL

## Authors <a name="Authors"></a>

* **Krzysztof Pieniążek** - *Development* - [pienikrz](https://github.com/pienikrz)
* **Michał Karolik** - *Development* - [michalkarolik](https://github.com/michalkarolik)
* **Zbigniew Rydlewski** - *Development* - [rydlu](https://github.com/rydlu)

## License <a name="License"></a>

[MIT licensed](./LICENSE).
