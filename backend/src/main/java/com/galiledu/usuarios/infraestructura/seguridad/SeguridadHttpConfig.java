package com.galiledu.usuarios.infraestructura.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadHttpConfig {

	@Bean
	SecurityFilterChain seguridadHttp(HttpSecurity http, SecurityContextRepository repositorioContexto) throws Exception {
		http
			.authorizeHttpRequests(autorizacion -> autorizacion
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/initial-password").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
				.anyRequest().denyAll()
			)
			.securityContext(contexto -> contexto.securityContextRepository(repositorioContexto))
			.exceptionHandling(excepciones -> excepciones.authenticationEntryPoint(
				(solicitud, respuesta, error) -> respuesta.sendError(401)))
			.logout(salida -> salida.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler((solicitud, respuesta, autenticacion) -> respuesta.setStatus(204)))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	SecurityContextRepository repositorioContexto() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	PasswordEncoder codificadorContrasenas() {
		return new BCryptPasswordEncoder(10);
	}

	// Evita que Spring Boot cree credenciales automáticas ajenas al flujo de GalilEdu.
	@Bean
	UserDetailsService usuariosPendientes() {
		return usuario -> {
			throw new UsernameNotFoundException("Autenticación de usuarios aún no implementada");
		};
	}
}
