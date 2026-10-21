package com.galiledu.configuracion.infraestructura;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.configuracion.dominio.AnioEscolar;

public interface AnioEscolarRepositorio extends JpaRepository<AnioEscolar, UUID> {
	boolean existsByAnio(short anio);
}
