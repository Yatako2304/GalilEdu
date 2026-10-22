package com.galiledu.configuracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * HU-74 · "Modal - Editar periodo": una prueba por regla de edición. El reloj está fijo en 15/09/2026: el III Bimestre
 * de 2026 está en curso, el I y el II terminaron y el IV no empieza hasta el 19/10.
 */
class PeriodoAcademicoIT extends PruebaIntegracionConfiguracion {
	private static final String ID_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ID_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String RUTA = "/configuracion/periodos-academicos";
	private static final String RUTA_ANIOS = "/configuracion/anios-escolares";
	private static final String INEXISTENTE = "00000000-0000-0000-0000-000000000000";

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private RelojPrueba reloj;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	/** Id del periodo {@code numero} (desde 1) del año, tomado del detalle. */
	private String periodo(String anioId, int numero) throws Exception {
		String detalle = mvc.perform(conToken(get(RUTA_ANIOS + "/" + anioId), personal)).andReturn().getResponse()
				.getContentAsString();
		return JsonPath.read(detalle, "$.periodos[" + (numero - 1) + "].id");
	}

	private static String cuerpo(String nombre, String inicio, String fin, String limite) {
		return """
				{"nombre":"%s","fechaInicio":"%s","fechaFin":"%s","fechaLimiteNotas":"%s"}"""
				.formatted(nombre, inicio, fin, limite);
	}

	private ResultActions editar(String periodoId, String cuerpo) throws Exception {
		return mvc.perform(conToken(put(RUTA + "/" + periodoId), personal, cuerpo));
	}

	@Test
	void editaUnPeriodoFuturoYDevuelveElPeriodoConSuEstadoCalculado() throws Exception {
		String segundo = periodo(ID_2027, 2);

		editar(segundo, cuerpo("II Trimestre", "2027-06-09", "2027-09-06", "2027-09-20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(segundo))
				.andExpect(jsonPath("$.nombre").value("II Trimestre"))
				.andExpect(jsonPath("$.fechaInicio").value("2027-06-09"))
				.andExpect(jsonPath("$.fechaFin").value("2027-09-06"))
				.andExpect(jsonPath("$.fechaLimiteNotas").value("2027-09-20"))
				.andExpect(jsonPath("$.duracionSemanas").value(13))
				.andExpect(jsonPath("$.estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$.registroNotasAbierto").value(false));
	}

	@Test
	void elCambioQuedaGuardadoEnElDetalleDelAnio() throws Exception {
		String segundo = periodo(ID_2027, 2);
		editar(segundo, cuerpo("Segundo Trimestre", "2027-06-09", "2027-09-06", "2027-09-20"))
				.andExpect(status().isOk());

		mvc.perform(conToken(get(RUTA_ANIOS + "/" + ID_2027), personal))
				.andExpect(jsonPath("$.periodos[1].nombre").value("Segundo Trimestre"))
				.andExpect(jsonPath("$.periodos[1].fechaLimiteNotas").value("2027-09-20"));
	}

	@Test
	void unPeriodoQueYaEmpezoNoCambiaSuFechaDeInicio() throws Exception {
		String tercero = periodo(ID_2026, 3); // 03/08 al 09/10

		editar(tercero, cuerpo("III Bimestre", "2026-08-04", "2026-10-09", "2026-10-16"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El periodo ya empezó: no se puede cambiar."));
	}

	@Test
	void unPeriodoQueYaEmpezoSiCambiaSuFinYSuFechaLimite() throws Exception {
		String tercero = periodo(ID_2026, 3);

		editar(tercero, cuerpo("III Bimestre", "2026-08-03", "2026-10-12", "2026-10-19"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fechaFin").value("2026-10-12"))
				.andExpect(jsonPath("$.estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.registroNotasAbierto").value(true));
	}

	@Test
	void unPeriodoQueYaTerminoSoloCambiaLaFechaLimiteDeNotas() throws Exception {
		String primero = periodo(ID_2026, 1); // 02/03 al 08/05

		editar(primero, cuerpo("Primer Bimestre", "2026-03-02", "2026-05-08", "2026-05-15"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("El periodo ya terminó: solo se puede cambiar la fecha límite de notas."));
		editar(primero, cuerpo("I Bimestre", "2026-03-02", "2026-05-09", "2026-05-15"))
				.andExpect(status().isConflict());
	}

	@Test
	void ampliarLaFechaLimiteDeUnPeriodoTerminadoReabreElRegistroDeNotas() throws Exception {
		String primero = periodo(ID_2026, 1);
		mvc.perform(conToken(get(RUTA_ANIOS + "/" + ID_2026), personal))
				.andExpect(jsonPath("$.periodos[0].registroNotasAbierto").value(false));

		editar(primero, cuerpo("I Bimestre", "2026-03-02", "2026-05-08", "2026-10-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CERRADO"))
				.andExpect(jsonPath("$.fechaLimiteNotas").value("2026-10-31"))
				.andExpect(jsonPath("$.registroNotasAbierto").value(true));
		mvc.perform(conToken(get(RUTA_ANIOS + "/" + ID_2026), personal))
				.andExpect(jsonPath("$.periodos[0].registroNotasAbierto").value(true));
	}

	@Test
	void elPlazoAmpliadoDeUnPeriodoTerminadoApareceEnLaCardDelAnioActual() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 10, 21)); // el III terminó el 09/10; el IV está en curso
		String tercero = periodo(ID_2026, 3);

		editar(tercero, cuerpo("III Bimestre", "2026-08-03", "2026-10-09", "2026-10-27")).andExpect(status().isOk());

		mvc.perform(conToken(get(RUTA_ANIOS + "/actual"), personal))
				.andExpect(jsonPath("$.periodoEnCurso.nombre").value("IV Bimestre"))
				.andExpect(jsonPath("$.periodoNotasAbiertas.nombre").value("III Bimestre"))
				.andExpect(jsonPath("$.periodoNotasAbiertas.fechaLimiteNotas").value("2026-10-27"))
				.andExpect(jsonPath("$.periodoNotasAbiertas.diasParaLimiteNotas").value(6))
				.andExpect(jsonPath("$.proximoPeriodo").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void unAnioCerradoNoEditaSusPeriodos() throws Exception {
		String primero = periodo(ID_2026, 1);
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE id = ?::uuid").param(ID_2026)
				.update();
		// El GET de arriba ya cargó el año como ACTIVO en esta transacción; hay que olvidarlo para que se relea.
		entityManager.clear();

		editar(primero, cuerpo("I Bimestre", "2026-03-02", "2026-05-08", "2026-10-31"))
				.andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.message").value("El año escolar está cerrado: no se puede editar sus periodos."));
	}

	@Test
	void elNombreEsObligatorio() throws Exception {
		String segundo = periodo(ID_2027, 2);

		editar(segundo, cuerpo("", "2027-06-08", "2027-09-07", "2027-09-12"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa el nombre del periodo."));
		editar(segundo,
				"{\"fechaInicio\":\"2027-06-08\",\"fechaFin\":\"2027-09-07\",\"fechaLimiteNotas\":\"2027-09-12\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa el nombre del periodo."));
	}

	@Test
	void elNombreNoPuedePasarDe30Caracteres() throws Exception {
		editar(periodo(ID_2027, 2), cuerpo("x".repeat(31), "2027-06-08", "2027-09-07", "2027-09-12"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El nombre del periodo no puede pasar de 30 caracteres."));
	}

	@Test
	void lasFechasSonObligatorias() throws Exception {
		editar(periodo(ID_2027, 2), "{\"nombre\":\"II Trimestre\",\"fechaInicio\":\"2027-06-08\"}")
				.andExpect(status().isBadRequest());
	}

	@Test
	void lasFechasDelPeriodoDebenEstarDentroDelAnio() throws Exception {
		String mensaje = "Las fechas del periodo deben estar dentro del año escolar (01/03/2027 al 17/12/2027).";

		editar(periodo(ID_2027, 1), cuerpo("I Trimestre", "2027-02-28", "2027-05-31", "2027-06-05"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(mensaje));
		editar(periodo(ID_2027, 3), cuerpo("III Trimestre", "2027-09-15", "2027-12-18", "2027-12-22"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(mensaje));
	}

	@Test
	void unPeriodoNoPuedeCruzarseConOtro() throws Exception {
		// Figma: "Se cruza con el IV Bimestre, que empieza el 19/10/2026."
		editar(periodo(ID_2026, 3), cuerpo("III Bimestre", "2026-08-03", "2026-10-20", "2026-10-27"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Se cruza con el IV Bimestre, que empieza el 19/10/2026."));
		editar(periodo(ID_2026, 4), cuerpo("IV Bimestre", "2026-10-05", "2026-12-18", "2026-12-23"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Se cruza con el III Bimestre, que empieza el 03/08/2026."));
	}

	@Test
	void laFechaDeFinDebeSerPosteriorALaDeInicio() throws Exception {
		editar(periodo(ID_2027, 2), cuerpo("II Trimestre", "2027-07-01", "2027-07-01", "2027-07-10"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La fecha de fin debe ser posterior a la fecha de inicio."));
	}

	@Test
	void laFechaLimiteNoPuedeSerAnteriorAlFinDelPeriodo() throws Exception {
		editar(periodo(ID_2027, 2), cuerpo("II Trimestre", "2027-06-08", "2027-09-07", "2027-09-06"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("La fecha límite de notas no puede ser anterior al fin del periodo."));
	}

	@Test
	void laFechaLimiteSiPuedeCaerDentroDelPeriodoSiguiente() throws Exception {
		// El II Trimestre empieza el 08/06/2027: el límite del I el 10/06 cae dentro de él.
		editar(periodo(ID_2027, 1), cuerpo("I Trimestre", "2027-03-01", "2027-05-31", "2027-06-10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fechaLimiteNotas").value("2027-06-10"));
	}

	@Test
	void editarUnPeriodoQueNoExisteDa404() throws Exception {
		editar(INEXISTENTE, cuerpo("I Trimestre", "2027-03-01", "2027-05-31", "2027-06-05"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El periodo académico no existe."));
	}

	@Test
	void soloElPersonalAdministrativoEditaPeriodos() throws Exception {
		String periodo = periodo(ID_2027, 2);
		String cuerpo = cuerpo("II Trimestre", "2027-06-08", "2027-09-07", "2027-09-12");
		for (String usuario : new String[]{DOCENTE, ADMIN}) {
			mvc.perform(conToken(put(RUTA + "/" + periodo), token(usuario), cuerpo)).andExpect(status().isForbidden());
		}
		mvc.perform(put(RUTA + "/" + periodo).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isUnauthorized());
	}
}
