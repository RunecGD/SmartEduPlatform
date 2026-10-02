package org.example.core.utils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
class JwtServiceTest {
 private final String secret="test-key-".repeat(8);
 private JwtService service(){var s=new JwtService();ReflectionTestUtils.setField(s,"secret",secret);ReflectionTestUtils.setField(s,"accessTtlHours",24L);s.validateConfiguration();return s;}
 @Test void signedTokenCarriesSubject(){var s=service();var token=s.generateToken("student@test.local");assertTrue(s.isValid(token));assertEquals("student@test.local",s.extractEmail(token));}
 @Test void expiredTokenIsRejected(){var token=Jwts.builder().subject("student@test.local").expiration(new Date(1)).signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();assertFalse(service().isValid(token));}
 @Test void foreignSignatureIsRejected(){var other=Keys.hmacShaKeyFor("another-key-".repeat(8).getBytes(StandardCharsets.UTF_8));assertFalse(service().isValid(Jwts.builder().subject("student@test.local").signWith(other).compact()));}
 @Test void malformedTokenIsRejected(){assertFalse(service().isValid("broken"));}
}
