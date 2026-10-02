package com.galiledu.usuarios.infraestructura.seguridad;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public final class RespuestaAutenticacion implements AuthenticationEntryPoint {
	@Override
	public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta,
		AuthenticationException error) throws IOException {
		respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		respuesta.setContentType("application/json;charset=UTF-8");
		respuesta.setHeader("Cache-Control", "no-store");
		if (error instanceof CuentaBloqueadaException bloqueada) {
			respuesta.getWriter().write("{\"mensaje\":\"Cuenta bloqueada temporalmente\","
				+ "\"segundosRestantes\":" + bloqueada.segundosRestantes() + "}");
		} else {
			respuesta.getWriter().write("{\"mensaje\":\"Credenciales incorrectas o sesión requerida\"}");
		}
	}
}
