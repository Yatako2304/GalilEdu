package com.galiledu.usuarios.infraestructura.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Locale;

@Configuration
public class SeguridadHttpConfig {

	@Bean
	SecurityFilterChain seguridadHttp(HttpSecurity http, SecurityContextRepository repositorioContexto,
		AuthenticationProvider autenticacion, RespuestaAutenticacion respuestaAutenticacion) throws Exception {
		http
			.authorizeHttpRequests(autorizacion -> autorizacion
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/api/auth/csrf").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/auth/password").authenticated()
				.requestMatchers("/api/usuarios", "/api/personas/**", "/api/estudiantes/**").hasRole("ADMINISTRADOR")
				.requestMatchers("/api/configuracion/areas-curriculares", "/api/configuracion/areas-curriculares/**",
					"/api/configuracion/competencias", "/api/configuracion/competencias/**")
					.hasAnyRole("ADMINISTRADOR", "PERSONAL_ADMINISTRATIVO")
				.requestMatchers("/api/pagos/tarifarios", "/api/pagos/tarifarios/**")
					.hasAnyRole("ADMINISTRADOR", "PERSONAL_ADMINISTRATIVO")
				.anyRequest().denyAll()
			)
			.authenticationProvider(autenticacion)
			.securityContext(contexto -> contexto.securityContextRepository(repositorioContexto))
			.exceptionHandling(excepciones -> excepciones.authenticationEntryPoint(respuestaAutenticacion)
				.accessDeniedHandler((solicitud, respuesta, error) -> respuesta.sendError(403)))
			.logout(salida -> salida.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler((solicitud, respuesta, sesion) -> respuesta.setStatus(204)))
			.httpBasic(basic -> basic.authenticationEntryPoint(respuestaAutenticacion));
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

	@Bean
	AuthenticationProvider autenticacion(AutenticadorPostgres postgres) {
		return new AuthenticationProvider() {
			@Override
			public Authentication authenticate(Authentication solicitud) {
				var resultado = postgres.verificar(solicitud.getName(), String.valueOf(solicitud.getCredentials()));
				if (resultado.estado() == com.galiledu.usuarios.aplicacion.ResultadoInicioSesion.Estado.BLOQUEADO) {
					throw new CuentaBloqueadaException(resultado.segundosRestantesBloqueo());
				}
				if (resultado.estado() != com.galiledu.usuarios.aplicacion.ResultadoInicioSesion.Estado.EXITOSO) {
					throw new BadCredentialsException("Credenciales incorrectas");
				}
				return UsernamePasswordAuthenticationToken.authenticated(resultado.nombreUsuario(), null,
					resultado.roles().stream()
						.map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.codigo().toUpperCase(Locale.ROOT))).toList());
			}

			@Override
			public boolean supports(Class<?> tipo) {
				return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
			}
		};
	}
}
