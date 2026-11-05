package com.appasadito.AsaditoRg.config;

import com.appasadito.AsaditoRg.repositorios.UsuarioRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Locale;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService users(UsuarioRepository repository) {
        return email -> repository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .map(u -> User.withUsername(u.getEmail()).password(u.getPasswordHash()).roles(u.getRol()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests(auth -> auth
            .antMatchers("/", "/index.html", "/gestion.html", "/assets/**", "/error", "/api/auth/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/productos", "/api/locales", "/api/precios", "/api/precios/ultimos").permitAll()
            .antMatchers(HttpMethod.POST, "/api/presupuestos/calcular").permitAll()
            .antMatchers(HttpMethod.POST, "/api/admin/precios").hasRole("OPERADOR")
            .antMatchers(HttpMethod.POST, "/api/admin/productos", "/api/admin/locales").hasRole("ADMIN")
            .antMatchers(HttpMethod.PUT, "/api/admin/productos/*", "/api/admin/locales/*").hasRole("ADMIN")
            .antMatchers(HttpMethod.DELETE, "/api/admin/productos/*", "/api/admin/locales/*").hasRole("ADMIN")
            .antMatchers("/api/admin/**").denyAll()
            .anyRequest().authenticated())
            // CSRF remains enabled for login, logout, registration and every mutation.
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .successHandler((req, res, auth) -> res.setStatus(204))
                .failureHandler((req, res, ex) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"Email o contraseña incorrectos\"}");
                }).permitAll())
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"Iniciá sesión para continuar\"}");
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"Acceso denegado o sesión vencida. Recargá la página\"}");
                }));
        return http.build();
    }
}
