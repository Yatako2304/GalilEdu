package com.galiledu.asistencia.infraestructura;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.asistencia.dominio.RegistroAsistencia;

public interface RegistroAsistenciaRepositorio extends JpaRepository<RegistroAsistencia, UUID> {
	Optional<RegistroAsistencia> findBySeccionIdAndFecha(UUID seccionId, LocalDate fecha);
}
