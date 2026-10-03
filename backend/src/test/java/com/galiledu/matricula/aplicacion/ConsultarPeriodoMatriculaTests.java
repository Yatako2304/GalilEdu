package com.galiledu.matricula.aplicacion;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.galiledu.matricula.dominio.PeriodoMatricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarPeriodoMatriculaTests {
	private final UUID anioId = UUID.randomUUID();
	private final PeriodoMatricula periodo = new PeriodoMatricula(anioId,
		LocalDate.of(2027, 1, 10), LocalDate.of(2027, 2, 20));
	private final ConsultarPeriodoMatricula consulta = new ConsultarPeriodoMatricula(
		anio -> anio.equals(anioId) ? Optional.of(periodo) : Optional.empty());

	@Test
	void habilitaElPeriodoIncluyendoInicioYFin() {
		assertThat(consulta.ejecutar(anioId, periodo.inicio()).orElseThrow().habilitado()).isTrue();
		assertThat(consulta.ejecutar(anioId, periodo.fin()).orElseThrow().habilitado()).isTrue();
	}

	@Test
	void noHabilitaFueraDelPeriodoYNuncaInventaUnoNoConfigurado() {
		assertThat(consulta.ejecutar(anioId, periodo.inicio().minusDays(1))
			.orElseThrow().habilitado()).isFalse();
		assertThat(consulta.ejecutar(anioId, periodo.fin().plusDays(1))
			.orElseThrow().habilitado()).isFalse();
		assertThat(consulta.ejecutar(UUID.randomUUID(), periodo.inicio())).isEmpty();
	}

	@Test
	void rechazaUnRangoInvertido() {
		assertThatThrownBy(() -> new PeriodoMatricula(anioId, periodo.fin(), periodo.inicio()))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
