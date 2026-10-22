package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import com.galiledu.configuracion.dominio.CalendarioCuotas.Cuota;

class TarifarioTests {
	private static final LocalDate INICIO = LocalDate.of(2027, 3, 1);
	private static final LocalDate FIN = LocalDate.of(2027, 12, 17);

	private static BigDecimal d(String valor) {
		return new BigDecimal(valor);
	}

	/** Los datos de P19 para 2027: S/ 500, S/ 450, S/ 620, 5 días de gracia, 2 % y tope 10 %. */
	private static DatosTarifario datos() {
		return new DatosTarifario(d("500"), d("450"), d("620"), 5, d("2"), d("10"));
	}

	private static Tarifario confirmado() {
		Tarifario t = Tarifario.nuevo(UUID.randomUUID());
		t.confirmar(datos(), INICIO, FIN);
		return t;
	}

	private static void rechaza(String mensaje, ThrowingCallable accion) {
		assertThatThrownBy(accion).isInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
	}

	private static void rechazaDatos(String mensaje, DatosTarifario datos) {
		rechaza(mensaje, () -> Tarifario.nuevo(UUID.randomUUID()).confirmar(datos, INICIO, FIN));
	}

	@Test
	void unTarifarioNuevoNoEstaConfirmado() {
		Tarifario t = Tarifario.nuevo(UUID.randomUUID());

		assertThat(t.getEstado()).isEqualTo(EstadoTarifario.EN_CONFIGURACION);
		assertThat(t.estaConfirmado()).isFalse();
	}

	@Test
	void confirmarGuardaLosDatosGeneraElCalendarioYDejaElTarifarioConfirmado() {
		Tarifario t = confirmado();

		assertThat(t.estaConfirmado()).isTrue();
		assertThat(t.getMontoCuotaIngreso()).isEqualByComparingTo("500");
		assertThat(t.getMontoMatricula()).isEqualByComparingTo("450");
		assertThat(t.getMontoPension()).isEqualByComparingTo("620");
		assertThat(t.getDiasGracia()).isEqualTo(5);
		assertThat(t.getRecargoPorcentaje()).isEqualByComparingTo("2");
		assertThat(t.getTopeRecargoPorcentaje()).isEqualByComparingTo("10");
		assertThat(t.getVencimientoMatricula()).isEqualTo(LocalDate.of(2027, 2, 28));
		assertThat(t.getCantidadCuotas()).isEqualTo(10);
		assertThat(t.getCuotas()).hasSize(10);
		assertThat(t.getCuotas().getLast().getFechaVencimiento()).isEqualTo(LocalDate.of(2027, 12, 15));
		assertThat(t.totalPensiones()).isEqualByComparingTo("6200");
	}

	@Test
	void laCuotaDeIngresoEsOpcional() {
		Tarifario t = Tarifario.nuevo(UUID.randomUUID());
		t.confirmar(new DatosTarifario(null, d("450"), d("620"), 5, d("2"), d("10")), INICIO, FIN);

		assertThat(t.getMontoCuotaIngreso()).isNull();
		assertThat(t.estaConfirmado()).isTrue();
	}

	@Test
	void unTarifarioConfirmadoSePuedeConfirmarOtraVezConOtrosDatos() {
		Tarifario t = confirmado();
		t.quitarCuotas();

		t.confirmar(new DatosTarifario(d("400"), d("460"), d("650.50"), 7, d("3"), d("12")), INICIO,
				LocalDate.of(2027, 11, 20));

		assertThat(t.estaConfirmado()).isTrue();
		assertThat(t.getMontoMatricula()).isEqualByComparingTo("460");
		assertThat(t.getMontoPension()).isEqualByComparingTo("650.50");
		assertThat(t.getCantidadCuotas()).isEqualTo(9);
		assertThat(t.getCuotas()).hasSize(9);
		assertThat(t.getCuotas().getLast().getFechaVencimiento()).isEqualTo(LocalDate.of(2027, 11, 15));
	}

	@Test
	void unDatoInvalidoNoCambiaElTarifario() {
		Tarifario t = confirmado();

		assertThatThrownBy(() -> t.confirmar(new DatosTarifario(d("500"), d("999"), d("-1"), 5, d("2"), d("10")),
				INICIO, FIN)).isInstanceOf(IllegalArgumentException.class);

		assertThat(t.getMontoMatricula()).isEqualByComparingTo("450");
		assertThat(t.getCuotas()).hasSize(10);
	}

	@Test
	void regenerarElCalendarioConservaMontosMorosidadYEstado() {
		Tarifario t = confirmado();
		t.quitarCuotas();

		t.regenerarCalendario(LocalDate.of(2027, 4, 5), LocalDate.of(2027, 12, 10));

		assertThat(t.getCantidadCuotas()).isEqualTo(9);
		assertThat(t.getCuotas()).extracting(CuotaTarifario::getNombre).first().isEqualTo("Pensión de abril");
		assertThat(t.getCuotas().getLast().getFechaVencimiento()).isEqualTo(LocalDate.of(2027, 12, 10));
		assertThat(t.getVencimientoMatricula()).isEqualTo(LocalDate.of(2027, 3, 31));
		assertThat(t.getMontoPension()).isEqualByComparingTo("620");
		assertThat(t.getDiasGracia()).isEqualTo(5);
		assertThat(t.estaConfirmado()).isTrue();
	}

	// --- Montos ---

	@Test
	void laMatriculaEsObligatoriaYNoPuedeSerNegativa() {
		rechazaDatos("Ingresa el monto de la matrícula.",
				new DatosTarifario(d("500"), null, d("620"), 5, d("2"), d("10")));
		rechazaDatos("El monto de la matrícula no puede ser negativo.",
				new DatosTarifario(d("500"), d("-1"), d("620"), 5, d("2"), d("10")));
	}

	@Test
	void laMatriculaPuedeSerCero() {
		Tarifario t = Tarifario.nuevo(UUID.randomUUID());
		t.confirmar(new DatosTarifario(d("500"), d("0"), d("620"), 5, d("2"), d("10")), INICIO, FIN);

		assertThat(t.getMontoMatricula()).isEqualByComparingTo("0");
	}

	@Test
	void laPensionEsObligatoriaYMayorQueCero() {
		rechazaDatos("Ingresa el monto de la pensión.",
				new DatosTarifario(d("500"), d("450"), null, 5, d("2"), d("10")));
		rechazaDatos("El monto de la pensión debe ser mayor que cero.",
				new DatosTarifario(d("500"), d("450"), d("0"), 5, d("2"), d("10")));
		rechazaDatos("El monto de la pensión no puede ser negativo.",
				new DatosTarifario(d("500"), d("450"), d("-5"), 5, d("2"), d("10")));
	}

	@Test
	void laCuotaDeIngresoNoPuedeSerNegativa() {
		rechazaDatos("El monto de la cuota de ingreso no puede ser negativo.",
				new DatosTarifario(d("-1"), d("450"), d("620"), 5, d("2"), d("10")));
	}

	@Test
	void losMontosAdmitenComoMaximo2DecimalesYElLimiteDeLaColumna() {
		rechazaDatos("El monto de la pensión admite hasta 2 decimales.",
				new DatosTarifario(d("500"), d("450"), d("620.505"), 5, d("2"), d("10")));
		rechazaDatos("El monto de la matrícula no puede superar S/ 99999999.99.",
				new DatosTarifario(d("500"), d("100000000"), d("620"), 5, d("2"), d("10")));
		Tarifario t = Tarifario.nuevo(UUID.randomUUID());
		t.confirmar(new DatosTarifario(d("500"), d("450.50"), d("620.00"), 5, d("2"), d("10")), INICIO, FIN);
		assertThat(t.getMontoMatricula()).isEqualByComparingTo("450.5");
	}

	// --- Morosidad (RF-72) ---

	@Test
	void losDiasDeGraciaVanDe0A60() {
		rechazaDatos("Ingresa los días de gracia.",
				new DatosTarifario(d("500"), d("450"), d("620"), null, d("2"), d("10")));
		rechazaDatos("Los días de gracia deben estar entre 0 y 60.",
				new DatosTarifario(d("500"), d("450"), d("620"), -1, d("2"), d("10")));
		rechazaDatos("Los días de gracia deben estar entre 0 y 60.",
				new DatosTarifario(d("500"), d("450"), d("620"), 61, d("2"), d("10")));
		Tarifario t = Tarifario.nuevo(UUID.randomUUID());
		t.confirmar(new DatosTarifario(d("500"), d("450"), d("620"), 60, d("2"), d("10")), INICIO, FIN);
		assertThat(t.getDiasGracia()).isEqualTo(60);
	}

	@Test
	void elRecargoVaDe0A100() {
		rechazaDatos("Ingresa el recargo por cada 30 días.",
				new DatosTarifario(d("500"), d("450"), d("620"), 5, null, d("10")));
		rechazaDatos("El recargo debe estar entre 0 y 100 %.",
				new DatosTarifario(d("500"), d("450"), d("620"), 5, d("-1"), d("10")));
		rechazaDatos("El recargo debe estar entre 0 y 100 %.",
				new DatosTarifario(d("500"), d("450"), d("620"), 5, d("100.5"), d("100")));
		rechazaDatos("El recargo admite hasta 2 decimales.",
				new DatosTarifario(d("500"), d("450"), d("620"), 5, d("2.555"), d("10")));
	}

	@Test
	void elTopeDeRecargoEsObligatorioYVaDe0A100() {
		rechazaDatos("Ingresa el tope de recargo.", new DatosTarifario(d("500"), d("450"), d("620"), 5, d("2"), null));
		rechazaDatos("El tope de recargo debe estar entre 0 y 100 %.",
				new DatosTarifario(d("500"), d("450"), d("620"), 5, d("2"), d("101")));
		rechazaDatos("El tope de recargo debe estar entre 0 y 100 %.",
				new DatosTarifario(d("500"), d("450"), d("620"), 5, d("0"), d("-1")));
	}

	@Test
	void elRecargoNoPuedeSerMayorQueElTope() {
		rechazaDatos("El recargo no puede ser mayor que el tope de recargo.",
				new DatosTarifario(d("500"), d("450"), d("620"), 5, d("11"), d("10")));
		Tarifario t = Tarifario.nuevo(UUID.randomUUID());
		t.confirmar(new DatosTarifario(d("500"), d("450"), d("620"), 5, d("10"), d("10")), INICIO, FIN);
		assertThat(t.estaConfirmado()).isTrue();
	}

	// --- Calendario (RF-71) ---

	@Test
	void elCalendarioNoPuedeTenerMasDe12Cuotas() {
		rechaza("El calendario debe tener entre 1 y 12 cuotas, pero el año escolar abarca 13 meses.",
				() -> Tarifario.nuevo(UUID.randomUUID()).confirmar(datos(), LocalDate.of(2027, 3, 1),
						LocalDate.of(2028, 3, 20)));
	}

	@Test
	void unAnioConUnaCuotaFueraDelAnioSeRechaza() {
		// 20/03 al 25/03: la única cuota vence el 15/03, antes del inicio.
		rechaza("Las cuotas deben vencer dentro del año escolar (20/03/2027 al 25/03/2027).",
				() -> Tarifario.nuevo(UUID.randomUUID()).confirmar(datos(), LocalDate.of(2027, 3, 20),
						LocalDate.of(2027, 3, 25)));
	}

	@Test
	void noPermiteDosCuotasConLaMismaFechaDeVencimiento() {
		List<Cuota> repetidas = List.of(new Cuota(1, "Pensión de marzo", LocalDate.of(2027, 3, 31)),
				new Cuota(2, "Pensión de abril", LocalDate.of(2027, 3, 31)));

		rechaza("No puede haber dos cuotas con la misma fecha de vencimiento.",
				() -> Tarifario.validarCalendario(repetidas, INICIO, FIN));
	}

	@Test
	void unCalendarioVacioSeRechaza() {
		rechaza("El calendario debe tener entre 1 y 12 cuotas, pero el año escolar abarca 0 meses.",
				() -> Tarifario.validarCalendario(List.of(), INICIO, FIN));
	}
}
