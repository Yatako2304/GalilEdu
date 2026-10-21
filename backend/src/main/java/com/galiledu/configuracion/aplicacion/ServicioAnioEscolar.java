package com.galiledu.configuracion.aplicacion;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarDetalle;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarResumen;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudAnioEscolar;
import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.infraestructura.AnioEscolarRepositorio;
import com.galiledu.configuracion.infraestructura.ConsultasConfiguracion;

/** HU-74: años escolares y sus periodos. Por ahora solo listar, ver el detalle y crear un año PLANIFICADO. */
@Service
public class ServicioAnioEscolar {
	private final AnioEscolarRepositorio anios;
	private final ConsultasConfiguracion consultas;

	public ServicioAnioEscolar(AnioEscolarRepositorio anios, ConsultasConfiguracion consultas) {
		this.anios = anios;
		this.consultas = consultas;
	}

	@Transactional(readOnly = true)
	public List<AnioEscolarResumen> listar() {
		return consultas.anios().stream().map(AnioEscolarResumen::de).toList();
	}

	@Transactional(readOnly = true)
	public AnioEscolarDetalle detalle(UUID id) {
		return AnioEscolarDetalle.de(anios.findById(id)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe.")));
	}

	/** Crea el año PLANIFICADO y genera sus periodos (4 bimestres o 3 trimestres) con fechas sugeridas. */
	@Transactional
	public AnioEscolarDetalle crear(SolicitudAnioEscolar solicitud) {
		if (!solicitud.fechaFin().isAfter(solicitud.fechaInicio())) {
			throw ExcepcionNegocio.invalido("La fecha de fin debe ser posterior a la fecha de inicio.");
		}
		long dias = ChronoUnit.DAYS.between(solicitud.fechaInicio(), solicitud.fechaFin()) + 1;
		if (dias < AnioEscolar.diasMinimos(solicitud.regimen())) {
			throw ExcepcionNegocio.invalido("El año escolar es muy corto para dividirlo en "
					+ solicitud.regimen().cantidadPeriodos() + " periodos.");
		}
		if (anios.existsByAnio(solicitud.anio().shortValue())) {
			throw ExcepcionNegocio.conflicto("Ya existe un año escolar " + solicitud.anio() + ".");
		}
		AnioEscolar nuevo = AnioEscolar.planificar(solicitud.anio(), solicitud.fechaInicio(), solicitud.fechaFin(),
				solicitud.regimen());
		return AnioEscolarDetalle.de(anios.save(nuevo));
	}
}
