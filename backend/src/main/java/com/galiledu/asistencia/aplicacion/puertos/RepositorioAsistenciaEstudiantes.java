package com.galiledu.asistencia.aplicacion.puertos;

import java.time.LocalDate;
import java.util.Optional;

import com.galiledu.asistencia.dominio.RegistroAsistenciaEstudiante;

/** Acceso a las marcaciones de estudiantes sin fijar tablas ni tecnología. */
public interface RepositorioAsistenciaEstudiantes {
	Optional<RegistroAsistenciaEstudiante> buscar(String estudiante, LocalDate fecha);

	void guardar(RegistroAsistenciaEstudiante registro);
}
