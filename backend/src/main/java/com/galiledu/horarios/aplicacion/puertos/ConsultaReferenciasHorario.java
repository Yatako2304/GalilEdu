package com.galiledu.horarios.aplicacion.puertos;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.galiledu.horarios.dominio.DiaSemana;

/**
 * Lectura de datos de Configuración Institucional y Matrícula que Horarios necesita para
 * validar. Configuración no tiene módulo propio todavía, por lo que este puerto es el contrato
 * de solo lectura; no modifica nada.
 */
public interface ConsultaReferenciasHorario {
	Optional<SeccionReferencia> buscarSeccion(UUID id);

	Optional<CargaAcademicaReferencia> buscarCargaAcademica(UUID id);

	Optional<BloqueReferencia> buscarBloque(UUID id);

	/** Devuelve solo los periodos que existen. */
	List<PeriodoReferencia> buscarPeriodos(Collection<UUID> ids);

	boolean existeEspacioActivo(UUID id);

	Optional<MatriculaReferencia> buscarMatricula(UUID id);

	/** {@code nombre} incluye el grado, por ejemplo «Primero A». */
	record SeccionReferencia(UUID id, UUID anioId, String nombre, boolean activa) {}

	record CargaAcademicaReferencia(UUID id, UUID seccionId, UUID docenteId, String curso, boolean activa) {}

	/** El año y los días lectivos vienen de la estructura horaria a la que pertenece el bloque. */
	record BloqueReferencia(UUID id, UUID anioId, Set<DiaSemana> diasLectivos, boolean activo) {}

	record PeriodoReferencia(UUID id, UUID anioId, boolean activo) {}

	record MatriculaReferencia(UUID id, UUID seccionId, boolean activa) {}
}
