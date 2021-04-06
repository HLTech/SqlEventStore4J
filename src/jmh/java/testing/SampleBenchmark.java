package testing;

import com.hltech.store.DummyEvent;
import org.openjdk.jmh.annotations.Benchmark;
import testing.prerequisites.AggregatesTestData;
import testing.prerequisites.PostgresEventStorePerfTestsPreparation;

import java.util.UUID;

public class SampleBenchmark {

    private static final UUID AGGREGATE_ID = UUID.randomUUID();
    private static final String AGGREGATE_NAME = UUID.randomUUID().toString();

    @Benchmark
    public void saveEventsForSameAggregateIdAndAggregateName(PostgresEventStorePerfTestsPreparation postgres) {
        postgres.getEventStore().save(new DummyEvent(AGGREGATE_ID), AGGREGATE_NAME);
    }

    @Benchmark
    public void saveEventsByManyThreads(PostgresEventStorePerfTestsPreparation postgres, AggregatesTestData aggregatesTestData) {
        UUID randomAggregateId = aggregatesTestData.getRandomAggregateId();
        postgres.getEventStore().save(new DummyEvent(randomAggregateId), randomAggregateId.toString());
    }
}
