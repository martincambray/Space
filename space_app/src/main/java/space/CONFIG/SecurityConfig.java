package space.CONFIG;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.List;

/* ==========================================================================
   C'est ici qu'on gère toute la sécurité. On a 
    - les URL publiques et les autres qui ont besoin d'un JWT
    - où on place le filtre JWT dans le processus de secu
    - Les bean partagé : PasswordEncoder, AuthenticationManager
 ================================================================================== */

@Configuration /** déclare des beans Spring */
@EnableMethodSecurity(prePostEnabled = true) /* active @PreAuthorize("hasRole('ADMIN')") dans les contrôleurs */
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtHeaderFilter jwtHeaderFilter) throws Exception 
    {
        http.cors(Customizer.withDefaults());
        http.authorizeHttpRequests(auth -> 
        {
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            auth.requestMatchers("/api/auth").permitAll();
            auth.requestMatchers("/login/**", "/menu/**", "/css/**", "/js/**").permitAll();
            auth.requestMatchers("/api/**").authenticated();
            auth.requestMatchers("/**").permitAll();
        });

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.addFilterBefore(jwtHeaderFilter, UsernamePasswordAuthenticationFilter.class);
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        http.exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() 
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception 
    {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() 
    {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
