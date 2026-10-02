package com.galiledu.usuarios.infraestructura.demo;

import java.time.Clock;
import java.time.Year;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.galiledu.usuarios.aplicacion.CambiarContrasenaInicial;
import com.galiledu.usuarios.aplicacion.IniciarSesion;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import com.galiledu.usuarios.dominio.CuentaUsuario;
import com.galiledu.usuarios.dominio.DatosPersonales;
import com.galiledu.usuarios.dominio.GeneradorNombreUsuario;
import com.galiledu.usuarios.dominio.Rol;
import com.galiledu.usuarios.dominio.TipoDocumento;

@Configuration
@Profile("demo")
public class UsuariosDemoConfig {
	@Bean
	RepositorioUsuariosDemo repositorioUsuariosDemo(
		@Value("${galiledu.demo.admin.password}") String temporal,
		ServicioContrasenas contrasenas, Clock reloj) {
		if (temporal.isBlank()) {
			throw new IllegalStateException("Defina DEMO_ADMIN_TEMP_PASSWORD para iniciar el perfil demo");
		}
		DatosPersonales datos = new DatosPersonales("Admin", "Demostración", "Local",
			TipoDocumento.DNI, "00000000", "admin@demo.invalid", "000000000");
		String nombre = GeneradorNombreUsuario.generar(Year.now(reloj).getValue(), 1);
		CuentaUsuario cuenta = CuentaUsuario.nueva(datos, nombre, contrasenas.codificar(temporal),
			Set.of(new Rol("ADMINISTRADOR")));
		return new RepositorioUsuariosDemo(cuenta);
	}

	@Bean
	IniciarSesion iniciarSesion(RepositorioUsuariosDemo usuarios, ServicioContrasenas contrasenas,
		Clock reloj) {
		return new IniciarSesion(usuarios, contrasenas, reloj);
	}

	@Bean
	CambiarContrasenaInicial cambiarContrasenaInicial(RepositorioUsuariosDemo usuarios,
		ServicioContrasenas contrasenas) {
		return new CambiarContrasenaInicial(usuarios, contrasenas);
	}
}
