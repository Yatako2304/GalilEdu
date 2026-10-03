package com.galiledu.matricula.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;

import com.galiledu.matricula.dominio.PeriodoMatricula;

/** Lectura del periodo de matrícula, distinto del periodo de clases. */
public interface ConsultaPeriodoMatricula {
	Optional<PeriodoMatricula> buscarPorAnio(UUID anioEscolarId);
}
