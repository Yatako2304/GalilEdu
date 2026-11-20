package com.galiledu.asistencia.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.asistencia.aplicacion.CalendarioEscolar.CalendarioDelNivel;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.SeccionAsistencia;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.ArchivoReporte;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.FilaReporte;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.OpcionesReporte;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.ReporteAsistencia;
import com.galiledu.asistencia.aplicacion.DtosReporteAsistencia.TotalesReporte;
import com.galiledu.asistencia.aplicacion.RangoReporte.Rango;
import com.galiledu.asistencia.aplicacion.RangoReporte.Tipo;
import com.galiledu.asistencia.aplicacion.SeccionesTutoradas.SeccionTutorada;
import com.galiledu.asistencia.infraestructura.ArchivoReporteAsistencia;
import com.galiledu.asistencia.infraestructura.ConsultasReporteAsistencia;
import com.galiledu.asistencia.infraestructura.ConsultasReporteAsistencia.Totales;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * HU-68 · RF-146: consolidado de inasistencias y tardanzas de la sección del tutor, por mes o por periodo académico,
 * ordenado por apellidos y exportable a PDF.
 */
@Service
public class ServicioReporteAsistencia {
	private final SeccionesTutoradas secciones;
	private final CalendarioEscolar calendario;
	private final ConsultasReporteAsistencia consultas;
	private final Clock reloj;

	public ServicioReporteAsistencia(SeccionesTutoradas secciones, CalendarioEscolar calendario,
			ConsultasReporteAsistencia consultas, Clock reloj) {
		this.secciones = secciones;
		this.calendario = calendario;
		this.consultas = consultas;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public ReporteAsistencia reporte(UUID usuarioId, UUID seccionId, Tipo tipo, String valor) {
		SeccionTutorada seccion = secciones.delUsuario(usuarioId).stream().filter(s -> s.id().equals(seccionId))
				.findFirst().orElseThrow(() -> ExcepcionNegocio.noEncontrado("No eres tutor de esta sección."));
		CalendarioDelNivel delNivel = calendario.de(seccion.anioEscolarId(), seccion.nivel())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el año escolar de la sección."));
		LocalDate hoy = LocalDate.now(reloj);
		Rango rango;
		try {
			rango = RangoReporte.resolver(tipo, valor, delNivel, hoy);
		} catch (IllegalArgumentException excepcion) {
			throw ExcepcionNegocio.invalido(excepcion.getMessage());
		}
		Map<UUID, Totales> totales = consultas.totales(seccion.id(), rango.desde(), rango.hasta());
		List<FilaReporte> filas = secciones.estudiantes(seccion.id()).stream().map(e -> {
			Totales t = totales.getOrDefault(e.matriculaId(), Totales.CERO);
			return new FilaReporte(e.matriculaId(), e.nombres(), e.apellidos(), e.documento(), t.faltas(),
					t.faltasJustificadas(), t.tardanzas(), t.tardanzasJustificadas());
		}).toList();
		TotalesReporte suma = new TotalesReporte(filas.stream().mapToInt(FilaReporte::faltas).sum(),
				filas.stream().mapToInt(FilaReporte::faltasJustificadas).sum(),
				filas.stream().mapToInt(FilaReporte::tardanzas).sum(),
				filas.stream().mapToInt(FilaReporte::tardanzasJustificadas).sum());
		return new ReporteAsistencia(
				new SeccionAsistencia(seccion.id(), seccion.anio(), seccion.grado(), seccion.nombre(), seccion.cargo()),
				rango.tipo(), rango.valor(), rango.etiqueta(), rango.desde(), rango.hasta(),
				consultas.diasRegistrados(seccion.id(), rango.desde(), rango.hasta()),
				new OpcionesReporte(RangoReporte.meses(delNivel, hoy), RangoReporte.periodos(delNivel, hoy)), filas,
				suma);
	}

	/** RF-146: el mismo reporte en PDF ({@code reporte-asistencia-1-secundaria-A-2026-11.pdf}). */
	@Transactional(readOnly = true)
	public ArchivoReporte pdf(UUID usuarioId, UUID seccionId, Tipo tipo, String valor) {
		ReporteAsistencia reporte = reporte(usuarioId, seccionId, tipo, valor);
		String nombre = ("reporte-asistencia-" + reporte.seccion().grado() + "-" + reporte.seccion().nombre() + "-"
				+ reporte.valor()).toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("-+", "-") + ".pdf";
		return new ArchivoReporte(nombre, ArchivoReporteAsistencia.pdf(reporte, OffsetDateTime.now(reloj)));
	}
}
