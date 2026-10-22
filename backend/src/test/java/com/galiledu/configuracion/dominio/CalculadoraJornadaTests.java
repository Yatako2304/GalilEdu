package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.galiledu.configuracion.dominio.CalculadoraJornada.FilaJornada;
import com.galiledu.configuracion.dominio.CalculadoraJornada.Jornada;
import com.galiledu.configuracion.dominio.CalculadoraJornada.TipoFila;

/** HU-44: el cálculo de la jornada con los ejemplos de Figma, sin base de datos. */
class CalculadoraJornadaTests {
	private static int minutos(int hora, int minuto) {
		return hora * 60 + minuto;
	}

	@Test
	void primariaDeFigmaEntraALas0745ySaleALas1420() {
		// CI-12, EH-PRI-26: 8 bloques de 45 min, recesos de 20 min tras el 3 y de 15 min tras el 6.
		Jornada jornada = CalculadoraJornada.calcular(LocalTime.of(7, 45), 8, 45,
				List.of(new DatosReceso(3, 20), new DatosReceso(6, 15)));

		assertThat(jornada.salidaMinutos()).isEqualTo(minutos(14, 20));
		assertThat(jornada.filas()).hasSize(10);
		assertThat(jornada.filas().get(0))
				.isEqualTo(new FilaJornada(TipoFila.BLOQUE, 1, minutos(7, 45), minutos(8, 30)));
		// El receso largo va entre el bloque 3 (09:15–10:00) y el 4 (10:20–11:05).
		assertThat(jornada.filas().get(3))
				.isEqualTo(new FilaJornada(TipoFila.RECESO, null, minutos(10, 0), minutos(10, 20)));
		assertThat(jornada.filas().get(3).duracionMinutos()).isEqualTo(20);
		assertThat(jornada.filas().get(4))
				.isEqualTo(new FilaJornada(TipoFila.BLOQUE, 4, minutos(10, 20), minutos(11, 5)));
		assertThat(jornada.filas().get(7))
				.isEqualTo(new FilaJornada(TipoFila.RECESO, null, minutos(12, 35), minutos(12, 50)));
		assertThat(jornada.filas().get(9))
				.isEqualTo(new FilaJornada(TipoFila.BLOQUE, 8, minutos(13, 35), minutos(14, 20)));
	}

	@Test
	void elOrdenEnQueLlegaLaListaDeRecesosNoCambiaElResultado() {
		Jornada enOrden = CalculadoraJornada.calcular(LocalTime.of(7, 45), 8, 45,
				List.of(new DatosReceso(3, 20), new DatosReceso(6, 15)));
		Jornada alReves = CalculadoraJornada.calcular(LocalTime.of(7, 45), 8, 45,
				List.of(new DatosReceso(6, 15), new DatosReceso(3, 20)));

		assertThat(alReves).isEqualTo(enOrden);
	}

	@Test
	void secundariaDeFigmaSaleALas1450() {
		// CI-12, EH-SEC-26: entrada 07:30, 9 bloques de 45 min y los mismos recesos.
		Jornada jornada = CalculadoraJornada.calcular(LocalTime.of(7, 30), 9, 45,
				List.of(new DatosReceso(3, 20), new DatosReceso(6, 15)));

		assertThat(jornada.salidaMinutos()).isEqualTo(minutos(14, 50));
	}

	@Test
	void elModalDeFigmaTerminaExactamenteALas1800() {
		// Modal: entrada 14:45, 4 bloques de 45 min, receso de 15 min tras el 2.
		Jornada jornada = CalculadoraJornada.calcular(LocalTime.of(14, 45), 4, 45, List.of(new DatosReceso(2, 15)));

		assertThat(jornada.salidaMinutos()).isEqualTo(minutos(18, 0));
		assertThat(jornada.filas()).extracting(FilaJornada::tipo)
				.containsExactly(TipoFila.BLOQUE, TipoFila.BLOQUE, TipoFila.RECESO, TipoFila.BLOQUE, TipoFila.BLOQUE);
		assertThat(jornada.filas().get(2))
				.isEqualTo(new FilaJornada(TipoFila.RECESO, null, minutos(16, 15), minutos(16, 30)));
		assertThat(jornada.filas().get(4))
				.isEqualTo(new FilaJornada(TipoFila.BLOQUE, 4, minutos(17, 15), minutos(18, 0)));
	}

	@Test
	void sinRecesosLosBloquesVanSeguidos() {
		Jornada jornada = CalculadoraJornada.calcular(LocalTime.of(8, 0), 3, 40, List.of());

		assertThat(jornada.filas()).extracting(FilaJornada::tipo).containsOnly(TipoFila.BLOQUE);
		assertThat(jornada.filas()).extracting(FilaJornada::numero).containsExactly(1, 2, 3);
		assertThat(jornada.salidaMinutos()).isEqualTo(minutos(10, 0));
	}

	@Test
	void pasadaLaMedianocheLaSalidaNoDaLaVuelta() {
		// Con LocalTime, 23:00 + 4 × 60 min serían las 03:00 y parecería una jornada válida.
		Jornada jornada = CalculadoraJornada.calcular(LocalTime.of(23, 0), 4, 60, List.of());

		assertThat(jornada.salidaMinutos()).isEqualTo(minutos(27, 0));
		assertThat(CalculadoraJornada.formatoHora(jornada.salidaMinutos())).isEqualTo("27:00");
	}

	@Test
	void formateaLasHorasConDosDigitos() {
		assertThat(CalculadoraJornada.formatoHora(minutos(7, 5))).isEqualTo("07:05");
		assertThat(CalculadoraJornada.formatoHora(minutos(14, 20))).isEqualTo("14:20");
	}
}
