package br.com.stipz.service;

import br.com.stipz.domain.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final String issuer;
    private final long expiracaoSegundos;

    public JwtService(
            @Value("${stipz.security.jwt.secret}") String secret,
            @Value("${stipz.security.jwt.issuer:stipz-api}") String issuer,
            @Value("${stipz.security.jwt.expiracao-segundos:7200}") long expiracaoSegundos
    ) {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException("Configure JWT_SECRET com pelo menos 32 caracteres.");
        }

        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expiracaoSegundos = expiracaoSegundos;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(usuario.getEmail())
                .withClaim("id", usuario.getId())
                .withClaim("nome", usuario.getNome())
                .withClaim("perfil", usuario.getPerfil().name())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plusSeconds(expiracaoSegundos))
                .sign(algorithm);
    }

    public String validarToken(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException ex) {
            return null;
        }
    }
}
