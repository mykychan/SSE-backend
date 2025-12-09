
package it.unisa.c02.rently.rently_application;

import it.unisa.c02.rently.rently_application.commons.jsonHelper.JsonHelper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmark per misurare la serializzazione JSON:
 *  - JsonHelper baseline: new ObjectMapper() + writerWithDefaultPrettyPrinter
 *  - Jackson con riuso di ObjectMapper (compact e pretty)
 *  - Gson compact e pretty
 *
 * Parametri: tipo payload e dimensione.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS) // ordini di grandezza tipici
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class JsonHelperBenchmark {

    // ----------------- Parametri -----------------

    /** Tipologia del payload da serializzare. */
    @Param({"simple", "nested", "list"})
    public String payloadType;

    /** Dimensione del payload:
     *  - simple/nested: moltiplica qualche lista/field
     *  - list: numero di elementi nella lista
     */
    @Param({"1", "10", "100"})
    public int size;

    // ----------------- Stato -----------------

    private final JsonHelper helper = new JsonHelper();

    private Object payload;

    // Jackson riusato
    private ObjectMapper jackson;
    private ObjectWriter jacksonCompact;
    private ObjectWriter jacksonPretty;

    // Gson riusato
    private Gson gsonCompact;
    private Gson gsonPretty;

    @Setup(Level.Trial)
    public void setup() {
        // Costruzione payload deterministica
        Random rnd = new Random(123);
        payload = buildPayload(payloadType, size, rnd);

        // Jackson: riuso di ObjectMapper e writer
        jackson = new ObjectMapper();
        jacksonCompact = jackson.writer();
        jacksonPretty = jackson.writerWithDefaultPrettyPrinter();

        // Gson: riuso di istanze
        gsonCompact = new Gson();
        gsonPretty = new GsonBuilder().setPrettyPrinting().create();
    }

    // ----------------- Benchmark -----------------

    /** Baseline: tua implementazione attuale (nuova ObjectMapper a ogni chiamata, pretty). */
    @Benchmark
    public void baseline_jsonHelper_pretty(Blackhole bh) {
        String json = helper.getJsonFromObject(payload);
        bh.consume(json);
    }

    /** Jackson compatto, riuso di ObjectMapper. */
    @Benchmark
    public void jackson_compact_reuse(Blackhole bh) {
        try {
            String json = jacksonCompact.writeValueAsString(payload);
            bh.consume(json);
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /** Jackson pretty, riuso di ObjectMapper. */
    @Benchmark
    public void jackson_pretty_reuse(Blackhole bh) {
        try {
            String json = jacksonPretty.writeValueAsString(payload);
            bh.consume(json);
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /** Antipattern: nuova ObjectMapper a ogni chiamata (pretty). Serve come confronto. */
    @Benchmark
    public void jackson_newMapper_eachCall_pretty(Blackhole bh) {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(payload);
            bh.consume(json);
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /** Gson compatto. */
    @Benchmark
    public void gson_compact_reuse(Blackhole bh) {
        String json = gsonCompact.toJson(payload);
        bh.consume(json);
    }

    /** Gson pretty. */
    @Benchmark
    public void gson_pretty_reuse(Blackhole bh) {
        String json = gsonPretty.toJson(payload);
        bh.consume(json);
    }

    // ----------------- Costruzione payload -----------------

    private static Object buildPayload(String type, int size, Random rnd) {
        switch (type) {
            case "simple":
                return makeSimpleUser(size, rnd);
            case "nested":
                return makeNestedObject(size, rnd);
            case "list":
                return makeUserList(size, rnd);
            default:
                return makeSimpleUser(size, rnd);
        }
    }

    // Un POJO "utente" semplice
    public static class Address {
        public String street;
        public String city;
        public String zip;
        public Address(String street, String city, String zip) {
            this.street = street; this.city = city; this.zip = zip;
        }
    }

    public static class SimpleUser {
        public String id;
        public String name;
        public String email;
        public int age;
        public boolean active;
        public List<String> roles;
        public Address address;
        public Map<String, Object> metadata;
        public SimpleUser(String id, String name, String email, int age, boolean active,
                          List<String> roles, Address address, Map<String, Object> metadata) {
            this.id = id; this.name = name; this.email = email; this.age = age;
            this.active = active; this.roles = roles; this.address = address;
            this.metadata = metadata;
        }
    }

    private static SimpleUser makeSimpleUser(int mult, Random rnd) {
        List<String> roles = new ArrayList<>();
        for (int i = 0; i < Math.max(1, mult); i++) roles.add("ROLE_" + i);

        Map<String, Object> meta = new LinkedHashMap<>();
        for (int i = 0; i < Math.max(1, mult); i++) meta.put("k" + i, "v" + i);

        Address addr = new Address("Via " + rnd.nextInt(100), "Napoli", String.format("%05d", rnd.nextInt(100000)));
        return new SimpleUser(
                UUID.randomUUID().toString(),
                "Luca " + rnd.nextInt(1000),
                "luca" + rnd.nextInt(1000) + "@example.com",
                20 + rnd.nextInt(30),
                rnd.nextBoolean(),
                roles,
                addr,
                meta
        );
    }

    private static Map<String, Object> makeNestedObject(int mult, Random rnd) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (int i = 0; i < mult; i++) {
            Map<String, Object> child = new LinkedHashMap<>();
            child.put("val", i);
            child.put("str", "s" + i);
            child.put("user", makeSimpleUser(Math.max(1, mult / 2), rnd));
            child.put("list", makeUserList(Math.max(1, mult / 2), rnd));
            root.put("child" + i, child);
        }
        return root;
    }

    private static List<SimpleUser> makeUserList(int n, Random rnd) {
        List<SimpleUser> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(makeSimpleUser(1, rnd));
        return list;
    }
}
