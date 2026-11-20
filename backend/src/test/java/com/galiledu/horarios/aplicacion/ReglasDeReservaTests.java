package com.galiledu.horarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.AmbientesActivos.AmbienteDisponible;
import com.galiledu.horarios.aplicacion.CalendarioDelAnio.AnioConPeriodos;
import com.galiledu.horarios.aplicacion.CalendarioDelAnio.PeriodoDelAnio;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.BloqueDeJornada;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.aplicacion.ReglasDeReserva.CruceDeReserva;
import com.galiledu.horarios.aplicacion.ReglasDeReserva.DatosDeReserva;
import com.galiledu.horarios.aplicacion.ReglasDeReserva.ReservaVigente;
import com.galiledu.horarios.aplicacion.ReglasDeReserva.TipoCruceReserva;
import com.galiledu.horarios.dominio.ReservaAmbiente;

/** HU-48: las reglas de una reserva de espacio especial, sin Spring ni BD. */
class ReglasDeReservaTests {
	private static final UUID LAB = UUID.randomUUID();
	private static final UUID OTRO_LAB = UUID.randomUUID();
	private static final UUID AULA = UUID.randomUUID();
	private static final UUID SECCION = UUID.randomUUID();
	private static final UUID OTRA_SECCION = UUID.randomUUID();
	private static final UUID RESPONSABLE = UUID.randomUUID();
	private static final UUID OTRO_RESPONSABLE = UUID.randomUUID();
	private static final Map<UUID, String> TIPOS = Map.of(LAB, "ESPACIO_ESPECIAL", OTRO_LAB, "ESPACIO_ESPECIAL", AULA,
			"AULA_REGULAR");

	/** Un lunes del IV bimestre de 2026. */
	private static final LocalDate LUNES = LocalDate.of(2026, 11, 23);
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 1, 10, 0);

	private static final AnioConPeriodos ANIO_2026 = new AnioConPeriodos(UUID.randomUUID(), 2026, "ACTIVO",
			LocalDate.of(2026, 3, 2), LocalDate.of(2026, 12, 18),
			List.of(new PeriodoDelAnio(1, "I Bimestre", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 5, 8)),
					new PeriodoDelAnio(2, "II Bimestre", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 7, 24)),
					new PeriodoDelAnio(3, "III Bimestre", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 10, 9)),
					new PeriodoDelAnio(4, "IV Bimestre", LocalDate.of(2026, 10, 19), LocalDate.of(2026, 12, 18))));

	/** Lunes a viernes, tres bloques de 45 minutos desde las 8:00 (hasta las 10:15). */
	private static final JornadaEstructura JORNADA = new JornadaEstructura(UUID.randomUUID(), "EH-SEC-26", true,
			List.of(DiaSemana.LUN, DiaSemana.MAR, DiaSemana.MIE, DiaSemana.JUE, DiaSemana.VIE),
			List.of(new BloqueDeJornada(1, hora("08:00"), hora("08:45")),
					new BloqueDeJornada(2, hora("08:45"), hora("09:30")),
					new BloqueDeJornada(3, hora("09:30"), hora("10:15"))),
			List.of());

	private final ReglasDeReserva reglas = new ReglasDeReserva();

	private static LocalTime hora(String texto) {
		return LocalTime.parse(texto);
	}

	private static DatosDeReserva datos(String inicio, String fin) {
		return new DatosDeReserva(LAB, SECCION, RESPONSABLE, LUNES, hora(inicio), hora(fin));
	}

	private static DatosDeReserva datosEn(LocalDate fecha, String inicio, String fin) {
		return new DatosDeReserva(LAB, SECCION, RESPONSABLE, fecha, hora(inicio), hora(fin));
	}

	private static AmbienteDisponible ambiente(String tipo, int aforo) {
		return new AmbienteDisponible(LAB, "Laboratorio de ciencias", "LAB-01", tipo, aforo);
	}

	private static ReservaVigente reserva(UUID id, UUID ambiente, UUID seccion, UUID responsable, LocalDate fecha,
			String inicio, String fin) {
		return new ReservaVigente(id, ambiente, seccion, responsable, fecha, hora(inicio), hora(fin), "1.° B",
				"Laboratorio de ciencias", "Rosa Quispe", "Experimento");
	}

	private static BloqueOcupado bloque(UUID seccion, DiaSemana dia, String inicio, String fin, UUID docente,
			UUID ambiente, String nombreAmbiente) {
		return new BloqueOcupado(seccion, dia, hora(inicio), hora(fin), docente, ambiente, "1.° A", "Álgebra",
				"Rosa Quispe", nombreAmbiente);
	}

	private List<CruceDeReserva> cruces(DatosDeReserva datos, UUID excluirId, List<ReservaVigente> reservas,
			List<BloqueOcupado> bloques) {
		return reglas.buscarCruces(datos, excluirId, reservas, bloques, TIPOS);
	}

	private static void assertRechaza(Runnable regla, HttpStatus estado, String mensaje) {
		assertThatThrownBy(regla::run).isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
			assertThat(e.estado()).isEqualTo(estado);
			assertThat(e.getMessage()).isEqualTo(mensaje);
		});
	}

	// ---------- Regla 1: el ambiente ----------

	@DisplayName("Un espacio especial activo se puede reservar")
	@Test
	void unEspacioEspecialActivoSePuedeReservar() {
		assertThatCode(() -> reglas.validarAmbiente(ambiente("ESPACIO_ESPECIAL", 30))).doesNotThrowAnyException();
	}

	@DisplayName("Un ambiente inexistente o inactivo se rechaza con un400")
	@Test
	void unAmbienteInexistenteOInactivoSeRechazaConUn400() {
		assertRechaza(() -> reglas.validarAmbiente(null), HttpStatus.BAD_REQUEST, "Elige un espacio especial activo.");
	}

	@DisplayName("Un aula regular se rechaza con un400")
	@Test
	void unAulaRegularSeRechazaConUn400() {
		assertRechaza(() -> reglas.validarAmbiente(ambiente("AULA_REGULAR", 30)), HttpStatus.BAD_REQUEST,
				"Elige un espacio especial activo.");
	}

	// ---------- Regla 2: año y aforo ----------

	@DisplayName("Con el año cerrado no se reserva y se responde409")
	@Test
	void conElAnioCerradoNoSeReservaYSeResponde409() {
		var cerrado = new AnioConPeriodos(UUID.randomUUID(), 2025, "CERRADO", LocalDate.of(2025, 3, 3),
				LocalDate.of(2025, 12, 19), List.of());

		assertRechaza(() -> reglas.validarAnio(cerrado), HttpStatus.CONFLICT,
				"No se puede reservar un espacio en un año cerrado.");
	}

	@DisplayName("Un año planificado o activo se acepta")
	@Test
	void unAnioPlanificadoOActivoSeAcepta() {
		var planificado = new AnioConPeriodos(UUID.randomUUID(), 2027, "PLANIFICADO", LocalDate.of(2027, 3, 1),
				LocalDate.of(2027, 12, 17), List.of());

		assertThatCode(() -> {
			reglas.validarAnio(ANIO_2026);
			reglas.validarAnio(planificado);
		}).doesNotThrowAnyException();
	}

	@DisplayName("Más alumnos que el aforo se rechaza con un400")
	@Test
	void masAlumnosQueElAforoSeRechazaConUn400() {
		assertRechaza(() -> reglas.validarAforo(ambiente("ESPACIO_ESPECIAL", 30), 31), HttpStatus.BAD_REQUEST,
				"Laboratorio de ciencias admite hasta 30 personas y la sección tiene 31 matriculados.");
	}

	@DisplayName("Tantos alumnos como el aforo se acepta")
	@Test
	void tantosAlumnosComoElAforoSeAcepta() {
		assertThatCode(() -> reglas.validarAforo(ambiente("ESPACIO_ESPECIAL", 30), 30)).doesNotThrowAnyException();
	}

	// ---------- Regla 3: fecha y hora ----------

	private void validarFecha(DatosDeReserva datos) {
		reglas.validarFechaYHora(datos, AHORA, ANIO_2026, JORNADA);
	}

	@DisplayName("Una reserva dentro del año el período el día lectivo y la jornada se acepta")
	@Test
	void unaReservaDentroDelAnioElPeriodoElDiaLectivoYLaJornadaSeAcepta() {
		assertThatCode(() -> validarFecha(datos("08:00", "10:15"))).doesNotThrowAnyException();
	}

	@DisplayName("La hora de fin debe ser posterior a la de inicio")
	@Test
	void laHoraDeFinDebeSerPosteriorALaDeInicio() {
		assertRechaza(() -> validarFecha(datos("09:00", "09:00")), HttpStatus.BAD_REQUEST,
				"La hora de fin debe ser posterior a la de inicio.");
		assertRechaza(() -> validarFecha(datos("09:30", "09:00")), HttpStatus.BAD_REQUEST,
				"La hora de fin debe ser posterior a la de inicio.");
	}

	@DisplayName("Una fecha y hora pasada se rechaza con un400")
	@Test
	void unaFechaYHoraPasadaSeRechazaConUn400() {
		var despues = LocalDateTime.of(LUNES, hora("08:30"));

		assertRechaza(() -> reglas.validarFechaYHora(datos("08:00", "09:30"), despues, ANIO_2026, JORNADA),
				HttpStatus.BAD_REQUEST, "No se puede reservar en una fecha u hora que ya pasó.");
	}

	@DisplayName("Iniciar justo ahora no es pasado")
	@Test
	void iniciarJustoAhoraNoEsPasado() {
		var ahora = LocalDateTime.of(LUNES, hora("08:00"));

		assertThatCode(() -> reglas.validarFechaYHora(datos("08:00", "09:30"), ahora, ANIO_2026, JORNADA))
				.doesNotThrowAnyException();
	}

	@DisplayName("Una fecha fuera del año se rechaza con un400")
	@Test
	void unaFechaFueraDelAnioSeRechazaConUn400() {
		// Lunes dentro de la jornada, antes del inicio del año y después de su fin ("ahora" es anterior a las dos).
		var enero = LocalDateTime.of(2026, 1, 5, 10, 0);

		assertRechaza(
				() -> reglas.validarFechaYHora(datosEn(LocalDate.of(2026, 2, 23), "08:00", "09:00"), enero, ANIO_2026,
						JORNADA),
				HttpStatus.BAD_REQUEST, "La fecha debe estar dentro del año escolar 2026.");
		assertRechaza(
				() -> reglas.validarFechaYHora(datosEn(LocalDate.of(2026, 12, 21), "08:00", "09:00"), enero, ANIO_2026,
						JORNADA),
				HttpStatus.BAD_REQUEST, "La fecha debe estar dentro del año escolar 2026.");
	}

	@DisplayName("Una fecha entre dos períodos se rechaza con un400")
	@Test
	void unaFechaEntreDosPeriodosSeRechazaConUn400() {
		// Lunes 27 de julio: vacaciones entre el II y el III bimestre.
		assertRechaza(() -> validarFecha(datosEn(LocalDate.of(2026, 7, 27), "08:00", "09:00")), HttpStatus.BAD_REQUEST,
				"La fecha no está dentro de ningún periodo académico del año.");
	}

	@DisplayName("Los días limite de un período se aceptan")
	@Test
	void losDiasLimiteDeUnPeriodoSeAceptan() {
		assertThatCode(() -> {
			validarFecha(datosEn(LocalDate.of(2026, 5, 18), "08:00", "09:00")); // lunes, inicio del II
			validarFecha(datosEn(LocalDate.of(2026, 7, 24), "08:00", "09:00")); // viernes, fin del II
		}).doesNotThrowAnyException();
	}

	@DisplayName("Un día que no es lectivo se rechaza con un400")
	@Test
	void unDiaQueNoEsLectivoSeRechazaConUn400() {
		assertRechaza(() -> validarFecha(datosEn(LocalDate.of(2026, 11, 28), "08:00", "09:00")),
				HttpStatus.BAD_REQUEST, "El sábado no es un día de clases de esta sección.");
		assertRechaza(() -> validarFecha(datosEn(LocalDate.of(2026, 11, 22), "08:00", "09:00")),
				HttpStatus.BAD_REQUEST, "El domingo no es un día de clases de esta sección.");
	}

	@DisplayName("Una reserva fuera de la jornada se rechaza con un400")
	@Test
	void unaReservaFueraDeLaJornadaSeRechazaConUn400() {
		var mensaje = "La reserva debe estar dentro de la jornada de la sección, de 8:00 a 10:15.";

		assertRechaza(() -> validarFecha(datos("07:45", "09:00")), HttpStatus.BAD_REQUEST, mensaje);
		assertRechaza(() -> validarFecha(datos("09:30", "10:30")), HttpStatus.BAD_REQUEST, mensaje);
	}

	// ---------- Regla 6: reserva finalizada o cancelada ----------

	private static ReservaAmbiente reservaGuardada() {
		return ReservaAmbiente.crear(LAB, SECCION, RESPONSABLE, "Experimento", LUNES, hora("08:00"), hora("09:30"),
				UUID.randomUUID());
	}

	@DisplayName("Una reserva pendiente se puede editar")
	@Test
	void unaReservaPendienteSePuedeEditar() {
		var antes = LocalDateTime.of(LUNES, hora("08:00"));

		assertThatCode(() -> reglas.validarEditable(reservaGuardada(), antes)).doesNotThrowAnyException();
	}

	@DisplayName("Una reserva finalizada no se edita y se responde409")
	@Test
	void unaReservaFinalizadaNoSeEditaYSeResponde409() {
		var alFinalizar = LocalDateTime.of(LUNES, hora("09:30"));

		assertRechaza(() -> reglas.validarEditable(reservaGuardada(), alFinalizar), HttpStatus.CONFLICT,
				"La reserva ya finalizó y no se puede modificar.");
	}

	@DisplayName("Una reserva cancelada no se edita y se responde409")
	@Test
	void unaReservaCanceladaNoSeEditaYSeResponde409() {
		var reserva = reservaGuardada();
		reserva.cancelar();

		assertRechaza(() -> reglas.validarEditable(reserva, AHORA), HttpStatus.CONFLICT,
				"La reserva ya fue cancelada.");
	}

	// ---------- Regla 4: otra reserva del mismo ambiente ----------

	@DisplayName("Sin reservas ni bloques no hay cruces")
	@Test
	void sinReservasNiBloquesNoHayCruces() {
		assertThat(cruces(datos("08:00", "09:30"), null, List.of(), List.of())).isEmpty();
	}

	@DisplayName("Otra reserva del mismo ambiente superpuesta es un cruce de ambiente")
	@Test
	void otraReservaDelMismoAmbienteSuperpuestaEsUnCruceDeAmbiente() {
		var otra = reserva(UUID.randomUUID(), LAB, OTRA_SECCION, OTRO_RESPONSABLE, LUNES, "09:00", "10:00");

		List<CruceDeReserva> cruces = cruces(datos("08:00", "09:30"), null, List.of(otra), List.of());

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruceReserva.AMBIENTE_RESERVADO);
			assertThat(c.aExcepcion().estado()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(c.aExcepcion().getMessage()).isEqualTo("Ambiente no disponible: Laboratorio de ciencias "
					+ "ya está reservado por 1.° B para Experimento el lunes 23/11/2026 de 9:00 a 10:00.");
		});
	}

	@DisplayName("Dos reservas contiguas no se cruzan")
	@Test
	void dosReservasContiguasNoSeCruzan() {
		var otra = reserva(UUID.randomUUID(), LAB, OTRA_SECCION, OTRO_RESPONSABLE, LUNES, "09:30", "10:15");

		assertThat(cruces(datos("08:00", "09:30"), null, List.of(otra), List.of())).isEmpty();
	}

	@DisplayName("Otro ambiente otra fecha o ninguna reserva del mismo ambiente no se cruzan")
	@Test
	void otroAmbienteOtraFechaONingunaReservaDelMismoAmbienteNoSeCruzan() {
		var otroAmbiente = reserva(UUID.randomUUID(), OTRO_LAB, OTRA_SECCION, OTRO_RESPONSABLE, LUNES, "08:00",
				"09:30");
		var otraFecha = reserva(UUID.randomUUID(), LAB, OTRA_SECCION, OTRO_RESPONSABLE, LUNES.plusDays(1), "08:00",
				"09:30");

		assertThat(cruces(datos("08:00", "09:30"), null, List.of(otroAmbiente, otraFecha), List.of())).isEmpty();
	}

	// ---------- Regla 5a: otra sección tiene el ambiente en su horario ----------

	@DisplayName("Otra sección con el ambiente en su horario es un cruce de ambiente")
	@Test
	void otraSeccionConElAmbienteEnSuHorarioEsUnCruceDeAmbiente() {
		var ocupado = bloque(OTRA_SECCION, DiaSemana.LUN, "08:00", "08:45", OTRO_RESPONSABLE, LAB, "LAB-01");

		List<CruceDeReserva> cruces = cruces(datos("08:00", "09:30"), null, List.of(), List.of(ocupado));

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruceReserva.AMBIENTE_EN_HORARIO_DE_OTRA);
			assertThat(c.aExcepcion().estado()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(c.aExcepcion().getMessage())
					.isEqualTo("Ambiente no disponible: 1.° A ya usa LAB-01 para Álgebra el lunes de 8:00 a 8:45.");
		});
	}

	@DisplayName("Otra sección con el ambiente justo antes o después o en otro día no se cruza")
	@Test
	void otraSeccionConElAmbienteJustoAntesODespuesOEnOtroDiaNoSeCruza() {
		var contiguo = bloque(OTRA_SECCION, DiaSemana.LUN, "07:15", "08:00", null, LAB, "LAB-01");
		var otroDia = bloque(OTRA_SECCION, DiaSemana.MAR, "08:00", "08:45", null, LAB, "LAB-01");
		var otroAmbiente = bloque(OTRA_SECCION, DiaSemana.LUN, "08:00", "08:45", null, OTRO_LAB, "LAB-02");

		assertThat(cruces(datos("08:00", "09:30"), null, List.of(), List.of(contiguo, otroDia, otroAmbiente)))
				.isEmpty();
	}

	// ---------- Regla 5b: la propia sección en su horario ----------

	@DisplayName("La clase de la propia sección en un aula regular se permite")
	@Test
	void laClaseDeLaPropiaSeccionEnUnAulaRegularSePermite() {
		var clase = bloque(SECCION, DiaSemana.LUN, "08:00", "08:45", null, AULA, "A-101");

		assertThat(cruces(datos("08:00", "09:30"), null, List.of(), List.of(clase))).isEmpty();
	}

	@DisplayName("El mismo ambiente en el horario de la propia sección se rechaza con un400 sin título")
	@Test
	void elMismoAmbienteEnElHorarioDeLaPropiaSeccionSeRechazaConUn400SinTitulo() {
		var clase = bloque(SECCION, DiaSemana.LUN, "08:00", "08:45", null, LAB, "LAB-01");

		List<CruceDeReserva> cruces = cruces(datos("08:00", "09:30"), null, List.of(), List.of(clase));

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruceReserva.MISMO_AMBIENTE_EN_SU_HORARIO);
			assertThat(c.aExcepcion().estado()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(c.aExcepcion().getMessage())
					.isEqualTo("1.° A ya tiene LAB-01 en su horario el lunes de 8:00 a 8:45; no necesita reserva.");
		});
	}

	@DisplayName("Otro espacio especial en el horario de la propia sección se rechaza con un409")
	@Test
	void otroEspacioEspecialEnElHorarioDeLaPropiaSeccionSeRechazaConUn409() {
		var clase = bloque(SECCION, DiaSemana.LUN, "08:45", "09:30", null, OTRO_LAB, "LAB-02");

		List<CruceDeReserva> cruces = cruces(datos("08:00", "09:30"), null, List.of(), List.of(clase));

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruceReserva.OTRO_ESPACIO_EN_SU_HORARIO);
			assertThat(c.aExcepcion().estado()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(c.aExcepcion().getMessage())
					.isEqualTo("1.° A tiene Álgebra en LAB-02 el lunes de 8:45 a 9:30.");
		});
	}

	@DisplayName("Un ambiente desconocido en el horario de la propia sección no se trata como espacio especial")
	@Test
	void unAmbienteDesconocidoEnElHorarioDeLaPropiaSeccionNoSeTrataComoEspacioEspecial() {
		var clase = bloque(SECCION, DiaSemana.LUN, "08:00", "08:45", null, UUID.randomUUID(), "A-205");

		assertThat(cruces(datos("08:00", "09:30"), null, List.of(), List.of(clase))).isEmpty();
	}

	@DisplayName("La clase de la propia sección fuera de la franja no importa")
	@Test
	void laClaseDeLaPropiaSeccionFueraDeLaFranjaNoImporta() {
		var clase = bloque(SECCION, DiaSemana.LUN, "09:30", "10:15", null, LAB, "LAB-01");

		assertThat(cruces(datos("08:00", "09:30"), null, List.of(), List.of(clase))).isEmpty();
	}

	// ---------- Regla 7a: el responsable dicta clase a otra sección ----------

	@DisplayName("Un responsable con clase en otra sección es un cruce con el título responsable no disponible")
	@Test
	void unResponsableConClaseEnOtraSeccionEsUnCruceConElTituloResponsableNoDisponible() {
		var clase = bloque(OTRA_SECCION, DiaSemana.LUN, "08:00", "08:45", RESPONSABLE, AULA, "A-102");

		List<CruceDeReserva> cruces = cruces(datos("08:00", "09:30"), null, List.of(), List.of(clase));

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruceReserva.RESPONSABLE_CON_CLASE);
			assertThat(c.aExcepcion().estado()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(c.aExcepcion().getMessage())
					.isEqualTo(
							"Responsable no disponible: Rosa Quispe ya dicta Álgebra con 1.° A el lunes de 8:00 a 8:45.");
		});
	}

	@DisplayName("Las clases del responsable con la propia sección se ignoran")
	@Test
	void lasClasesDelResponsableConLaPropiaSeccionSeIgnoran() {
		var clase = bloque(SECCION, DiaSemana.LUN, "08:00", "08:45", RESPONSABLE, AULA, "A-101");

		assertThat(cruces(datos("08:00", "09:30"), null, List.of(), List.of(clase))).isEmpty();
	}

	@DisplayName("La clase de otro docente o contigua o en otro día no cruza al responsable")
	@Test
	void laClaseDeOtroDocenteOContiguaOEnOtroDiaNoCruzaAlResponsable() {
		var otroDocente = bloque(OTRA_SECCION, DiaSemana.LUN, "08:00", "08:45", OTRO_RESPONSABLE, AULA, "A-102");
		var contigua = bloque(OTRA_SECCION, DiaSemana.LUN, "09:30", "10:15", RESPONSABLE, AULA, "A-102");
		var otroDia = bloque(OTRA_SECCION, DiaSemana.MAR, "08:00", "08:45", RESPONSABLE, AULA, "A-102");

		assertThat(cruces(datos("08:00", "09:30"), null, List.of(), List.of(otroDocente, contigua, otroDia)))
				.isEmpty();
	}

	// ---------- Regla 7b: el responsable de otra reserva ----------

	@DisplayName("Un responsable de otra reserva superpuesta es un cruce")
	@Test
	void unResponsableDeOtraReservaSuperpuestaEsUnCruce() {
		var otra = reserva(UUID.randomUUID(), OTRO_LAB, OTRA_SECCION, RESPONSABLE, LUNES, "09:00", "10:00");

		List<CruceDeReserva> cruces = cruces(datos("08:00", "09:30"), null, List.of(otra), List.of());

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruceReserva.RESPONSABLE_CON_RESERVA);
			assertThat(c.aExcepcion().getMessage()).isEqualTo("Responsable no disponible: Rosa Quispe ya es "
					+ "responsable de Experimento con 1.° B el lunes 23/11/2026 de 9:00 a 10:00.");
		});
	}

	// ---------- Regla 8: la sección con otra reserva ----------

	@DisplayName("Una sección con otra reserva superpuesta es un cruce")
	@Test
	void unaSeccionConOtraReservaSuperpuestaEsUnCruce() {
		var otra = reserva(UUID.randomUUID(), OTRO_LAB, SECCION, OTRO_RESPONSABLE, LUNES, "09:00", "10:00");

		List<CruceDeReserva> cruces = cruces(datos("08:00", "09:30"), null, List.of(otra), List.of());

		assertThat(cruces).singleElement().satisfies(c -> {
			assertThat(c.tipo()).isEqualTo(TipoCruceReserva.SECCION_CON_RESERVA);
			assertThat(c.aExcepcion().getMessage()).isEqualTo("Sección no disponible: 1.° B ya tiene reservado "
					+ "Laboratorio de ciencias para Experimento el lunes 23/11/2026 de 9:00 a 10:00.");
		});
	}

	// ---------- Al editar: la propia reserva no cuenta (4, 7b y 8) ----------

	@DisplayName("Al editar la reserva no se cruza con si misma")
	@Test
	void alEditarLaReservaNoSeCruzaConSiMisma() {
		UUID propia = UUID.randomUUID();
		var guardada = reserva(propia, LAB, SECCION, RESPONSABLE, LUNES, "08:00", "09:30");

		assertThat(cruces(datos("08:00", "09:30"), propia, List.of(guardada), List.of())).isEmpty();
		// Sin excluirla (una reserva nueva) sí chocaría en las tres reglas.
		assertThat(cruces(datos("08:00", "09:30"), null, List.of(guardada), List.of())).extracting("tipo")
				.containsExactly(TipoCruceReserva.AMBIENTE_RESERVADO, TipoCruceReserva.RESPONSABLE_CON_RESERVA,
						TipoCruceReserva.SECCION_CON_RESERVA);
	}

	@DisplayName("Al editar si se cruza con las demas reservas")
	@Test
	void alEditarSiSeCruzaConLasDemasReservas() {
		UUID propia = UUID.randomUUID();
		var guardada = reserva(propia, LAB, SECCION, RESPONSABLE, LUNES, "08:00", "09:30");
		var otra = reserva(UUID.randomUUID(), LAB, OTRA_SECCION, OTRO_RESPONSABLE, LUNES, "09:00", "10:00");

		assertThat(cruces(datos("08:00", "09:30"), propia, List.of(guardada, otra), List.of()))
				.extracting(CruceDeReserva::tipo).containsExactly(TipoCruceReserva.AMBIENTE_RESERVADO);
	}

	// ---------- Orden ----------

	@DisplayName("Los cruces salen en el orden de las reglas")
	@Test
	void losCrucesSalenEnElOrdenDeLasReglas() {
		var reservaDelLab = reserva(UUID.randomUUID(), LAB, OTRA_SECCION, OTRO_RESPONSABLE, LUNES, "08:00", "09:30");
		var reservaDelResponsable = reserva(UUID.randomUUID(), OTRO_LAB, OTRA_SECCION, RESPONSABLE, LUNES, "08:00",
				"09:30");
		var reservaDeLaSeccion = reserva(UUID.randomUUID(), OTRO_LAB, SECCION, OTRO_RESPONSABLE, LUNES, "08:00",
				"09:30");
		var otraSeccionConLab = bloque(OTRA_SECCION, DiaSemana.LUN, "08:00", "08:45", null, LAB, "LAB-01");
		var propiaConOtroLab = bloque(SECCION, DiaSemana.LUN, "08:45", "09:30", null, OTRO_LAB, "LAB-02");
		var responsableConClase = bloque(OTRA_SECCION, DiaSemana.LUN, "08:00", "08:45", RESPONSABLE, AULA, "A-102");

		var resultado = cruces(datos("08:00", "09:30"), null,
				List.of(reservaDeLaSeccion, reservaDelResponsable, reservaDelLab),
				List.of(responsableConClase, propiaConOtroLab, otraSeccionConLab));

		assertThat(resultado).extracting(CruceDeReserva::tipo).containsExactly(TipoCruceReserva.AMBIENTE_RESERVADO,
				TipoCruceReserva.OTRO_ESPACIO_EN_SU_HORARIO, TipoCruceReserva.AMBIENTE_EN_HORARIO_DE_OTRA,
				TipoCruceReserva.RESPONSABLE_CON_CLASE, TipoCruceReserva.RESPONSABLE_CON_RESERVA,
				TipoCruceReserva.SECCION_CON_RESERVA);
	}

	@DisplayName("Cada tipo de cruce tiene su código y su título")
	@Test
	void cadaTipoDeCruceTieneSuCodigoYSuTitulo() {
		assertThat(TipoCruceReserva.values()).extracting(TipoCruceReserva::estado, TipoCruceReserva::titulo)
				.containsExactly(tuple(HttpStatus.CONFLICT, "Ambiente no disponible"),
						tuple(HttpStatus.CONFLICT, "Ambiente no disponible"), tuple(HttpStatus.BAD_REQUEST, null),
						tuple(HttpStatus.CONFLICT, null), tuple(HttpStatus.CONFLICT, "Responsable no disponible"),
						tuple(HttpStatus.CONFLICT, "Responsable no disponible"),
						tuple(HttpStatus.CONFLICT, "Sección no disponible"));
	}
}
