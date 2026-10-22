package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AnioEscolarTests {
	private static final LocalDate INICIO = LocalDate.of(2028, 3, 1);
	private static final LocalDate FIN = LocalDate.of(2028, 12, 15);

	private static AnioEscolar anio(LocalDate inicio, LocalDate fin, Regimen regimen) {
		return AnioEscolar.planificar(2028, inicio, fin, regimen);
	}

	private static LocalDate fecha(int mes, int dia) {
		return LocalDate.of(2028, mes, dia);
	}

	/**
	 * Año de 80 días (01/03 al 19/05) en 4 bimestres de 20 días, con fechas fáciles de leer:
	 * I 01/03–20/03 (límite 25/03) · II 21/03–09/04 (límite 14/04) · III 10/04–29/04 (límite 04/05) ·
	 * IV 30/04–19/05 (límite 24/05). Los periodos reciben un id porque solo JPA se lo asigna.
	 */
	private static AnioEscolar anioCorto() {
		AnioEscolar anio = anio(fecha(3, 1), fecha(5, 19), Regimen.BIMESTRAL);
		anio.getPeriodos().forEach(p -> ReflectionTestUtils.setField(p, "id", UUID.randomUUID()));
		return anio;
	}

	private static AnioEscolar anioActivo() {
		AnioEscolar anio = anioCorto();
		anio.activar();
		return anio;
	}

	private static AnioEscolar anioCerrado() {
		AnioEscolar anio = anioActivo();
		anio.cerrar(fecha(5, 25));
		return anio;
	}

	private static PeriodoAcademico periodo(AnioEscolar anio, int numero) {
		return anio.getPeriodos().get(numero - 1);
	}

	/** Edita el periodo {@code numero} del año con los valores dados. */
	private static void editar(AnioEscolar anio, int numero, String nombre, LocalDate inicio, LocalDate fin,
			LocalDate limite, LocalDate hoy) {
		anio.editarPeriodo(periodo(anio, numero).getId(), nombre, inicio, fin, limite, hoy);
	}

	private static void rechazaPorEstado(ThrowingCallable operacion, String mensaje) {
		assertThatThrownBy(operacion).isExactlyInstanceOf(IllegalStateException.class).hasMessage(mensaje);
	}

	private static void rechazaPorDato(ThrowingCallable operacion, String mensaje) {
		assertThatThrownBy(operacion).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
	}

	@Test
	void unAnioBimestralGeneraCuatroBimestresNumeradosDesdeUno() {
		var periodos = anio(INICIO, FIN, Regimen.BIMESTRAL).getPeriodos();

		assertThat(periodos).extracting(PeriodoAcademico::getNumero).containsExactly(1, 2, 3, 4);
		assertThat(periodos).extracting(PeriodoAcademico::getNombre)
				.containsExactly("I Bimestre", "II Bimestre", "III Bimestre", "IV Bimestre");
	}

	@Test
	void unAnioTrimestralGeneraTresTrimestresNumeradosDesdeUno() {
		var periodos = anio(INICIO, FIN, Regimen.TRIMESTRAL).getPeriodos();

		assertThat(periodos).extracting(PeriodoAcademico::getNumero).containsExactly(1, 2, 3);
		assertThat(periodos).extracting(PeriodoAcademico::getNombre)
				.containsExactly("I Trimestre", "II Trimestre", "III Trimestre");
	}

	@Test
	void elAnioNacePlanificadoYSusPeriodosAunNoEmpiezan() {
		var anio = anio(INICIO, FIN, Regimen.BIMESTRAL);
		LocalDate antesDelAnio = INICIO.minusDays(1);

		assertThat(anio.getEstado()).isEqualTo(EstadoAnioEscolar.PLANIFICADO);
		assertThat(anio.getPeriodos()).extracting(p -> p.estadoEn(antesDelAnio, anio.getEstado()))
				.containsOnly(EstadoPeriodo.PLANIFICADO);
	}

	@Test
	void losPeriodosCubrenElAnioSinCruzarseNiDejarHuecos() {
		for (Regimen regimen : Regimen.values()) {
			List<PeriodoAcademico> periodos = anio(INICIO, FIN, regimen).getPeriodos();

			assertThat(periodos.getFirst().getFechaInicio()).isEqualTo(INICIO);
			assertThat(periodos.getLast().getFechaFin()).isEqualTo(FIN);
			for (int i = 1; i < periodos.size(); i++) {
				assertThat(periodos.get(i).getFechaInicio())
						.isEqualTo(periodos.get(i - 1).getFechaFin().plusDays(1));
			}
		}
	}

	@Test
	void cadaPeriodoQuedaDentroDelAnioYTerminaDespuesDeEmpezar() {
		for (Regimen regimen : Regimen.values()) {
			for (PeriodoAcademico p : anio(INICIO, FIN, regimen).getPeriodos()) {
				assertThat(p.getFechaInicio()).isAfterOrEqualTo(INICIO);
				assertThat(p.getFechaFin()).isBeforeOrEqualTo(FIN).isAfter(p.getFechaInicio());
			}
		}
	}

	@Test
	void laFechaLimiteDeNotasEsCincoDiasDespuesDelFinDelPeriodo() {
		for (PeriodoAcademico p : anio(INICIO, FIN, Regimen.BIMESTRAL).getPeriodos()) {
			assertThat(p.getFechaLimiteNotas()).isEqualTo(p.getFechaFin().plusDays(5));
		}
	}

	@Test
	void losDiasQueSobranSeRepartenEntreLosPrimerosPeriodos() {
		// 10 días en 4 bimestres: 3 + 3 + 2 + 2.
		var periodos = anio(LocalDate.of(2028, 3, 1), LocalDate.of(2028, 3, 10), Regimen.BIMESTRAL).getPeriodos();

		assertThat(periodos).extracting(p -> p.getFechaFin().getDayOfMonth() - p.getFechaInicio().getDayOfMonth() + 1)
				.containsExactly(3, 3, 2, 2);
	}

	@Test
	void editarLasFechasRegeneraLosPeriodosDentroDelNuevoRango() {
		var anio = anio(INICIO, FIN, Regimen.BIMESTRAL);
		LocalDate nuevoInicio = LocalDate.of(2028, 4, 3);
		LocalDate nuevoFin = LocalDate.of(2028, 11, 30);

		anio.editarFechas(nuevoInicio, nuevoFin);

		assertThat(anio.getFechaInicio()).isEqualTo(nuevoInicio);
		assertThat(anio.getFechaFin()).isEqualTo(nuevoFin);
		assertThat(anio.getPeriodos()).hasSize(4);
		assertThat(anio.getPeriodos().getFirst().getFechaInicio()).isEqualTo(nuevoInicio);
		assertThat(anio.getPeriodos().getLast().getFechaFin()).isEqualTo(nuevoFin);
	}

	@Test
	void cambiarElRegimenRegeneraLosPeriodosConLaNuevaCantidadYNombres() {
		var anio = anio(INICIO, FIN, Regimen.BIMESTRAL);

		anio.cambiarRegimen(Regimen.TRIMESTRAL);

		assertThat(anio.getRegimen()).isEqualTo(Regimen.TRIMESTRAL);
		assertThat(anio.getPeriodos()).extracting(PeriodoAcademico::getNombre)
				.containsExactly("I Trimestre", "II Trimestre", "III Trimestre");
		assertThat(anio.getPeriodos().getFirst().getFechaInicio()).isEqualTo(INICIO);
		assertThat(anio.getPeriodos().getLast().getFechaFin()).isEqualTo(FIN);
	}

	@Test
	void quitarLosPeriodosVaciaLaLista() {
		var anio = anio(INICIO, FIN, Regimen.BIMESTRAL);

		anio.quitarPeriodos();

		assertThat(anio.getPeriodos()).isEmpty();
	}

	@Test
	void regenerarDespuesDeQuitarNoDuplicaLosPeriodos() {
		var anio = anio(INICIO, FIN, Regimen.BIMESTRAL);

		anio.quitarPeriodos();
		anio.cambiarRegimen(Regimen.TRIMESTRAL);

		assertThat(anio.getPeriodos()).hasSize(3);
	}

	@Test
	void soloUnAnioPlanificadoSePuedeModificar() {
		for (AnioEscolar anio : List.of(anioActivo(), anioCerrado())) {
			String mensaje = "Solo se puede modificar un año planificado.";

			rechazaPorEstado(() -> anio.editarFechas(INICIO, FIN), mensaje);
			rechazaPorEstado(() -> anio.cambiarRegimen(Regimen.TRIMESTRAL), mensaje);
			rechazaPorEstado(anio::quitarPeriodos, mensaje);
			assertThat(anio.getPeriodos()).hasSize(4);
		}
	}

	@Test
	void conLosDiasMinimosCadaPeriodoSigueTerminandoDespuesDeEmpezar() {
		for (Regimen regimen : Regimen.values()) {
			LocalDate fin = INICIO.plusDays(AnioEscolar.diasMinimos(regimen) - 1);

			for (PeriodoAcademico p : anio(INICIO, fin, regimen).getPeriodos()) {
				assertThat(p.getFechaFin()).isAfter(p.getFechaInicio());
			}
		}
	}

	// --- Estado calculado del periodo ---

	@Test
	void elPeriodoEstaPlanificadoAntesDeEmpezarEnCursoEntreSusFechasYCerradoDespues() {
		var anio = anioCorto();
		PeriodoAcademico segundo = periodo(anio, 2);

		assertThat(segundo.estadoEn(fecha(3, 20), EstadoAnioEscolar.ACTIVO)).isEqualTo(EstadoPeriodo.PLANIFICADO);
		assertThat(segundo.estadoEn(fecha(3, 21), EstadoAnioEscolar.ACTIVO)).isEqualTo(EstadoPeriodo.EN_CURSO);
		assertThat(segundo.estadoEn(fecha(4, 9), EstadoAnioEscolar.ACTIVO)).isEqualTo(EstadoPeriodo.EN_CURSO);
		assertThat(segundo.estadoEn(fecha(4, 10), EstadoAnioEscolar.ACTIVO)).isEqualTo(EstadoPeriodo.CERRADO);
	}

	@Test
	void unAnioCerradoCierraTodosSusPeriodosAunqueLaFechaEsteDentro() {
		var anio = anioCerrado();

		assertThat(anio.getPeriodos()).extracting(p -> p.estadoEn(fecha(3, 25), anio.getEstado()))
				.containsOnly(EstadoPeriodo.CERRADO);
	}

	@Test
	void elRegistroDeNotasEstaAbiertoHastaLaFechaLimiteInclusive() {
		var primero = periodo(anioCorto(), 1);

		assertThat(primero.registroNotasAbierto(fecha(3, 10), EstadoAnioEscolar.ACTIVO)).isTrue();
		assertThat(primero.registroNotasAbierto(fecha(3, 25), EstadoAnioEscolar.ACTIVO)).isTrue();
		assertThat(primero.registroNotasAbierto(fecha(3, 26), EstadoAnioEscolar.ACTIVO)).isFalse();
	}

	@Test
	void unAnioCerradoNoAdmiteRegistroDeNotas() {
		var primero = periodo(anioCorto(), 1);

		assertThat(primero.registroNotasAbierto(fecha(3, 10), EstadoAnioEscolar.CERRADO)).isFalse();
	}

	@Test
	void unAnioPlanificadoNoTienePeriodosEnCursoNiRegistroDeNotasAunqueLasFechasIncluyanHoy() {
		var anio = anioCorto();
		LocalDate hoy = fecha(3, 22); // dentro del II Bimestre

		assertThat(anio.getEstado()).isEqualTo(EstadoAnioEscolar.PLANIFICADO);
		assertThat(anio.getPeriodos()).extracting(p -> p.estadoEn(hoy, anio.getEstado()))
				.containsOnly(EstadoPeriodo.PLANIFICADO);
		assertThat(anio.getPeriodos()).extracting(p -> p.registroNotasAbierto(hoy, anio.getEstado()))
				.containsOnly(false);
		assertThat(anio.periodoEnCurso(hoy)).isEmpty();
	}

	@Test
	void unPeriodoFuturoDeUnAnioActivoEstaPlanificadoYSinRegistroDeNotas() {
		var anio = anioActivo();
		PeriodoAcademico tercero = periodo(anio, 3);
		LocalDate hoy = fecha(3, 22);

		assertThat(tercero.estadoEn(hoy, anio.getEstado())).isEqualTo(EstadoPeriodo.PLANIFICADO);
		assertThat(tercero.registroNotasAbierto(hoy, anio.getEstado())).isFalse();
		assertThat(tercero.registroNotasAbierto(fecha(4, 9), anio.getEstado())).isFalse();
		assertThat(tercero.registroNotasAbierto(fecha(4, 10), anio.getEstado())).isTrue();
	}

	@Test
	void unPeriodoTerminadoConLaFechaLimiteVigenteEstaCerradoPeroAunAdmiteNotas() {
		var anio = anioActivo();
		PeriodoAcademico primero = periodo(anio, 1);
		LocalDate hoy = fecha(3, 22); // terminó el 20/03 y su límite es el 25/03

		assertThat(primero.estadoEn(hoy, anio.getEstado())).isEqualTo(EstadoPeriodo.CERRADO);
		assertThat(primero.registroNotasAbierto(hoy, anio.getEstado())).isTrue();
		assertThat(primero.registroNotasAbierto(fecha(3, 26), anio.getEstado())).isFalse();
	}

	@Test
	void laDuracionEnSemanasSeRedondeaComoEnFigma() {
		// 292 días en 4 bimestres de 73 días = 10,4 semanas = "10 sem."; 20 días = 2,9 semanas = "3 sem.".
		var bimestral = anio(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 12, 18), Regimen.BIMESTRAL);

		assertThat(bimestral.getPeriodos()).extracting(PeriodoAcademico::duracionSemanas).containsExactly(10, 10, 10,
				10);
		assertThat(periodo(anioCorto(), 1).duracionSemanas()).isEqualTo(3);
	}

	@Test
	void elRegimenDiceSuUnidadEnPlural() {
		assertThat(Regimen.BIMESTRAL.unidadPlural()).isEqualTo("bimestres");
		assertThat(Regimen.TRIMESTRAL.unidadPlural()).isEqualTo("trimestres");
	}

	@Test
	void elPeriodoEnCursoEsElQueIncluyeHoy() {
		var anio = anioActivo();

		assertThat(anio.periodoEnCurso(fecha(4, 12))).get().extracting(PeriodoAcademico::getNombre)
				.isEqualTo("III Bimestre");
		assertThat(anio.periodoEnCurso(fecha(3, 20))).get().extracting(PeriodoAcademico::getNumero).isEqualTo(1);
	}

	@Test
	void elPeriodoConNotasAbiertasEsUnoYaTerminadoNuncaElPeriodoEnCurso() {
		var anio = anioActivo();

		// 22/03: el I terminó el 20/03 pero su límite es el 25/03; el II está en curso desde el 21/03.
		assertThat(anio.periodoEnCurso(fecha(3, 22))).get().extracting(PeriodoAcademico::getNumero).isEqualTo(2);
		assertThat(anio.periodoNotasAbiertas(fecha(3, 22))).get().extracting(PeriodoAcademico::getNumero)
				.isEqualTo(1);
		assertThat(anio.proximoPeriodo(fecha(3, 22))).get().extracting(PeriodoAcademico::getNumero).isEqualTo(3);
		// El día 10/03 el I está en curso: no hay ningún periodo terminado.
		assertThat(anio.periodoNotasAbiertas(fecha(3, 10))).isEmpty();
	}

	@Test
	void conVariosPeriodosTerminadosConNotasAbiertasSeElijeElMasAntiguo() {
		var anio = anioActivo();
		// Se amplía el plazo del I hasta el 20/04; el del II vence el 14/04.
		editar(anio, 1, "I Bimestre", fecha(3, 1), fecha(3, 20), fecha(4, 20), fecha(4, 12));

		assertThat(anio.periodoEnCurso(fecha(4, 12))).get().extracting(PeriodoAcademico::getNumero).isEqualTo(3);
		assertThat(anio.periodoNotasAbiertas(fecha(4, 12))).get().extracting(PeriodoAcademico::getNumero)
				.isEqualTo(1);
		// Pasado el 20/04 solo queda abierto el que vence después, si lo hay: aquí ninguno.
		assertThat(anio.periodoNotasAbiertas(fecha(4, 21))).isEmpty();
	}

	@Test
	void entreDosPeriodosLasNotasSiguenAbiertasEnElQueTerminoYElProximoEsElQueEmpieza() {
		var anio = anioActivo();
		// El II empieza el 30/03 en vez del 21/03: del 21 al 29/03 hoy no cae en ningún periodo.
		editar(anio, 2, "II Bimestre", fecha(3, 30), fecha(4, 9), fecha(4, 14), fecha(2, 1));
		LocalDate hoy = fecha(3, 22);

		assertThat(anio.periodoEnCurso(hoy)).isEmpty();
		assertThat(anio.periodoNotasAbiertas(hoy)).get().extracting(PeriodoAcademico::getNombre)
				.isEqualTo("I Bimestre");
		assertThat(anio.proximoPeriodo(hoy)).get().extracting(PeriodoAcademico::getNombre).isEqualTo("II Bimestre");
	}

	@Test
	void sinPeriodosPorEmpezarNiPlazosVigentesNoHayNadaQueMostrar() {
		var anio = anioActivo();
		LocalDate despuesDelUltimoPlazo = fecha(5, 25);

		assertThat(anio.periodoNotasAbiertas(despuesDelUltimoPlazo)).isEmpty();
		assertThat(anio.proximoPeriodo(despuesDelUltimoPlazo)).isEmpty();
		assertThat(anio.proximoPeriodo(fecha(5, 10))).isEmpty(); // el IV ya empezó el 30/04
	}

	@Test
	void unAnioPlanificadoOCerradoNoTienePeriodoConNotasAbiertasNiProximo() {
		for (AnioEscolar anio : List.of(anioCorto(), anioCerrado())) {
			assertThat(anio.periodoNotasAbiertas(fecha(3, 22))).isEmpty();
			assertThat(anio.proximoPeriodo(fecha(2, 1))).isEmpty();
		}
	}

	@Test
	void fueraDelAnioOConElAnioCerradoNoHayPeriodoEnCurso() {
		assertThat(anioActivo().periodoEnCurso(fecha(2, 1))).isEmpty();
		assertThat(anioActivo().periodoEnCurso(fecha(5, 20))).isEmpty();
		assertThat(anioCerrado().periodoEnCurso(fecha(4, 12))).isEmpty();
	}

	@Test
	void elAvanceEsElPorcentajeEnteroDeDiasTranscurridosEntre0Y100() {
		var anio = anioCorto();

		assertThat(anio.avance(fecha(2, 28))).isZero();
		assertThat(anio.avance(fecha(3, 1))).isEqualTo(1); // 1 de 80 días
		assertThat(anio.avance(fecha(4, 9))).isEqualTo(50); // 40 de 80
		assertThat(anio.avance(fecha(5, 19))).isEqualTo(100);
		assertThat(anio.avance(fecha(7, 1))).isEqualTo(100);
	}

	// --- Editar periodo ---

	@Test
	void unPeriodoFuturoCambiaNombreFechasYLimite() {
		var anio = anioCorto();

		editar(anio, 2, " Segundo Bimestre ", fecha(3, 22), fecha(4, 8), fecha(4, 20), fecha(2, 1));

		PeriodoAcademico segundo = periodo(anio, 2);
		assertThat(segundo.getNombre()).isEqualTo("Segundo Bimestre");
		assertThat(segundo.getFechaInicio()).isEqualTo(fecha(3, 22));
		assertThat(segundo.getFechaFin()).isEqualTo(fecha(4, 8));
		assertThat(segundo.getFechaLimiteNotas()).isEqualTo(fecha(4, 20));
	}

	@Test
	void unAnioActivoTambienPermiteEditarUnPeriodoFuturo() {
		var anio = anioActivo();

		editar(anio, 4, "IV Bimestre", fecha(5, 1), fecha(5, 19), fecha(5, 24), fecha(3, 10));

		assertThat(periodo(anio, 4).getFechaInicio()).isEqualTo(fecha(5, 1));
	}

	@Test
	void unAnioCerradoNoEditaSusPeriodos() {
		var anio = anioCerrado();

		rechazaPorEstado(() -> editar(anio, 1, "I Bimestre", fecha(3, 1), fecha(3, 20), fecha(3, 30), fecha(6, 1)),
				"El año escolar está cerrado: no se puede editar sus periodos.");
	}

	@Test
	void unPeriodoQueYaEmpezoNoCambiaSuFechaDeInicio() {
		var anio = anioCorto();

		rechazaPorEstado(() -> editar(anio, 1, "I Bimestre", fecha(3, 2), fecha(3, 20), fecha(3, 25), fecha(3, 10)),
				"El periodo ya empezó: no se puede cambiar.");
		// El día de inicio ya cuenta como empezado.
		rechazaPorEstado(() -> editar(anio, 2, "II Bimestre", fecha(3, 22), fecha(4, 9), fecha(4, 14), fecha(3, 21)),
				"El periodo ya empezó: no se puede cambiar.");
	}

	@Test
	void unPeriodoQueYaEmpezoSiCambiaNombreFinYLimite() {
		var anio = anioCorto();

		editar(anio, 1, "Primer Bimestre", fecha(3, 1), fecha(3, 18), fecha(3, 23), fecha(3, 10));

		PeriodoAcademico primero = periodo(anio, 1);
		assertThat(primero.getNombre()).isEqualTo("Primer Bimestre");
		assertThat(primero.getFechaFin()).isEqualTo(fecha(3, 18));
		assertThat(primero.getFechaLimiteNotas()).isEqualTo(fecha(3, 23));
	}

	@Test
	void unPeriodoQueYaTerminoSoloAmpliaLaFechaLimiteDeNotas() {
		var anio = anioCorto();

		editar(anio, 1, "I Bimestre", fecha(3, 1), fecha(3, 20), fecha(4, 30), fecha(4, 12));

		assertThat(periodo(anio, 1).getFechaLimiteNotas()).isEqualTo(fecha(4, 30));
		rechazaPorEstado(() -> editar(anio, 1, "I Bimestre", fecha(3, 1), fecha(3, 19), fecha(4, 30), fecha(4, 12)),
				"El periodo ya terminó: solo se puede cambiar la fecha límite de notas.");
		rechazaPorEstado(() -> editar(anio, 1, "Otro nombre", fecha(3, 1), fecha(3, 20), fecha(4, 30), fecha(4, 12)),
				"El periodo ya terminó: solo se puede cambiar la fecha límite de notas.");
		rechazaPorEstado(() -> editar(anio, 1, "I Bimestre", fecha(3, 2), fecha(3, 20), fecha(4, 30), fecha(4, 12)),
				"El periodo ya terminó: solo se puede cambiar la fecha límite de notas.");
	}

	@Test
	void elNombreEsObligatorio() {
		var anio = anioCorto();

		rechazaPorDato(() -> editar(anio, 2, "  ", fecha(3, 21), fecha(4, 9), fecha(4, 14), fecha(2, 1)),
				"Ingresa el nombre del periodo.");
		rechazaPorDato(() -> editar(anio, 2, null, fecha(3, 21), fecha(4, 9), fecha(4, 14), fecha(2, 1)),
				"Ingresa el nombre del periodo.");
	}

	@Test
	void lasFechasDelPeriodoDebenEstarDentroDelAnio() {
		var anio = anioCorto();
		String mensaje = "Las fechas del periodo deben estar dentro del año escolar (01/03/2028 al 19/05/2028).";

		rechazaPorDato(() -> editar(anio, 1, "I Bimestre", fecha(2, 28), fecha(3, 20), fecha(3, 25), fecha(2, 1)),
				mensaje);
		rechazaPorDato(() -> editar(anio, 4, "IV Bimestre", fecha(4, 30), fecha(5, 20), fecha(5, 25), fecha(2, 1)),
				mensaje);
	}

	@Test
	void elPeriodoNoPuedeCruzarseConOtro() {
		var anio = anioCorto();

		rechazaPorDato(() -> editar(anio, 2, "II Bimestre", fecha(3, 15), fecha(4, 9), fecha(4, 14), fecha(2, 1)),
				"Se cruza con el I Bimestre, que empieza el 01/03/2028.");
		rechazaPorDato(() -> editar(anio, 2, "II Bimestre", fecha(3, 21), fecha(4, 15), fecha(4, 20), fecha(2, 1)),
				"Se cruza con el III Bimestre, que empieza el 10/04/2028.");
	}

	@Test
	void unPeriodoPuedeTerminarElDiaAnteriorAlInicioDelSiguiente() {
		var anio = anioCorto();

		editar(anio, 2, "II Bimestre", fecha(3, 21), fecha(4, 9), fecha(4, 14), fecha(2, 1));

		assertThat(periodo(anio, 2).getFechaFin()).isEqualTo(fecha(4, 9));
	}

	@Test
	void laFechaDeFinDebeSerPosteriorALaDeInicio() {
		var anio = anioCorto();

		rechazaPorDato(() -> editar(anio, 2, "II Bimestre", fecha(3, 25), fecha(3, 25), fecha(4, 14), fecha(2, 1)),
				"La fecha de fin debe ser posterior a la fecha de inicio.");
	}

	@Test
	void laFechaLimiteNoPuedeSerAnteriorAlFinPeroPuedeIgualarlo() {
		var anio = anioCorto();

		rechazaPorDato(() -> editar(anio, 2, "II Bimestre", fecha(3, 21), fecha(4, 9), fecha(4, 8), fecha(2, 1)),
				"La fecha límite de notas no puede ser anterior al fin del periodo.");
		editar(anio, 2, "II Bimestre", fecha(3, 21), fecha(4, 9), fecha(4, 9), fecha(2, 1));
		assertThat(periodo(anio, 2).getFechaLimiteNotas()).isEqualTo(fecha(4, 9));
	}

	@Test
	void laFechaLimiteSiPuedeCaerDentroDelPeriodoSiguiente() {
		var anio = anioCorto();

		editar(anio, 1, "I Bimestre", fecha(3, 1), fecha(3, 20), fecha(4, 5), fecha(2, 1));

		assertThat(periodo(anio, 1).getFechaLimiteNotas()).isEqualTo(fecha(4, 5));
	}

	@Test
	void todasLasFechasSonObligatorias() {
		var anio = anioCorto();

		rechazaPorDato(() -> editar(anio, 2, "II Bimestre", null, fecha(4, 9), fecha(4, 14), fecha(2, 1)),
				"Ingresa las fechas del periodo y la fecha límite de notas.");
		rechazaPorDato(() -> editar(anio, 2, "II Bimestre", fecha(3, 21), fecha(4, 9), null, fecha(2, 1)),
				"Ingresa las fechas del periodo y la fecha límite de notas.");
	}

	@Test
	void unPeriodoQueNoEsDelAnioSeRechaza() {
		var anio = anioCorto();

		rechazaPorDato(() -> anio.editarPeriodo(UUID.randomUUID(), "II Bimestre", fecha(3, 21), fecha(4, 9),
				fecha(4, 14), fecha(2, 1)), "El periodo no pertenece a este año escolar.");
	}

	@Test
	void unaEdicionRechazadaNoCambiaElPeriodo() {
		var anio = anioCorto();

		assertThatThrownBy(() -> editar(anio, 2, "Nuevo", fecha(3, 15), fecha(4, 9), fecha(4, 14), fecha(2, 1)))
				.isInstanceOf(IllegalArgumentException.class);

		PeriodoAcademico segundo = periodo(anio, 2);
		assertThat(segundo.getNombre()).isEqualTo("II Bimestre");
		assertThat(segundo.getFechaInicio()).isEqualTo(fecha(3, 21));
	}

	// --- Activar y cerrar el año ---

	@Test
	void activarPasaElAnioPlanificadoAActivoSinTocarLosPeriodos() {
		var anio = anioCorto();
		List<LocalDate> fechasAntes = anio.getPeriodos().stream().map(PeriodoAcademico::getFechaInicio).toList();

		anio.activar();

		assertThat(anio.getEstado()).isEqualTo(EstadoAnioEscolar.ACTIVO);
		assertThat(anio.getPeriodos()).extracting(PeriodoAcademico::getFechaInicio).isEqualTo(fechasAntes);
		assertThat(anio.getPeriodos()).hasSize(4);
	}

	@Test
	void soloSePuedeActivarUnAnioPlanificado() {
		rechazaPorEstado(anioActivo()::activar, "Solo se puede activar un año planificado.");
		rechazaPorEstado(anioCerrado()::activar, "Solo se puede activar un año planificado.");
	}

	@Test
	void soloSePuedeCerrarUnAnioActivo() {
		rechazaPorEstado(() -> anioCorto().cerrar(fecha(6, 1)), "Solo se puede cerrar un año activo.");
		rechazaPorEstado(() -> anioCerrado().cerrar(fecha(6, 1)), "Solo se puede cerrar un año activo.");
	}

	@Test
	void noSeCierraElAnioHastaPasarLaFechaLimiteDeNotasDelUltimoPeriodo() {
		var anio = anioActivo();

		rechazaPorEstado(() -> anio.cerrar(fecha(5, 19)), "Los docentes pueden registrar notas hasta el 24/05/2028.");
		rechazaPorEstado(() -> anio.cerrar(fecha(5, 24)), "Los docentes pueden registrar notas hasta el 24/05/2028.");
		assertThat(anio.getEstado()).isEqualTo(EstadoAnioEscolar.ACTIVO);
	}

	@Test
	void elDiaDespuesDeLaFechaLimiteDelUltimoPeriodoSeCierraElAnio() {
		var anio = anioActivo();

		anio.cerrar(fecha(5, 25));

		assertThat(anio.getEstado()).isEqualTo(EstadoAnioEscolar.CERRADO);
	}

	@Test
	void unAnioCerradoNoSeReabre() {
		rechazaPorEstado(anioCerrado()::activar, "Solo se puede activar un año planificado.");
	}
}
