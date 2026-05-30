package br.com.stipz.config;

import br.com.stipz.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/regras-acesso").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/notificacoes/stream").permitAll()
                        .requestMatchers("/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios/**", "/salas/**", "/recursos/**", "/reservas/**", "/eventos/**")
                        .hasAnyRole("ADMIN", "COMUM")
                        .requestMatchers(HttpMethod.POST, "/reservas", "/eventos").hasAnyRole("ADMIN", "COMUM")
                        .requestMatchers(HttpMethod.PATCH, "/reservas/*/cancelar").hasAnyRole("ADMIN", "COMUM")
                        .requestMatchers(HttpMethod.POST, "/notificacoes/teste").hasAnyRole("ADMIN", "COMUM")
                        .requestMatchers("/backups/auditoria/**").hasRole("ADMIN")
                        .requestMatchers("/usuarios/**", "/salas/**", "/recursos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/reservas/*/aprovar", "/reservas/*/rejeitar").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return email -> usuarioRepository.findByEmail(email)
                .map(usuario -> User.withUsername(usuario.getEmail())
                        .password(usuario.getSenha())
                        .roles(usuario.getPerfil().name())
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Usuário não encontrado"));
    }

}
