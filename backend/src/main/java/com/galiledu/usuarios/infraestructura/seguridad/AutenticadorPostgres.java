package com.galiledu.usuarios.infraestructura.seguridad;

import java.time.Clock;

import com.galiledu.usuarios.aplicacion.IniciarSesion;
import com.galiledu.usuarios.aplicacion.ResultadoInicioSesion;
import com.galiledu.usuarios.aplicacion.puertos.RepositorioAccesos;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticadorPostgres {
	private final IniciarSesion iniciarSesion;

	public AutenticadorPostgres(RepositorioAccesos accesos, ServicioContrasenas contrasenas, Clock reloj) {
		this.iniciarSesion = new IniciarSesion(accesos, contrasenas, reloj);
	}

	@Transactional
	public ResultadoInicioSesion verificar(String usuario, String contrasena) {
		return iniciarSesion.ejecutar(usuario, contrasena);
	}
}
