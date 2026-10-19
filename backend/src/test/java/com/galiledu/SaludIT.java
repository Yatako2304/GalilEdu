package com.galiledu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * El chequeo de salud lo usan Docker y el despliegue para saber si la app arrancó. Debe responder UP aunque no haya
 * SMTP configurado (el correo es opcional).
 */
class SaludIT extends PruebaIntegracion {

	@Test
	void elChequeoDeSaludRespondeUpSinAutenticacion() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}
}
