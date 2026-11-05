package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

/** Stub del contrato de la Fase 0. TODO (Jose, HU-45): reemplazar por la lectura SQL real. */
@Component
public class OcupacionHorariosStub implements OcupacionHorarios {
	@Override
	public List<BloqueOcupado> bloquesOcupados(UUID anioId, UUID excluirSeccionId) {
		return List.of();
	}
}
