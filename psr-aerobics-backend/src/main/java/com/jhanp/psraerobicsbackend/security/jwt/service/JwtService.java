package com.jhanp.psraerobicsbackend.security.jwt.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Clave secreta usada para firmar y verificar los tokens.
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-in-ms}")
    private long expirationInMs;

    // La clave se decodifica UNA vez al arrancar, no en cada firma. Además así un secreto
    // mal puesto revienta el arranque con un mensaje claro, en vez de esperar al primer
    // login y salir como un 500 con una traza de Base64 que no dice de dónde viene.
    private SecretKey key;

    @PostConstruct
    void initKey() {
        byte[] keyBytes;
        try {
            // trim() por si el .env dejó un espacio o un salto de línea al final
            keyBytes = Decoders.BASE64.decode(secretKey.trim());
        } catch (DecodingException e) {
            throw new IllegalStateException(
                    "JWT_SECRET no es Base64 válido (alfabeto: A-Z a-z 0-9 + / =). " +
                            "Genéralo con: [Convert]::ToBase64String((New-Object byte[] 64))", e);
        }
        // HMAC-SHA256 exige 32 bytes como mínimo. Con menos, Keys.hmacShaKeyFor lanza
        // WeakKeyException, así que más vale decir aquí cuántos bytes hay realmente.
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET decodifica a " + keyBytes.length + " bytes y HMAC-SHA necesita al menos 32");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // Punto de entrada público: genera un token para un usuario, sin claims extra.
    public String getToken(UserDetails user) {
        return getToken(new HashMap<>(), user);
    }

    // Construye el token: mete claims, subject (username), fechas y lo firma.
    private String getToken(Map<String, Object> extraClaims, UserDetails user) {
        // Una sola lectura del reloj: con dos llamadas separadas, emisión y expiración
        // pueden caer en milisegundos distintos.
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationInMs))
                .signWith(key)
                .compact();
    }

    // Extrae el username (subject) guardado dentro del token.
    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    // Verifica que el token pertenezca al usuario dado y que no haya expirado.
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // Desempaqueta el token y verifica su firma. Si fue alterado, lanza excepción aquí.
    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Helper genérico para extraer cualquier claim sin repetir código.
    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Obtiene la fecha de expiración guardada en el token.
    private Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    // Compara la expiración del token contra la fecha actual.
    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }
}