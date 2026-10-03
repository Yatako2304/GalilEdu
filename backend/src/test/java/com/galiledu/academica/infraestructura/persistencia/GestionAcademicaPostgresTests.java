package com.galiledu.academica.infraestructura.persistencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.galiledu.academica.aplicacion.AccesoAcademicoDenegadoException;
import com.galiledu.academica.aplicacion.ConsolidacionNotas;
import com.galiledu.academica.aplicacion.ConsultaNotas;
import com.galiledu.academica.aplicacion.CriteriosEvaluacion;
import com.galiledu.academica.aplicacion.CriteriosEvaluacion.DatosItem;
import com.galiledu.academica.aplicacion.RegistroCalificaciones;
import com.galiledu.academica.aplicacion.SolicitudAcademicaInvalidaException;
import com.galiledu.academica.dominio.Calificacion;
import com.galiledu.academica.dominio.EvidenciaPedagogica;
import com.galiledu.academica.dominio.NivelCualitativo;
import com.galiledu.academica.dominio.TipoItemEvaluacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba real en PostgreSQL con datos propios; todos los cambios se revierten al terminar. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GALILEDU_POSTGRES_TEST", matches = "true")
class GestionAcademicaPostgresTests {
	@Autowired private CriteriosEvaluacion criterios;
	@Autowired private RegistroCalificaciones registro;
	@Autowired private ConsolidacionNotas consolidacion;
	@Autowired private ConsultaNotas consulta;
	@Autowired private JdbcOperations jdbc;

	@DynamicPropertySource
	static void postgres(DynamicPropertyRegistry propiedades) {
		propiedades.add("spring.datasource.url", () -> System.getenv("DB_URL"));
		propiedades.add("spring.datasource.username", () -> System.getenv("DB_USER"));
		propiedades.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
		propiedades.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		propiedades.add("spring.jpa.hibernate.ddl-auto", () -> "none");
	}

	@Test
	@Transactional
	void registraCorrigeAuditaConsolidaYConsultaContraElEsquemaReal() {
		var datos = new Escenario();

		var item = criterios.registrarItem(datos.usuarioDocente, datos.carga, datos.competencia, datos.periodo,
			new DatosItem("Examen bimestral", TipoItemEvaluacion.EXAMEN, LocalDate.now(), new BigDecimal("2")));
		assertThat(criterios.listarItems(datos.usuarioDocente, datos.carga, datos.periodo))
			.extracting(i -> i.id()).containsExactly(item.id());

		var primera = registro.registrar(datos.usuarioDocente, item.id(), datos.matricula,
			Calificacion.vigesimal(new BigDecimal("12.5")));
		var corregida = registro.registrar(datos.usuarioDocente, item.id(), datos.matricula,
			Calificacion.vigesimal(new BigDecimal("16")));
		assertThat(corregida.id()).isEqualTo(primera.id());
		registro.adjuntarEvidencia(datos.usuarioDocente, corregida.id(),
			new EvidenciaPedagogica("examen.pdf", "evidencias/examen.pdf", "application/pdf", 1024L));

		assertThat(jdbc.queryForObject("""
			SELECT count(*) FROM seguridad.registro_auditoria
			WHERE modulo = 'GESTION_ACADEMICA' AND entidad = 'Calificacion' AND entidad_id = ?
			""", Integer.class, corregida.id())).isEqualTo(2);
		assertThat(jdbc.queryForObject("""
			SELECT valor_anterior ->> 'valor' FROM seguridad.registro_auditoria
			WHERE entidad_id = ? AND operacion = 'CORREGIR_CALIFICACION'
			""", String.class, corregida.id())).isEqualTo("12.5");

		assertThatThrownBy(() -> registro.registrar(datos.usuarioDocente, item.id(), datos.matricula,
			Calificacion.cualitativa(NivelCualitativo.A))).isInstanceOf(SolicitudAcademicaInvalidaException.class);
		assertThatThrownBy(() -> registro.registrar(datos.usuarioEstudiante, item.id(), datos.matricula,
			Calificacion.vigesimal(new BigDecimal("20")))).isInstanceOf(AccesoAcademicoDenegadoException.class);

		var resultado = consolidacion.consolidarPeriodo(datos.usuarioDocente, datos.carga, datos.competencia,
			datos.periodo);
		assertThat(resultado.calculadas()).isEqualTo(1);

		var notas = consulta.notasDeMatricula(datos.usuarioEstudiante, datos.matricula);
		assertThat(notas.calificaciones()).singleElement()
			.satisfies(c -> assertThat(c.evidencia().nombreArchivo()).isEqualTo("examen.pdf"));
		assertThat(notas.notasPeriodo()).singleElement()
			.satisfies(n -> assertThat(n.nota().valor().vigesimal()).isEqualByComparingTo("16"));
		assertThat(notas.notasFinales()).singleElement()
			.satisfies(n -> assertThat(n.nota().valor().vigesimal()).isEqualByComparingTo("16"));

		assertThatThrownBy(() -> criterios.desactivarItem(datos.usuarioDocente, item.id()))
			.isInstanceOf(SolicitudAcademicaInvalidaException.class);
	}

	/** Crea en la transacción de prueba la cadena mínima persona → carga → matrícula. */
	private final class Escenario {
		final String usuarioDocente;
		final String usuarioEstudiante;
		final UUID carga = UUID.randomUUID();
		final UUID competencia = UUID.randomUUID();
		final UUID periodo = UUID.randomUUID();
		final UUID matricula = UUID.randomUUID();

		Escenario() {
			UUID docente = persona("Docente");
			jdbc.update("INSERT INTO personas.docente (id) VALUES (?)", docente);
			usuarioDocente = usuario(docente);
			UUID estudiante = persona("Estudiante");
			jdbc.update("INSERT INTO personas.estudiante (id) VALUES (?)", estudiante);
			usuarioEstudiante = usuario(estudiante);

			int numero = aleatorio(100_000, 999_999);
			UUID anio = insertar("""
				INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen_periodos, estado)
				VALUES (?, ?, ?, ?, 'BIMESTRAL', 'ACTIVO')
				""", numero, LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(8));
			jdbc.update("""
				INSERT INTO configuracion.periodo_academico
				(id, anio_id, nombre, orden, fecha_inicio, fecha_fin, fecha_limite_notas, estado)
				VALUES (?, ?, 'I Bimestre', 1, ?, ?, ?, 'EN_CURSO')
				""", periodo, anio, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1),
				java.sql.Timestamp.valueOf(LocalDate.now().plusYears(1).atStartOfDay()));
			UUID grado = insertar("""
				INSERT INTO configuracion.grado (id, nivel, numero, nombre) VALUES (?, 'PRIMARIA', ?, 'Grado prueba')
				""", numero);
			UUID seccion = insertar("""
				INSERT INTO configuracion.seccion (id, anio_id, grado_id, nombre, capacidad_maxima)
				VALUES (?, ?, ?, 'A', 30)
				""", anio, grado);
			UUID area = insertar("INSERT INTO configuracion.area_curricular (id, nombre) VALUES (?, ?)",
				"Área prueba " + UUID.randomUUID());
			UUID curso = insertar("INSERT INTO configuracion.curso (id, area_curricular_id, nombre) VALUES (?, ?, 'Curso')",
				area);
			UUID competenciaBase = insertar("""
				INSERT INTO configuracion.competencia (id, area_curricular_id, nombre) VALUES (?, ?, 'Resuelve problemas')
				""", area);
			UUID oferta = insertar("""
				INSERT INTO configuracion.oferta_curso (id, anio_id, grado_id, curso_id, horas_semanales)
				VALUES (?, ?, ?, ?, 4)
				""", anio, grado, curso);
			jdbc.update("""
				INSERT INTO configuracion.configuracion_competencia
				(id, oferta_curso_id, competencia_id, tipo_escala, regla_agregacion, regla_consolidacion)
				VALUES (?, ?, ?, 'VIGESIMAL', 'PROMEDIO_PONDERADO', 'ULTIMO_PERIODO')
				""", competencia, oferta, competenciaBase);
			jdbc.update("""
				INSERT INTO configuracion.carga_academica (id, oferta_curso_id, seccion_id, docente_id)
				VALUES (?, ?, ?, ?)
				""", carga, oferta, seccion, docente);
			jdbc.update("""
				INSERT INTO matricula.matricula (id, estudiante_id, anio_id, seccion_id, tipo, estado)
				VALUES (?, ?, ?, ?, 'REGULAR', 'MATRICULADA')
				""", matricula, estudiante, anio, seccion);
		}

		private UUID persona(String nombres) {
			UUID id = UUID.randomUUID();
			jdbc.update("""
				INSERT INTO personas.persona (id, tipo_documento, numero_documento, nombres, primer_apellido, correo)
				VALUES (?, 'DNI', ?, ?, 'Prueba', ?)
				""", id, String.valueOf(aleatorio(10_000_000, 99_999_999)), nombres, id + "@prueba.test");
			return id;
		}

		private String usuario(UUID personaId) {
			String username = "U2099" + aleatorio(1000, 9999);
			jdbc.update("""
				INSERT INTO seguridad.usuario (id, persona_id, username, password_hash) VALUES (?, ?, ?, 'sin-uso')
				""", UUID.randomUUID(), personaId, username);
			return username;
		}

		private UUID insertar(String sql, Object... valores) {
			UUID id = UUID.randomUUID();
			Object[] argumentos = new Object[valores.length + 1];
			argumentos[0] = id;
			System.arraycopy(valores, 0, argumentos, 1, valores.length);
			jdbc.update(sql, argumentos);
			return id;
		}

		private int aleatorio(int desde, int hasta) {
			return ThreadLocalRandom.current().nextInt(desde, hasta + 1);
		}
	}
}
