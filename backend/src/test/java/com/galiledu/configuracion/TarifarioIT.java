package com.galiledu.configuracion;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * HU-34: GET y confirmar del tarifario (P19) por HTTP, y permisos. Datos de prueba: 2026 ACTIVO con tarifario
 * CONFIRMADO
 * y el compromiso de Sofía; 2027 PLANIFICADO con tarifario EN_CONFIGURACION.
 */
class TarifarioIT extends PruebaIntegracion {
	private static final String ID_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ID_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String RUTA = "/configuracion/tarifarios";

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private static String cuerpo(String ingreso, String matricula, String pension, String gracia, String recargo,
			String tope) {
		return """
				{"montoCuotaIngreso":%s,"montoMatricula":%s,"montoPension":%s,"diasGracia":%s,"recargoPorcentaje":%s,"topeRecargoPorcentaje":%s}"""
				.formatted(ingreso, matricula, pension, gracia, recargo, tope);
	}

	private static String cuerpoValido() {
		return cuerpo("400", "460", "650.50", "7", "3", "12");
	}

	private String anioSinTarifario(int anio, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (:id, :anio, :inicio, :fin, 'TRIMESTRAL', :estado)""")
				.param("id", id).param("anio", anio).param("inicio", LocalDate.of(anio, 3, 1))
				.param("fin", LocalDate.of(anio, 12, 15)).param("estado", estado).update();
		entityManager.clear();
		return id.toString();
	}

	// --- GET ---

	@Test
	void elTarifario2027EstaEnConfiguracionConSuCalendario() throws Exception {
		mvc.perform(conToken(get(RUTA).param("anioEscolarId", ID_2027), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2027))
				.andExpect(jsonPath("$.existe").value(true))
				.andExpect(jsonPath("$.estado").value("EN_CONFIGURACION"))
				.andExpect(jsonPath("$.soloLectura").value(false))
				.andExpect(jsonPath("$.motivoSoloLectura").value(nullValue()))
				.andExpect(jsonPath("$.calendarioDesactualizado").value(false))
				.andExpect(jsonPath("$.conceptos.montoCuotaIngreso").value(500.0))
				.andExpect(jsonPath("$.conceptos.montoMatricula").value(450.0))
				.andExpect(jsonPath("$.conceptos.vencimientoMatricula").value("2027-02-28"))
				.andExpect(jsonPath("$.conceptos.montoPension").value(620.0))
				.andExpect(jsonPath("$.conceptos.cantidadCuotas").value(10))
				.andExpect(jsonPath("$.morosidad.diasGracia").value(5))
				.andExpect(jsonPath("$.morosidad.recargoPorcentaje").value(2.0))
				.andExpect(jsonPath("$.morosidad.topeRecargoPorcentaje").value(10.0))
				.andExpect(jsonPath("$.calendario.cuotas.length()").value(10))
				.andExpect(jsonPath("$.calendario.cuotas[0].numero").value(1))
				.andExpect(jsonPath("$.calendario.cuotas[0].nombre").value("Pensión de marzo"))
				.andExpect(jsonPath("$.calendario.cuotas[0].fechaVencimiento").value("2027-03-31"))
				.andExpect(jsonPath("$.calendario.cuotas[0].monto").value(620.0))
				.andExpect(jsonPath("$.calendario.cuotas[9].fechaVencimiento").value("2027-12-15"))
				.andExpect(jsonPath("$.calendario.totalPensiones").value(6200.0));
	}

	@Test
	void elTarifario2026EsDeSoloLecturaPorTenerCompromisos() throws Exception {
		mvc.perform(conToken(get(RUTA).param("anioEscolarId", ID_2026), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"))
				.andExpect(jsonPath("$.soloLectura").value(true))
				.andExpect(jsonPath("$.motivoSoloLectura").value(
						"Ya se generaron compromisos de pago 2026: los montos y el calendario no se pueden modificar."))
				.andExpect(jsonPath("$.calendarioDesactualizado").value(false))
				.andExpect(jsonPath("$.calendario.totalPensiones").value(6000.0));
	}

	@Test
	void unAnioSinTarifarioDevuelveUnaPropuestaConLosValoresDelAnioAnterior() throws Exception {
		String id2028 = anioSinTarifario(2028, "PLANIFICADO");

		mvc.perform(conToken(get(RUTA).param("anioEscolarId", id2028), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.existe").value(false))
				.andExpect(jsonPath("$.estado").value("EN_CONFIGURACION"))
				.andExpect(jsonPath("$.conceptos.montoPension").value(620.0))
				.andExpect(jsonPath("$.conceptos.vencimientoMatricula").value("2028-02-29"))
				.andExpect(jsonPath("$.calendario.cuotas.length()").value(10));
	}

	@Test
	void unAnioSinTarifarioNiAnioAnteriorDevuelveLaPropuestaVacia() throws Exception {
		String id2030 = anioSinTarifario(2030, "PLANIFICADO");

		mvc.perform(conToken(get(RUTA).param("anioEscolarId", id2030), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.existe").value(false))
				.andExpect(jsonPath("$.conceptos.montoPension").value(nullValue()))
				.andExpect(jsonPath("$.morosidad.diasGracia").value(nullValue()))
				.andExpect(jsonPath("$.calendario.cuotas.length()").value(10))
				.andExpect(jsonPath("$.calendario.totalPensiones").value(nullValue()));
	}

	@Test
	void unAnioCerradoEsDeSoloLecturaAunqueNoTengaTarifario() throws Exception {
		String id2025 = anioSinTarifario(2025, "CERRADO");

		mvc.perform(conToken(get(RUTA).param("anioEscolarId", id2025), personal))
				.andExpect(jsonPath("$.soloLectura").value(true))
				.andExpect(jsonPath("$.motivoSoloLectura")
						.value("Año escolar cerrado: el tarifario se conserva como registro histórico."));
	}

	@Test
	void elTarifarioDeUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(get(RUTA).param("anioEscolarId", "00000000-0000-0000-0000-000000000000"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	// --- POST /{anioEscolarId}/confirmar ---

	@Test
	void confirmaElTarifarioGuardaLosDatosYQuedaConfirmado() throws Exception {
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/confirmar"), personal, cuerpoValido()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.existe").value(true))
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"))
				.andExpect(jsonPath("$.conceptos.montoCuotaIngreso").value(400.0))
				.andExpect(jsonPath("$.conceptos.montoMatricula").value(460.0))
				.andExpect(jsonPath("$.conceptos.montoPension").value(650.5))
				.andExpect(jsonPath("$.morosidad.diasGracia").value(7))
				.andExpect(jsonPath("$.calendario.cuotas.length()").value(10))
				.andExpect(jsonPath("$.calendario.totalPensiones").value(6505.0));

		mvc.perform(conToken(get(RUTA).param("anioEscolarId", ID_2027), personal))
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"))
				.andExpect(jsonPath("$.conceptos.montoPension").value(650.5));
	}

	@Test
	void confirmarDosVecesRegeneraElCalendarioSinChocarConLosUnique() throws Exception {
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/confirmar"), personal, cuerpoValido()))
				.andExpect(status().isOk());
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/confirmar"), personal,
				cuerpo("null", "450", "620", "5", "2", "10")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"))
				.andExpect(jsonPath("$.conceptos.montoCuotaIngreso").value(nullValue()))
				.andExpect(jsonPath("$.calendario.cuotas.length()").value(10));
	}

	@Test
	void confirmarCreaElTarifarioDeUnAnioQueNoLoTenia() throws Exception {
		String id2028 = anioSinTarifario(2028, "PLANIFICADO");

		mvc.perform(conToken(post(RUTA + "/" + id2028 + "/confirmar"), personal, cuerpoValido()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.existe").value(true))
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"));
		mvc.perform(conToken(get(RUTA).param("anioEscolarId", id2028), personal))
				.andExpect(jsonPath("$.existe").value(true));
	}

	@Test
	void confirmarRechazaLosDatosInvalidosSinTocarElTarifario() throws Exception {
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/confirmar"), personal,
				cuerpo("500", "450", "620", "5", "2", "null")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa el tope de recargo."));
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/confirmar"), personal,
				cuerpo("500", "450", "620", "61", "2", "10")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Los días de gracia deben estar entre 0 y 60."));
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/confirmar"), personal, "{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa el monto de la matrícula."));

		mvc.perform(conToken(get(RUTA).param("anioEscolarId", ID_2027), personal))
				.andExpect(jsonPath("$.estado").value("EN_CONFIGURACION"))
				.andExpect(jsonPath("$.calendario.cuotas.length()").value(10));
	}

	@Test
	void confirmarUnTarifarioConCompromisosDa409() throws Exception {
		mvc.perform(conToken(post(RUTA + "/" + ID_2026 + "/confirmar"), personal, cuerpoValido()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Ya se generaron compromisos de pago 2026: los montos y el calendario no se pueden modificar."));
	}

	@Test
	void confirmarUnAnioCerradoDa409() throws Exception {
		String id2025 = anioSinTarifario(2025, "CERRADO");

		mvc.perform(conToken(post(RUTA + "/" + id2025 + "/confirmar"), personal, cuerpoValido()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Año escolar cerrado: el tarifario se conserva como registro histórico."));
	}

	@Test
	void confirmarEnUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(post(RUTA + "/00000000-0000-0000-0000-000000000000/confirmar"), personal, cuerpoValido()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	// --- Editar las fechas del año (HU-74) regenera el calendario ---

	@Test
	void editarLasFechasDelAnioRegeneraElCalendarioDelTarifario() throws Exception {
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/confirmar"), personal, cuerpoValido()))
				.andExpect(status().isOk());

		mvc.perform(conToken(put("/configuracion/anios-escolares/" + ID_2027), personal,
				"""
						{"fechaInicio":"2027-04-05","fechaFin":"2027-11-26"}"""))
				.andExpect(status().isOk());

		mvc.perform(conToken(get(RUTA).param("anioEscolarId", ID_2027), personal))
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"))
				.andExpect(jsonPath("$.conceptos.cantidadCuotas").value(8))
				.andExpect(jsonPath("$.conceptos.vencimientoMatricula").value("2027-03-31"))
				.andExpect(jsonPath("$.conceptos.montoPension").value(650.5))
				.andExpect(jsonPath("$.calendario.cuotas[0].nombre").value("Pensión de abril"))
				.andExpect(jsonPath("$.calendario.cuotas[7].fechaVencimiento").value("2027-11-15"))
				.andExpect(jsonPath("$.calendarioDesactualizado").value(false));
	}

	// --- Permisos ---

	@Test
	void soloElPersonalAdministrativoUsaElTarifario() throws Exception {
		for (String usuario : new String[]{DOCENTE, ADMIN}) {
			String token = token(usuario);
			mvc.perform(conToken(get(RUTA).param("anioEscolarId", ID_2027), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/confirmar"), token, cuerpoValido()))
					.andExpect(status().isForbidden());
		}
		mvc.perform(get(RUTA).param("anioEscolarId", ID_2027)).andExpect(status().isUnauthorized());
		mvc.perform(post(RUTA + "/" + ID_2027 + "/confirmar").contentType("application/json").content(cuerpoValido()))
				.andExpect(status().isUnauthorized());
	}
}
