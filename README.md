# SQL Event Store For Java

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Table of Contents
1. [**Overview**](#Overview)
2. [**How to add it to project**](#HowToAddItToProject)
3. [**How to use it**](#HowToUseIt)
4. [**Optimistic locking**](#OptimisticLocking)
5. [**Events versioning strategies**](#EventsVersioningStrategies)
6. [**Databases**](#Databases)
7. [**Authors**](#Authors)
8. [**License**](#License)

## Overview <a name="Overview"></a>

If you want to use event sourcing together with java and sql database this library is for you.
In addition to its primary goal, which is event persistence, it also:
* helps to deal with DDD aggregates (supports aggregate recreation from events)
* supports hexagonal architecture approach (does not force your events and aggregates to extend library-specific classes)
* supports multiple [strategies of events versioning](#EventsVersioningStrategies)
* supports [optimistic locking](#OptimisticLocking)
* COMING SOON: supports pessimistic locking
* COMING SOON: supports copy&replace approach

## How to add it to project <a name="HowToAddItToProject"></a>

### Add dependency

If you are using gradle add this to build.gradle:
```groovy
dependencies {
    implementation "com.hltech:sql-event-store-4j:version"
    implementation "com.fasterxml.jackson.core:jackson-databind:2.12.1"
}
```

If you are using maven add this to pom.xml:
```xml
  <dependencies>
    <dependency>
      <groupId>com.hltech</groupId>
      <artifactId>sql-event-store-4j</artifactId>
      <version>version</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.12.1</version>
    </dependency>
  </dependencies>
```

### Migrate database

Use those [scripts](https://github.com/HLTech/SqlEventStore4J/tree/main/src/test/resources/db/migration) to create required tables in you database.

## How to use it <a name="HowToUseIt"></a>

Below, very simple example, is here for quick overview. For more complex examples please visit [SqlEventStore4JExamples](https://github.com/HLTech/SqlEventStore4JExamples)

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
* `EventVersioningStrategy<Event> eventVersioningStrategy = new MappingBasedVersioning<>();`

    EventVersioningStrategy is here to determinate how to deal with events schemas changes.
    It is detailed described in [events versioning strategies](#EventsVersioningStrategies).
    For MappingBasedVersioning strategy, that is using it this example, the following configuration is required:
    ```
    eventVersioningStrategy.registerEvent(OrderPlaced.class, "OrderPlaced");
    eventVersioningStrategy.registerEvent(OrderCancelled.class, "OrderCancelled");
    ```

* `DataSource dataSource`

    DataSource to be used to connect to the database.

Now you are ready to create event store using its constructor:

```java
EventStore<Event> eventStore =
    new PostgresEventStore(
        eventIdExtractor,
        aggregateIdExtractor,
        eventVersioningStrategy,
        dataSource
    );
```

### Using event store

Event store is ready to use. Its API allows to save and find events.

```java
UUID aggregateId = UUID.randomUUID();
String aggregateName = 'Order';
eventStore.save(new OrderPlaced(UUID.randomUUID(), aggregateId, "PizzaOrder3214"), aggregateName);
eventStore.save(new OrderCancelled(UUID.randomUUID(), aggregateId, "I'm not hungry anymore"), aggregateName);
List<Event> events = eventStore.findAll(aggregateId, aggregateName);
```

You can stop here if it's all you need, but what about aggregates?

### Dealing with aggregates <a name="DealingWithAggregates"></a>

Let's assume that you have `Order` aggregate in your code. Let's also assume that your events affect that aggregate:

```java
class Order {

    String status;

    Order apply(Event event) {
        if (event instanceof OrderPlaced) {
            status = "Placed";
        } else if (event instanceof OrderCancelled) {
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
    private static final String AGGREGATE_NAME = "Order";

    public OrderRepository(EventStore<Event> eventStore) {
        super(
                eventStore,
                AGGREGATE_NAME,
                INITIAL_AGGREGATE_STATE_SUPPLIER,
                AGGREGATE_EVENT_APPLIER
        );
    }

}
```

Let's now create an instance of `OrderRepository` passing previously created event store into it and use them to deal with `Order` aggregate:

```java
OrderRepository repository = new OrderRepository(eventStore);
UUID aggregateId = UUID.randomUUID();
repository.save(new OrderPlaced(UUID.randomUUID(), aggregateId, "PizzaOrder3214"));
repository.save(new OrderCancelled(UUID.randomUUID(), aggregateId, "I'm not hungry anymore"));
Optional<Order> order = repository.find(aggregateId);
```

## Optimistic locking <a name="OptimisticLocking"></a>

Let's assume that you have `Order` aggregate in your code, with the rule that if order has been sent, it can't be cancelled anymore.
To ensure that rule we can use optimistic locking and to do that we have to add version field to `Order` aggregate.

```java
class Order {

    UUID id;
    String status;
    Integer version;

    static OrderPlaced place() {
        return new OrderPlaced(
                generateEventId(),
                generateAggregateId()
        );
    }

    OrderCancelled cancel() {
        if ("Sent".equals(status)) {
            throw new IllegalStateException("Once an order has been sent, it cannot be cancel");
        }
        return new OrderCancelled(generateEventId(), id);
    }

    OrderSent send() {
        if ("Cancelled".equals(status)) {
            throw new IllegalStateException("Once an order has been cancelled, it cannot be send");
        }
        return new OrderSent(generateEventId(), id);
    }


    Order applyEvent(Event event) {
        if (event instanceof OrderPlaced) {
            id = event.getAggregateId();
            status = "Placed";
        } else if (event instanceof OrderCancelled) {
            status = "Cancelled";
        } else if (event instanceof OrderSent) {
            status = "Sent";
        }
        return this;
    }

    Order applyVersion(Integer version) {
        this.version = version;
        return this;
    }

}
```

Now we have to create a repository for `Order` aggregate

```java
class OrderRepository extends AggregateRepository<Order, Event> {

    private static final Supplier<Order> INITIAL_AGGREGATE_STATE_SUPPLIER = Order::new;
    private static final BiFunction<Order, Event, Order> AGGREGATE_EVENT_APPLIER = Order::applyEvent;
    private static final BiFunction<Order, Integer, Order> AGGREGATE_VERSION_APPLIER = Order::applyVersion;
    private static final String AGGREGATE_NAME = "Order";

    public OrderRepository(EventStore<Event> eventStore) {
        super(
                eventStore,
                AGGREGATE_NAME,
                INITIAL_AGGREGATE_STATE_SUPPLIER,
                AGGREGATE_EVENT_APPLIER,
                AGGREGATE_VERSION_APPLIER
        );
    }

}
```

Please note, that in addition to repository created in [dealing with aggregates](#DealingWithAggregates) chapter, there is additional parameter AGGREGATE_VERSION_APPLIER.
Repository will use that to set current version of aggregate. After that we can pass that version when saving events in repository, to ensure that you deal with latest version of aggregate.
Let’s now use OrderRepository to deal with optimistic locking.

```java
class OrderService {

    private final OrderRepository repository;

    UUID placeOrder() {
        OrderPlaced event = Order.place();
        repository.save(event);
        return event.getAggregateId();
    }

    void cancelOrder(UUID orderId) {
        Order order = repository.get(orderId);
        OrderCancelled event = order.cancel();
        try {
            repository.save(event, order.getVersion());
        } catch (OptimisticLockingException ex) {
            // Optimistic locking handling
        }
    }

    void sendOrder(UUID orderId) {
        Order order = repository.get(orderId);
        OrderSent event = order.send();
        try {
            repository.save(event, order.getVersion());
        } catch (OptimisticLockingException ex) {
            // Optimistic locking handling
        }
    }

}
```

## Events versioning strategies <a name="EventsVersioningStrategies"></a>

### Multiple versions <a name="MultipleVersionsBasedVersioning"></a>

Let’s assume that you have an actual version of OrderPlaced event:

```java
class OrderPlacedV2 implements Event {

    private UUID id;
    private UUID aggregateId;
    private String orderNumber;

    // No args and all args constructors and getters here
}
```

but you also have deprecated version of the same event, because some time ago order number was not required:

```java
class OrderPlacedV1 implements Event {

    private UUID id;
    private UUID aggregateId;

    // No args and all args constructors and getters here

}
```

proper configuration for such situation would be:

```java
MultipleVersionsBasedVersioning<Event> eventVersioningStrategy = new MultipleVersionsBasedVersioning<>();
eventVersioningStrategy.registerEvent(OrderPlacedV2.class, "OrderPlaced", 2);
eventVersioningStrategy.registerEvent(OrderPlacedV1.class, "OrderPlaced", 1);
```

In this strategy multiple versions of the event have to be supported in the application code.
The application must contain knowledge of all deprecated event versions in order to support them.
To avoid that consider using [upcasting based versioning](#UpcastingBasedVersioning)

Please note, that using this strategy is recommended only if you have one instance of your application running at the same time.
Using this strategy in multi instance case leads to the situation, where all instances must be updated
to understand latest event version, before any instance produces it. For multi instance case consider using [mapping based versioning](#MappingBasedVersioning)

### Upcasting <a name="UpcastingBasedVersioning"></a>

Not yet implemented

### Mapping <a name="MappingBasedVersioning"></a>

Let’s assume that you have an OrderPlaced event:

```java
class OrderPlaced implements Event {

    private UUID id;
    private UUID aggregateId;
    private String orderNumber;

    // No args and all args constructors and getters here

}
```

Let's say that you want to change that event, because now you want to set priority for orders.

```java
class OrderPlaced implements Event {

    private static final String DEFAULT_PRIORITY = "low";

    private UUID id;
    private UUID aggregateId;
    private String orderNumber;
    private String priority;

    // No args and all args constructors and getters here

    String getPriority() {
        return priority != null ? priority : DEFAULT_PRIORITY;
    }

}
```

proper configuration for such situation would be:

```java
MappingBasedVersioning<Event> eventVersioningStrategy = new MappingBasedVersioning<>();
eventVersioningStrategy.registerEvent(OrderPlaced.class, "OrderPlaced");
```

In this strategy every event exists only in latest version, so that the application code has to support only one version of the event.
The mapping strategy is based on three simple principles:
- When attribute exists on both json and class then set the value from json
- When attribute exists on json but not on class then do nothing
- When attribute exists on class but not in json then set default value

This strategy is recommended when you have a multiple instance of your application running at the same time, because it supports backward and forward compatibility.
Be aware that it also has one important and annoying drawback. You are no longer allowed to rename event attribute.
What you can do when attribute name is no longer valid, is:
- add new attribute with valid name and support both attributes
- use copy and replace approach to fix no longer valid attribute name
- use [wrapping based versioning](#WrappingBasedVersioning) instead

### Wrapping <a name="WrappingBasedVersioning"></a>

Not yet implemented

### Mixed <a name="MixedStrategyBasedVersioning"></a>

Not yet implemented

## Databases <a name="Databases"></a>

Supported databases:
* PostgreSQL

## Authors <a name="Authors"></a>

* **Krzysztof Pieniążek** - *Development* - [pienikrz](https://github.com/pienikrz)
* **Michał Karolik** - *Development* - [michalkarolik](https://github.com/michalkarolik)
* **Zbigniew Rydlewski** - *Development* - [rydlu](https://github.com/rydlu)

## License <a name="License"></a>

[MIT licensed](./LICENSE).
