package com.galiledu.horarios.aplicacion;

import java.util.List;

import com.galiledu.horarios.dominio.CruceHorario;

/** La asignación no se guardó porque choca con otra; informa cada conflicto (RF-117, RF-118). */
public class CruceHorarioException extends RuntimeException {
	private final List<CruceHorario> cruces;

	public CruceHorarioException(List<CruceHorario> cruces) {
		super("La asignación tiene " + cruces.size() + " cruce(s) de horario");
		this.cruces = List.copyOf(cruces);
	}

	public List<CruceHorario> cruces() {
		return cruces;
	}
}
