package com.galiledu.usuarios.infraestructura.seguridad;

import java.io.Console;
import java.util.List;
import java.util.Set;

import com.galiledu.usuarios.aplicacion.GestionUsuarios;
import com.galiledu.usuarios.dominio.DatosPersonales;
import com.galiledu.usuarios.dominio.TipoDocumento;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Alta inicial explícita, interactiva y solo cuando todavía no hay cuentas. */
@Component
@ConditionalOnProperty(name = "galiledu.bootstrap-admin", havingValue = "true")
public class InicializarPrimerAdministrador implements ApplicationRunner {
	private final JdbcOperations jdbc;
	private final GestionUsuarios gestion;
	private final TransactionTemplate transacciones;

	public InicializarPrimerAdministrador(JdbcOperations jdbc, GestionUsuarios gestion,
		TransactionTemplate transacciones) {
		this.jdbc = jdbc;
		this.gestion = gestion;
		this.transacciones = transacciones;
	}

	@Override
	public void run(ApplicationArguments argumentos) {
		Console consola = System.console();
		if (consola == null) throw new IllegalStateException("El alta inicial requiere una terminal interactiva");
		if (jdbc.queryForObject("SELECT count(*) FROM seguridad.usuario", Integer.class) != 0) {
			throw new IllegalStateException("Ya hay cuentas; el alta inicial no está disponible");
		}
		consola.printf("Datos reales del primer administrador (no se guardarán en archivos):%n");
		String nombres = consola.readLine("Nombres: ");
		String primerApellido = consola.readLine("Primer apellido: ");
		String segundoApellido = consola.readLine("Segundo apellido (opcional): ");
		String documento = consola.readLine("DNI (8 dígitos): ");
		String correo = consola.readLine("Correo: ");
		String telefono = consola.readLine("Teléfono (9 dígitos): ");
		DatosPersonales datos = new DatosPersonales(nombres, primerApellido,
			segundoApellido.isBlank() ? null : segundoApellido, TipoDocumento.DNI,
			documento, correo, telefono);
		var cuenta = transacciones.execute(estado -> {
			jdbc.execute("SELECT pg_advisory_xact_lock(20261002)");
			if (jdbc.queryForObject("SELECT count(*) FROM seguridad.usuario", Integer.class) != 0) {
				throw new IllegalStateException("Otra cuenta fue creada durante la inicialización");
			}
			for (String rol : List.of("ADMINISTRADOR", "DOCENTE", "ESTUDIANTE", "APODERADO",
				"TUTOR", "COORDINADOR", "PERSONAL_ADMINISTRATIVO")) {
				jdbc.update("""
					INSERT INTO seguridad.rol (nombre)
					SELECT ? WHERE NOT EXISTS
					(SELECT 1 FROM seguridad.rol WHERE UPPER(nombre) = ?)
					""", rol, rol);
			}
			return gestion.crearCuenta(datos, Set.of(), List.of(), Set.of("ADMINISTRADOR"));
		});
		consola.printf("Administrador creado: %s%nContraseña inicial (guárdala ahora): %s%n",
			cuenta.nombreUsuario(), cuenta.contrasenaInicial());
		consola.printf("Detén el servidor y vuelve a iniciarlo sin --galiledu.bootstrap-admin=true.%n");
	}
}
