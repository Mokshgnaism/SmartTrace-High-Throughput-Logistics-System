package EntryPoint.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
@Service
public class JwtUtil {
    private static final String SECRET_KEY =
            "___GET___FROM___ENV___";

    private static final long EXPIRATION_MS = 15 * 60 * 1000;
    public String generateJWT(String userId,String role){
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+EXPIRATION_MS))
                .setClaims(Collections.singletonMap("role",role))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()),SignatureAlgorithm.HS256)
                .compact();
    }
    public Claims parseJWT(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(jwt).getBody();
    }
    public String getRole(String jwt) {
        return parseJWT(jwt).get("role",String.class);
    }
    public String getUserId(String jwt) {
        return parseJWT(jwt).getSubject();
    }
}
