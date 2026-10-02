package com.galiledu.academica.aplicacion.puertos;

import java.util.Optional;

import com.galiledu.academica.dominio.EscalaCalificacion;

/** Consulta la escala elegida para una competencia de un curso y grado (RF-130). */
public interface ConsultaEscalaCompetencia {
	Optional<EscalaCalificacion> buscar(String curso, String grado, String competencia);
}
