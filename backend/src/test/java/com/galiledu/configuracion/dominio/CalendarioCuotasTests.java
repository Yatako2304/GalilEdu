package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.configuracion.dominio.CalendarioCuotas.Cuota;

class CalendarioCuotasTests {
	private static LocalDate fecha(int anio, int mes, int dia) {
		return LocalDate.of(anio, mes, dia);
	}

	@DisplayName("Reproduce el calendario de2026 de los datos de prueba")
	@Test
	void reproduceElCalendarioDe2026DeLosDatosDePrueba() {
		List<Cuota> cuotas = CalendarioCuotas.generar(fecha(2026, 3, 2), fecha(2026, 12, 18));

		assertThat(cuotas).extracting(Cuota::numero).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		assertThat(cuotas).extracting(Cuota::nombre).containsExactly("Pensión de marzo", "Pensión de abril",
				"Pensión de mayo", "Pensión de junio", "Pensión de julio", "Pensión de agosto",
				"Pensión de septiembre", "Pensión de octubre", "Pensión de noviembre", "Pensión de diciembre");
		assertThat(cuotas).extracting(Cuota::fechaVencimiento).containsExactly(fecha(2026, 3, 31), fecha(2026, 4, 30),
				fecha(2026, 5, 31), fecha(2026, 6, 30), fecha(2026, 7, 31), fecha(2026, 8, 31), fecha(2026, 9, 30),
				fecha(2026, 10, 31), fecha(2026, 11, 30), fecha(2026, 12, 15));
	}

	@DisplayName("Reproduce el calendario de2027 de los datos de prueba")
	@Test
	void reproduceElCalendarioDe2027DeLosDatosDePrueba() {
		List<Cuota> cuotas = CalendarioCuotas.generar(fecha(2027, 3, 1), fecha(2027, 12, 17));

		assertThat(cuotas).hasSize(10);
		assertThat(cuotas).extracting(Cuota::fechaVencimiento).containsExactly(fecha(2027, 3, 31), fecha(2027, 4, 30),
				fecha(2027, 5, 31), fecha(2027, 6, 30), fecha(2027, 7, 31), fecha(2027, 8, 31), fecha(2027, 9, 30),
				fecha(2027, 10, 31), fecha(2027, 11, 30), fecha(2027, 12, 15));
		assertThat(cuotas.getFirst().nombre()).isEqualTo("Pensión de marzo");
	}

	@DisplayName("Si el año termina antes del dia15 del último mes la última cuota vence el día de fin")
	@Test
	void siElAnioTerminaAntesDelDia15DelUltimoMesLaUltimaCuotaVenceElDiaDeFin() {
		List<Cuota> cuotas = CalendarioCuotas.generar(fecha(2028, 3, 1), fecha(2028, 12, 10));

		assertThat(cuotas).hasSize(10);
		assertThat(cuotas.getLast().nombre()).isEqualTo("Pensión de diciembre");
		assertThat(cuotas.getLast().fechaVencimiento()).isEqualTo(fecha(2028, 12, 10));
		assertThat(cuotas.get(8).fechaVencimiento()).isEqualTo(fecha(2028, 11, 30));
	}

	@DisplayName("Un año que termina en otro mes también tiene la última cuota el dia15")
	@Test
	void unAnioQueTerminaEnOtroMesTambienTieneLaUltimaCuotaElDia15() {
		List<Cuota> cuotas = CalendarioCuotas.generar(fecha(2028, 3, 1), fecha(2028, 11, 28));

		assertThat(cuotas).hasSize(9);
		assertThat(cuotas.getLast().nombre()).isEqualTo("Pensión de noviembre");
		assertThat(cuotas.getLast().fechaVencimiento()).isEqualTo(fecha(2028, 11, 15));
	}

	@DisplayName("Un año de un solo mes tiene una cuota")
	@Test
	void unAnioDeUnSoloMesTieneUnaCuota() {
		List<Cuota> cuotas = CalendarioCuotas.generar(fecha(2028, 3, 1), fecha(2028, 3, 28));

		assertThat(cuotas).extracting(Cuota::fechaVencimiento).containsExactly(fecha(2028, 3, 15));
	}

	@DisplayName("La matrícula vence el último día del mes anterior al inicio de clases")
	@Test
	void laMatriculaVenceElUltimoDiaDelMesAnteriorAlInicioDeClases() {
		assertThat(CalendarioCuotas.vencimientoMatricula(fecha(2026, 3, 2))).isEqualTo(fecha(2026, 2, 28));
		assertThat(CalendarioCuotas.vencimientoMatricula(fecha(2027, 3, 1))).isEqualTo(fecha(2027, 2, 28));
		assertThat(CalendarioCuotas.vencimientoMatricula(fecha(2028, 3, 15))).isEqualTo(fecha(2028, 2, 29));
		assertThat(CalendarioCuotas.vencimientoMatricula(fecha(2028, 1, 10))).isEqualTo(fecha(2027, 12, 31));
	}
}
