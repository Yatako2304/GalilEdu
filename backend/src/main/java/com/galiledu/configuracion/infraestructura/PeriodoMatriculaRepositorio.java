package com.galiledu.configuracion.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.configuracion.dominio.EstadoPeriodoMatricula;
import com.galiledu.configuracion.dominio.PeriodoMatricula;

public interface PeriodoMatriculaRepositorio extends JpaRepository<PeriodoMatricula, UUID> {
	/** Hay uno por año (matricula.periodo_matricula.anio_escolar_id es UNIQUE). */
	Optional<PeriodoMatricula> findByAnioEscolarId(UUID anioEscolarId);

	List<PeriodoMatricula> findByEstado(EstadoPeriodoMatricula estado);

	/** El más reciente primero, como el listado de la pantalla 13. */
	List<PeriodoMatricula> findAllByOrderByInicioDesc();
}
