package com.galiledu.configuracion.dominio;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatosCatalogoCurricularTests {
	@Test
	void exigeDescripcionYNormalizaLosTextosDelArea() {
		var datos = new DatosAreaCurricular(" Matemática ", " Área de matemáticas ");
		assertThat(datos.nombre()).isEqualTo("Matemática");
		assertThat(datos.descripcion()).isEqualTo("Área de matemáticas");
		assertThatThrownBy(() -> new DatosAreaCurricular("Matemática", " "))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void competenciaNecesitaAreaNombreYDescripcion() {
		UUID areaId = UUID.randomUUID();
		assertThat(new DatosCompetencia(areaId, " Resuelve problemas ", " Descripción ").nombre())
			.isEqualTo("Resuelve problemas");
		assertThatThrownBy(() -> new DatosCompetencia(null, "Nombre", "Descripción"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new DatosCompetencia(areaId, " ", "Descripción"))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
