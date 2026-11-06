package com.galiledu.horarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.galiledu.configuracion.dominio.DiaSemana;

/** HU-46: el título de la alerta y el texto con título del 409 del PUT. */
class MensajesCruceTests {
	@Test
	void cadaTipoTieneSuTitulo() {
		assertThat(MensajesCruce.titulo(TipoCruce.DOCENTE)).isEqualTo("Docente no disponible");
		assertThat(MensajesCruce.titulo(TipoCruce.AMBIENTE)).isEqualTo("Ambiente no disponible");
	}

	@Test
	void conTituloAnteponeElTituloDelTipoAlMensajeDeUnCruceDeDocente() {
		var cruce = new CruceDetectado(TipoCruce.DOCENTE, DiaSemana.LUN, 1,
				"María Fernández Ríos ya dicta Álgebra con 1.° C el lunes de 8:00 a 8:45.", "1.° C", "Álgebra");

		assertThat(MensajesCruce.conTitulo(cruce)).isEqualTo(
				"Docente no disponible: María Fernández Ríos ya dicta Álgebra con 1.° C el lunes de 8:00 a 8:45.");
	}

	@Test
	void conTituloAnteponeElTituloDelTipoAlMensajeDeUnCruceDeAmbiente() {
		var cruce = new CruceDetectado(TipoCruce.AMBIENTE, DiaSemana.MIE, 3,
				"5.° B ya usa Aula 12 para Álgebra el miércoles de 10:00 a 10:45.", "5.° B", "Álgebra");

		assertThat(MensajesCruce.conTitulo(cruce))
				.isEqualTo("Ambiente no disponible: 5.° B ya usa Aula 12 para Álgebra el miércoles de 10:00 a 10:45.");
	}
}
