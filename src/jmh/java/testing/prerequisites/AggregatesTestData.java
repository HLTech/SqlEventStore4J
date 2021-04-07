package testing.prerequisites;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@State(Scope.Benchmark)
public class AggregatesTestData {

    private final List<UUID> aggregatesIds;
    private final Random random = new Random();

    public AggregatesTestData() {
        this.aggregatesIds = IntStream.range(0, 100)
                .mapToObj(num -> UUID.randomUUID())
                .collect(Collectors.toList());
    }

    public UUID getRandomAggregateId() {
        return aggregatesIds.get(random.nextInt(100));
    }
}
