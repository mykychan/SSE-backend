
package it.unisa.c02.rently.rently_application;

import it.unisa.c02.rently.rently_application.security.JwtProvider;
import it.unisa.c02.rently.rently_application.data.dto.UtenteDTO;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Misura le parti "core" del filtro AuthorizationFilter:
 *  - verifyJwt: verifica della firma e decodifica del token
 *  - JSON parse: parsing della claim "user" come ObjectNode
 *  - convertValue: conversione ObjectNode -> UtenteDTO
 *
 * Non dipende dal DAO o da HttpServletRequest/FilterChain.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class AuthorizationFilterBenchmark {

    @Param({"16","64"})
    public int emailLen;

    @Param({"0","10","50"})
    public int rolesCount;

    @Param({"true"})
    public boolean fixedSecret;

    private ObjectMapper mapper;
    private String token;

    @Setup(Level.Trial)
    public void setup() {
        mapper = new ObjectMapper();

        // Inizializza JwtProvider "come fa Spring" senza dipendere da Environment
        JwtProvider.secret = fixedSecret ? "bench-secret-123" : UUID.randomUUID().toString();
        JwtProvider.headerParam = "Authorization";
        JwtProvider.prefix = "Bearer ";

        // Costruisci JSON utente come la claim "user"
        final Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", 123L);
        userMap.put("email", randomEmail(emailLen));
        userMap.put("name", "Luca");
        userMap.put("roles", makeRoles(rolesCount));

        final String userJson;
        try {
            userJson = mapper.writeValueAsString(userMap);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }

        // Crea un token con claim "user" = JSON string
        final Map<String, Object> claims = new HashMap<>();
        claims.put("user", userJson);
        token = JwtProvider.createJwt("subject", claims);
    }

    // Verifica solo la firma + decodifica
    @Benchmark
    public void verify_only(final Blackhole bh) {
        final DecodedJWT decoded = JwtProvider.verifyJwt(token);
        bh.consume(decoded);
    }

    // Verifica + parsing in ObjectNode
    @Benchmark
    public void verify_and_parse_user_claim(final Blackhole bh) {
        final DecodedJWT decoded = JwtProvider.verifyJwt(token);
        try {
            final ObjectNode userNode = new ObjectMapper()
                    .readValue(decoded.getClaim("user").asString(), ObjectNode.class);
            bh.consume(userNode);
        } catch (final Exception e) {
            bh.consume(e);
        }
    }

    // Verifica + parsing + conversione a UtenteDTO
    @Benchmark
    public void verify_parse_convert_to_dto(final Blackhole bh) {
        final DecodedJWT decoded = JwtProvider.verifyJwt(token);
        try {
            final ObjectNode userNode = mapper.readValue(decoded.getClaim("user").asString(), ObjectNode.class);
            final UtenteDTO user = mapper.convertValue(userNode, UtenteDTO.class);
            bh.consume(user);
        } catch (final Exception e) {
            bh.consume(e);
        }
    }

    // ---- utils ----
    private static String randomEmail(final int len) {
        final StringBuilder sb = new StringBuilder(len + 12);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        sb.append("@example.com");
        return sb.toString();
    }

    private static List<String> makeRoles(final int n) {
        final List<String> r = new ArrayList<>(n);
        for (int i = 0; i < n; i++) r.add("ROLE_" + i);
        return r;
    }
}
