package com.galiledu.asistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.configuracion.PruebaIntegracionConfiguracion;
import com.galiledu.configuracion.RelojPrueba;

/**
 * HU-65 · RF-143: registro y corrección de la asistencia diaria. Datos de prueba: María Fernández (U20260004) es
 * tutora de 1.° de secundaria A de 2026, con 3 estudiantes MATRICULADO. El reloj está fijo en el martes 15/09/2026
 * (III Bimestre, día lectivo).
 */
class AsistenciaIT extends PruebaIntegracionConfiguracion {
	private static final String TUTORA = DOCENTE;
	private static final String HOY = "2026-09-15";

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private RelojPrueba reloj;

	private String seccion;
	private List<String> matriculas;

	@BeforeEach
	void cargarSeccion() {
		seccion = jdbc.sql("""
				SELECT s.id::text FROM configuracion.seccion s
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				JOIN seguridad.usuario u ON u.id = s.tutor_id
				WHERE a.anio = 2026 AND u.nombre_usuario = ?""").param(TUTORA).query(String.class).single();
		matriculas = jdbc.sql("""
				SELECT m.id::text FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE m.seccion_id = ?::uuid AND m.estado = 'MATRICULADO'
				ORDER BY p.apellido_paterno, p.apellido_materno, p.nombres""").param(seccion).query(String.class)
				.list();
		assertThat(matriculas).hasSize(3);
	}

	@Test
	void laTutoraVeSuSeccion() throws Exception {
		mvc.perform(conToken(get("/asistencia/secciones"), token(TUTORA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$[0].nombre").value("A"))
				.andExpect(jsonPath("$[0].cargo").value("TUTOR"));
	}

	@Test
	void sinRegistrarTodosEstanSinMarcarYSePuedeEditar() throws Exception {
		mvc.perform(conToken(get(dia(HOY)), token(TUTORA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.editable").value(true))
				.andExpect(jsonPath("$.registro").value(nullValue()))
				.andExpect(jsonPath("$.resumen.sinMarcar").value(3))
				.andExpect(jsonPath("$.estudiantes", hasSize(3)))
				.andExpect(jsonPath("$.estudiantes[0].estado").value(nullValue()));
	}

	@Test
	void registraLaAsistenciaConLaHoraDeLaTardanza() throws Exception {
		mvc.perform(conToken(put(dia(HOY)), token(TUTORA), cuerpo(null, "PRESENTE", "TARDANZA", "FALTA")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.registro.registradoPor").value("María Fernández"))
				.andExpect(jsonPath("$.registro.version").value(0))
				.andExpect(jsonPath("$.resumen.presentes").value(1))
				.andExpect(jsonPath("$.resumen.tardanzas").value(1))
				.andExpect(jsonPath("$.resumen.faltas").value(1))
				.andExpect(jsonPath("$.resumen.sinMarcar").value(0))
				.andExpect(jsonPath("$.estudiantes[1].horaLlegada").value("08:10:00"))
				.andExpect(jsonPath("$.estudiantes[0].horaLlegada").value(nullValue()))
				.andExpect(jsonPath("$.estudiantes[0].modificado").value(false));
	}

	@Test
	void noGuardaSiFaltaMarcarAUnEstudiante() throws Exception {
		String cuerpo = """
				{"asistencias":[{"matriculaId":"%s","estado":"PRESENTE"},{"matriculaId":"%s","estado":"PRESENTE"}]}"""
				.formatted(matriculas.get(0), matriculas.get(1));
		mvc.perform(conToken(put(dia(HOY)), token(TUTORA), cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Falta 1 estudiante por marcar."));
	}

	@Test
	void laTardanzaExigeLaHoraDeLlegada() throws Exception {
		String cuerpo = """
				{"asistencias":[{"matriculaId":"%s","estado":"TARDANZA"},{"matriculaId":"%s","estado":"PRESENTE"},
				{"matriculaId":"%s","estado":"PRESENTE"}]}""".formatted(matriculas.get(0), matriculas.get(1),
				matriculas.get(2));
		mvc.perform(conToken(put(dia(HOY)), token(TUTORA), cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Indica la hora de llegada de cada tardanza."));
	}

	@Test
	void rechazaEstudiantesDeOtraSeccion() throws Exception {
		String cuerpo = """
				{"asistencias":[{"matriculaId":"%s","estado":"PRESENTE"},{"matriculaId":"%s","estado":"PRESENTE"},
				{"matriculaId":"30000000-0000-0000-0000-000000000099","estado":"PRESENTE"}]}"""
				.formatted(matriculas.get(0), matriculas.get(1));
		mvc.perform(conToken(put(dia(HOY)), token(TUTORA), cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Uno de los estudiantes no pertenece a la sección."));
	}

	@Test
	void soloSeRegistraLaAsistenciaDeHoy() throws Exception {
		mvc.perform(conToken(put(dia("2026-09-14")), token(TUTORA), cuerpo(null, "PRESENTE", "PRESENTE", "FALTA")))
				.andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.message").value("Solo puedes registrar o corregir la asistencia del día de hoy."));
	}

	@Test
	void unSabadoNoEsDiaDeClases() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 9, 19));
		mvc.perform(conToken(get(dia("2026-09-19")), token(TUTORA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.editable").value(false))
				.andExpect(jsonPath("$.motivoNoEscolar").value("El sábado no es día de clases."));
		mvc.perform(conToken(put(dia("2026-09-19")), token(TUTORA), cuerpo(null, "PRESENTE", "PRESENTE", "FALTA")))
				.andExpect(status().isConflict());
	}

	@Test
	void entreBimestresSonVacaciones() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 10, 13));
		mvc.perform(conToken(get(dia("2026-10-13")), token(TUTORA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.editable").value(false))
				.andExpect(jsonPath("$.motivoNoEscolar")
						.value("La fecha cae entre dos periodos académicos (vacaciones)."));
	}

	@Test
	void corrigeElMismoDiaYDejaHistorial() throws Exception {
		String tutora = token(TUTORA);
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(null, "PRESENTE", "PRESENTE", "FALTA")))
				.andExpect(status().isOk());
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(0, "PRESENTE", "PRESENTE", "TARDANZA")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.registro.version").value(1))
				.andExpect(jsonPath("$.registro.corregidoPor").value("María Fernández"))
				.andExpect(jsonPath("$.estudiantes[2].estado").value("TARDANZA"))
				.andExpect(jsonPath("$.estudiantes[2].modificado").value(true))
				.andExpect(jsonPath("$.estudiantes[0].modificado").value(false));
		assertThat(jdbc.sql("""
				SELECT estado_anterior || '>' || estado_nuevo || ' ' || hora_nueva FROM asistencia.correccion""")
				.query(String.class).list()).containsExactly("FALTA>TARDANZA 08:10:00");
	}

	@Test
	void unaCorreccionConUnaVersionViejaSeRechaza() throws Exception {
		String tutora = token(TUTORA);
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(null, "PRESENTE", "PRESENTE", "FALTA")))
				.andExpect(status().isOk());
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(0, "PRESENTE", "FALTA", "FALTA")))
				.andExpect(status().isOk());
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(0, "FALTA", "FALTA", "FALTA")))
				.andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.message").value("La asistencia cambió mientras la editabas. Vuelve a cargarla."));
	}

	@Test
	void registrarDosVecesSinVersionSeRechaza() throws Exception {
		String tutora = token(TUTORA);
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(null, "PRESENTE", "PRESENTE", "FALTA")))
				.andExpect(status().isOk());
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(null, "PRESENTE", "PRESENTE", "PRESENTE")))
				.andExpect(status().isConflict());
	}

	@Test
	void unaCorreccionSinCambiosNoSeGuarda() throws Exception {
		String tutora = token(TUTORA);
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(null, "PRESENTE", "PRESENTE", "FALTA")))
				.andExpect(status().isOk());
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(0, "PRESENTE", "PRESENTE", "FALTA")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("No hay cambios que guardar."));
	}

	@Test
	void alDiaSiguienteYaNoSePuedeCorregir() throws Exception {
		String tutora = token(TUTORA);
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(null, "PRESENTE", "PRESENTE", "FALTA")))
				.andExpect(status().isOk());
		reloj.fijarEn(LocalDate.of(2026, 9, 16));
		mvc.perform(conToken(get(dia(HOY)), tutora))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.editable").value(false))
				.andExpect(jsonPath("$.estudiantes[2].estado").value("FALTA"));
		mvc.perform(conToken(put(dia(HOY)), tutora, cuerpo(0, "PRESENTE", "PRESENTE", "PRESENTE")))
				.andExpect(status().isConflict());
	}

	@Test
	void unaSeccionAjenaNoSeVe() throws Exception {
		mvc.perform(conToken(get("/asistencia/secciones/{id}/dias/{fecha}", "30000000-0000-0000-0000-000000000099",
				HOY), token(TUTORA)))
				.andExpect(status().isNotFound());
	}

	@Test
	void soloElTutorUsaAsistencia() throws Exception {
		mvc.perform(conToken(get("/asistencia/secciones"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}

	private String dia(String fecha) {
		return "/asistencia/secciones/" + seccion + "/dias/" + fecha;
	}

	/** Marca a los 3 estudiantes, por apellido, con los estados dados; la tardanza llega a las 08:10. */
	private String cuerpo(Integer version, String... estados) {
		StringBuilder json = new StringBuilder("{\"version\":" + version + ",\"asistencias\":[");
		for (int i = 0; i < estados.length; i++) {
			json.append(i == 0 ? "" : ",").append("{\"matriculaId\":\"").append(matriculas.get(i))
					.append("\",\"estado\":\"").append(estados[i]).append('"');
			if ("TARDANZA".equals(estados[i])) {
				json.append(",\"horaLlegada\":\"08:10\"");
			}
			json.append('}');
		}
		return json.append("]}").toString();
	}
}
