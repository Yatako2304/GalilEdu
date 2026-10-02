package com.galiledu.usuarios.api;

import java.util.List;

import com.galiledu.usuarios.aplicacion.CambiarContrasenaInicial;
import com.galiledu.usuarios.aplicacion.IniciarSesion;
import com.galiledu.usuarios.aplicacion.ResultadoInicioSesion;
import com.galiledu.usuarios.aplicacion.puertos.RepositorioCuentas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
@RequestMapping("/api/auth")
public class AutenticacionController {
	private final IniciarSesion iniciarSesion;
	private final CambiarContrasenaInicial cambiarContrasena;
	private final RepositorioCuentas cuentas;
	private final SecurityContextRepository contextos;

	public AutenticacionController(IniciarSesion iniciarSesion,
		CambiarContrasenaInicial cambiarContrasena, RepositorioCuentas cuentas,
		SecurityContextRepository contextos) {
		this.iniciarSesion = iniciarSesion;
		this.cambiarContrasena = cambiarContrasena;
		this.cuentas = cuentas;
		this.contextos = contextos;
	}

	@GetMapping("/csrf")
	public CsrfRespuesta csrf(HttpServletRequest solicitud) {
		CsrfToken token = (CsrfToken) solicitud.getAttribute(CsrfToken.class.getName());
		return new CsrfRespuesta(token.getHeaderName(), token.getToken());
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginSolicitud datos,
		HttpServletRequest solicitud, HttpServletResponse respuesta) {
		ResultadoInicioSesion resultado = iniciarSesion.ejecutar(datos.nombreUsuario(), datos.contrasena());
		if (resultado.estado() == ResultadoInicioSesion.Estado.CREDENCIALES_INVALIDAS) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ErrorRespuesta("Credenciales inválidas o cuenta inactiva"));
		}
		if (resultado.estado() == ResultadoInicioSesion.Estado.BLOQUEADO) {
			return ResponseEntity.status(HttpStatus.LOCKED)
				.body(new BloqueoRespuesta("Cuenta bloqueada temporalmente",
					resultado.segundosRestantesBloqueo()));
		}

		List<String> roles = resultado.roles().stream().map(rol -> rol.codigo()).sorted().toList();
		Authentication autenticacion = UsernamePasswordAuthenticationToken.authenticated(
			resultado.nombreUsuario(), null,
			roles.stream().map(rol -> new SimpleGrantedAuthority("ROLE_" + rol)).toList());
		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(autenticacion);
		solicitud.getSession(true);
		solicitud.changeSessionId();
		SecurityContextHolder.setContext(contexto);
		contextos.saveContext(contexto, solicitud, respuesta);
		return ResponseEntity.ok(new CuentaRespuesta(resultado.nombreUsuario(), roles,
			resultado.cambioContrasenaObligatorio()));
	}

	@PostMapping("/initial-password")
	public ResponseEntity<?> cambiarContrasenaInicial(@Valid @RequestBody CambioSolicitud datos,
		Authentication autenticacion) {
		try {
			cambiarContrasena.ejecutar(autenticacion.getName(), datos.contrasenaTemporal(),
				datos.nuevaContrasena());
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException | IllegalStateException error) {
			return ResponseEntity.badRequest().body(new ErrorRespuesta(error.getMessage()));
		}
	}

	@GetMapping("/me")
	public ResponseEntity<?> me(Authentication autenticacion) {
		return cuentas.buscarPorNombreUsuario(autenticacion.getName())
			.filter(cuenta -> cuenta.activa())
			.map(cuenta -> ResponseEntity.ok(new CuentaRespuesta(cuenta.nombreUsuario(),
				cuenta.roles().stream().map(rol -> rol.codigo()).sorted().toList(),
				cuenta.cambioContrasenaObligatorio())))
			.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
	}

	public record LoginSolicitud(@NotBlank String nombreUsuario, @NotBlank String contrasena) {}
	public record CambioSolicitud(@NotBlank String contrasenaTemporal, @NotBlank String nuevaContrasena) {}
	public record CsrfRespuesta(String headerName, String token) {}
	public record CuentaRespuesta(String nombreUsuario, List<String> roles,
		boolean cambioContrasenaObligatorio) {}
	public record ErrorRespuesta(String mensaje) {}
	public record BloqueoRespuesta(String mensaje, long segundosRestantes) {}
}
