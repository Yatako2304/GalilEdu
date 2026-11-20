package com.galiledu.asistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.galiledu.configuracion.PruebaIntegracionConfiguracion;
import com.galiledu.configuracion.RelojPrueba;

/**
 * HU-67 · RF-145: justificación de faltas y tardanzas. Datos de prueba: María Fernández (U20260004) es tutora de 1.°
 * de secundaria A de 2026. El martes 15/09/2026 se registra: 1.° presente, 2.° tardanza (08:10) y 3.° falta.
 */
class JustificacionIT extends PruebaIntegracionConfiguracion {
	private static final String TUTORA = DOCENTE;
	private static final String HOY = "2026-09-15";
	private static final byte[] PDF = "%PDF-1.4 descanso médico".getBytes();

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private RelojPrueba reloj;

	private String seccion;
	private List<String> matriculas;
	private String token;

	@BeforeEach
	void registrarElDia() throws Exception {
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
		token = token(TUTORA);
		mvc.perform(conToken(put(dia(HOY)), token, """
				{"version":null,"asistencias":[{"matriculaId":"%s","estado":"PRESENTE"},
				{"matriculaId":"%s","estado":"TARDANZA","horaLlegada":"08:10"},
				{"matriculaId":"%s","estado":"FALTA"}]}""".formatted(matriculas.get(0), matriculas.get(1),
				matriculas.get(2)))).andExpect(status().isOk());
	}

	@Test
	void justificaUnaFaltaConMotivoDetalleYSustento() throws Exception {
		mvc.perform(justificar(HOY, matriculas.get(2), "CITA_MEDICA", "Reposo médico por indicación del pediatra.",
				new MockMultipartFile("archivo", "descanso.pdf", "application/pdf", PDF)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estudiantes[2].estado").value("FALTA"))
				.andExpect(jsonPath("$.estudiantes[2].justificacion.motivo").value("CITA_MEDICA"))
				.andExpect(jsonPath("$.estudiantes[2].justificacion.detalle")
						.value("Reposo médico por indicación del pediatra."))
				.andExpect(jsonPath("$.estudiantes[2].justificacion.archivoNombre").value("descanso.pdf"))
				.andExpect(jsonPath("$.estudiantes[2].justificacion.registradaPor").value("María Fernández"))
				.andExpect(jsonPath("$.estudiantes[0].justificacion").value(nullValue()));

		byte[] descargado = mvc.perform(conToken(get(dia(HOY) + "/justificaciones/{m}/sustento", matriculas.get(2)),
				token))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/pdf"))
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(descargado).isEqualTo(PDF);
	}

	@Test
	void unaTardanzaSeJustificaSinSustento() throws Exception {
		mvc.perform(justificar(HOY, matriculas.get(1), "TRAMITE_OFICIAL", null, null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estudiantes[1].justificacion.motivo").value("TRAMITE_OFICIAL"))
				.andExpect(jsonPath("$.estudiantes[1].justificacion.archivoNombre").value(nullValue()));
		mvc.perform(conToken(get(dia(HOY) + "/justificaciones/{m}/sustento", matriculas.get(1)), token))
				.andExpect(status().isNotFound());
	}

	@Test
	void unPresenteNoSeJustifica() throws Exception {
		mvc.perform(justificar(HOY, matriculas.get(0), "ENFERMEDAD", null, null))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Solo se justifica una falta o una tardanza."));
	}

	@Test
	void elMotivoEsObligatorioYOtroPideDetalle() throws Exception {
		mvc.perform(justificar(HOY, matriculas.get(2), null, "Sin motivo", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige el motivo de la justificación."));
		mvc.perform(justificar(HOY, matriculas.get(2), "OTRO", "  ", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Describe el motivo en el detalle."));
	}

	@Test
	void elSustentoDebeSerPdfOImagenDeHasta5Mb() throws Exception {
		mvc.perform(justificar(HOY, matriculas.get(2), "ENFERMEDAD", null,
				new MockMultipartFile("archivo", "video.mp4", "video/mp4", new byte[]{1, 2, 3})))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El sustento debe ser PDF, JPG o PNG."));
		mvc.perform(justificar(HOY, matriculas.get(2), "ENFERMEDAD", null,
				new MockMultipartFile("archivo", "foto.png", "image/png", new byte[5 * 1024 * 1024 + 1])))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El sustento no puede pesar más de 5 MB."));
	}

	@Test
	void noSeJustificaDosVeces() throws Exception {
		mvc.perform(justificar(HOY, matriculas.get(2), "ENFERMEDAD", null, null)).andExpect(status().isOk());
		mvc.perform(justificar(HOY, matriculas.get(2), "CITA_MEDICA", null, null))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Esta falta o tardanza ya está justificada."));
	}

	@Test
	void seJustificaUnDiaPasado() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 9, 22));
		mvc.perform(justificar(HOY, matriculas.get(2), "ENFERMEDAD", "Gripe", null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.editable").value(false))
				.andExpect(jsonPath("$.estudiantes[2].justificacion.motivo").value("ENFERMEDAD"));
	}

	@Test
	void unaFaltaJustificadaYaNoSeCorrige() throws Exception {
		mvc.perform(justificar(HOY, matriculas.get(2), "ENFERMEDAD", null, null)).andExpect(status().isOk());
		mvc.perform(conToken(put(dia(HOY)), token, """
				{"version":0,"asistencias":[{"matriculaId":"%s","estado":"PRESENTE"},
				{"matriculaId":"%s","estado":"TARDANZA","horaLlegada":"08:10"},
				{"matriculaId":"%s","estado":"PRESENTE"}]}""".formatted(matriculas.get(0), matriculas.get(1),
				matriculas.get(2))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("No puedes corregir una falta o tardanza que ya está justificada."));
	}

	@Test
	void unDiaSinRegistroNoTieneNadaQueJustificar() throws Exception {
		mvc.perform(justificar("2026-09-14", matriculas.get(2), "ENFERMEDAD", null, null))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No hay asistencia registrada ese día."));
	}

	@Test
	void otroRolNoJustifica() throws Exception {
		MockMultipartHttpServletRequestBuilder peticion = multipart(dia(HOY) + "/justificaciones");
		peticion.param("matriculaId", matriculas.get(2)).param("motivo", "ENFERMEDAD");
		mvc.perform(conToken(peticion, token(PERSONAL_ADMINISTRATIVO))).andExpect(status().isForbidden());
	}

	private MockMultipartHttpServletRequestBuilder justificar(String fecha, String matricula, String motivo,
			String detalle, MockMultipartFile archivo) {
		MockMultipartHttpServletRequestBuilder peticion = multipart(dia(fecha) + "/justificaciones");
		if (archivo != null) {
			peticion.file(archivo);
		}
		peticion.param("matriculaId", matricula);
		if (motivo != null) {
			peticion.param("motivo", motivo);
		}
		if (detalle != null) {
			peticion.param("detalle", detalle);
		}
		return conToken(peticion, token);
	}

	private String dia(String fecha) {
		return "/asistencia/secciones/" + seccion + "/dias/" + fecha;
	}
}
