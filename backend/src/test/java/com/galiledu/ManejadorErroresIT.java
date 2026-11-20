package com.galiledu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Errores del cliente (ruta inexistente, id mal escrito, método no permitido) no deben responder 500: el frontend
 * mostraría "error inesperado" y en los registros se mezclarían con los errores reales del servidor.
 */
class ManejadorErroresIT extends PruebaIntegracion {

	@DisplayName("Una ruta que no existe responde404")
	@Test
	void unaRutaQueNoExisteResponde404() throws Exception {
		mvc.perform(conToken(get("/horarios/mi-seccion"), token(ADMIN)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No encontramos lo que buscas."));
	}

	@DisplayName("Un id con formato inválido responde400")
	@Test
	void unIdConFormatoInvalidoResponde400() throws Exception {
		mvc.perform(conToken(get("/usuarios/no-es-un-uuid"), token(ADMIN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La solicitud tiene un formato inválido"));
	}

	@DisplayName("Un método no permitido responde405")
	@Test
	void unMetodoNoPermitidoResponde405() throws Exception {
		mvc.perform(conToken(put("/auth/login"), token(ADMIN)))
				.andExpect(status().isMethodNotAllowed());
	}
}
