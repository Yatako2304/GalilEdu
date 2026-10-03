package com.galiledu.matricula.aplicacion.puertos;

import com.galiledu.matricula.dominio.PeriodoMatricula;

/** Persists one regular enrollment window per school year. */
public interface RegistroPeriodoMatricula {
	void guardar(PeriodoMatricula periodo);
}
