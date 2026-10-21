package com.galiledu.configuracion.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;

public interface AnioEscolarRepositorio extends JpaRepository<AnioEscolar, UUID> {
	boolean existsByAnio(short anio);

	/** Una lista porque solo ACTIVO es único (ux_anio_escolar_un_activo); puede haber varios PLANIFICADO o CERRADO. */
	List<AnioEscolar> findByEstado(EstadoAnioEscolar estado);

	boolean existsByEstado(EstadoAnioEscolar estado);

	/** El año al que pertenece un periodo. */
	Optional<AnioEscolar> findByPeriodosId(UUID periodoId);
}
