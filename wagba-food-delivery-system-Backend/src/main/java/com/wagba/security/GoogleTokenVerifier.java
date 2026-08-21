package com.wagba.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;

public class GoogleTokenVerifier {

    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String ISSUER = "https://accounts.google.com";

    private final String clientId;

    public GoogleTokenVerifier(String clientId) {
        this.clientId = clientId;
    }

    public String verifyAndGetEmail(String idToken) {
        try {
            String json = fetchJwks();
            JwkSet jwkSet = Jwks.setParser().build().parse(json);

            for (Jwk<?> jwk : jwkSet.getKeys()) {
                try {
                    PublicKey key = (PublicKey) jwk.toKey();
                    Claims claims = Jwts.parser()
                            .verifyWith(key)
                            .requireIssuer(ISSUER)
                            .requireAudience(clientId)
                            .build()
                            .parseSignedClaims(idToken)
                            .getPayload();

                    Boolean emailVerified = claims.get("email_verified", Boolean.class);
                    if (emailVerified != null && !emailVerified) {
                        throw new RuntimeException("Google email is not verified");
                    }

                    return claims.get("email", String.class);
                } catch (JwtException e) {
                    // wrong key, try the next one
                }
            }

            throw new RuntimeException("Unable to verify token with any Google key");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google token verification failed: " + e.getMessage());
        }
    }

    private String fetchJwks() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(JWKS_URL)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
