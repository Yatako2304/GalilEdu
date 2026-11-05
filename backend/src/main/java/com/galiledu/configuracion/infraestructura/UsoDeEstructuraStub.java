package com.galiledu.configuracion.infraestructura;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.galiledu.configuracion.aplicacion.UsoDeEstructura;
import com.galiledu.configuracion.dominio.DiaSemana;

/** Stub del contrato de la Fase 0. TODO (Jose, HU-45): reemplazar por la lectura SQL de horarios.horario_bloque. */
@Component
public class UsoDeEstructuraStub implements UsoDeEstructura {
	@Override
	public Map<DiaSemana, Integer> usoPorDia(UUID estructuraId) {
		return Map.of();
	}
}
