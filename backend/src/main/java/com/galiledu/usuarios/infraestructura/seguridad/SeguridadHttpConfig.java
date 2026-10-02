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
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadHttpConfig {

	@Bean
	SecurityFilterChain seguridadHttp(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(autorizacion -> autorizacion
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
				.anyRequest().denyAll()
			)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	PasswordEncoder codificadorContrasenas() {
		return new BCryptPasswordEncoder(10);
	}

	// Evita que Spring Boot cree credenciales de demostración antes de implementar HU-1.
	@Bean
	UserDetailsService usuariosPendientes() {
		return usuario -> {
			throw new UsernameNotFoundException("Autenticación de usuarios aún no implementada");
		};
	}
}
