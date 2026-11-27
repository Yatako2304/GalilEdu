package com.galiledu.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.configuracion.PruebaIntegracionConfiguracion;
import com.galiledu.configuracion.RelojPrueba;

/**
 * Retiro del alumno del colegio (fila 36 del plan de pruebas). Datos de prueba: Sofía Paredes, matriculada en 2026
 * (MAT-2026-0001, confirmada el 12/02/2026) con compromiso; septiembre vencida y octubre a diciembre pendientes.
 * Valeria Sánchez: matriculada en 2026 sin compromiso. El reloj está en el 01/11/2026.
 */
class RetiroAlumnoIT extends PruebaIntegracionConfiguracion {
	private static final String SOFIA = "20000000-0000-0000-0000-000000000002";
	private static final String MATRICULA_SOFIA = "50000000-0000-0000-0000-000000000001";
	private static final String VALERIA = "20000000-0000-0000-0000-000000000003";
	private static final String APODERADO = "U20260003";

	@Autowired
	private RelojPrueba reloj;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager em;

	private String token;

	@BeforeEach
	void prepararDia() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 11, 1));
		token = token(PERSONAL_ADMINISTRATIVO);
	}

	private org.springframework.test.web.servlet.ResultActions retirar(String estudianteId, String cuerpo)
			throws Exception {
		return mvc.perform(conToken(post("/matricula/matriculas/de-estudiante/{id}/retiro", estudianteId), token,
				cuerpo));
	}

	private String uno(String sql, String id) {
		em.flush();
		return jdbc.sql(sql).param(id).query(String.class).single();
	}

	@DisplayName("El retiro deja la matrícula y al estudiante retirados y anula las pensiones futuras")
	@Test
	void elRetiroDejaLaMatriculaYAlEstudianteRetiradosYAnulaLasPensionesFuturas() throws Exception {
		retirar(SOFIA, "{\"fechaRetiro\":\"2026-10-15\",\"motivo\":\"Traslado a otra ciudad\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matriculaId").value(MATRICULA_SOFIA))
				.andExpect(jsonPath("$.codigo").value("MAT-2026-0001"))
				// Octubre, noviembre y diciembre vencen después del 15/10; septiembre ya estaba vencida.
				.andExpect(jsonPath("$.cuotasAnuladas").value(3));

		assertThat(uno("SELECT estado || '|' || fecha_retiro || '|' || motivo_retiro FROM matricula.matricula"
				+ " WHERE id = ?::uuid", MATRICULA_SOFIA)).isEqualTo("RETIRADO|2026-10-15|Traslado a otra ciudad");
		assertThat(uno("SELECT estado FROM estudiantes.estudiante WHERE id = ?::uuid", SOFIA)).isEqualTo("RETIRADO");
		assertThat(uno("""
				SELECT q.estado FROM pagos.cuota q JOIN pagos.compromiso_pago c ON c.id = q.compromiso_id
				WHERE c.matricula_id = ?::uuid AND q.nombre = 'Pensión de septiembre'""", MATRICULA_SOFIA))
				.isEqualTo("PENDIENTE");

		// El detalle de la matrícula muestra el retiro y ya no ofrece retirar.
		mvc.perform(conToken(get("/matricula/matriculas/{id}", MATRICULA_SOFIA), token))
				.andExpect(jsonPath("$.estado").value("RETIRADO"))
				.andExpect(jsonPath("$.fechaRetiro").value("2026-10-15"))
				.andExpect(jsonPath("$.motivoRetiro").value("Traslado a otra ciudad"))
				.andExpect(jsonPath("$.puedeRetirar").value(false));
		// Un segundo retiro no encuentra matrícula vigente.
		retirar(SOFIA, "{\"fechaRetiro\":\"2026-10-15\",\"motivo\":\"Otra vez\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("matrícula vigente")));
	}

	@DisplayName("Las notas del retirado se siguen viendo en la libreta")
	@Test
	void lasNotasDelRetiradoSeSiguenViendoEnLaLibreta() throws Exception {
		retirar(SOFIA, "{\"fechaRetiro\":\"2026-10-15\",\"motivo\":\"Traslado\"}").andExpect(status().isOk());

		mvc.perform(conToken(get("/calificaciones/reportes/matriculas/{id}/notas", MATRICULA_SOFIA),
				token(COORDINADOR)))
				.andExpect(status().isOk());
		mvc.perform(conToken(get("/calificaciones/apoderado/estudiantes"), token(APODERADO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].estudiante", hasItem("Sofía Paredes Rojas")));
	}

	@DisplayName("Sin compromiso el retiro se registra igual, sin pensiones que anular")
	@Test
	void sinCompromisoElRetiroSeRegistraIgualSinPensionesQueAnular() throws Exception {
		retirar(VALERIA, "{\"fechaRetiro\":\"2026-11-01\",\"motivo\":\"Cambio de colegio\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cuotasAnuladas").value(0));
		assertThat(uno("SELECT estado FROM estudiantes.estudiante WHERE id = ?::uuid", VALERIA))
				.isEqualTo("RETIRADO");
	}

	@DisplayName("Pide motivo y una fecha que no sea futura ni anterior a la matrícula")
	@Test
	void pideMotivoYUnaFechaQueNoSeaFuturaNiAnteriorALaMatricula() throws Exception {
		retirar(SOFIA, "{\"fechaRetiro\":\"2026-10-15\",\"motivo\":\" \"}").andExpect(status().isBadRequest());
		retirar(SOFIA, "{\"fechaRetiro\":\"2026-11-02\",\"motivo\":\"Traslado\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("posterior a hoy")));
		retirar(SOFIA, "{\"fechaRetiro\":\"2026-01-10\",\"motivo\":\"Traslado\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("12/02/2026")));
		assertThat(uno("SELECT estado FROM matricula.matricula WHERE id = ?::uuid", MATRICULA_SOFIA))
				.isEqualTo("MATRICULADO");
	}

	@DisplayName("Un estudiante sin matrícula vigente no se puede retirar")
	@Test
	void unEstudianteSinMatriculaVigenteNoSePuedeRetirar() throws Exception {
		retirar(java.util.UUID.randomUUID().toString(), "{\"fechaRetiro\":\"2026-10-15\",\"motivo\":\"Traslado\"}")
				.andExpect(status().isConflict());
	}

	@DisplayName("Solo el Personal administrativo registra retiros")
	@Test
	void soloElPersonalAdministrativoRegistraRetiros() throws Exception {
		for (String usuario : new String[]{APODERADO, DOCENTE, COORDINADOR}) {
			mvc.perform(conToken(post("/matricula/matriculas/de-estudiante/{id}/retiro", SOFIA), token(usuario),
					"{\"fechaRetiro\":\"2026-10-15\",\"motivo\":\"Traslado\"}"))
					.andExpect(status().isForbidden());
		}
	}
}
