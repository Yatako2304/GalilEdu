package com.galiledu.academica.aplicacion;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.galiledu.academica.dominio.Calificacion;
import com.galiledu.academica.dominio.EscalaCalificacion;
import com.galiledu.academica.dominio.NivelCualitativo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificarEscalaCalificacionTests {
	private final VerificarEscalaCalificacion verificacion = new VerificarEscalaCalificacion(
		(curso, grado, competencia) -> competencia.equals("C1")
			? Optional.of(EscalaCalificacion.VIGESIMAL) : Optional.empty());

	@Test
	void aceptaNotaDeLaEscalaConfigurada() {
		assertThat(verificacion.ejecutar("Matemática", "5", "C1",
			Calificacion.vigesimal(new BigDecimal("17.5"))))
			.isEqualTo(VerificarEscalaCalificacion.Resultado.COMPATIBLE);
	}

	@Test
	void distingueEscalaIncompatibleDeEscalaNoConfigurada() {
		var nota = Calificacion.cualitativa(NivelCualitativo.AD);
		assertThat(verificacion.ejecutar("Matemática", "5", "C1", nota))
			.isEqualTo(VerificarEscalaCalificacion.Resultado.ESCALA_INCOMPATIBLE);
		assertThat(verificacion.ejecutar("Matemática", "5", "C2", nota))
			.isEqualTo(VerificarEscalaCalificacion.Resultado.ESCALA_NO_CONFIGURADA);
	}

	@Test
	void rechazaNotasFueraDeCeroAVeinte() {
		assertThatThrownBy(() -> Calificacion.vigesimal(new BigDecimal("20.1")))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Calificacion.vigesimal(new BigDecimal("-0.1")))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
