package it.unisa.c02.rently.rently_application;

import it.unisa.c02.rently.rently_application.commons.services.regexService.RegexTester;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
@State(Scope.Thread)
public class RegexTesterBenchmark {

    // ----------------- Parametri del benchmark -----------------

    /** Numero di coppie (stringa -> regex) nella mappa. */
    //@Param({"1", "10", "100", "1000"})
    @Param({"100", "1000"})
    public int mapSize;

    /** Lunghezza delle stringhe generate. */
    //@Param({"8", "32", "128"})
    @Param({"128"})
    public int strLen;

    /** Tipo di regex: controlla complessità dell'espressione regolare. */
    //@Param({"literal", "charclass", "prefix"})
    @Param({"charclass"})
    public String patternType;

    /**
     * Percentuale di coppie che devono MATCHARE.
     * 1.0 = tutte matchano, 0.5 = metà, 0.0 = nessuna.
     */
    //@Param({"1.0", "0.5", "0.0"})
    @Param({"1.0", "0.5"})
    public double matchRatio;

    // ----------------- Dati condivisi tra i metodi -----------------

    private final RegexTester underTest = new RegexTester();

    /** Mappa usata dal metodo attuale (String -> String regex). */
    private HashMap<String, String> map;

    /** Variante precompilata: String -> Pattern */
    private LinkedHashMap<String, Pattern> compiled;

    /** Variante con pre-allocazione dei Matcher per ridurre allocazioni (solo per misurazione alternativa). */
    private List<Matcher> prebuiltMatchers; // opzionale per patternType costanti

    @Setup(Level.Trial)
    public void setup() {
        final Random rnd = new Random(12345);

        map = new HashMap<>(mapSize * 2);
        compiled = new LinkedHashMap<>(mapSize * 2);
        prebuiltMatchers = new ArrayList<>(mapSize);

        final int matchesTarget = (int) Math.round(mapSize * matchRatio);
        int matchesSoFar = 0;

        for (int i = 0; i < mapSize; i++) {
            String s = randomLowercase(rnd, strLen);

            String regex = switch (patternType) {
                case "literal" -> s;                     // il pattern è letterale uguale alla stringa
                case "charclass" -> "[a-z]{" + strLen + "}"; // matcha esattamente strLen minuscole
                case "prefix" -> s.substring(0, Math.max(1, strLen / 4)) + ".*"; // prefisso + wildcard
                default -> s;
            };

            boolean shouldMatch;
            if (matchesSoFar < matchesTarget) {
                // cerchiamo di creare una coppia che matcha
                shouldMatch = true;
            } else {
                shouldMatch = false;
            }

            if (!shouldMatch) {
                // forza non-match: altera la stringa o il pattern
                if (patternType.equals("literal")) {
                    s = mutateString(s);
                } else if (patternType.equals("charclass")) {
                    // introduce un carattere non [a-z]
                    s = s.substring(0, s.length() - 1) + "_";
                } else if (patternType.equals("prefix")) {
                    // cambia il primo carattere del prefisso
                    char first = s.charAt(0);
                    char different = (first == 'a') ? 'z' : 'a';
                    s = different + s.substring(1);
                }
            } else {
                matchesSoFar++;
            }

            map.put(s, regex);

            // Precompilazione per le varianti ottimizzate
            Pattern p = Pattern.compile(regex);
            compiled.put(s, p);

            // Pre-costruzione di Matcher (non usato in tutti i test, ma utile in una variante)
            prebuiltMatchers.add(p.matcher(s));
        }
    }

    // ----------------- Metodi di benchmark -----------------

    /** Baseline: la tua implementazione attuale (stream + String.matches) */
    @Benchmark
    public void baseline_stream_matches(Blackhole bh) {
        boolean out = underTest.toTest(map);
        bh.consume(out);
    }

    /** Variante equivalente senza stream/lambda: for loop + String.matches. */
    @Benchmark
    public void forloop_matches_compileEveryCall(Blackhole bh) {
        boolean all = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            String s = e.getKey();
            if (s == null) continue;
            if (!s.matches(e.getValue())) {
                all = false;
                break;
            }
        }
        bh.consume(all);
    }

    /** Variante con precompilazione Pattern: for loop + Pattern.matcher(s).matches(). */
    @Benchmark
    public void forloop_precompiledPattern(Blackhole bh) {
        boolean all = true;
        for (Map.Entry<String, Pattern> e : compiled.entrySet()) {
            String s = e.getKey();
            if (s == null) continue;
            if (!e.getValue().matcher(s).matches()) {
                all = false;
                break;
            }
        }
        bh.consume(all);
    }

    /** Variante con stream ma usando Pattern precompilati. */
    @Benchmark
    public void stream_precompiledPattern(Blackhole bh) {
        boolean out = compiled.entrySet().stream().allMatch(e -> {
            String s = e.getKey();
            if (s == null) return true;
            return e.getValue().matcher(s).matches();
        });
        bh.consume(out);
    }

    /** Variante che riusa Matcher pre-costruiti (quando possibile). */
    @Benchmark
    public void forloop_reuseMatcher(Blackhole bh) {
        boolean all = true;
        int i = 0;
        for (Map.Entry<String, Pattern> e : compiled.entrySet()) {
            String s = e.getKey();
            if (s == null) continue;
            Matcher m = prebuiltMatchers.get(i++);
            // il Matcher è costruito sul (pattern, stringa) corretti in setup()
            if (!m.matches()) {
                all = false;
                break;
            }
        }
        bh.consume(all);
    }

    // ----------------- Utils -----------------

    private static String randomLowercase(Random rnd, int len) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = (char) ('a' + rnd.nextInt(26));
        }
        return new String(buf);
    }

    private static String mutateString(String s) {
        if (s.isEmpty()) return s;
        char first = s.charAt(0);
        char different = (first == 'a') ? 'z' : 'a';
        return different + s.substring(1);
    }
}
