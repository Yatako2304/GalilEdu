package com.galiledu.asistencia.pruebas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.GalilEduBackendApplication;
import com.galiledu.asistencia.aplicacion.GestionAsistencia;
import com.galiledu.asistencia.dominio.CondicionAsistencia;
import com.galiledu.asistencia.dominio.EstadoAsistencia;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Ejecuta el CRUD basico de asistencia contra PostgreSQL desde IntelliJ. */
public final class TestAsistencia {
	private TestAsistencia() {}

	public static void main(String[] args) throws IOException {
		cargarConfiguracionPostgres();

		try (ConfigurableApplicationContext contexto = new SpringApplicationBuilder(GalilEduBackendApplication.class)
			.web(WebApplicationType.SERVLET)
			.properties("server.port=0")
			.run(args)) {
			JdbcOperations jdbc = contexto.getBean(JdbcOperations.class);
			GestionAsistencia asistencia = contexto.getBean(GestionAsistencia.class);
			TransactionTemplate transaccion = new TransactionTemplate(
				contexto.getBean(PlatformTransactionManager.class));

			transaccion.executeWithoutResult(estado -> {
				String base = jdbc.queryForObject("SELECT current_database()", String.class);
				System.out.println("[ASISTENCIA][OK] Conexion a PostgreSQL: " + base);
				Fixture fixture = prepararDatosTemporales(jdbc);
				probarJornada(asistencia, fixture.cargaAcademicaId());
				probarAsistenciaAlumno(asistencia, fixture.cargaAcademicaId(), fixture.matriculaId());
				probarAsistenciaDocente(asistencia, fixture.docenteId());
				estado.setRollbackOnly();
				System.out.println("[ASISTENCIA][OK] Pruebas terminadas; los datos temporales se revirtieron.");
			});
		}
	}

	private static void probarJornada(GestionAsistencia asistencia, UUID cargaId) {
		LocalDate fecha = LocalDate.now();
		var creada = asistencia.crearJornada(cargaId, fecha);
		System.out.println("[ASISTENCIA][OK] Jornada registrada correctamente: " + creada.id());
		confirmar(asistencia.buscarJornada(creada.id()).id().equals(creada.id()), "buscarJornada");
		confirmar(asistencia.listarJornadas().stream().anyMatch(j -> j.id().equals(creada.id())), "listarJornadas");
		var actualizada = asistencia.actualizarJornada(creada.id(), cargaId, fecha.plusDays(1));
		confirmar(actualizada.fecha().equals(fecha.plusDays(1)), "actualizarJornada");
		asistencia.desactivarJornada(creada.id());
		confirmarNoEncontrada(() -> asistencia.buscarJornada(creada.id()), "desactivarJornada");
	}

	private static void probarAsistenciaAlumno(GestionAsistencia asistencia, UUID cargaId, UUID matriculaId) {
		var jornada = asistencia.crearJornada(cargaId, LocalDate.now());
		var creada = asistencia.crearDetalle(jornada.id(), matriculaId, EstadoAsistencia.PRESENTE,
			null, LocalTime.of(8, 0), "Prueba ejecutable IntelliJ");
		System.out.println("[ASISTENCIA][OK] Asistencia del alumno registrada correctamente: " + creada.id());
		confirmar(asistencia.buscarDetalle(creada.id()).id().equals(creada.id()), "buscarDetalle");
		confirmar(asistencia.listarDetalles(jornada.id()).stream().anyMatch(d -> d.id().equals(creada.id())),
			"listarDetalles");
		var actualizada = asistencia.actualizarDetalle(creada.id(), jornada.id(), matriculaId,
			EstadoAsistencia.AUSENTE, CondicionAsistencia.JUSTIFICADA, null, "Prueba de actualización");
		confirmar(actualizada.estado() == EstadoAsistencia.AUSENTE, "actualizarDetalle");
		asistencia.desactivarDetalle(creada.id());
		confirmarNoEncontrada(() -> asistencia.buscarDetalle(creada.id()), "desactivarDetalle");
		asistencia.desactivarJornada(jornada.id());
	}

	private static void probarAsistenciaDocente(GestionAsistencia asistencia, UUID docenteId) {
		LocalDate fecha = LocalDate.now();
		var creada = asistencia.crearAsistenciaDocente(docenteId, fecha,
			LocalTime.of(7, 30), LocalTime.of(14, 0), "Prueba ejecutable IntelliJ");
		System.out.println("[ASISTENCIA][OK] Asistencia del docente registrada correctamente: " + creada.id());
		confirmar(asistencia.buscarAsistenciaDocente(creada.id()).id().equals(creada.id()), "buscarAsistenciaDocente");
		confirmar(asistencia.listarAsistenciasDocentes().stream().anyMatch(a -> a.id().equals(creada.id())),
			"listarAsistenciasDocentes");
		var actualizada = asistencia.actualizarAsistenciaDocente(creada.id(), docenteId, fecha,
			LocalTime.of(7, 35), LocalTime.of(14, 5), "Prueba de actualización");
		confirmar(actualizada.horaEntrada().equals(LocalTime.of(7, 35)), "actualizarAsistenciaDocente");
		asistencia.desactivarAsistenciaDocente(creada.id());
		confirmarNoEncontrada(() -> asistencia.buscarAsistenciaDocente(creada.id()), "desactivarAsistenciaDocente");
	}

	private static Fixture prepararDatosTemporales(JdbcOperations jdbc) {
		String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		UUID docenteId = crearPersonaYRol(jdbc, true);
		UUID estudianteId = crearPersonaYRol(jdbc, false);

		Integer anio = jdbc.queryForObject(
			"SELECT COALESCE(MAX(anio), 2000) + 1 FROM configuracion.anio_escolar", Integer.class);
		UUID anioId = UUID.randomUUID();
		jdbc.update("INSERT INTO configuracion.anio_escolar(id, anio, fecha_inicio, fecha_fin, regimen_periodos) "
			+ "VALUES (?, ?, ?, ?, 'BIMESTRAL'::configuracion.regimen_periodos)", anioId, anio,
			LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31));

		Integer numeroGrado = jdbc.queryForObject("SELECT COALESCE(MAX(numero), 0) + 1 FROM configuracion.grado "
			+ "WHERE nivel = 'PRIMARIA'::configuracion.nivel_educativo", Integer.class);
		UUID gradoId = UUID.randomUUID();
		jdbc.update("INSERT INTO configuracion.grado(id, nivel, numero, nombre) "
			+ "VALUES (?, 'PRIMARIA'::configuracion.nivel_educativo, ?, ?)", gradoId, numeroGrado,
			"Grado prueba " + sufijo);

		UUID seccionId = UUID.randomUUID();
		jdbc.update("INSERT INTO configuracion.seccion(id, anio_id, grado_id, nombre, capacidad_maxima) "
			+ "VALUES (?, ?, ?, ?, 30)", seccionId, anioId, gradoId, "S" + sufijo.substring(0, 10));
		UUID areaId = UUID.randomUUID();
		jdbc.update("INSERT INTO configuracion.area_curricular(id, nombre) VALUES (?, ?)",
			areaId, "Área prueba " + sufijo);
		UUID cursoId = UUID.randomUUID();
		jdbc.update("INSERT INTO configuracion.curso(id, area_curricular_id, nombre) VALUES (?, ?, ?)",
			cursoId, areaId, "Curso prueba " + sufijo);
		UUID ofertaId = UUID.randomUUID();
		jdbc.update("INSERT INTO configuracion.oferta_curso(id, anio_id, grado_id, curso_id, horas_semanales) "
			+ "VALUES (?, ?, ?, ?, 5)", ofertaId, anioId, gradoId, cursoId);
		UUID cargaId = UUID.randomUUID();
		jdbc.update("INSERT INTO configuracion.carga_academica(id, oferta_curso_id, seccion_id, docente_id) "
			+ "VALUES (?, ?, ?, ?)", cargaId, ofertaId, seccionId, docenteId);
		UUID matriculaId = UUID.randomUUID();
		jdbc.update("INSERT INTO matricula.matricula(id, estudiante_id, anio_id, seccion_id, tipo, estado) "
			+ "VALUES (?, ?, ?, ?, 'REGULAR'::matricula.tipo_matricula, "
			+ "'MATRICULADA'::matricula.estado_matricula)", matriculaId, estudianteId, anioId, seccionId);
		System.out.println("[ASISTENCIA][OK] Preparados alumno y docente temporales para las pruebas.");
		return new Fixture(cargaId, matriculaId, docenteId);
	}

	private static UUID crearPersonaYRol(JdbcOperations jdbc, boolean docente) {
		UUID id = UUID.randomUUID();
		String documento = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		jdbc.update("INSERT INTO personas.persona(id, tipo_documento, numero_documento, nombres, primer_apellido) "
			+ "VALUES (?, 'CARNET_EXTRANJERIA'::personas.tipo_documento, ?, 'Prueba', 'Asistencia')", id, documento);
		jdbc.update(docente ? "INSERT INTO personas.docente(id) VALUES (?)"
			: "INSERT INTO personas.estudiante(id) VALUES (?)", id);
		return id;
	}

	private static void confirmar(boolean correcto, String funcion) {
		if (!correcto) {
			throw new IllegalStateException("Falló la prueba de " + funcion);
		}
		System.out.println("[ASISTENCIA][OK] " + funcion + " verificada.");
	}

	private static void confirmarNoEncontrada(Runnable busqueda, String funcion) {
		try {
			busqueda.run();
			throw new IllegalStateException("El registro desactivado aún aparece en " + funcion);
		} catch (NoSuchElementException esperado) {
			System.out.println("[ASISTENCIA][OK] " + funcion + " verificada.");
		}
	}

	private static void cargarConfiguracionPostgres() throws IOException {
		for (String variable : List.of("DB_URL", "DB_USER", "DB_PASSWORD")) {
			String valor = System.getenv(variable);
			if (valor == null || valor.isBlank()) {
				valor = leerVariableEnv(variable);
			}
			if (valor == null || valor.isBlank()) {
				throw new IllegalStateException("Falta " + variable + ". Verifica el archivo backend/.env.");
			}
			System.setProperty(variable, valor);
		}
	}

	private static String leerVariableEnv(String nombre) throws IOException {
		for (Path archivo : List.of(Path.of(".env"), Path.of("backend", ".env"), Path.of("..", "backend", ".env"))) {
			if (!Files.isRegularFile(archivo)) {
				continue;
			}
			for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
				String contenido = linea.strip();
				if (contenido.isEmpty() || contenido.startsWith("#")) {
					continue;
				}
				int separador = contenido.indexOf('=');
				if (separador < 0 || !contenido.substring(0, separador).strip().equals(nombre)) {
					continue;
				}
				String valor = contenido.substring(separador + 1).strip();
				if (valor.length() >= 2 && ((valor.startsWith("\"") && valor.endsWith("\""))
					|| (valor.startsWith("'") && valor.endsWith("'")))) {
					valor = valor.substring(1, valor.length() - 1);
				}
				return valor;
			}
		}
		return null;
	}

	private record Fixture(UUID cargaAcademicaId, UUID matriculaId, UUID docenteId) {}
}
