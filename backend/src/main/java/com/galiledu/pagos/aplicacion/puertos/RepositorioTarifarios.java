package com.galiledu.pagos.aplicacion.puertos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.pagos.dominio.ConfiguracionTarifario;

public interface RepositorioTarifarios {
	UUID crear(ConfiguracionTarifario datos);
	Optional<Tarifario> buscarPorAnio(UUID anioEscolarId);

	record Tarifario(UUID id, UUID anioEscolarId, String estado,
		BigDecimal montoMatricula, BigDecimal montoPension, int cantidadCuotas,
		LocalDate vencimientoMatricula, List<LocalDate> vencimientosPensiones, int diasGracia,
		BigDecimal tasaMensual, BigDecimal topePorcentaje) {}
}
