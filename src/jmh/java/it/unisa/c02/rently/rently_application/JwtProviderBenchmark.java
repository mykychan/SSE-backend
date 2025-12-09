
package it.unisa.c02.rently.rently_application;

import it.unisa.c02.rently.rently_application.security.JwtProvider;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmark per misurare:
 *  - createJwt(subject, claims): costo di costruzione e firma del token
 *  - verifyJwt(jwt): costo di verifica/signature check
 *
 * Nota: non dipende da Spring. Inizializziamo manualmente i campi statici di JwtProvider.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS) // JWT ha costi nell'ordine dei µs
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class JwtProviderBenchmark {

    // ----------------- Parametri -----------------

    /** Lunghezza del subject del token */
    @Param({"8", "32", "128"})
    public int subjectLen;

    /** Numero di claim nel payload */
    @Param({"0", "5", "20", "50"})
    public int claimCount;

    /** Dimensione media dei valori dei claim (caratteri) */
    @Param({"8", "64"})
    public int claimValueLen;

    /** Se verificare sempre token firmati con lo stesso segreto (true) o rigenerare con segreti diversi (false) */
    @Param({"true"})
    public boolean fixedSecret;

    // ----------------- Dati -----------------

    private String subject;
    private Map<String, Object> claims;

    private String secret;
    private String headerParam;
    private String prefix;

    // token pre-generati per il benchmark di verify
    private List<String> tokensForVerify;

    @Setup(Level.Trial)
    public void setupTrial() {
        // Inizializza i campi statici usati da JwtProvider, simulando Spring Environment
        secret = fixedSecret ? "super-secret-1234567890" : UUID.randomUUID().toString();
        headerParam = "Authorization";
        prefix = "Bearer ";

        JwtProvider.secret = secret;
        JwtProvider.headerParam = headerParam;
        JwtProvider.prefix = prefix;

        // Costruisci subject deterministico
        subject = randomAlpha(subjectLen, new Random(42));

        // Costruisci claims deterministici
        claims = new LinkedHashMap<>(claimCount * 2);
        Random rnd = new Random(123);
        for (int i = 0; i < claimCount; i++) {
            String key = "k" + i;
            String value = randomAlpha(claimValueLen, rnd);
            claims.put(key, value);
        }

        // Prepara una lista di token da verificare
        tokensForVerify = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> c = (i % 2 == 0) ? claims : slightVariantClaims(claims, i, claimValueLen);
            // Se fixedSecret=false, cambiamo anche il secret per stressare il verify (non consigliato normalmente)
            if (!fixedSecret) {
                JwtProvider.secret = "secret-" + i;
            }
            String t = JwtProvider.createJwt(subject, c);
            tokensForVerify.add(t);
        }

        // Ripristina il secret principale
        JwtProvider.secret = secret;
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        // Se fixedSecret=false, cambia il secret a ogni iterazione per simulare variazione
        if (!fixedSecret) {
            JwtProvider.secret = UUID.randomUUID().toString();
        }
    }

    // ----------------- Benchmark: Create -----------------

    /** Crea e firma un JWT a partire da subject e mappa claims. */
    @Benchmark
    public void createJwt_buildAndSign(Blackhole bh) {
        String token = JwtProvider.createJwt(subject, claims);
        bh.consume(token);
    }

    /** Crea e firma un JWT con claims leggermente variati (stressa la creazione) */
    @Benchmark
    public void createJwt_withVariantClaims(Blackhole bh) {
        Map<String, Object> variant = slightVariantClaims(claims, 1, claimValueLen);
        String token = JwtProvider.createJwt(subject, variant);
        bh.consume(token);
    }

    // ----------------- Benchmark: Verify -----------------

    /** Verifica un token pre-generato (lista ciclica) */
    @Benchmark
    public void verifyJwt_prebuiltTokens(Blackhole bh) {
        String token = tokensForVerify.get((int) (System.nanoTime() % tokensForVerify.size()));
        DecodedJWT jwt = JwtProvider.verifyJwt(token);
        bh.consume(jwt);
    }

    /** Verifica un token appena creato (include costo di creazione + verifica) */
    @Benchmark
    public void createThenVerifyJwt(Blackhole bh) {
        String token = JwtProvider.createJwt(subject, claims);
        DecodedJWT jwt = JwtProvider.verifyJwt(token);
        bh.consume(jwt);
    }

    // ----------------- Utils -----------------

    private static String randomAlpha(int len, Random rnd) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = (char) ('a' + rnd.nextInt(26));
        }
        return new String(buf);
    }

    private static Map<String, Object> slightVariantClaims(Map<String, Object> base, int seed, int valLen) {
        Map<String, Object> out = new LinkedHashMap<>(base.size() * 2);
        out.putAll(base);
        // Cambia/aggiunge un singolo claim per differenziare leggermente
        out.put("v" + seed, "x".repeat(Math.max(1, valLen)));
        return out;
    }
}
