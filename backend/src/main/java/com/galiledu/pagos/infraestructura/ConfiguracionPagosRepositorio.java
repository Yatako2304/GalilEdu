package com.galiledu.pagos.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galiledu.pagos.dominio.ConfiguracionPagos;

public interface ConfiguracionPagosRepositorio extends JpaRepository<ConfiguracionPagos, Short> {
}
