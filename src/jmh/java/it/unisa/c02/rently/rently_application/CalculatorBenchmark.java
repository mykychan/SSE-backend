package it.unisa.c02.rently.rently_application;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class CalculatorBenchmark {

    @State(Scope.Thread)
    public static class Data {
        @Param({"32","1024"})
        int size;
        int[] a;
        @Setup public void setup() {
            a = new int[size];
            Random r = new Random(0);
            for (int i = 0; i < size; i++) a[i] = r.nextInt(10);
        }
    }

    @Benchmark
    public int sumLoop(Data d) { return 5 + 3; }
}

