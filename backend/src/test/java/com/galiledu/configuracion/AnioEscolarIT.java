package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.jayway.jsonpath.JsonPath;

/**
 * HU-74: listar, ver y crear años escolares con sus periodos, y permisos. El reloj está fijo en 15/09/2026, así que el
 * III Bimestre de 2026 está en curso.
 */
class AnioEscolarIT extends PruebaIntegracionConfiguracion {
	private static final String ID_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ID_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String RUTA = "/configuracion/anios-escolares";

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private RelojPrueba reloj;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private static String solicitud(int anio, String inicio, String fin, String regimen) {
		return """
				{"anio":%d,"fechaInicio":"%s","fechaFin":"%s","regimen":"%s",
				 "escalaEvaluacion":"VIGESIMAL","reglaNota":"PROMEDIO_PONDERADO",
				 "consolidacionPeriodos":"PROMEDIO_PERIODOS"}""".formatted(anio, inicio, fin, regimen);
	}

	@DisplayName("Crea un año bimestral planificado con cuatro períodos")
	@Test
	void creaUnAnioBimestralPlanificadoConCuatroPeriodos() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-03-01", "2028-12-15", "BIMESTRAL")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.anio").value(2028))
				.andExpect(jsonPath("$.estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$.regimen").value("BIMESTRAL"))
				.andExpect(jsonPath("$.periodos.length()").value(4))
				.andExpect(jsonPath("$.periodos[0].numero").value(1))
				.andExpect(jsonPath("$.periodos[0].nombre").value("I Bimestre"))
				.andExpect(jsonPath("$.periodos[0].fechaInicio").value("2028-03-01"))
				.andExpect(jsonPath("$.periodos[0].estado").value("PLANIFICADO"))
				.andExpect(jsonPath("$.periodos[3].nombre").value("IV Bimestre"))
				.andExpect(jsonPath("$.periodos[3].fechaFin").value("2028-12-15"))
				.andExpect(jsonPath("$.periodos[3].fechaLimiteNotas").value("2028-12-20"));
	}

	@DisplayName("Crea un año trimestral con tres períodos")
	@Test
	void creaUnAnioTrimestralConTresPeriodos() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2029, "2029-03-01", "2029-12-14", "TRIMESTRAL")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.periodos.length()").value(3))
				.andExpect(jsonPath("$.periodos[2].nombre").value("III Trimestre"))
				.andExpect(jsonPath("$.periodos[2].fechaFin").value("2029-12-14"));
	}

	@DisplayName("El año creado aparece en el listado y en el detalle")
	@Test
	void elAnioCreadoApareceEnElListadoYEnElDetalle() throws Exception {
		String creado = mvc.perform(conToken(post(RUTA), personal,
				solicitud(2028, "2028-03-01", "2028-12-15", "BIMESTRAL")))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(creado, "$.id");
		// La prueba corre dentro de una transacción que no se confirma, así que el INSERT de JPA sigue pendiente en
		// memoria. El listado lee con JdbcClient (otra consulta SQL) y no lo vería; en producción crear() hace commit.
		entityManager.flush();

		mvc.perform(conToken(get(RUTA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].anio").value(2028))
				.andExpect(jsonPath("$[0].cantidadPeriodos").value(4));
		mvc.perform(conToken(get(RUTA + "/" + id), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodos.length()").value(4));
	}

	@DisplayName("Lista los años de los datos de prueba del más reciente al más antiguo")
	@Test
	void listaLosAniosDeLosDatosDePruebaDelMasRecienteAlMasAntiguo() throws Exception {
		mvc.perform(conToken(get(RUTA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].anio").value(2027))
				.andExpect(jsonPath("$[0].regimen").value("TRIMESTRAL"))
				.andExpect(jsonPath("$[0].cantidadPeriodos").value(3))
				.andExpect(jsonPath("$[1].anio").value(2026))
				.andExpect(jsonPath("$[1].estado").value("ACTIVO"))
				.andExpect(jsonPath("$[1].cantidadPeriodos").value(4));
	}

	@DisplayName("El detalle trae los períodos ordenados por número")
	@Test
	void elDetalleTraeLosPeriodosOrdenadosPorNumero() throws Exception {
		mvc.perform(conToken(get(RUTA + "/" + ID_2026), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.periodos.length()").value(4))
				.andExpect(jsonPath("$.periodos[0].nombre").value("I Bimestre"))
				.andExpect(jsonPath("$.periodos[2].estado").value("EN_CURSO"));
	}

	@DisplayName("El detalle trae los parametros comunes y bloquea los del año que ya empezo")
	@Test
	void elDetalleTraeLosParametrosComunesYBloqueaLosDelAnioQueYaEmpezo() throws Exception {
		mvc.perform(conToken(get(RUTA + "/" + ID_2026), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parametrosEvaluacion.escalaEvaluacion").value("VIGESIMAL"))
				.andExpect(jsonPath("$.parametrosEvaluacion.reglaNota").value("PROMEDIO_PONDERADO"))
				.andExpect(jsonPath("$.parametrosEvaluacion.consolidacionPeriodos").value("PROMEDIO_PERIODOS"))
				.andExpect(jsonPath("$.parametrosEvaluacion.editable").value(false));
	}

	@DisplayName("Configura los parametros antes de que empiece el año")
	@Test
	void configuraLosParametrosAntesDeQueEmpieceElAnio() throws Exception {
		String parametros = """
				{"escalaEvaluacion":"CUALITATIVA","reglaNota":"ULTIMO_NIVEL",
				 "consolidacionPeriodos":"ULTIMO_PERIODO"}""";
		mvc.perform(conToken(put(RUTA + "/" + ID_2027 + "/parametros-evaluacion"), personal, parametros))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.escalaEvaluacion").value("CUALITATIVA"))
				.andExpect(jsonPath("$.reglaNota").value("ULTIMO_NIVEL"))
				.andExpect(jsonPath("$.consolidacionPeriodos").value("ULTIMO_PERIODO"))
				.andExpect(jsonPath("$.editable").value(true));
	}

	@DisplayName("No modifica los parametros cuando el año ya empezo")
	@Test
	void noModificaLosParametrosCuandoElAnioYaEmpezo() throws Exception {
		String parametros = """
				{"escalaEvaluacion":"VIGESIMAL","reglaNota":"PROMEDIO_SIMPLE",
				 "consolidacionPeriodos":"ULTIMO_PERIODO"}""";
		mvc.perform(conToken(put(RUTA + "/" + ID_2026 + "/parametros-evaluacion"), personal, parametros))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Los parámetros de evaluación no se pueden modificar porque el año escolar ya empezó."));
	}

	@DisplayName("Rechaza una regla que no corresponde a la escala del año")
	@Test
	void rechazaUnaReglaQueNoCorrespondeALaEscalaDelAnio() throws Exception {
		String parametros = """
				{"escalaEvaluacion":"CUALITATIVA","reglaNota":"PROMEDIO_SIMPLE",
				 "consolidacionPeriodos":"PROMEDIO_PERIODOS"}""";
		mvc.perform(conToken(put(RUTA + "/" + ID_2027 + "/parametros-evaluacion"), personal, parametros))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La regla de cálculo no corresponde a la escala elegida."));
	}

	@DisplayName("El estado del período se calcula con las fechas y el reloj de hoy")
	@Test
	void elEstadoDelPeriodoSeCalculaConLasFechasYElRelojDeHoy() throws Exception {
		mvc.perform(conToken(get(RUTA + "/" + ID_2026), personal))
				.andExpect(jsonPath("$.periodos[0].estado").value("CERRADO"))
				.andExpect(jsonPath("$.periodos[1].estado").value("CERRADO"))
				.andExpect(jsonPath("$.periodos[2].estado").value("EN_CURSO"))
				.andExpect(jsonPath("$.periodos[3].estado").value("PLANIFICADO"));
	}

	@DisplayName("Detalle de un año que no existe da404")
	@Test
	void detalleDeUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(get(RUTA + "/00000000-0000-0000-0000-000000000000"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@DisplayName("Rechaza un año duplicado")
	@Test
	void rechazaUnAnioDuplicado() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2027, "2027-03-01", "2027-12-17", "BIMESTRAL")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un año escolar 2027."));
	}

	@DisplayName("Rechaza fecha de fin no posterior a la de inicio")
	@Test
	void rechazaFechaDeFinNoPosteriorALaDeInicio() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-12-15", "2028-03-01", "BIMESTRAL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La fecha de fin debe ser posterior a la fecha de inicio."));
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-03-01", "2028-03-01", "BIMESTRAL")))
				.andExpect(status().isBadRequest());
	}

	@DisplayName("Rechaza un año muy corto para el regimen")
	@Test
	void rechazaUnAnioMuyCortoParaElRegimen() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-03-01", "2028-03-05", "BIMESTRAL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("4 periodos")));
	}

	@DisplayName("Válida el rango del año y los campos obligatorios")
	@Test
	void validaElRangoDelAnioYLosCamposObligatorios() throws Exception {
		mvc.perform(conToken(post(RUTA), personal, solicitud(1999, "2028-03-01", "2028-12-15", "BIMESTRAL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El año escolar debe estar entre 2000 y 2100."));
		mvc.perform(conToken(post(RUTA), personal, solicitud(2101, "2028-03-01", "2028-12-15", "BIMESTRAL")))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(post(RUTA), personal, "{\"anio\":2028}"))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(post(RUTA), personal, solicitud(2028, "2028-03-01", "2028-12-15", "SEMESTRAL")))
				.andExpect(status().isBadRequest());
	}

	@DisplayName("Solo el personal administrativo puede usar la configuración")
	@Test
	void soloElPersonalAdministrativoPuedeUsarLaConfiguracion() throws Exception {
		String cuerpo = solicitud(2028, "2028-03-01", "2028-12-15", "BIMESTRAL");
		for (String usuario : new String[]{DOCENTE, ADMIN}) {
			String token = token(usuario);
			mvc.perform(conToken(get(RUTA), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/" + ID_2026), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(RUTA), token, cuerpo)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/actual"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/" + ID_2027 + "/requisitos"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(put(RUTA + "/" + ID_2027), token, fechas("2027-03-08", "2027-12-10")))
					.andExpect(status().isForbidden());
			mvc.perform(conToken(put(RUTA + "/" + ID_2027 + "/regimen"), token, regimen("BIMESTRAL")))
					.andExpect(status().isForbidden());
		}
		mvc.perform(get(RUTA + "/actual")).andExpect(status().isUnauthorized());
		mvc.perform(
				put(RUTA + "/" + ID_2027).contentType("application/json").content(fechas("2027-03-08", "2027-12-10")))
				.andExpect(status().isUnauthorized());
	}

	// --- GET /actual (CI-01) ---

	@DisplayName("El año actual trae el período en curso los conteos y el avance")
	@Test
	void elAnioActualTraeElPeriodoEnCursoLosConteosYElAvance() throws Exception {
		mvc.perform(conToken(get(RUTA + "/actual"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2026))
				.andExpect(jsonPath("$.periodoEnCurso.nombre").value("III Bimestre"))
				.andExpect(jsonPath("$.periodoEnCurso.fechaLimiteNotas").value("2026-10-16"))
				.andExpect(jsonPath("$.periodoEnCurso.diasParaLimiteNotas").value(31))
				.andExpect(jsonPath("$.periodoNotasAbiertas").value(nullValue()))
				.andExpect(jsonPath("$.proximoPeriodo.nombre").value("IV Bimestre"))
				.andExpect(jsonPath("$.proximoPeriodo.fechaInicio").value("2026-10-19"))
				.andExpect(jsonPath("$.seccionesActivas").value(10))
				.andExpect(jsonPath("$.alumnosMatriculados").value(5))
				.andExpect(jsonPath("$.estructurasHorarias").value(2))
				.andExpect(jsonPath("$.codigosEstructuras[0]").value("EH-PRI-26"))
				.andExpect(jsonPath("$.avance").value(67));
	}

	@DisplayName("Entre dos períodos la card muestra el plazo de notas y el próximo período")
	@Test
	void entreDosPeriodosLaCardMuestraElPlazoDeNotasYElProximoPeriodo() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 10, 12));

		mvc.perform(conToken(get(RUTA + "/actual"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodoEnCurso").value(nullValue()))
				.andExpect(jsonPath("$.periodoNotasAbiertas.nombre").value("III Bimestre"))
				.andExpect(jsonPath("$.periodoNotasAbiertas.fechaFin").value("2026-10-09"))
				.andExpect(jsonPath("$.periodoNotasAbiertas.diasParaLimiteNotas").value(4))
				.andExpect(jsonPath("$.proximoPeriodo.nombre").value("IV Bimestre"));
	}

	@DisplayName("Sin año activo no hay año actual")
	@Test
	void sinAnioActivoNoHayAnioActual() throws Exception {
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE anio = 2026").update();

		mvc.perform(conToken(get(RUTA + "/actual"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No hay un año escolar activo."));
	}

	// --- PUT /{id} (CI-06 · Editar) ---

	@DisplayName("Edita las fechas de un año planificado y regenera sus períodos")
	@Test
	void editaLasFechasDeUnAnioPlanificadoYRegeneraSusPeriodos() throws Exception {
		mvc.perform(conToken(put(RUTA + "/" + ID_2027), personal, fechas("2027-03-08", "2027-12-10")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fechaInicio").value("2027-03-08"))
				.andExpect(jsonPath("$.fechaFin").value("2027-12-10"))
				.andExpect(jsonPath("$.periodos.length()").value(3))
				.andExpect(jsonPath("$.periodos[0].fechaInicio").value("2027-03-08"))
				.andExpect(jsonPath("$.periodos[0].id").isNotEmpty())
				.andExpect(jsonPath("$.periodos[2].fechaFin").value("2027-12-10"));
		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.periodo_academico WHERE anio_escolar_id = ?::uuid")
				.param(ID_2027).query(Integer.class).single()).isEqualTo(3);
	}

	@DisplayName("Editar el año válida las fechas")
	@Test
	void editarElAnioValidaLasFechas() throws Exception {
		mvc.perform(conToken(put(RUTA + "/" + ID_2027), personal, fechas("2027-12-10", "2027-03-08")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La fecha de fin debe ser posterior a la fecha de inicio."));
		mvc.perform(conToken(put(RUTA + "/" + ID_2027), personal, fechas("2027-03-01", "2027-03-03")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El año escolar es muy corto para dividirlo en 3 periodos."));
		mvc.perform(conToken(put(RUTA + "/" + ID_2027), personal, "{\"fechaFin\":\"2027-12-10\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa la fecha de inicio."));
	}

	@DisplayName("Solo se edita un año planificado")
	@Test
	void soloSeEditaUnAnioPlanificado() throws Exception {
		mvc.perform(conToken(put(RUTA + "/" + ID_2026), personal, fechas("2026-03-02", "2026-12-18")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Solo se puede modificar un año planificado."));
		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.periodo_academico WHERE anio_escolar_id = ?::uuid")
				.param(ID_2026).query(Integer.class).single()).isEqualTo(4);
	}

	@DisplayName("Editar un año que no existe da404")
	@Test
	void editarUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(put(RUTA + "/00000000-0000-0000-0000-000000000000"), personal,
				fechas("2027-03-08", "2027-12-10")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	// --- PUT /{id}/regimen (CI-06 · Cambiar régimen) ---

	@DisplayName("Cambia el regimen de un año planificado y regenera los períodos con fechas sugeridas")
	@Test
	void cambiaElRegimenDeUnAnioPlanificadoYRegeneraLosPeriodosConFechasSugeridas() throws Exception {
		mvc.perform(conToken(put(RUTA + "/" + ID_2027 + "/regimen"), personal, regimen("BIMESTRAL")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.regimen").value("BIMESTRAL"))
				.andExpect(jsonPath("$.periodos.length()").value(4))
				.andExpect(jsonPath("$.periodos[0].nombre").value("I Bimestre"))
				.andExpect(jsonPath("$.periodos[0].fechaInicio").value("2027-03-01"))
				.andExpect(jsonPath("$.periodos[3].fechaFin").value("2027-12-17"))
				.andExpect(jsonPath("$.periodos[3].id").isNotEmpty());
		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.periodo_academico WHERE anio_escolar_id = ?::uuid")
				.param(ID_2027).query(Integer.class).single()).isEqualTo(4);
	}

	@DisplayName("Solo se cambia el regimen de un año planificado")
	@Test
	void soloSeCambiaElRegimenDeUnAnioPlanificado() throws Exception {
		mvc.perform(conToken(put(RUTA + "/" + ID_2026 + "/regimen"), personal, regimen("TRIMESTRAL")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Solo se puede modificar un año planificado."));
	}

	@DisplayName("El regimen es obligatorio y debe ser válido")
	@Test
	void elRegimenEsObligatorioYDebeSerValido() throws Exception {
		mvc.perform(conToken(put(RUTA + "/" + ID_2027 + "/regimen"), personal, "{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige el régimen de periodos."));
		mvc.perform(conToken(put(RUTA + "/" + ID_2027 + "/regimen"), personal, regimen("SEMESTRAL")))
				.andExpect(status().isBadRequest());
	}

	@DisplayName("Cambiar el regimen de un año que no existe da404")
	@Test
	void cambiarElRegimenDeUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(put(RUTA + "/00000000-0000-0000-0000-000000000000/regimen"), personal,
				regimen("BIMESTRAL")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	// --- GET /{id}/requisitos (CI-06 · Antes de activar el año) ---

	@DisplayName("Los requisitos del año planificado muestran los obligatorios y los recomendados")
	@Test
	void losRequisitosDelAnioPlanificadoMuestranLosObligatoriosYLosRecomendados() throws Exception {
		mvc.perform(conToken(get(RUTA + "/" + ID_2027 + "/requisitos"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.puedeActivar").value(false))
				.andExpect(jsonPath("$.obligatorios.length()").value(3))
				.andExpect(jsonPath("$.obligatorios[0].texto").value("Periodos académicos definidos"))
				.andExpect(jsonPath("$.obligatorios[0].cumplido").value(true))
				.andExpect(jsonPath("$.obligatorios[0].detalle").value("3 trimestres del 01/03/2027 al 17/12/2027"))
				.andExpect(jsonPath("$.obligatorios[1].detalle").value("10 secciones activas en 5 grados"))
				.andExpect(jsonPath("$.obligatorios[2].cumplido").value(false))
				.andExpect(jsonPath("$.recomendados.length()").value(2))
				.andExpect(jsonPath("$.recomendados[0].detalle").value("10 de 10 secciones aún no tienen tutor"))
				.andExpect(jsonPath("$.recomendados[1].detalle").value("0 de 5 grados con cursos asignados"));
	}

	@DisplayName("Un año con sus requisitos obligatorios puede activarse")
	@Test
	void unAnioConSusRequisitosObligatoriosPuedeActivarse() throws Exception {
		mvc.perform(conToken(get(RUTA + "/" + ID_2026 + "/requisitos"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.puedeActivar").value(true))
				.andExpect(jsonPath("$.obligatorios[2].detalle").value("EH-PRI-26 · EH-SEC-26"));
	}

	@DisplayName("Los requisitos de un año que no existe dan404")
	@Test
	void losRequisitosDeUnAnioQueNoExisteDan404() throws Exception {
		mvc.perform(conToken(get(RUTA + "/00000000-0000-0000-0000-000000000000/requisitos"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	// --- Listado y detalle ampliados ---

	@DisplayName("El listado y el detalle traen las cantidades de secciones y estructuras")
	@Test
	void elListadoYElDetalleTraenLasCantidadesDeSeccionesYEstructuras() throws Exception {
		mvc.perform(conToken(get(RUTA), personal))
				.andExpect(jsonPath("$[1].seccionesActivas").value(10))
				.andExpect(jsonPath("$[1].estructurasHorarias").value(2))
				.andExpect(jsonPath("$[0].estructurasHorarias").value(0));
		mvc.perform(conToken(get(RUTA + "/" + ID_2026), personal))
				.andExpect(jsonPath("$.cantidadSecciones").value(10))
				.andExpect(jsonPath("$.cantidadEstructuras").value(2))
				.andExpect(jsonPath("$.periodos[0].duracionSemanas").value(10))
				.andExpect(jsonPath("$.periodos[3].duracionSemanas").value(9))
				.andExpect(jsonPath("$.periodos[2].registroNotasAbierto").value(true))
				.andExpect(jsonPath("$.periodos[0].registroNotasAbierto").value(false));
	}

	// --- POST /{id}/activar y /{id}/cerrar ---

	private void cerrarElAnio2026PorSql() {
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE id = CAST(:id AS uuid)")
				.param("id", ID_2026)
				.update();
		entityManager.clear();
	}

	private void registrarEstructura2027PorSql() {
		jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria (anio_escolar_id, codigo, dias_lectivos, hora_entrada,
				                                              bloques_por_dia, duracion_bloque_min, nivel)
				VALUES (CAST(:id AS uuid), 'EH-SEC-27', ARRAY['LUN'], '07:30', 8, 45, 'SECUNDARIA')""")
				.param("id", ID_2027).update();
	}

	@DisplayName("Activa un año planificado con sus requisitos y sin otro activo")
	@Test
	void activaUnAnioPlanificadoConSusRequisitosYSinOtroActivo() throws Exception {
		cerrarElAnio2026PorSql();
		registrarEstructura2027PorSql();

		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/activar"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2027))
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.periodos.length()").value(3));
		mvc.perform(conToken(get(RUTA + "/actual"), personal))
				.andExpect(jsonPath("$.anio").value(2027));
	}

	@DisplayName("No activa un año mientras otro sigue activo")
	@Test
	void noActivaUnAnioMientrasOtroSigueActivo() throws Exception {
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/activar"), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El año 2026 sigue activo. Ciérralo antes de activar este."));
	}

	@DisplayName("No activa un año sin sus requisitos obligatorios")
	@Test
	void noActivaUnAnioSinSusRequisitosObligatorios() throws Exception {
		cerrarElAnio2026PorSql();

		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/activar"), personal))
				.andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.message").value("Faltan requisitos obligatorios: Estructura horaria registrada."));
	}

	@DisplayName("Solo activa un año planificado")
	@Test
	void soloActivaUnAnioPlanificado() throws Exception {
		mvc.perform(conToken(post(RUTA + "/" + ID_2026 + "/activar"), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Solo se puede activar un año planificado."));
	}

	@DisplayName("Activar un año que no existe da404")
	@Test
	void activarUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(post(RUTA + "/00000000-0000-0000-0000-000000000000/activar"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@DisplayName("Cierra el año cuando ya vencio el plazo de notas del último período")
	@Test
	void cierraElAnioCuandoYaVencioElPlazoDeNotasDelUltimoPeriodo() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 12, 24)); // el límite del IV Bimestre es el 23/12

		mvc.perform(conToken(post(RUTA + "/" + ID_2026 + "/cerrar"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CERRADO"))
				.andExpect(jsonPath("$.periodos[3].estado").value("CERRADO"));
		mvc.perform(conToken(get(RUTA + "/actual"), personal)).andExpect(status().isNotFound());
	}

	@DisplayName("No cierra el año antes de que venza el plazo de notas")
	@Test
	void noCierraElAnioAntesDeQueVenzaElPlazoDeNotas() throws Exception {
		reloj.fijarEn(LocalDate.of(2026, 12, 23)); // el último día todavía se registran notas

		mvc.perform(conToken(post(RUTA + "/" + ID_2026 + "/cerrar"), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Los docentes pueden registrar notas hasta el 23/12/2026."));
	}

	@DisplayName("Solo cierra un año activo")
	@Test
	void soloCierraUnAnioActivo() throws Exception {
		mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/cerrar"), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Solo se puede cerrar un año activo."));
	}

	@DisplayName("Cerrar un año que no existe da404")
	@Test
	void cerrarUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(post(RUTA + "/00000000-0000-0000-0000-000000000000/cerrar"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@DisplayName("Activar y cerrar solo lo hace el personal administrativo")
	@Test
	void activarYCerrarSoloLoHaceElPersonalAdministrativo() throws Exception {
		for (String usuario : new String[]{DOCENTE, ADMIN}) {
			String token = token(usuario);
			mvc.perform(conToken(post(RUTA + "/" + ID_2027 + "/activar"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(RUTA + "/" + ID_2026 + "/cerrar"), token)).andExpect(status().isForbidden());
		}
		mvc.perform(post(RUTA + "/" + ID_2027 + "/activar")).andExpect(status().isUnauthorized());
		mvc.perform(post(RUTA + "/" + ID_2026 + "/cerrar")).andExpect(status().isUnauthorized());
	}

	private static String fechas(String inicio, String fin) {
		return """
				{"fechaInicio":"%s","fechaFin":"%s"}""".formatted(inicio, fin);
	}

	private static String regimen(String regimen) {
		return """
				{"regimen":"%s"}""".formatted(regimen);
	}
}
