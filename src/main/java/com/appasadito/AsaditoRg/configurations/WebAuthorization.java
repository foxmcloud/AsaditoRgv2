package com.appasadito.AsaditoRg.configurations;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import javax.servlet.http.*;

@Configuration
@EnableWebSecurity
public class WebAuthorization {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/", "/index.html", "/gestion.html", "/assets/**", "/error", "/api/login", "/api/logout", "/api/auth/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/productos", "/api/locales", "/api/precios", "/api/precios/ultimos").permitAll()
            .antMatchers(HttpMethod.GET, "/api/admin/operadores").hasAuthority("ADMIN")
            .antMatchers(HttpMethod.POST, "/api/admin/operadores", "/api/admin/productos", "/api/admin/locales").hasAuthority("ADMIN")
            .antMatchers(HttpMethod.PUT, "/api/admin/productos/*", "/api/admin/locales/*").hasAuthority("ADMIN")
            .antMatchers(HttpMethod.DELETE, "/api/admin/productos/*", "/api/admin/locales/*").hasAuthority("ADMIN")
            .antMatchers(HttpMethod.POST, "/api/admin/precios").hasAuthority("OPERADOR")
            .antMatchers("/api/admin/**").denyAll()
            .anyRequest().authenticated();
        http.formLogin().loginProcessingUrl("/api/login")
            .usernameParameter("email").passwordParameter("password")
            .successHandler((req, res, auth) -> {
                clearAuthenticationAttributes(req);
                res.setStatus(HttpServletResponse.SC_NO_CONTENT);
            })
            .failureHandler((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
        http.exceptionHandling().authenticationEntryPoint((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
        http.logout().logoutUrl("/api/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID")
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT));
        // CSRF se mantiene porque el navegador autentica con una cookie de sesión.
        return http.build();
    }
    private void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}
