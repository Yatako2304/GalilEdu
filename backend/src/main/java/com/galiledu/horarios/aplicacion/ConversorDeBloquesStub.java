package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

/** Stub del contrato de la Fase 0. TODO (Jose, HU-45): calcular las horas y resolver el docente. */
@Component
public class ConversorDeBloquesStub implements ConversorDeBloques {
	@Override
	public List<BloqueSolicitado> aBloquesSolicitados(UUID seccionId, List<CeldaHorario> celdas) {
		return List.of();
	}
}
