package fr.miage.toulouse.odoru.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange

                        // Préflight CORS
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Actuator
                        .pathMatchers("/actuator/**").permitAll()

                        // Inscription publique d'un nouveau membre
                        .pathMatchers(HttpMethod.POST, "/api/members").permitAll()

                        // Statistiques réservées au président
                        .pathMatchers("/api/statistics/**").hasRole("PRESIDENT")

                        // Gestion administrative des membres
                        .pathMatchers(HttpMethod.PATCH, "/api/members/*/registration-review")
                        .hasAnyRole("SECRETARY", "PRESIDENT")

                        .pathMatchers(HttpMethod.PATCH, "/api/members/*/expertise-level")
                        .hasAnyRole("SECRETARY", "PRESIDENT")

                        .pathMatchers(HttpMethod.PATCH, "/api/members/*/roles")
                        .hasAnyRole("SECRETARY", "PRESIDENT")

                        // Gestion des cours par les enseignants
                        .pathMatchers(HttpMethod.POST, "/api/courses/**")
                        .hasAnyRole("TEACHER", "PRESIDENT")

                        .pathMatchers(HttpMethod.PUT, "/api/courses/**")
                        .hasAnyRole("TEACHER", "PRESIDENT")

                        .pathMatchers(HttpMethod.DELETE, "/api/courses/**")
                        .hasAnyRole("TEACHER", "PRESIDENT")

                        // Gestion des compétitions par les enseignants
                        .pathMatchers(HttpMethod.POST, "/api/competitions/**")
                        .hasAnyRole("TEACHER", "PRESIDENT")

                        .pathMatchers(HttpMethod.PUT, "/api/competitions/**")
                        .hasAnyRole("TEACHER", "PRESIDENT")

                        .pathMatchers(HttpMethod.DELETE, "/api/competitions/**")
                        .hasAnyRole("TEACHER", "PRESIDENT")

                        // Création / association / dissociation des badges
                        .pathMatchers(HttpMethod.POST, "/api/badges")
                        .hasAnyRole("SECRETARY", "PRESIDENT")

                        .pathMatchers(HttpMethod.PATCH, "/api/badges/*/assign")
                        .hasAnyRole("SECRETARY", "PRESIDENT")

                        .pathMatchers(HttpMethod.PATCH, "/api/badges/*/unassign")
                        .hasAnyRole("SECRETARY", "PRESIDENT")

                        // Le boîtier de badgeage est simulé par appel REST
                        .pathMatchers(HttpMethod.POST, "/api/badges/scan")
                        .permitAll()

                        // Consultations accessibles à tout utilisateur authentifié
                        .pathMatchers(HttpMethod.GET, "/api/members/**").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/courses/**").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/competitions/**").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/badges/**").authenticated()

                        // Toute autre route doit être authentifiée
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(this::extractRealmRoles);

        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || realmAccess.get("roles") == null) {
            return authorities;
        }

        Object rolesObject = realmAccess.get("roles");

        if (rolesObject instanceof Collection<?> roles) {
            roles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()))
            );
        }

        return authorities;
    }
}