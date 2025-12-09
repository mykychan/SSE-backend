
package it.unisa.c02.rently.rently_application;

import it.unisa.c02.rently.rently_application.commons.services.responseService.ResponseServiceImpl;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmark per misurare i metodi di ResponseServiceImpl:
 *  - Ok(Object)
 *  - Ok()
 *  - InternalError(Object)
 *  - InternalError()
 *
 * Include payload parametrizzato per dimensione/struttura.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class ResponseServiceImplBenchmark {

    // ----------------- Parametri -----------------

    /** Tipo di payload da serializzare. */
    @Param({"simple", "nested", "list"})
    public String payloadType;

    /** Dimensione del payload:
     *  - simple/nested: moltiplicatore di campi/nodi
     *  - list: numero di elementi nella lista
     */
    @Param({"1", "10", "100"})
    public int size;

    // ----------------- Stato -----------------

    private ResponseServiceImpl service;
    private Object payload;

    @Setup(Level.Trial)
    public void setup() {
        service = new ResponseServiceImpl();

        // Costruzione payload deterministica
        payload = buildPayload(payloadType, size, new Random(123));
    }

    // ----------------- Benchmark -----------------

    /** Ok(Object data): serializzazione + costruzione ResponseEntity con 201 (CREATED). */
    @Benchmark
    public void ok_with_data(Blackhole bh) {
        ResponseEntity<String> resp = service.Ok(payload);
        bh.consume(resp);
        // Volendo: bh.consume(resp.getStatusCode()); bh.consume(resp.getBody());
    }

    /** Ok(): costruzione ResponseEntity vuoto con 201. */
    @Benchmark
    public void ok_empty(Blackhole bh) {
        ResponseEntity<String> resp = service.Ok();
        bh.consume(resp);
    }

    /** InternalError(Object data): serializzazione + 500. */
    @Benchmark
    public void internal_with_data(Blackhole bh) {
        ResponseEntity<String> resp = service.InternalError(payload);
        bh.consume(resp);
    }

    /** InternalError(): costruzione vuota con 500. */
    @Benchmark
    public void internal_empty(Blackhole bh) {
        ResponseEntity<String> resp = service.InternalError();
        bh.consume(resp);
    }

    // ----------------- Costruzione payload -----------------

    private static Object buildPayload(String type, int size, Random rnd) {
        switch (type) {
            case "simple":
                return makeSimpleMap(size, rnd);
            case "nested":
                return makeNestedObject(size, rnd);
            case "list":
                return makeList(size, rnd);
            default:
                return makeSimpleMap(size, rnd);
        }
    }

    private static Map<String, Object> makeSimpleMap(int mult, Random rnd) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < Math.max(1, mult); i++) {
            m.put("k" + i, "v" + i);
        }
        m.put("id", UUID.randomUUID().toString());
        m.put("name", "Luca " + rnd.nextInt(1000));
        m.put("active", rnd.nextBoolean());
        m.put("age", 20 + rnd.nextInt(30));
        return m;
    }

    private static Map<String, Object> makeNestedObject(int mult, Random rnd) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (int i = 0; i < mult; i++) {
            Map<String, Object> child = new LinkedHashMap<>();
            child.put("val", i);
            child.put("str", "s" + i);
            child.put("meta", makeSimpleMap(Math.max(1, mult / 2), rnd));
            child.put("list", makeList(Math.max(1, mult / 2), rnd));
            root.put("child" + i, child);
        }
        return root;
    }

    private static List<Map<String, Object>> makeList(int n, Random rnd) {
        List<Map<String, Object>> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(makeSimpleMap(1, rnd));
        }
        return list;
    }
}
