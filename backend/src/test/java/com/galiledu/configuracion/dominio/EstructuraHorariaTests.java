package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.galiledu.configuracion.dominio.CalculadoraJornada.FilaJornada;
import com.galiledu.configuracion.dominio.CalculadoraJornada.Jornada;
import com.galiledu.configuracion.dominio.CalculadoraJornada.TipoFila;

/** HU-44: reglas de una estructura horaria, sin base de datos. */
class EstructuraHorariaTests {
	private static final UUID ANIO = UUID.randomUUID();
	private static final List<String> LUN_A_VIE = List.of("LUN", "MAR", "MIE", "JUE", "VIE");

	private static DefinicionEstructura primaria() {
		return new DefinicionEstructura("EH-PRI-26", "Primaria · jornada de mañana", LUN_A_VIE, LocalTime.of(7, 45), 8,
				45, List.of(new DatosReceso(3, 20), new DatosReceso(6, 15)));
	}

	/** Definición válida con un solo dato cambiado, para probar una regla a la vez. */
	private static DefinicionEstructura conRecesos(List<DatosReceso> recesos) {
		DefinicionEstructura base = primaria();
		return new DefinicionEstructura(base.codigo(), base.descripcion(), base.diasLectivos(), base.horaEntrada(),
				base.bloquesPorDia(), base.duracionBloqueMin(), recesos);
	}

	private static DefinicionEstructura conBloquesYDuracion(int bloquesPorDia, int duracionBloqueMin) {
		DefinicionEstructura base = primaria();
		return new DefinicionEstructura(base.codigo(), base.descripcion(), base.diasLectivos(), base.horaEntrada(),
				bloquesPorDia, duracionBloqueMin, List.of());
	}

	private static void assertErrorDeCreacion(DefinicionEstructura definicion, String mensaje) {
		assertThatThrownBy(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, definicion))
				.isInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
	}

	@Test
	void creaLaEstructuraDeFigmaConSusRecesosYSusBloquesPorSemana() {
		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "PRIMARIA", true, primaria());

		assertThat(e.getCodigo()).isEqualTo("EH-PRI-26");
		assertThat(e.getNivel()).isEqualTo("PRIMARIA");
		assertThat(e.esPrincipal()).isTrue();
		assertThat(e.getDiasLectivos()).containsExactly("LUN", "MAR", "MIE", "JUE", "VIE");
		assertThat(e.getRecesos()).extracting(Receso::getDespuesDeBloque).containsExactly(3, 6);
		assertThat(e.bloquesPorSemana()).isEqualTo(40);
		assertThat(e.jornada().salidaMinutos()).isEqualTo(14 * 60 + 20);
	}

	@Test
	void elModalDeFigmaEs12BloquesPorSemanaConTresDias() {
		DefinicionEstructura talleres = new DefinicionEstructura("EH-TAR-26", "Secundaria · talleres de tarde",
				List.of("LUN", "MIE", "VIE"), LocalTime.of(14, 45), 4, 45, List.of(new DatosReceso(2, 15)));

		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "SECUNDARIA", false, talleres);

		assertThat(e.bloquesPorSemana()).isEqualTo(12);
		assertThat(e.esPrincipal()).isFalse();
	}

	@Test
	void normalizaElCodigoLaDescripcionYElOrdenDeLosDias() {
		DefinicionEstructura desordenada = new DefinicionEstructura("  eh-pri-26 ", "   ",
				List.of("vie", "LUN", " mar "),
				LocalTime.of(8, 0), 2, 45, List.of(new DatosReceso(1, 10)));

		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "  primaria ", true, desordenada);

		assertThat(e.getCodigo()).isEqualTo("EH-PRI-26");
		assertThat(e.getDescripcion()).isNull();
		assertThat(e.getNivel()).isEqualTo("PRIMARIA");
		assertThat(e.getDiasLectivos()).containsExactly("LUN", "MAR", "VIE");
	}

	// ---------- Reglas de código, descripción y días ----------

	@Test
	void elCodigoEsObligatorioYAdmiteHasta20Caracteres() {
		DefinicionEstructura base = primaria();
		assertErrorDeCreacion(
				new DefinicionEstructura("  ", base.descripcion(), base.diasLectivos(), base.horaEntrada(),
						8, 45, List.of()),
				"Ingresa el código de la estructura.");
		assertErrorDeCreacion(new DefinicionEstructura("A".repeat(21), base.descripcion(), base.diasLectivos(),
				base.horaEntrada(), 8, 45, List.of()), "El código admite hasta 20 caracteres.");
		assertThatCode(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, new DefinicionEstructura("A".repeat(20),
				null, base.diasLectivos(), base.horaEntrada(), 8, 45, List.of()))).doesNotThrowAnyException();
	}

	@Test
	void elCodigoSeValidaYaNormalizadoSinContarLosEspaciosDelBorde() {
		DefinicionEstructura base = primaria();

		// 20 letras con espacios a los lados y en minúsculas: guardado mide 20 y en mayúsculas, así que es válido.
		EstructuraHoraria conEspacios = EstructuraHoraria.crear(ANIO, "PRIMARIA", true, new DefinicionEstructura(
				"  " + "a".repeat(20) + "  ", null, base.diasLectivos(), base.horaEntrada(), 8, 45, List.of()));
		assertThat(conEspacios.getCodigo()).isEqualTo("A".repeat(20));
		// 21 letras con espacios: mide 21 ya sin ellos, se rechaza.
		assertErrorDeCreacion(new DefinicionEstructura("  " + "a".repeat(21) + "  ", null, base.diasLectivos(),
				base.horaEntrada(), 8, 45, List.of()), "El código admite hasta 20 caracteres.");
		// Un código solo de espacios queda vacío.
		assertErrorDeCreacion(new DefinicionEstructura("      ", null, base.diasLectivos(), base.horaEntrada(), 8, 45,
				List.of()), "Ingresa el código de la estructura.");
		// La vista previa ve lo mismo que el guardado.
		assertThat(EstructuraHoraria.detectarErrores(new DefinicionEstructura("  " + "a".repeat(20) + "  ", null,
				base.diasLectivos(), base.horaEntrada(), 8, 45, List.of()))).isEmpty();
	}

	@Test
	void laDescripcionEsOpcionalYAdmiteHasta100Caracteres() {
		DefinicionEstructura base = primaria();
		assertThatCode(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, new DefinicionEstructura("EH-1", null,
				base.diasLectivos(), base.horaEntrada(), 8, 45, List.of()))).doesNotThrowAnyException();
		assertThatCode(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, new DefinicionEstructura("EH-1",
				"D".repeat(100), base.diasLectivos(), base.horaEntrada(), 8, 45, List.of())))
				.doesNotThrowAnyException();
		assertErrorDeCreacion(new DefinicionEstructura("EH-1", "D".repeat(101), base.diasLectivos(),
				base.horaEntrada(), 8, 45, List.of()), "La descripción admite hasta 100 caracteres.");
	}

	@Test
	void exigeAlMenosUnDiaLectivoSinRepetirYSoloDeLunesASabado() {
		DefinicionEstructura base = primaria();
		assertErrorDeCreacion(new DefinicionEstructura("EH-1", null, List.of(), base.horaEntrada(), 8, 45, List.of()),
				"Elige al menos un día lectivo.");
		assertErrorDeCreacion(new DefinicionEstructura("EH-1", null, List.of("LUN", "LUN"), base.horaEntrada(), 8, 45,
				List.of()), "No repitas los días lectivos.");
		assertErrorDeCreacion(new DefinicionEstructura("EH-1", null, List.of("LUN", "DOM"), base.horaEntrada(), 8, 45,
				List.of()), "Día lectivo no válido: DOM.");
		assertThatCode(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, new DefinicionEstructura("EH-1", null,
				List.of("LUN", "MAR", "MIE", "JUE", "VIE", "SAB"), base.horaEntrada(), 8, 45, List.of())))
				.doesNotThrowAnyException();
	}

	// ---------- Reglas de bloques, duración y recesos ----------

	@Test
	void losBloquesPorDiaVanDe1A12() {
		assertErrorDeCreacion(conBloquesYDuracion(0, 45), "Los bloques por día van de 1 a 12.");
		assertErrorDeCreacion(conBloquesYDuracion(13, 20), "Los bloques por día van de 1 a 12.");
		assertThatCode(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, conBloquesYDuracion(1, 45)))
				.doesNotThrowAnyException();
		assertThatCode(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, conBloquesYDuracion(12, 45)))
				.doesNotThrowAnyException();
	}

	@Test
	void laDuracionDelBloqueVaDe20A120Minutos() {
		assertErrorDeCreacion(conBloquesYDuracion(2, 19), "La duración del bloque va de 20 a 120 minutos.");
		assertErrorDeCreacion(conBloquesYDuracion(2, 121), "La duración del bloque va de 20 a 120 minutos.");
		assertThatCode(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, conBloquesYDuracion(2, 20)))
				.doesNotThrowAnyException();
		assertThatCode(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, conBloquesYDuracion(2, 120)))
				.doesNotThrowAnyException();
	}

	@Test
	void elRecesoDuraDe5A60Minutos() {
		assertErrorDeCreacion(conRecesos(List.of(new DatosReceso(3, 4))), "El receso dura de 5 a 60 minutos.");
		assertErrorDeCreacion(conRecesos(List.of(new DatosReceso(3, 61))), "El receso dura de 5 a 60 minutos.");
		assertThatCode(
				() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, conRecesos(List.of(new DatosReceso(3, 5)))))
				.doesNotThrowAnyException();
	}

	@Test
	void elRecesoVaDespuesDeUnBloqueEntre1YBloquesMenos1() {
		// Con 8 bloques, el último receso posible va después del 7: tras el 8 no tendría sentido.
		assertErrorDeCreacion(conRecesos(List.of(new DatosReceso(0, 15))),
				"Un receso va después de un bloque entre 1 y 7.");
		assertErrorDeCreacion(conRecesos(List.of(new DatosReceso(8, 15))),
				"Un receso va después de un bloque entre 1 y 7.");
		assertThatCode(
				() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true, conRecesos(List.of(new DatosReceso(7, 15)))))
				.doesNotThrowAnyException();
	}

	@Test
	void conUnSoloBloqueNoSePuedePonerUnReceso() {
		DefinicionEstructura base = primaria();
		assertErrorDeCreacion(new DefinicionEstructura("EH-1", null, base.diasLectivos(), base.horaEntrada(), 1, 45,
				List.of(new DatosReceso(1, 15))), "Con un solo bloque por día no hay dónde poner un receso.");
	}

	@Test
	void noPermiteDosRecesosDespuesDelMismoBloque() {
		assertErrorDeCreacion(conRecesos(List.of(new DatosReceso(3, 10), new DatosReceso(3, 15))),
				"No puede haber dos recesos después del mismo bloque.");
	}

	// ---------- Hora límite de las 18:00 ----------

	@Test
	void laJornadaPuedeTerminarExactamenteALas1800() {
		DefinicionEstructura limite = new DefinicionEstructura("EH-TAR-26", null, List.of("LUN"), LocalTime.of(14, 45),
				4,
				45, List.of(new DatosReceso(2, 15)));

		assertThat(EstructuraHoraria.detectarErrores(limite)).isEmpty();
		assertThat(EstructuraHoraria.calcularJornada(limite).orElseThrow().salidaMinutos()).isEqualTo(18 * 60);
	}

	@Test
	void unMinutoDespuesDeLas1800SeRechazaYLaVistaPreviaConservaLaJornada() {
		DefinicionEstructura pasada = new DefinicionEstructura("EH-TAR-26", null, List.of("LUN"), LocalTime.of(14, 46),
				4,
				45, List.of(new DatosReceso(2, 15)));

		assertErrorDeCreacion(pasada, "La jornada debe terminar como máximo a las 18:00.");
		Jornada jornada = EstructuraHoraria.calcularJornada(pasada).orElseThrow();
		assertThat(jornada.salidaMinutos()).isEqualTo(18 * 60 + 1);
	}

	@Test
	void unaJornadaQueCruzaLaMedianocheSeRechaza() {
		DefinicionEstructura noche = new DefinicionEstructura("EH-NOC", null, List.of("LUN"), LocalTime.of(23, 0), 4,
				60,
				List.of());

		assertErrorDeCreacion(noche, "La jornada debe terminar como máximo a las 18:00.");
	}

	@Test
	void sinDatosParaCalcularNoHayJornada() {
		assertThat(EstructuraHoraria.calcularJornada(conBloquesYDuracion(13, 45))).isEmpty();
		DefinicionEstructura sinEntrada = new DefinicionEstructura("EH-1", null, LUN_A_VIE, null, 8, 45, List.of());
		assertThat(EstructuraHoraria.calcularJornada(sinEntrada)).isEmpty();
		assertErrorDeCreacion(sinEntrada, "Ingresa la hora de entrada.");
	}

	@Test
	void detectaTodosLosErroresJuntosParaLaVistaPrevia() {
		DefinicionEstructura mala = new DefinicionEstructura(" ", null, List.of(), LocalTime.of(8, 0), 13, 10,
				List.of(new DatosReceso(3, 4)));

		assertThat(EstructuraHoraria.detectarErrores(mala)).containsExactly(
				"Ingresa el código de la estructura.",
				"Elige al menos un día lectivo.",
				"Los bloques por día van de 1 a 12.",
				"La duración del bloque va de 20 a 120 minutos.",
				"El receso dura de 5 a 60 minutos.");
	}

	// ---------- Nivel y año ----------

	@Test
	void elNivelEsPrimariaOSecundaria() {
		assertThat(EstructuraHoraria.normalizarNivel(" secundaria ")).isEqualTo("SECUNDARIA");
		assertThatThrownBy(() -> EstructuraHoraria.normalizarNivel("INICIAL"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Elige el nivel: primaria o secundaria.");
		assertThatThrownBy(() -> EstructuraHoraria.normalizarNivel(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void soloUnAnioPlanificadoSeModifica() {
		assertThatCode(() -> EstructuraHoraria.validarAnioModificable(EstadoAnioEscolar.PLANIFICADO))
				.doesNotThrowAnyException();
		for (EstadoAnioEscolar estado : List.of(EstadoAnioEscolar.ACTIVO, EstadoAnioEscolar.CERRADO)) {
			assertThatThrownBy(() -> EstructuraHoraria.validarAnioModificable(estado))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("Solo se puede modificar la estructura horaria de un año planificado.");
		}
	}

	// ---------- Edición y principal ----------

	@Test
	void editarCambiaLosDatosPeroNoElNivelNiLaPrincipal() {
		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "PRIMARIA", true, primaria());

		e.editar(new DefinicionEstructura("eh-pri-27", "Nueva", List.of("LUN", "MAR"), LocalTime.of(8, 0), 6, 40,
				List.of(new DatosReceso(3, 25))));

		assertThat(e.getCodigo()).isEqualTo("EH-PRI-27");
		assertThat(e.bloquesPorSemana()).isEqualTo(12);
		assertThat(e.getNivel()).isEqualTo("PRIMARIA");
		assertThat(e.esPrincipal()).isTrue();
	}

	@Test
	void editarConservaLosRecesosQueSiguenEnElMismoBloqueYSoloCambiaSuDuracion() {
		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "PRIMARIA", true, primaria());
		Receso despuesDelTercero = e.getRecesos().get(0);

		// Quita el receso del bloque 6, cambia la duración del del 3 y agrega uno nuevo tras el bloque 5.
		e.editar(conRecesos(List.of(new DatosReceso(3, 30), new DatosReceso(5, 10))));

		assertThat(e.getRecesos()).extracting(Receso::getDespuesDeBloque).containsExactly(3, 5);
		assertThat(e.getRecesos()).extracting(Receso::getDuracionMin).containsExactly(30, 10);
		assertThat(e.getRecesos().get(0)).isSameAs(despuesDelTercero);
	}

	@Test
	void trasEditarLosRecesosEstanEnOrdenYLaJornadaEsCorrectaSinRecargarDeLaBase() {
		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "PRIMARIA", true, primaria());

		// Tenía recesos tras 3 y 6; ahora pide 3, 4 y 6. El del 4 se agrega al final de la lista interna.
		e.editar(conRecesos(List.of(new DatosReceso(3, 20), new DatosReceso(4, 10), new DatosReceso(6, 15))));

		assertThat(e.getRecesos()).extracting(Receso::getDespuesDeBloque).containsExactly(3, 4, 6);
		Jornada jornada = e.jornada();
		assertThat(jornada.filas()).extracting(FilaJornada::tipo).containsExactly(TipoFila.BLOQUE, TipoFila.BLOQUE,
				TipoFila.BLOQUE, TipoFila.RECESO, TipoFila.BLOQUE, TipoFila.RECESO, TipoFila.BLOQUE, TipoFila.BLOQUE,
				TipoFila.RECESO, TipoFila.BLOQUE, TipoFila.BLOQUE);
		// Receso tras el bloque 4: el bloque 4 termina a las 11:05 (07:45 + 3 × 45 + 20 + 45) y el receso dura 10 min.
		assertThat(jornada.filas().get(5))
				.isEqualTo(new FilaJornada(TipoFila.RECESO, null, 11 * 60 + 5, 11 * 60 + 15));
		// Salida: 14:20 de antes más los 10 min del receso nuevo.
		assertThat(jornada.salidaMinutos()).isEqualTo(14 * 60 + 30);
	}

	@Test
	void conBloquesInvalidosNoSeMuestraUnRangoDeRecesoSinSentido() {
		DefinicionEstructura base = primaria();
		DefinicionEstructura sinBloques = new DefinicionEstructura(base.codigo(), base.descripcion(),
				base.diasLectivos(), base.horaEntrada(), 0, 45, List.of(new DatosReceso(0, 15)));
		DefinicionEstructura demasiadosBloques = new DefinicionEstructura(base.codigo(), base.descripcion(),
				base.diasLectivos(), base.horaEntrada(), 13, 45, List.of(new DatosReceso(0, 15)));

		// Solo el error de los bloques: nada de "entre 1 y -1".
		assertThat(EstructuraHoraria.detectarErrores(sinBloques)).containsExactly("Los bloques por día van de 1 a 12.");
		assertThat(EstructuraHoraria.detectarErrores(demasiadosBloques))
				.containsExactly("Los bloques por día van de 1 a 12.");
		// Al corregir los bloques, aparece el error del receso con su rango real.
		assertErrorDeCreacion(conRecesos(List.of(new DatosReceso(0, 15))),
				"Un receso va después de un bloque entre 1 y 7.");
	}

	@Test
	void editarSoloLosRecesosActualizaLaFechaDeActualizacion() {
		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "PRIMARIA", true, primaria());
		// Una fecha muy antigua, fijada a mano, hace la comparación independiente del reloj.
		ReflectionTestUtils.setField(e, "actualizadoEn", Instant.EPOCH);

		e.editar(conRecesos(List.of(new DatosReceso(3, 20), new DatosReceso(4, 10), new DatosReceso(6, 15))));

		assertThat(e.getActualizadoEn()).isAfter(Instant.EPOCH);
	}

	@Test
	void editarConDatosInvalidosNoCambiaNada() {
		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "PRIMARIA", true, primaria());

		assertThatThrownBy(() -> e.editar(conBloquesYDuracion(13, 45))).isInstanceOf(IllegalArgumentException.class);

		assertThat(e.getBloquesPorDia()).isEqualTo(8);
		assertThat(e.getRecesos()).hasSize(2);
	}

	@Test
	void marcarYDesmarcarComoPrincipal() {
		EstructuraHoraria e = EstructuraHoraria.crear(ANIO, "PRIMARIA", false, primaria());

		e.marcarComoPrincipal();
		assertThat(e.esPrincipal()).isTrue();
		e.dejarDeSerPrincipal();
		assertThat(e.esPrincipal()).isFalse();
	}
}
