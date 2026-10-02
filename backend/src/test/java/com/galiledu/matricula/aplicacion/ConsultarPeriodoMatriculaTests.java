package com.galiledu.matricula.aplicacion;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.galiledu.matricula.dominio.PeriodoMatricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarPeriodoMatriculaTests {
	private final PeriodoMatricula periodo = new PeriodoMatricula(2027,
		LocalDate.of(2027, 1, 10), LocalDate.of(2027, 2, 20));
	private final ConsultarPeriodoMatricula consulta = new ConsultarPeriodoMatricula(
		anio -> anio == 2027 ? Optional.of(periodo) : Optional.empty());

	@Test
	void habilitaElPeriodoIncluyendoInicioYFin() {
		assertThat(consulta.ejecutar(2027, periodo.inicio()).orElseThrow().habilitado()).isTrue();
		assertThat(consulta.ejecutar(2027, periodo.fin()).orElseThrow().habilitado()).isTrue();
	}

	@Test
	void noHabilitaFueraDelPeriodoYNuncaInventaUnoNoConfigurado() {
		assertThat(consulta.ejecutar(2027, periodo.inicio().minusDays(1))
			.orElseThrow().habilitado()).isFalse();
		assertThat(consulta.ejecutar(2027, periodo.fin().plusDays(1))
			.orElseThrow().habilitado()).isFalse();
		assertThat(consulta.ejecutar(2028, periodo.inicio())).isEmpty();
	}

	@Test
	void rechazaUnRangoInvertido() {
		assertThatThrownBy(() -> new PeriodoMatricula(2027, periodo.fin(), periodo.inicio()))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
