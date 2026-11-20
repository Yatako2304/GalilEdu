package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.TestContext;

class RestauradorRelojPruebaTests {
	private final RestauradorRelojPrueba restaurador = new RestauradorRelojPrueba();

	private static TestContext contextoCon(GenericApplicationContext aplicacion) {
		TestContext contexto = mock(TestContext.class);
		when(contexto.hasApplicationContext()).thenReturn(true);
		when(contexto.getApplicationContext()).thenReturn(aplicacion);
		return contexto;
	}

	@DisplayName("Después de una prueba el reloj vuelve a su fecha inicial")
	@Test
	void despuesDeUnaPruebaElRelojVuelveASuFechaInicial() {
		var reloj = new RelojPrueba();
		var aplicacion = new GenericApplicationContext();
		aplicacion.registerBean(RelojPrueba.class, () -> reloj);
		aplicacion.refresh();
		var inicial = reloj.instant();
		reloj.fijarEn(LocalDate.of(2026, 12, 20));
		assertThat(reloj.instant()).isNotEqualTo(inicial);

		restaurador.afterTestMethod(contextoCon(aplicacion));

		assertThat(reloj.instant()).isEqualTo(inicial);
	}

	@DisplayName("Sin reloj de prueba en el contexto no hace nada")
	@Test
	void sinRelojDePruebaEnElContextoNoHaceNada() {
		var aplicacion = new GenericApplicationContext();
		aplicacion.refresh();

		restaurador.afterTestMethod(contextoCon(aplicacion));
	}

	@DisplayName("Sin contexto cargado no lo carga")
	@Test
	void sinContextoCargadoNoLoCarga() {
		TestContext contexto = mock(TestContext.class);
		when(contexto.hasApplicationContext()).thenReturn(false);

		restaurador.afterTestMethod(contexto);

		org.mockito.Mockito.verify(contexto, org.mockito.Mockito.never()).getApplicationContext();
	}
}
