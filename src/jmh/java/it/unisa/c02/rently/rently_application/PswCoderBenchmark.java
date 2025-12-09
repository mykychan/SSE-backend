
package it.unisa.c02.rently.rently_application;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
@State(Scope.Thread) // stato per thread: sicuro per riuso del MessageDigest
public class PswCoderBenchmark {

    @Param({"8", "16", "32", "64", "128"})
    public int length;

    private String pwd;
    private ThreadLocal<MessageDigest> sha256TL;

    @Setup(Level.Trial)
    public void init() throws NoSuchAlgorithmException {
        // input deterministico di lunghezza 'length'
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) ('a' + (i % 26));
        }
        pwd = new String(chars);

        // MessageDigest per-thread
        sha256TL = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /**
     * APPROCCIO ATTUALE: crea il digest a ogni chiamata e usa hex manuale
     */
    @Benchmark
    public void perCall_getInstance_and_manualHex(Blackhole bh) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encoded = digest.digest(pwd.getBytes(StandardCharsets.UTF_8));
        String out = manualHex(encoded);
        bh.consume(out);
    }

    /**
     * RIUSO PER THREAD: evita getInstance ad ogni chiamata
     */
    @Benchmark
    public void threadLocal_digest_and_manualHex(Blackhole bh) {
        MessageDigest digest = sha256TL.get();
        digest.reset();
        byte[] encoded = digest.digest(pwd.getBytes(StandardCharsets.UTF_8));
        String out = manualHex(encoded);
        bh.consume(out);
    }

    /**
     * Conversione HEX moderna (Java 17+)
     */
    @Benchmark
    public void threadLocal_digest_and_hexFormat(Blackhole bh) {
        MessageDigest digest = sha256TL.get();
        digest.reset();
        byte[] encoded = digest.digest(pwd.getBytes(StandardCharsets.UTF_8));
        String out = HexFormat.of().formatHex(encoded);
        bh.consume(out);
    }

    // ---- helper identico alla tua implementazione ----
    private static String manualHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}