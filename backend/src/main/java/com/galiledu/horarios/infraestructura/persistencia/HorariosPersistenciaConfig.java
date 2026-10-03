package com.galiledu.horarios.infraestructura.persistencia;

import com.galiledu.horarios.aplicacion.ConsultarHorarios;
import com.galiledu.horarios.aplicacion.VerificarDisponibilidadHorario;
import com.galiledu.horarios.aplicacion.puertos.ConsultaHorarioSemanal;
import com.galiledu.horarios.aplicacion.puertos.ConsultaOcupacionesHorario;
import com.galiledu.horarios.dominio.ValidadorCrucesHorario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HorariosPersistenciaConfig {
	@Bean
	ValidadorCrucesHorario validadorCrucesHorario() {
		return new ValidadorCrucesHorario();
	}

	@Bean
	VerificarDisponibilidadHorario verificarDisponibilidadHorario(ConsultaOcupacionesHorario ocupaciones,
		ValidadorCrucesHorario validador) {
		return new VerificarDisponibilidadHorario(ocupaciones, validador);
	}

	@Bean
	ConsultarHorarios consultarHorarios(ConsultaHorarioSemanal consulta) {
		return new ConsultarHorarios(consulta);
	}
}
