package com.galiledu.horarios.infraestructura.persistencia;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.galiledu.horarios.aplicacion.CruceHorarioException;
import com.galiledu.horarios.aplicacion.ConsultarHorarios;
import com.galiledu.horarios.aplicacion.GestionHorarios;
import com.galiledu.horarios.aplicacion.GestionHorarios.SolicitudAsignacion;
import com.galiledu.horarios.aplicacion.HorarioInvalidoException;
import com.galiledu.horarios.dominio.DiaSemana;
import com.galiledu.horarios.dominio.TipoCruceHorario;
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

/**
 * Prueba real en PostgreSQL con los scripts del equipo de BD; todo se revierte al terminar.
 * Crea sus propios datos de configuración y no depende de los inserts de demostración.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GALILEDU_POSTGRES_TEST", matches = "true")
class GestionHorariosPostgresTests {
	@Autowired private GestionHorarios gestion;
	@Autowired private ConsultarHorarios consultas;
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
	void registraDetectaCrucesYConsultaElHorarioVigente() {
		Escenario e = escenario();
		var horarioA = gestion.crearBorrador(e.seccionA);
		var horarioB = gestion.crearBorrador(e.seccionB);
		assertThat(horarioA.version()).isEqualTo(1);
		assertThat(gestion.crearBorrador(e.seccionA).version()).isEqualTo(2);

		var solicitudB = new SolicitudAsignacion(e.cargaB, e.bloque, e.espacio, null, "TEORIA",
			DiaSemana.LUNES, Set.of(e.periodo));
		var registrada = gestion.registrarAsignacion(horarioB.id(), solicitudB);
		assertThat(gestion.listarAsignaciones(horarioB.id())).singleElement()
			.satisfies(a -> assertThat(a.periodos()).containsExactly(e.periodo));

		// El mismo docente y aula, en la sección A, el mismo bloque: dos cruces.
		var solicitudA = new SolicitudAsignacion(e.cargaA, e.bloque, e.espacio, null, "TEORIA",
			DiaSemana.LUNES, Set.of(e.periodo));
		assertThatThrownBy(() -> gestion.registrarAsignacion(horarioA.id(), solicitudA))
			.isInstanceOfSatisfying(CruceHorarioException.class, error ->
				assertThat(error.cruces()).extracting(c -> c.tipo())
					.containsExactly(TipoCruceHorario.DOCENTE, TipoCruceHorario.AULA));

		// Un día libre sí se acepta, y la asignación puede modificarse sin cruzarse consigo misma.
		var libre = gestion.registrarAsignacion(horarioA.id(), new SolicitudAsignacion(e.cargaA, e.bloque,
			e.espacio, null, "TEORIA", DiaSemana.MARTES, Set.of(e.periodo)));
		gestion.modificarAsignacion(libre.id(), new SolicitudAsignacion(e.cargaA, e.bloque, null, null,
			null, DiaSemana.MARTES, Set.of(e.periodo)));

		// Un borrador no se muestra; al quedar vigente aparece en la vista del docente.
		assertThat(consultas.porDocente(e.docente, e.periodo)).isEmpty();
		jdbc.update("UPDATE horarios.horario_seccion SET estado = 'VIGENTE' WHERE id = ?", horarioB.id());
		assertThat(consultas.porDocente(e.docente, e.periodo)).singleElement().satisfies(entrada -> {
			assertThat(entrada.asignacionId()).isEqualTo(registrada.id());
			assertThat(entrada.dia()).isEqualTo(DiaSemana.LUNES);
			assertThat(entrada.ordenBloque()).isEqualTo(1);
		});

		gestion.retirarAsignacion(registrada.id());
		assertThat(consultas.porDocente(e.docente, e.periodo)).isEmpty();
	}

	@Test
	@Transactional
	void subgruposYSusMiembrosDefinenQueVeCadaMatricula() {
		Escenario e = escenario();
		var horario = gestion.crearBorrador(e.seccionA);
		var grupo = gestion.crearSubgrupo(e.seccionA, "Grupo 1");
		gestion.agregarMiembro(grupo.id(), e.matriculaA);
		assertThat(gestion.listarMiembros(grupo.id())).containsExactly(e.matriculaA);
		assertThatThrownBy(() -> gestion.crearSubgrupo(e.seccionA, "Grupo 1"))
			.isInstanceOf(HorarioInvalidoException.class);

		gestion.registrarAsignacion(horario.id(), new SolicitudAsignacion(e.cargaA, e.bloque, null,
			grupo.id(), "LABORATORIO", DiaSemana.LUNES, Set.of(e.periodo)));
		jdbc.update("UPDATE horarios.horario_seccion SET estado = 'VIGENTE' WHERE id = ?", horario.id());

		assertThat(consultas.porMatricula(e.matriculaA, e.periodo)).singleElement()
			.satisfies(entrada -> assertThat(entrada.subgrupo()).isEqualTo("Grupo 1"));
		assertThat(consultas.porMatricula(e.matriculaB, e.periodo)).isEmpty();
		assertThatThrownBy(() -> gestion.desactivarSubgrupo(grupo.id()))
			.isInstanceOf(HorarioInvalidoException.class);

		gestion.retirarMiembro(grupo.id(), e.matriculaA);
		assertThat(consultas.porMatricula(e.matriculaA, e.periodo)).isEmpty();
		gestion.agregarMiembro(grupo.id(), e.matriculaA);
		assertThat(gestion.listarMiembros(grupo.id())).containsExactly(e.matriculaA);
	}

	/** Dos secciones del mismo grado y año con el mismo docente, más un bloque, periodo y aula. */
	private Escenario escenario() {
		var aleatorio = ThreadLocalRandom.current();
		UUID anio = UUID.randomUUID();
		UUID grado = UUID.randomUUID();
		UUID seccionA = UUID.randomUUID();
		UUID seccionB = UUID.randomUUID();
		UUID area = UUID.randomUUID();
		UUID curso = UUID.randomUUID();
		UUID oferta = UUID.randomUUID();
		UUID cargaA = UUID.randomUUID();
		UUID cargaB = UUID.randomUUID();
		UUID estructura = UUID.randomUUID();
		UUID bloque = UUID.randomUUID();
		UUID periodo = UUID.randomUUID();
		UUID espacio = UUID.randomUUID();
		UUID docente = UUID.randomUUID();
		String sufijo = UUID.randomUUID().toString().substring(0, 8);

		jdbc.update("""
			INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen_periodos)
			VALUES (?, ?, DATE '2030-03-01', DATE '2030-12-20', 'BIMESTRAL')
			""", anio, 5000 + aleatorio.nextInt(1_000_000));
		jdbc.update("INSERT INTO configuracion.grado (id, nivel, numero, nombre) VALUES (?, 'PRIMARIA', ?, ?)",
			grado, 1000 + aleatorio.nextInt(1_000_000), "Grado " + sufijo);
		jdbc.update("""
			INSERT INTO configuracion.seccion (id, anio_id, grado_id, nombre, capacidad_maxima)
			VALUES (?, ?, ?, 'A', 30), (?, ?, ?, 'B', 30)
			""", seccionA, anio, grado, seccionB, anio, grado);
		jdbc.update("INSERT INTO configuracion.area_curricular (id, nombre) VALUES (?, ?)", area, "Area " + sufijo);
		jdbc.update("INSERT INTO configuracion.curso (id, area_curricular_id, nombre) VALUES (?, ?, ?)",
			curso, area, "Curso " + sufijo);
		jdbc.update("""
			INSERT INTO configuracion.oferta_curso (id, anio_id, grado_id, curso_id, horas_semanales)
			VALUES (?, ?, ?, ?, 4)
			""", oferta, anio, grado, curso);
		jdbc.update("INSERT INTO configuracion.carga_academica (id, oferta_curso_id, seccion_id, docente_id) VALUES (?, ?, ?, ?)",
			cargaA, oferta, seccionA, docente);
		jdbc.update("INSERT INTO configuracion.carga_academica (id, oferta_curso_id, seccion_id, docente_id) VALUES (?, ?, ?, ?)",
			cargaB, oferta, seccionB, docente);
		jdbc.update("""
			INSERT INTO configuracion.estructura_horaria
			(id, anio_id, codigo, dias_lectivos, bloques_por_dia, duracion_bloque, hora_inicio)
			VALUES (?, ?, ?, ARRAY['LUNES','MARTES']::configuracion.dia_semana[], 7, 45, TIME '08:00')
			""", estructura, anio, sufijo);
		jdbc.update("""
			INSERT INTO configuracion.bloque_horario (id, estructura_horaria_id, orden, hora_inicio, hora_fin)
			VALUES (?, ?, 1, TIME '08:00', TIME '08:45')
			""", bloque, estructura);
		jdbc.update("""
			INSERT INTO configuracion.periodo_academico
			(id, anio_id, nombre, orden, fecha_inicio, fecha_fin, fecha_limite_notas)
			VALUES (?, ?, 'Bimestre 1', 1, DATE '2030-03-01', DATE '2030-05-15', TIMESTAMP '2030-05-20 23:59:00')
			""", periodo, anio);
		jdbc.update("""
			INSERT INTO configuracion.espacio_fisico (id, codigo, nombre, tipo, aforo_maximo)
			VALUES (?, ?, 'Aula de prueba', 'AULA_REGULAR', 30)
			""", espacio, "T" + sufijo);
		return new Escenario(seccionA, seccionB, cargaA, cargaB, bloque, periodo, espacio, docente,
			matricula(anio, seccionA), matricula(anio, seccionB));
	}

	private UUID matricula(UUID anio, UUID seccion) {
		UUID persona = UUID.randomUUID();
		UUID matricula = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO personas.persona (id, tipo_documento, numero_documento, nombres, primer_apellido)
			VALUES (?, 'CARNET_EXTRANJERIA', ?, 'Prueba', 'Horarios')
			""", persona, persona.toString().replace("-", "").substring(0, 12));
		jdbc.update("INSERT INTO personas.estudiante (id) VALUES (?)", persona);
		jdbc.update("""
			INSERT INTO matricula.matricula (id, estudiante_id, anio_id, seccion_id, tipo, estado)
			VALUES (?, ?, ?, ?, 'REGULAR', 'MATRICULADA')
			""", matricula, persona, anio, seccion);
		return matricula;
	}

	private record Escenario(UUID seccionA, UUID seccionB, UUID cargaA, UUID cargaB, UUID bloque,
		UUID periodo, UUID espacio, UUID docente, UUID matriculaA, UUID matriculaB) {}
}
