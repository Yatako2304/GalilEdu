package com.galiledu.pagos.infraestructura.persistencia;

import com.galiledu.pagos.aplicacion.VerificarMontoConfirmado;
import com.galiledu.pagos.aplicacion.puertos.ConsultaOrdenPago;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PagosPersistenciaConfig {
	@Bean
	VerificarMontoConfirmado verificarMontoConfirmado(ConsultaOrdenPago ordenes) {
		return new VerificarMontoConfirmado(ordenes);
	}
}
