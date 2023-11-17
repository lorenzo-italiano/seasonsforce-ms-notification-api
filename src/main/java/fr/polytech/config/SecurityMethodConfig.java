package fr.polytech.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityMethodConfig {

    @Bean
    public SecurityMethods securityMethods() {
        return new SecurityMethods();
    }

    public static class SecurityMethods {
        public boolean isSender(String userId, String token) {
            String pureToken = token.replace("Bearer ", "");
            DecodedJWT decodedToken = JWT.decode(pureToken);
            return decodedToken.getSubject().equals(userId);
        }
    }
}
