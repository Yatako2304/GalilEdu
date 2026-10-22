package com.galiledu.configuracion;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Devuelve el {@link RelojPrueba} a su fecha inicial después de cada prueba, para que una prueba que lo movió no afecte
 * a las demás (el contexto de Spring se comparte entre clases). Lo registra {@link PruebaIntegracionConfiguracion}; si
 * el
 * contexto no tiene ese reloj, no hace nada.
 */
public class RestauradorRelojPrueba implements TestExecutionListener {
	@Override
	public void afterTestMethod(TestContext contexto) {
		if (contexto.hasApplicationContext()) {
			contexto.getApplicationContext().getBeanProvider(RelojPrueba.class).ifAvailable(RelojPrueba::reiniciar);
		}
	}
}
