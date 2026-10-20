package com.galiledu.academico.aplicacion;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.academico.aplicacion.DtosAcademico.SolicitudTablaConversion;
import com.galiledu.academico.aplicacion.DtosAcademico.TablaConversionDetalle;
import com.galiledu.academico.dominio.EstadoVersion;
import com.galiledu.academico.dominio.RangoNivel;
import com.galiledu.academico.dominio.TablaConversion;
import com.galiledu.academico.infraestructura.TablaConversionRepositorio;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Escala de calificación (Figma CI-23, HU-55): versiones de la tabla de conversión vigesimal → AD/A/B/C. Una versión
 * con fecha futura queda programada; la vigente pasa al historial cuando empieza la nueva.
 */
@Service
public class ServicioEscalaCalificacion {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");
	private static final DateTimeFormatter DD_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final TablaConversionRepositorio tablas;

	public ServicioEscalaCalificacion(TablaConversionRepositorio tablas) {
		this.tablas = tablas;
	}

	/** Todas las versiones, de la más nueva a la más antigua, con su estado respecto de hoy. */
	@Transactional(readOnly = true)
	public List<TablaConversionDetalle> listar() {
		LocalDate hoy = hoy();
		UUID vigente = tablas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(hoy)
				.map(TablaConversion::getId).orElse(null);
		return tablas.findAllByOrderByVigenteDesdeDesc().stream()
				.map(t -> TablaConversionDetalle.de(t, estado(t, hoy, vigente)))
				.toList();
	}

	@Transactional(readOnly = true)
	public TablaConversionDetalle vigente() {
		return tablas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(hoy())
				.map(t -> TablaConversionDetalle.de(t, EstadoVersion.VIGENTE))
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("Aún no hay una tabla de conversión vigente."));
	}

	@Transactional
	public TablaConversionDetalle crear(SolicitudTablaConversion solicitud, UUID usuario) {
		LocalDate hoy = hoy();
		LocalDate desde = solicitud.vigenteDesde();
		if (desde.isBefore(hoy)) {
			throw ExcepcionNegocio.invalido("La fecha de vigencia no puede ser anterior a hoy.");
		}
		if (tablas.existsByVigenteDesde(desde)) {
			throw ExcepcionNegocio.conflicto(
					"Ya hay una versión que rige desde el %s.".formatted(desde.format(DD_MM_AAAA)));
		}
		List<RangoNivel> niveles = solicitud.niveles().stream()
				.map(n -> new RangoNivel(n.nivel(), n.notaDesde(), n.notaHasta()))
				.toList();
		TablaConversion tabla;
		try {
			tabla = new TablaConversion(desde, niveles, usuario);
		} catch (IllegalArgumentException e) {
			throw ExcepcionNegocio.invalido(e.getMessage());
		}
		tabla = tablas.save(tabla);
		return TablaConversionDetalle.de(tabla, desde.isAfter(hoy) ? EstadoVersion.PROGRAMADA : EstadoVersion.VIGENTE);
	}

	private static EstadoVersion estado(TablaConversion t, LocalDate hoy, UUID vigente) {
		if (t.getVigenteDesde().isAfter(hoy)) {
			return EstadoVersion.PROGRAMADA;
		}
		return t.getId().equals(vigente) ? EstadoVersion.VIGENTE : EstadoVersion.ANTERIOR;
	}

	private static LocalDate hoy() {
		return LocalDate.now(LIMA);
	}
}
