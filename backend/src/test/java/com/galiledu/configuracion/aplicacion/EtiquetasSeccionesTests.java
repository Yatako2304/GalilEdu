package com.galiledu.configuracion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionConVacantes;

class EtiquetasSeccionesTests {
	private static SeccionConVacantes seccion(String grado, String nombre) {
		return new SeccionConVacantes(UUID.randomUUID(), grado, grado, nombre, null, 30, 0);
	}

	private static List<SeccionConVacantes> secciones(String nivel, int hasta, String... nombres) {
		List<SeccionConVacantes> lista = new ArrayList<>();
		for (int grado = 1; grado <= hasta; grado++) {
			for (String nombre : nombres) {
				lista.add(seccion(nivel + "-" + grado, nombre));
			}
		}
		return lista;
	}

	@DisplayName("La etiqueta usa el número del grado y el nombre de la sección")
	@Test
	void laEtiquetaUsaElNumeroDelGradoYElNombreDeLaSeccion() {
		assertThat(EtiquetasSecciones.etiqueta("SEC-1", "A")).isEqualTo("1.° A");
		assertThat(EtiquetasSecciones.etiqueta("PRI-6", "B")).isEqualTo("6.° B");
		assertThat(EtiquetasSecciones.numeroDeGrado("SEC-4")).isEqualTo(4);
	}

	@DisplayName("El resumen de un solo nivel es el de figma")
	@Test
	void elResumenDeUnSoloNivelEsElDeFigma() {
		assertThat(EtiquetasSecciones.resumen(secciones("SEC", 5, "A", "B"))).isEqualTo("1.° a 5.°, A y B");
	}

	@DisplayName("Un solo grado no lleva rango")
	@Test
	void unSoloGradoNoLlevaRango() {
		assertThat(EtiquetasSecciones.resumen(secciones("SEC", 1, "A"))).isEqualTo("1.°, A");
	}

	@DisplayName("Los nombres se unen con comas y una y")
	@Test
	void losNombresSeUnenConComasYUnaY() {
		assertThat(EtiquetasSecciones.resumen(secciones("SEC", 2, "A", "B", "C"))).isEqualTo("1.° a 2.°, A, B y C");
	}

	@DisplayName("Con dos niveles se indica cada uno")
	@Test
	void conDosNivelesSeIndicaCadaUno() {
		List<SeccionConVacantes> todas = new ArrayList<>(secciones("PRI", 6, "A"));
		todas.addAll(secciones("SEC", 5, "A", "B"));

		assertThat(EtiquetasSecciones.resumen(todas)).isEqualTo("Primaria 1.° a 6.°, Secundaria 1.° a 5.°, A y B");
	}

	@DisplayName("Sin secciones el resumen está vacio")
	@Test
	void sinSeccionesElResumenEstaVacio() {
		assertThat(EtiquetasSecciones.resumen(List.of())).isEmpty();
	}
}
