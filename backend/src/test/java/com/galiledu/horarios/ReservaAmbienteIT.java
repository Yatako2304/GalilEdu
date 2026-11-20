package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * HU-48 de extremo a extremo: la reserva de espacios especiales por HTTP, contra PostgreSQL real. «Hoy» es el 3 de
 * marzo de 2027 ({@link RelojDeReservasConfig}) y los datos se arman en 2027 (PLANIFICADO): estructura de secundaria de
 * 08:00 a 12:50, lunes a viernes, y el I trimestre de 1 de marzo a 31 de mayo. Las secciones son 1.° R1 y 1.° R2.
 */
@Import(RelojDeReservasConfig.class)
class ReservaAmbienteIT extends PruebaReservas {
	private static final String RUTA = "/horarios/reservas";
	/** Lunes 8 de marzo de 2027, el primer lunes después de «hoy». */
	private static final String LUNES = "2027-03-08";

	private UUID estructura;
	private UUID lab;
	private UUID labDos;
	private UUID aula;
	private UUID aulaDos;
	private UUID seccionA;
	private UUID seccionB;
	private UUID responsable;
	private UUID algebra;
	private String token;

	private void preparar() throws Exception {
		estructura = estructura("EH-T-27", "SECUNDARIA", true, 6);
		lab = espacio("TMP-LAB", "ACTIVO", 30);
		labDos = espacio("TMP-LAB2", "ACTIVO", 30);
		aula = ambiente("TMP-A1", "ACTIVO");
		aulaDos = ambiente("TMP-A2", "ACTIVO");
		seccionA = seccion("SEC-1", "R1", aula, null);
		seccionB = seccion("SEC-1", "R2", aulaDos, null);
		responsable = docente();
		algebra = plan("Álgebra", 5);
		token = token(COORDINADOR);
	}

	// ---------- Ayudas ----------

	private ResultActions enviar(String json) throws Exception {
		return mvc.perform(conToken(post(RUTA), token, json));
	}

	private ResultActions modificar(Object id, String json) throws Exception {
		return mvc.perform(conToken(put(RUTA + "/" + id), token, json));
	}

	private ResultActions eliminar(String ruta) throws Exception {
		return mvc.perform(conToken(delete(ruta), token));
	}

	private ResultActions leer(String ruta) throws Exception {
		return mvc.perform(conToken(get(ruta), token));
	}

	private static String cuerpoDe(ResultActions respuesta) throws Exception {
		return respuesta.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	/** Reserva LAB para la sección A con María, de {@code inicio} a {@code fin} el lunes. */
	private ResultActions reservar(String inicio, String fin) throws Exception {
		return enviar(cuerpoReserva(lab, seccionA, responsable, "Experimento", LUNES, inicio, fin));
	}

	private String idCreada(String inicio, String fin) throws Exception {
		return JsonPath.read(cuerpoDe(reservar(inicio, fin).andExpect(status().isCreated())), "$.id");
	}

	private void rechaza400(ResultActions respuesta, String mensaje) throws Exception {
		respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(mensaje));
	}

	private void rechaza409(ResultActions respuesta, String mensaje) throws Exception {
		respuesta.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(mensaje));
	}

	// ---------- Crear ----------

	@DisplayName("Crea la reserva con sus datos y el usuario que la registra")
	@Test
	void creaLaReservaConSusDatosYElUsuarioQueLaRegistra() throws Exception {
		preparar();

		reservar("08:00", "09:30").andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.ambiente.codigo").value("TMP-LAB"))
				.andExpect(jsonPath("$.ambiente.nombre").value("Espacio TMP-LAB"))
				.andExpect(jsonPath("$.actividad").value("Experimento"))
				.andExpect(jsonPath("$.anioId").value(ANIO_2027.toString()))
				.andExpect(jsonPath("$.gradoCodigo").value("SEC-1"))
				.andExpect(jsonPath("$.seccion.id").value(seccionA.toString()))
				.andExpect(jsonPath("$.seccion.etiqueta").value("1.° R1"))
				.andExpect(jsonPath("$.responsable.nombre").value("María Fernández Ríos"))
				.andExpect(jsonPath("$.fecha").value(LUNES)).andExpect(jsonPath("$.horaInicio").value("08:00"))
				.andExpect(jsonPath("$.horaFin").value("09:30")).andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.editable").value(true)).andExpect(jsonPath("$.version").value(0));

		assertThat(
				jdbc.sql("SELECT creado_por FROM horarios.reserva_ambiente WHERE seccion_id = :s").param("s", seccionA)
						.query(UUID.class).single())
				.isEqualTo(coordinador());
	}

	@DisplayName("Una reserva contigua a otra del mismo ambiente se permite")
	@Test
	void unaReservaContiguaAOtraDelMismoAmbienteSePermite() throws Exception {
		preparar();
		reserva(lab, seccionB, coordinador(), LUNES, "08:00", "09:30", "ACTIVA");

		reservar("09:30", "10:15").andExpect(status().isCreated());
	}

	// ---------- Regla 1: el ambiente ----------

	@DisplayName("Un aula regular se rechaza con un400")
	@Test
	void unAulaRegularSeRechazaConUn400() throws Exception {
		preparar();

		rechaza400(enviar(cuerpoReserva(aula, seccionA, responsable, "Experimento", LUNES, "08:00", "09:00")),
				"Elige un espacio especial activo.");
	}

	@DisplayName("Un espacio especial inactivo se rechaza con un400")
	@Test
	void unEspacioEspecialInactivoSeRechazaConUn400() throws Exception {
		preparar();
		UUID inactivo = espacio("TMP-INACT", "INACTIVO", 30);

		rechaza400(enviar(cuerpoReserva(inactivo, seccionA, responsable, "Experimento", LUNES, "08:00", "09:00")),
				"Elige un espacio especial activo.");
	}

	@DisplayName("Un ambiente inexistente se rechaza con un400")
	@Test
	void unAmbienteInexistenteSeRechazaConUn400() throws Exception {
		preparar();

		rechaza400(
				enviar(cuerpoReserva(UUID.randomUUID(), seccionA, responsable, "Experimento", LUNES, "08:00",
						"09:00")),
				"Elige un espacio especial activo.");
	}

	// ---------- Regla 2: año y aforo ----------

	@DisplayName("Más alumnos que el aforo se rechaza con un400")
	@Test
	void masAlumnosQueElAforoSeRechazaConUn400() throws Exception {
		preparar();
		UUID pequeno = espacio("TMP-PEQ", "ACTIVO", 2);
		matricular(seccionA, "SEC-1", 3);

		rechaza400(enviar(cuerpoReserva(pequeno, seccionA, responsable, "Experimento", LUNES, "08:00", "09:00")),
				"Espacio TMP-PEQ admite hasta 2 personas y la sección tiene 3 matriculados.");
	}

	@DisplayName("Tantos alumnos como el aforo se acepta")
	@Test
	void tantosAlumnosComoElAforoSeAcepta() throws Exception {
		preparar();
		UUID justo = espacio("TMP-JUSTO", "ACTIVO", 3);
		matricular(seccionA, "SEC-1", 3);

		enviar(cuerpoReserva(justo, seccionA, responsable, "Experimento", LUNES, "08:00", "09:00"))
				.andExpect(status().isCreated());
	}

	@DisplayName("Con el año cerrado se responde409")
	@Test
	void conElAnioCerradoSeResponde409() throws Exception {
		preparar();
		jdbc.sql("UPDATE configuracion.anio_escolar SET estado = 'CERRADO' WHERE id = :id").param("id", ANIO_2027)
				.update();

		rechaza409(reservar("08:00", "09:00"), "No se puede reservar un espacio en un año cerrado.");
	}

	@DisplayName("Un responsable que no es docente ni tutor se rechaza con un400")
	@Test
	void unResponsableQueNoEsDocenteNiTutorSeRechazaConUn400() throws Exception {
		preparar();

		rechaza400(enviar(cuerpoReserva(lab, seccionA, coordinador(), "Experimento", LUNES, "08:00", "09:00")),
				"Elige un docente o tutor activo como responsable.");
	}

	// ---------- Regla 3: fecha y hora ----------

	@DisplayName("Una fecha y hora pasada se rechaza con un400")
	@Test
	void unaFechaYHoraPasadaSeRechazaConUn400() throws Exception {
		preparar();

		// «Hoy» es el miércoles 3 de marzo a las 10:00: las 8:00 de hoy ya pasaron.
		rechaza400(enviar(cuerpoReserva(lab, seccionA, responsable, "Experimento", "2027-03-03", "08:00", "09:00")),
				"No se puede reservar en una fecha u hora que ya pasó.");
	}

	@DisplayName("Una fecha fuera del año se rechaza con un400")
	@Test
	void unaFechaFueraDelAnioSeRechazaConUn400() throws Exception {
		preparar();

		rechaza400(enviar(cuerpoReserva(lab, seccionA, responsable, "Experimento", "2028-03-06", "08:00", "09:00")),
				"La fecha debe estar dentro del año escolar 2027.");
	}

	@DisplayName("Una fecha entre dos períodos se rechaza con un400")
	@Test
	void unaFechaEntreDosPeriodosSeRechazaConUn400() throws Exception {
		preparar();

		// Lunes 7 de junio: el I trimestre termina el 31 de mayo y el II empieza el 8 de junio.
		rechaza400(enviar(cuerpoReserva(lab, seccionA, responsable, "Experimento", "2027-06-07", "08:00", "09:00")),
				"La fecha no está dentro de ningún periodo académico del año.");
	}

	@DisplayName("Un día que no es lectivo se rechaza con un400")
	@Test
	void unDiaQueNoEsLectivoSeRechazaConUn400() throws Exception {
		preparar();

		rechaza400(enviar(cuerpoReserva(lab, seccionA, responsable, "Experimento", "2027-03-13", "08:00", "09:00")),
				"El sábado no es un día de clases de esta sección.");
	}

	@DisplayName("Una reserva fuera de la jornada se rechaza con un400")
	@Test
	void unaReservaFueraDeLaJornadaSeRechazaConUn400() throws Exception {
		preparar();

		rechaza400(reservar("07:00", "08:00"),
				"La reserva debe estar dentro de la jornada de la sección, de 8:00 a 12:50.");
	}

	@DisplayName("La hora de fin debe ser posterior a la de inicio con un400")
	@Test
	void laHoraDeFinDebeSerPosteriorALaDeInicioConUn400() throws Exception {
		preparar();

		rechaza400(reservar("09:00", "08:00"), "La hora de fin debe ser posterior a la de inicio.");
	}

	// ---------- Regla 4: otra reserva del mismo ambiente ----------

	@DisplayName("Otra reserva del mismo ambiente superpuesta se rechaza con un409")
	@Test
	void otraReservaDelMismoAmbienteSuperpuestaSeRechazaConUn409() throws Exception {
		preparar();
		reserva(lab, seccionB, coordinador(), LUNES, "08:00", "09:30", "ACTIVA");

		rechaza409(reservar("09:00", "10:00"), "Ambiente no disponible: Espacio TMP-LAB ya está reservado por 1.° R2 "
				+ "para Experimento el lunes 08/03/2027 de 8:00 a 9:30.");
	}

	@DisplayName("Una reserva cancelada no cuenta para los cruces")
	@Test
	void unaReservaCanceladaNoCuentaParaLosCruces() throws Exception {
		preparar();
		reserva(lab, seccionB, coordinador(), LUNES, "08:00", "09:30", "CANCELADA");

		reservar("09:00", "10:00").andExpect(status().isCreated());
	}

	// ---------- Regla 5: el ambiente frente a los horarios semanales ----------

	@DisplayName("Otra sección con el ambiente en su horario se rechaza con un409")
	@Test
	void otraSeccionConElAmbienteEnSuHorarioSeRechazaConUn409() throws Exception {
		preparar();
		bloque(cabecera(seccionB, estructura), "LUN", 1, algebra, lab);

		rechaza409(reservar("08:00", "09:00"),
				"Ambiente no disponible: 1.° R2 ya usa TMP-LAB para Álgebra el lunes de 8:00 a 8:45.");
	}

	@DisplayName("La propia sección con un aula regular en su horario se permite")
	@Test
	void laPropiaSeccionConUnAulaRegularEnSuHorarioSePermite() throws Exception {
		preparar();
		bloque(cabecera(seccionA, estructura), "LUN", 1, algebra, aula);

		reservar("08:00", "09:00").andExpect(status().isCreated());
	}

	@DisplayName("La propia sección con el mismo ambiente en su horario se rechaza con un400")
	@Test
	void laPropiaSeccionConElMismoAmbienteEnSuHorarioSeRechazaConUn400() throws Exception {
		preparar();
		bloque(cabecera(seccionA, estructura), "LUN", 1, algebra, lab);

		rechaza400(reservar("08:00", "09:00"),
				"1.° R1 ya tiene TMP-LAB en su horario el lunes de 8:00 a 8:45; no necesita reserva.");
	}

	@DisplayName("La propia sección con otro espacio especial en su horario se rechaza con un409")
	@Test
	void laPropiaSeccionConOtroEspacioEspecialEnSuHorarioSeRechazaConUn409() throws Exception {
		preparar();
		bloque(cabecera(seccionA, estructura), "LUN", 1, algebra, labDos);

		rechaza409(reservar("08:00", "09:00"), "1.° R1 tiene Álgebra en TMP-LAB2 el lunes de 8:00 a 8:45.");
	}

	// ---------- Regla 7: el responsable ----------

	@DisplayName("Un responsable con clase en otra sección se rechaza con un409")
	@Test
	void unResponsableConClaseEnOtraSeccionSeRechazaConUn409() throws Exception {
		preparar();
		asignarDocente(algebra, seccionB, responsable);
		bloque(cabecera(seccionB, estructura), "LUN", 1, algebra, aulaDos);

		rechaza409(reservar("08:00", "09:00"), "Responsable no disponible: María Fernández Ríos ya dicta Álgebra "
				+ "con 1.° R2 el lunes de 8:00 a 8:45.");
	}

	@DisplayName("Un responsable con clase en la propia sección se permite")
	@Test
	void unResponsableConClaseEnLaPropiaSeccionSePermite() throws Exception {
		preparar();
		asignarDocente(algebra, seccionA, responsable);
		bloque(cabecera(seccionA, estructura), "LUN", 1, algebra, aula);

		reservar("08:00", "09:00").andExpect(status().isCreated());
	}

	@DisplayName("Un responsable de otra reserva superpuesta se rechaza con un409")
	@Test
	void unResponsableDeOtraReservaSuperpuestaSeRechazaConUn409() throws Exception {
		preparar();
		reserva(labDos, seccionB, responsable, LUNES, "08:00", "09:00", "ACTIVA");

		rechaza409(reservar("08:30", "09:30"), "Responsable no disponible: María Fernández Ríos ya es responsable "
				+ "de Experimento con 1.° R2 el lunes 08/03/2027 de 8:00 a 9:00.");
	}

	// ---------- Regla 8: la sección ----------

	@DisplayName("Una sección con otra reserva superpuesta se rechaza con un409")
	@Test
	void unaSeccionConOtraReservaSuperpuestaSeRechazaConUn409() throws Exception {
		preparar();
		reserva(labDos, seccionA, coordinador(), LUNES, "08:00", "09:00", "ACTIVA");

		rechaza409(reservar("08:30", "09:30"), "Sección no disponible: 1.° R1 ya tiene reservado Espacio TMP-LAB2 "
				+ "para Experimento el lunes 08/03/2027 de 8:00 a 9:00.");
	}

	// ---------- Editar ----------

	@DisplayName("Edita la reserva y sube la versión")
	@Test
	void editaLaReservaYSubeLaVersion() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");

		// Misma franja: la reserva no choca consigo misma.
		modificar(id, cuerpoEdicion(0, lab, seccionA, responsable, "Otro experimento", LUNES, "08:00", "09:00"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.actividad").value("Otro experimento"))
				.andExpect(jsonPath("$.version").value(1));
	}

	@DisplayName("Editar con una versión vieja se rechaza con un409")
	@Test
	void editarConUnaVersionViejaSeRechazaConUn409() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");
		modificar(id, cuerpoEdicion(0, lab, seccionA, responsable, "Cambio", LUNES, "08:00", "09:00"))
				.andExpect(status().isOk());

		rechaza409(modificar(id, cuerpoEdicion(0, lab, seccionA, responsable, "Otro", LUNES, "08:00", "09:00")),
				"La reserva cambió mientras la editabas. Vuelve a cargarla.");
	}

	@DisplayName("Editar contra otra reserva superpuesta se rechaza con un409")
	@Test
	void editarContraOtraReservaSuperpuestaSeRechazaConUn409() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");
		reserva(lab, seccionB, coordinador(), LUNES, "10:00", "11:00", "ACTIVA");

		modificar(id, cuerpoEdicion(0, lab, seccionA, responsable, "Experimento", LUNES, "10:30", "11:30"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(startsWith(
						"Ambiente no disponible: Espacio TMP-LAB ya está reservado por 1.° R2")));
	}

	@DisplayName("Editar una reserva finalizada se rechaza con un409")
	@Test
	void editarUnaReservaFinalizadaSeRechazaConUn409() throws Exception {
		preparar();
		String id = reserva(lab, seccionA, responsable, "2027-03-01", "08:00", "09:00", "ACTIVA").toString();

		rechaza409(modificar(id, cuerpoEdicion(0, lab, seccionA, responsable, "Cambio", LUNES, "08:00", "09:00")),
				"La reserva ya finalizó y no se puede modificar.");
	}

	@DisplayName("Editar una reserva cancelada se rechaza con un409")
	@Test
	void editarUnaReservaCanceladaSeRechazaConUn409() throws Exception {
		preparar();
		String id = reserva(lab, seccionA, responsable, LUNES, "08:00", "09:00", "CANCELADA").toString();

		rechaza409(modificar(id, cuerpoEdicion(0, lab, seccionA, responsable, "Cambio", LUNES, "08:00", "09:00")),
				"La reserva ya fue cancelada.");
	}

	@DisplayName("Editar una reserva inexistente responde404")
	@Test
	void editarUnaReservaInexistenteResponde404() throws Exception {
		preparar();

		modificar(UUID.randomUUID(),
				cuerpoEdicion(0, lab, seccionA, responsable, "Cambio", LUNES, "08:00", "09:00"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("La reserva no existe."));
	}

	// ---------- Cancelar ----------

	@DisplayName("Cancelar responde204 y la reserva desaparece del listado y de la lectura")
	@Test
	void cancelarResponde204YLaReservaDesapareceDelListadoYDeLaLectura() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");
		leer(RUTA + "?filtro=ANIO").andExpect(jsonPath("$.meta.total").value(1));

		eliminar(RUTA + "/" + id).andExpect(status().isNoContent());

		leer(RUTA + "?filtro=ANIO").andExpect(jsonPath("$.meta.total").value(0));
		leer(RUTA + "/" + id).andExpect(status().isNotFound());
		assertThat(jdbc.sql("SELECT estado FROM horarios.reserva_ambiente WHERE id = :id")
				.param("id", UUID.fromString(id)).query(String.class).single()).isEqualTo("CANCELADA");
	}

	@DisplayName("Una reserva cancelada libera el ambiente")
	@Test
	void unaReservaCanceladaLiberaElAmbiente() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");
		eliminar(RUTA + "/" + id).andExpect(status().isNoContent());

		enviar(cuerpoReserva(lab, seccionB, responsable, "Otra", LUNES,
				"08:00", "09:00")).andExpect(status().isCreated());
	}

	@DisplayName("Cancelar otra vez o una finalizada se rechaza con un409")
	@Test
	void cancelarOtraVezOUnaFinalizadaSeRechazaConUn409() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");
		eliminar(RUTA + "/" + id).andExpect(status().isNoContent());
		rechaza409(eliminar(RUTA + "/" + id), "La reserva ya fue cancelada.");

		String finalizada = reserva(lab, seccionA, responsable, "2027-03-01", "08:00", "09:00", "ACTIVA").toString();
		rechaza409(eliminar(RUTA + "/" + finalizada), "La reserva ya finalizó y no se puede modificar.");
	}

	@DisplayName("Cancelar con una versión vieja se rechaza con un409")
	@Test
	void cancelarConUnaVersionViejaSeRechazaConUn409() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");

		rechaza409(eliminar(RUTA + "/" + id + "?version=7"),
				"La reserva cambió mientras la editabas. Vuelve a cargarla.");
		eliminar(RUTA + "/" + id + "?version=0").andExpect(status().isNoContent());
	}

	@DisplayName("Cancelar una reserva inexistente responde404")
	@Test
	void cancelarUnaReservaInexistenteResponde404() throws Exception {
		preparar();

		eliminar(RUTA + "/" + UUID.randomUUID()).andExpect(status().isNotFound());
	}

	// ---------- Listado ----------

	/** Reservas en: 1 mar (semana), 7 mar (semana), 8 mar (mes), 5 abr y 20 dic (año), 31 dic 2026 y una cancelada. */
	private void reservasDeEjemplo() {
		reserva(lab, seccionA, responsable, "2027-03-01", "08:00", "09:00", "ACTIVA");
		reserva(lab, seccionA, responsable, "2027-03-07", "08:00", "09:00", "ACTIVA");
		reserva(lab, seccionA, responsable, "2027-03-08", "08:00", "09:00", "ACTIVA");
		reserva(lab, seccionA, responsable, "2027-04-05", "08:00", "09:00", "ACTIVA");
		reserva(lab, seccionA, responsable, "2027-12-20", "08:00", "09:00", "ACTIVA");
		reserva(lab, seccionA, responsable, "2026-12-31", "08:00", "09:00", "ACTIVA");
		reserva(lab, seccionA, responsable, "2027-03-02", "08:00", "09:00", "CANCELADA");
	}

	@DisplayName("Sin filtro lista la semana de hoy de la más reciente a la más antigua")
	@Test
	void sinFiltroListaLaSemanaDeHoyDeLaMasRecienteALaMasAntigua() throws Exception {
		preparar();
		reservasDeEjemplo();

		leer(RUTA).andExpect(status().isOk()).andExpect(jsonPath("$.meta.total").value(2))
				.andExpect(jsonPath("$.meta.page").value(1)).andExpect(jsonPath("$.meta.size").value(10))
				.andExpect(jsonPath("$.data[0].fecha").value("2027-03-07"))
				.andExpect(jsonPath("$.data[1].fecha").value("2027-03-01"))
				.andExpect(jsonPath("$.data[0].estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.data[0].editable").value(true))
				.andExpect(jsonPath("$.data[1].estado").value("FINALIZADO"))
				.andExpect(jsonPath("$.data[1].editable").value(false));
	}

	@DisplayName("El filtro semana es lo mismo que no poner filtro")
	@Test
	void elFiltroSemanaEsLoMismoQueNoPonerFiltro() throws Exception {
		preparar();
		reservasDeEjemplo();

		leer(RUTA + "?filtro=SEMANA").andExpect(jsonPath("$.meta.total").value(2));
	}

	@DisplayName("El filtro mes lista el mes calendario")
	@Test
	void elFiltroMesListaElMesCalendario() throws Exception {
		preparar();
		reservasDeEjemplo();

		String json = cuerpoDe(leer(RUTA + "?filtro=MES").andExpect(jsonPath("$.meta.total").value(3)));

		assertThat((List<String>) JsonPath.read(json, "$.data[*].fecha")).containsExactly("2027-03-08", "2027-03-07",
				"2027-03-01");
	}

	@DisplayName("El filtro año lista el año calendario sin las canceladas ni las de otro año")
	@Test
	void elFiltroAnioListaElAnioCalendarioSinLasCanceladasNiLasDeOtroAnio() throws Exception {
		preparar();
		reservasDeEjemplo();

		String json = cuerpoDe(leer(RUTA + "?filtro=ANIO").andExpect(jsonPath("$.meta.total").value(5)));

		assertThat((List<String>) JsonPath.read(json, "$.data[*].fecha")).containsExactly("2027-12-20", "2027-04-05",
				"2027-03-08", "2027-03-07", "2027-03-01");
	}

	@DisplayName("La paginación divide el listado y da los totales")
	@Test
	void laPaginacionDivideElListadoYDaLosTotales() throws Exception {
		preparar();
		reservasDeEjemplo();

		String segunda = cuerpoDe(leer(RUTA + "?filtro=ANIO&page=2&size=2").andExpect(jsonPath("$.meta.page").value(2))
				.andExpect(jsonPath("$.meta.size").value(2)).andExpect(jsonPath("$.meta.total").value(5))
				.andExpect(jsonPath("$.meta.totalPages").value(3)));
		assertThat((List<String>) JsonPath.read(segunda, "$.data[*].fecha")).containsExactly("2027-03-08",
				"2027-03-07");

		leer(RUTA + "?filtro=ANIO&page=3&size=2").andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].fecha").value("2027-03-01"));
		leer(RUTA + "?filtro=ANIO&page=9&size=2").andExpect(jsonPath("$.data.length()").value(0));
	}

	@DisplayName("Un filtro desconocido se rechaza con un400")
	@Test
	void unFiltroDesconocidoSeRechazaConUn400() throws Exception {
		preparar();

		leer(RUTA + "?filtro=SIEMPRE").andExpect(status().isBadRequest());
	}

	@DisplayName("Obtener devuelve la reserva y una inexistente responde404")
	@Test
	void obtenerDevuelveLaReservaYUnaInexistenteResponde404() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");

		leer(RUTA + "/" + id).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.seccion.etiqueta").value("1.° R1"));
		leer(RUTA + "/" + UUID.randomUUID()).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("La reserva no existe."));
	}

	// ---------- Opciones ----------

	@DisplayName("Las opciones solo traen espacios especiales activos por código y responsables docentes o tutores")
	@Test
	void lasOpcionesSoloTraenEspaciosEspecialesActivosPorCodigoYResponsablesDocentesOTutores() throws Exception {
		preparar();
		espacio("TMP-INACT", "INACTIVO", 30);
		usuario("79999941", "U20269941", "ACTIVO", false, "TUTOR");
		usuario("79999942", "U20269942", "INACTIVO", false, "DOCENTE");

		String json = cuerpoDe(leer(RUTA + "/opciones").andExpect(status().isOk()));

		List<String> codigos = JsonPath.read(json, "$.ambientes[*].codigo");
		assertThat(codigos).contains("TMP-LAB", "TMP-LAB2", "LAB-01", "AUD-01")
				.doesNotContain("TMP-A1", "TMP-INACT", "A-101", "A-205").isSorted();
		assertThat((List<String>) JsonPath.read(json, "$.ambientes[*].tipo")).containsOnly("ESPACIO_ESPECIAL");
		List<String> nombres = JsonPath.read(json, "$.responsables[*].nombre");
		assertThat(nombres).contains("María Fernández Ríos", "Usuario U20269941 Prueba Reserva")
				.doesNotContain("Carlos Ramírez Soto", "Usuario U20269942 Prueba Reserva");
		assertThat((List<Integer>) JsonPath.read(json, "$.ambientes[?(@.codigo=='TMP-LAB')].aforoMaximo"))
				.containsExactly(30);
	}

	// ---------- Cuerpo ----------

	@DisplayName("Un cuerpo vacio se rechaza con un400 con un mensaje por campo")
	@Test
	void unCuerpoVacioSeRechazaConUn400ConUnMensajePorCampo() throws Exception {
		preparar();

		enviar("{}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.ambienteId").value("Elige el ambiente."))
				.andExpect(jsonPath("$.errores.seccionId").value("Elige la sección."))
				.andExpect(jsonPath("$.errores.responsableId").value("Elige el responsable."))
				.andExpect(jsonPath("$.errores.actividad").value("Escribe la actividad."))
				.andExpect(jsonPath("$.errores.fecha").value("Elige la fecha."))
				.andExpect(jsonPath("$.errores.horaInicio").value("Elige la hora de inicio."))
				.andExpect(jsonPath("$.errores.horaFin").value("Elige la hora de fin."));
	}

	@DisplayName("La actividad en blanco o muy larga se rechaza con un400")
	@Test
	void laActividadEnBlancoOMuyLargaSeRechazaConUn400() throws Exception {
		preparar();

		enviar(cuerpoReserva(lab, seccionA, responsable, "   ", LUNES, "08:00", "09:00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.actividad").value("Escribe la actividad."));
		enviar(cuerpoReserva(lab, seccionA, responsable, "x".repeat(121), LUNES, "08:00", "09:00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.actividad").value("La actividad admite hasta 120 caracteres."));
	}

	@DisplayName("Una hora o una fecha mal escrita se rechaza con un400")
	@Test
	void unaHoraOUnaFechaMalEscritaSeRechazaConUn400() throws Exception {
		preparar();

		enviar(cuerpoReserva(lab, seccionA, responsable, "Experimento", LUNES, "25:00", "26:00"))
				.andExpect(status().isBadRequest());
		enviar(cuerpoReserva(lab, seccionA, responsable, "Experimento", "08/03/2027", "08:00", "09:00"))
				.andExpect(status().isBadRequest());
	}

	@DisplayName("Editar sin la versión se rechaza con un400")
	@Test
	void editarSinLaVersionSeRechazaConUn400() throws Exception {
		preparar();
		String id = idCreada("08:00", "09:00");

		modificar(id, cuerpoReserva(lab, seccionA, responsable, "Cambio", LUNES, "08:00", "09:00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.version").value("Falta la versión de la reserva."));
	}

	@DisplayName("Una sección inexistente responde404")
	@Test
	void unaSeccionInexistenteResponde404() throws Exception {
		preparar();

		enviar(cuerpoReserva(lab, UUID.randomUUID(), responsable, "Experimento", LUNES, "08:00", "09:00"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("La sección no existe."));
	}

	// ---------- Accesos ----------

	@DisplayName("Sin token cada endpoint responde401")
	@Test
	void sinTokenCadaEndpointResponde401() throws Exception {
		UUID id = UUID.randomUUID();
		String cuerpo = cuerpoReserva(id, id, id, "Experimento", LUNES, "08:00", "09:00");
		String edicion = cuerpoEdicion(0, id, id, id, "Experimento", LUNES, "08:00", "09:00");

		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
		mvc.perform(get(RUTA + "/opciones")).andExpect(status().isUnauthorized());
		mvc.perform(get(RUTA + "/" + id)).andExpect(status().isUnauthorized());
		mvc.perform(post(RUTA).contentType("application/json").content(cuerpo)).andExpect(status().isUnauthorized());
		mvc.perform(put(RUTA + "/" + id).contentType("application/json").content(edicion))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete(RUTA + "/" + id)).andExpect(status().isUnauthorized());
	}

	@DisplayName("Solo el coordinador entra cada otro rol recibe403 en cada endpoint")
	@Test
	void soloElCoordinadorEntraCadaOtroRolRecibe403EnCadaEndpoint() throws Exception {
		UUID id = UUID.randomUUID();
		String cuerpo = cuerpoReserva(id, id, id, "Experimento", LUNES, "08:00", "09:00");
		String edicion = cuerpoEdicion(0, id, id, id, "Experimento", LUNES, "08:00", "09:00");
		// ADMINISTRADOR, PERSONAL_ADMINISTRATIVO, APODERADO, DOCENTE (con TUTOR) y un TUTOR solo.
		for (String usuario : List.of(ADMIN, PERSONAL_ADMINISTRATIVO, "U20260003", DOCENTE, tutorConAcceso())) {
			String t = token(usuario);
			mvc.perform(conToken(get(RUTA), t)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/opciones"), t)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/" + id), t)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(RUTA), t, cuerpo)).andExpect(status().isForbidden());
			mvc.perform(conToken(put(RUTA + "/" + id), t, edicion)).andExpect(status().isForbidden());
			mvc.perform(conToken(delete(RUTA + "/" + id), t)).andExpect(status().isForbidden());
		}
	}
}
