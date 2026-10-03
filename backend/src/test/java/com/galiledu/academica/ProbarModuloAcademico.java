package com.galiledu.academica;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.galiledu.GalilEduBackendApplication;
import com.galiledu.academica.aplicacion.ConsolidacionNotas;
import com.galiledu.academica.aplicacion.ConsultaNotas;
import com.galiledu.academica.aplicacion.CriteriosEvaluacion;
import com.galiledu.academica.aplicacion.CriteriosEvaluacion.DatosItem;
import com.galiledu.academica.aplicacion.RegistroCalificaciones;
import com.galiledu.academica.dominio.Calificacion;
import com.galiledu.academica.dominio.NivelCualitativo;
import com.galiledu.academica.dominio.TipoItemEvaluacion;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Prueba manual del módulo académico contra la base configurada en application.properties
 * (o DB_URL/DB_USER/DB_PASSWORD). Crea datos de ejemplo, ejecuta los casos de uso, imprime
 * los resultados y al final hace ROLLBACK: la base queda exactamente igual.
 */
public final class ProbarModuloAcademico {
	private static JdbcOperations jdbc;

	public static void main(String[] args) {
		Properties archivoEnv = leer("backend/.env", ".env");
		Properties propiedades = leer("backend/src/main/resources/application.properties",
			"src/main/resources/application.properties");
		String url = valor("DB_URL", "spring.datasource.url", archivoEnv, propiedades);
		String usuario = valor("DB_USER", "spring.datasource.username", archivoEnv, propiedades);
		String clave = valor("DB_PASSWORD", "spring.datasource.password", archivoEnv, propiedades);
		if (url == null || usuario == null || clave == null) {
			System.err.println("No encontré la conexión. Define DB_URL, DB_USER y DB_PASSWORD (variables de entorno"
				+ " o backend/.env), por ejemplo DB_URL=jdbc:postgresql://localhost:5432/GaliEdu");
			return;
		}
		System.out.println("Conectando a " + url + " como " + usuario);
		String[] configuracion = {
			"--spring.datasource.url=" + url,
			"--spring.datasource.username=" + usuario,
			"--spring.datasource.password=" + clave,
			"--spring.datasource.driver-class-name=org.postgresql.Driver",
			"--spring.jpa.hibernate.ddl-auto=none",
			"--server.port=0",
			"--logging.level.root=WARN"
		};
		try (ConfigurableApplicationContext app = new SpringApplicationBuilder(GalilEduBackendApplication.class)
			.web(WebApplicationType.SERVLET).run(configuracion)) {
			jdbc = app.getBean(JdbcOperations.class);
			var criterios = app.getBean(CriteriosEvaluacion.class);
			var registro = app.getBean(RegistroCalificaciones.class);
			var consolidacion = app.getBean(ConsolidacionNotas.class);
			var consulta = app.getBean(ConsultaNotas.class);

			new TransactionTemplate(app.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx -> {
				try {
					var d = crearDatosDeEjemplo();

					titulo("1. El docente registra dos ítems (promedio ponderado: pesos 3 y 1)");
					var examen = criterios.registrarItem(d.docente(), d.carga(), d.competencia(), d.periodo(),
						new DatosItem("Examen bimestral", TipoItemEvaluacion.EXAMEN, LocalDate.now(), new BigDecimal("3")));
					var practica = criterios.registrarItem(d.docente(), d.carga(), d.competencia(), d.periodo(),
						new DatosItem("Práctica 1", TipoItemEvaluacion.PRACTICA, LocalDate.now(), BigDecimal.ONE));
					criterios.listarItems(d.docente(), d.carga(), d.periodo())
						.forEach(i -> System.out.println("   " + i.nombre() + " (peso " + i.peso() + ")"));

					titulo("2. Registra notas y corrige una (queda auditada)");
					registro.registrar(d.docente(), examen.id(), d.matricula(), vigesimal("12"));
					var corregida = registro.registrar(d.docente(), examen.id(), d.matricula(), vigesimal("18"));
					registro.registrar(d.docente(), practica.id(), d.matricula(), vigesimal("14"));
					System.out.println("   Examen corregido de 12 a " + corregida.valor().vigesimal());
					jdbc.query("""
						SELECT operacion, valor_anterior::text, valor_nuevo::text FROM seguridad.registro_auditoria
						WHERE entidad_id = ? ORDER BY fecha_hora
						""", rs -> {
						System.out.println("   Auditoría: " + rs.getString(1) + "  antes=" + rs.getString(2)
							+ "  después=" + rs.getString(3));
					}, corregida.id());

					titulo("3. Reglas que deben rechazarse");
					intentar("Nota cualitativa en competencia vigesimal", () ->
						registro.registrar(d.docente(), examen.id(), d.matricula(), Calificacion.cualitativa(NivelCualitativo.A)));
					intentar("El estudiante intenta ponerse nota", () ->
						registro.registrar(d.estudiante(), examen.id(), d.matricula(), vigesimal("20")));
					intentar("Retirar un ítem que ya tiene notas", () -> criterios.desactivarItem(d.docente(), examen.id()));

					titulo("4. Consolida el periodo: (18x3 + 14x1) / 4 = 17.00");
					var resultado = consolidacion.consolidarPeriodo(d.docente(), d.carga(), d.competencia(), d.periodo());
					System.out.println("   Calculadas: " + resultado.calculadas() + ", sin notas: "
						+ resultado.sinNotas().size() + ", pendientes: " + resultado.pendientes());

					titulo("5. El estudiante consulta sus notas");
					var notas = consulta.notasDeMatricula(d.estudiante(), d.matricula());
					notas.calificaciones().forEach(c -> System.out.println("   " + c.item() + ": " + c.calificacion().valor().vigesimal()));
					notas.notasPeriodo().forEach(n -> System.out.println("   Nota del periodo: " + n.nota().valor().vigesimal()
						+ " (equivalente " + n.nota().equivalenteCualitativo() + ")"));
					notas.notasFinales().forEach(n -> System.out.println("   Nota final: " + n.nota().valor().vigesimal()));
				} finally {
					tx.setRollbackOnly();
					titulo("ROLLBACK: no se guardó nada en la base");
				}
			});
		}
	}

	/** Cadena mínima: docente y estudiante con cuenta → año, periodo, sección, curso, carga → matrícula. */
	private static Datos crearDatosDeEjemplo() {
		UUID docente = persona("Docente");
		jdbc.update("INSERT INTO personas.docente (id) VALUES (?)", docente);
		UUID estudiante = persona("Estudiante");
		jdbc.update("INSERT INTO personas.estudiante (id) VALUES (?)", estudiante);
		int n = aleatorio(100_000, 999_999);
		UUID anio = insertar("""
			INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen_periodos, estado)
			VALUES (?, ?, ?, ?, 'BIMESTRAL', 'ACTIVO')""", n, LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(8));
		UUID periodo = insertar("""
			INSERT INTO configuracion.periodo_academico
			(id, anio_id, nombre, orden, fecha_inicio, fecha_fin, fecha_limite_notas, estado)
			VALUES (?, ?, 'I Bimestre', 1, ?, ?, ?, 'EN_CURSO')""", anio, LocalDate.now().minusMonths(1),
			LocalDate.now().plusMonths(1), Timestamp.valueOf(LocalDate.now().plusYears(1).atStartOfDay()));
		UUID grado = insertar("INSERT INTO configuracion.grado (id, nivel, numero, nombre) VALUES (?, 'PRIMARIA', ?, 'Grado demo')", n);
		UUID seccion = insertar("""
			INSERT INTO configuracion.seccion (id, anio_id, grado_id, nombre, capacidad_maxima)
			VALUES (?, ?, ?, 'A', 30)""", anio, grado);
		UUID area = insertar("INSERT INTO configuracion.area_curricular (id, nombre) VALUES (?, ?)", "Área demo " + n);
		UUID curso = insertar("INSERT INTO configuracion.curso (id, area_curricular_id, nombre) VALUES (?, ?, 'Matemática')", area);
		UUID competenciaBase = insertar("""
			INSERT INTO configuracion.competencia (id, area_curricular_id, nombre) VALUES (?, ?, 'Resuelve problemas')""", area);
		UUID oferta = insertar("""
			INSERT INTO configuracion.oferta_curso (id, anio_id, grado_id, curso_id, horas_semanales)
			VALUES (?, ?, ?, ?, 4)""", anio, grado, curso);
		UUID competencia = insertar("""
			INSERT INTO configuracion.configuracion_competencia
			(id, oferta_curso_id, competencia_id, tipo_escala, regla_agregacion, regla_consolidacion)
			VALUES (?, ?, ?, 'VIGESIMAL', 'PROMEDIO_PONDERADO', 'ULTIMO_PERIODO')""", oferta, competenciaBase);
		UUID carga = insertar("""
			INSERT INTO configuracion.carga_academica (id, oferta_curso_id, seccion_id, docente_id)
			VALUES (?, ?, ?, ?)""", oferta, seccion, docente);
		UUID matricula = insertar("""
			INSERT INTO matricula.matricula (id, estudiante_id, anio_id, seccion_id, tipo, estado)
			VALUES (?, ?, ?, ?, 'REGULAR', 'MATRICULADA')""", estudiante, anio, seccion);
		return new Datos(usuario(docente), usuario(estudiante), carga, competencia, periodo, matricula);
	}

	private static UUID persona(String nombres) {
		return insertar("""
			INSERT INTO personas.persona (id, tipo_documento, numero_documento, nombres, primer_apellido, correo)
			VALUES (?, 'DNI', ?, ?, 'Demo', ?)""", String.valueOf(aleatorio(10_000_000, 99_999_999)), nombres,
			"demo" + UUID.randomUUID() + "@prueba.test");
	}

	private static String usuario(UUID personaId) {
		String username = "U2099" + aleatorio(1000, 9999);
		insertar("INSERT INTO seguridad.usuario (id, persona_id, username, password_hash) VALUES (?, ?, ?, 'sin-uso')",
			personaId, username);
		return username;
	}

	private static UUID insertar(String sql, Object... valores) {
		UUID id = UUID.randomUUID();
		Object[] argumentos = new Object[valores.length + 1];
		argumentos[0] = id;
		System.arraycopy(valores, 0, argumentos, 1, valores.length);
		jdbc.update(sql, argumentos);
		return id;
	}

	/** Variable de entorno, luego backend/.env, luego application.properties (si no es un ${placeholder}). */
	private static String valor(String variable, String propiedad, Properties archivoEnv, Properties propiedades) {
		String valor = System.getenv(variable);
		if (valor == null || valor.isBlank()) valor = archivoEnv.getProperty(variable);
		if (valor == null || valor.isBlank()) valor = propiedades.getProperty(propiedad);
		return valor == null || valor.isBlank() || valor.contains("${") ? null : valor.strip();
	}

	/** Lee el primer archivo que exista; funciona tanto desde GalilEdu/ como desde backend/. */
	private static Properties leer(String... rutas) {
		Properties propiedades = new Properties();
		for (String ruta : rutas) {
			Path archivo = Path.of(ruta);
			if (Files.isRegularFile(archivo)) {
				try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
					propiedades.load(lector);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				break;
			}
		}
		return propiedades;
	}

	private static void intentar(String descripcion, Runnable accion) {
		try {
			accion.run();
			System.out.println("   [FALLO] " + descripcion + ": NO fue rechazado");
		} catch (RuntimeException e) {
			System.out.println("   [OK] " + descripcion + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private static Calificacion vigesimal(String valor) {
		return Calificacion.vigesimal(new BigDecimal(valor));
	}

	private static int aleatorio(int desde, int hasta) {
		return ThreadLocalRandom.current().nextInt(desde, hasta + 1);
	}

	private static void titulo(String texto) {
		System.out.println();
		System.out.println("== " + texto);
	}

	private record Datos(String docente, String estudiante, UUID carga, UUID competencia, UUID periodo, UUID matricula) {}
}
