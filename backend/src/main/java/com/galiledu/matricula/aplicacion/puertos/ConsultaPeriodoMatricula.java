package com.galiledu.matricula.aplicacion.puertos;

import java.util.Optional;

import com.galiledu.matricula.dominio.PeriodoMatricula;

/** Lectura del periodo configurado; el adaptador definitivo usará el esquema aprobado. */
public interface ConsultaPeriodoMatricula {
	Optional<PeriodoMatricula> buscarPorAnio(int anioEscolar);
}
