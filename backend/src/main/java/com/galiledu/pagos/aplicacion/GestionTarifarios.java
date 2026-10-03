package com.galiledu.pagos.aplicacion;

import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.ConsultaAnioEscolar;
import com.galiledu.pagos.aplicacion.puertos.RepositorioTarifarios;
import com.galiledu.pagos.dominio.ConfiguracionTarifario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionTarifarios {
	private final ConsultaAnioEscolar anios;
	private final RepositorioTarifarios tarifarios;

	public GestionTarifarios(ConsultaAnioEscolar anios, RepositorioTarifarios tarifarios) {
		this.anios = anios;
		this.tarifarios = tarifarios;
	}

	@Transactional
	public RepositorioTarifarios.Tarifario registrar(ConfiguracionTarifario datos) {
		var anio = anios.buscar(datos.anioEscolarId())
			.orElseThrow(() -> new NoSuchElementException("Año escolar no encontrado"));
		if ("CERRADO".equals(anio.estado())) {
			throw new IllegalStateException("El tarifario de un año escolar cerrado es histórico");
		}
		for (var fecha : datos.vencimientosPensiones()) {
			if (fecha.isBefore(anio.fechaInicio()) || fecha.isAfter(anio.fechaFin())) {
				throw new IllegalArgumentException("Los vencimientos deben estar dentro del año escolar");
			}
		}
		if (tarifarios.buscarPorAnio(datos.anioEscolarId()).isPresent()) {
			throw new IllegalStateException("El año escolar ya tiene un tarifario");
		}
		tarifarios.crear(datos);
		return consultar(datos.anioEscolarId());
	}

	@Transactional(readOnly = true)
	public RepositorioTarifarios.Tarifario consultar(UUID anioEscolarId) {
		return tarifarios.buscarPorAnio(anioEscolarId)
			.orElseThrow(() -> new NoSuchElementException("Tarifario no encontrado"));
	}
}
