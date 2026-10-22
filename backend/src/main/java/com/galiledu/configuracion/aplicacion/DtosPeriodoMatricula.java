package com.galiledu.configuracion.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoVisiblePeriodoMatricula;

/**
 * Contratos JSON de la pantalla 13 "Matrícula › Configuración de matrícula" (HU-21, HU-22, HU-23). Los campos vacíos
 * los
 * rechaza el dominio o el servicio con su mensaje (400), no Bean Validation.
 */
public final class DtosPeriodoMatricula {
	private DtosPeriodoMatricula() {
	}

	/**
	 * "Guardar configuración" de "Nuevo periodo de matrícula" (POST) y de editar (PUT). Fechas y horas son de Lima.
	 * {@code anioEscolarId} solo se usa al crear: un periodo no cambia de año. Las secciones que no vienen en
	 * {@code vacantes} conservan su capacidad. En {@code documentos}, {@code id} es null si el documento es nuevo.
	 */
	public record SolicitudPeriodoMatricula(UUID anioEscolarId, LocalDate fechaInicio, LocalTime horaInicio,
			LocalDate fechaCierre, LocalTime horaCierre, List<VacanteSeccion> vacantes,
			List<DocumentoSolicitud> documentos) {
	}

	public record VacanteSeccion(UUID seccionId, Integer capacidad) {
	}

	public record DocumentoSolicitud(UUID id, String nombre, String descripcion) {
	}

	/** Fila del listado. {@code vigente}: es el periodo que devuelve {@code vigente()}. */
	public record FilaPeriodoMatricula(UUID id, String nombre, UUID anioEscolarId, int anio, Instant inicio,
			Instant cierre, int matriculas, EstadoVisiblePeriodoMatricula estado, boolean vigente) {
	}

	/**
	 * Tarjeta "Vigente" del listado y periodo que Matrícula debería consultar: el que está ABIERTO ahora o, si no hay,
	 * el próximo PROGRAMADO.
	 */
	public record PeriodoVigente(UUID id, UUID anioEscolarId, int anio, String nombre, Instant inicio, Instant cierre,
			EstadoVisiblePeriodoMatricula estado, boolean bloquearPorMorosidad, boolean permitirExtemporanea,
			int vacantesTotales, int matriculasRegistradas) {
	}

	public record AnioDisponible(UUID id, int anio, EstadoAnioEscolar estado) {
	}

	/**
	 * El formulario de la pantalla 13 vacío (GET /nuevo), para editar o en solo lectura (GET /{id}), con el resumen del
	 * modal "Configuración · Matrícula". {@code id} y {@code estado} son null si el periodo aún no existe.
	 * {@code editable} es verdadero si el periodo no existe o está PROGRAMADO; {@code eliminable}, si además no tiene
	 * matrículas ni entregas. {@code diasDelPeriodo} es el largo de la barra de fechas. El monto es el del tarifario
	 * CONFIRMADO del año y es null si {@code tarifarioConfirmado} es false (la tarjeta muestra el aviso y el enlace a
	 * P19).
	 */
	public record FormularioPeriodoMatricula(UUID id, UUID anioEscolarId, int anio, String titulo,
			EstadoVisiblePeriodoMatricula estado, boolean editable, boolean eliminable, LocalDate fechaInicio,
			LocalTime horaInicio, LocalDate fechaCierre, LocalTime horaCierre, Long diasDelPeriodo,
			BigDecimal montoMatricula, boolean tarifarioConfirmado, int vacantesTotales, int matriculasRegistradas,
			int cantidadSecciones, String resumenSecciones, boolean bloquearPorMorosidad,
			boolean permitirExtemporanea, List<SeccionVacante> secciones, List<DocumentoVista> documentos) {
	}

	/** Fila de "Vacantes por grado y sección": {@code etiqueta} es "1.° A" y {@code tutor} puede ser null. */
	public record SeccionVacante(UUID seccionId, String etiqueta, String gradoCodigo, String gradoNombre, String tutor,
			int capacidad, int alumnosOcupanVacante) {
	}

	public record DocumentoVista(UUID id, String nombre, String descripcion, int orden, boolean tienePlantilla,
			String plantillaNombreArchivo, boolean tieneEntregas) {
	}
}
