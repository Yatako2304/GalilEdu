package com.galiledu.matricula.dominio;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeleccionSeccionTests {
	private final UUID a = UUID.randomUUID();
	private final UUID b = UUID.randomUUID();
	private final UUID c = UUID.randomUUID();
	private final UUID gradoId = UUID.randomUUID();

	@Test
	void respetaLaSeccionSolicitadaCuandoTieneCupo() {
		assertThat(SeleccionSeccion.elegir(c, List.of(opcion(a, "A", 1, 0),
			opcion(c, "C", 1, 0)))).isEqualTo(c);
	}

	@Test
	void eligeLaPrimeraAlternativaAlfabeticaConCupo() {
		assertThat(SeleccionSeccion.elegir(c, List.of(opcion(c, "C", 1, 1),
			opcion(b, "B", 2, 1), opcion(a, "A", 1, 0)))).isEqualTo(a);
	}

	@Test
	void noAsignaSinCuposNiUnaSeccionAjena() {
		assertThatThrownBy(() -> SeleccionSeccion.elegir(c,
			List.of(opcion(c, "C", 1, 1), opcion(a, "A", 1, 1))))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> SeleccionSeccion.elegir(b,
			List.of(opcion(c, "C", 1, 0))))
			.isInstanceOf(java.util.NoSuchElementException.class);
		assertThatThrownBy(() -> SeleccionSeccion.elegir(c,
			List.of(opcion(c, "C", 1, 1), new SeleccionSeccion.Opcion(
				a, UUID.randomUUID(), "A", 1, 0))))
			.isInstanceOf(IllegalStateException.class);
	}

	private SeleccionSeccion.Opcion opcion(UUID id, String nombre, int capacidad, int ocupados) {
		return new SeleccionSeccion.Opcion(id, gradoId, nombre, capacidad, ocupados);
	}
}
