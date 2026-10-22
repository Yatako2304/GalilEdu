package com.galiledu.configuracion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.DatosTarifario;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoPeriodo;
import com.galiledu.configuracion.dominio.EstadoTarifario;
import com.galiledu.configuracion.dominio.PeriodoAcademico;
import com.galiledu.configuracion.dominio.Regimen;
import com.galiledu.configuracion.infraestructura.ConsultasConfiguracion.AnioListado;
import com.galiledu.configuracion.infraestructura.ConsultasConfiguracion.ConteosAnio;

/** Contratos JSON del módulo de configuración. Deben coincidir con frontend/src/types/api.ts. */
public final class DtosConfiguracion {
	private DtosConfiguracion() {
	}

	/** Figma "Modal - Nuevo año escolar" (HU-74). */
	public record SolicitudAnioEscolar(
			@NotNull(message = "Ingresa el año escolar.") @Min(value = 2000, message = "El año escolar debe estar entre 2000 y 2100.") @Max(value = 2100, message = "El año escolar debe estar entre 2000 y 2100.") Integer anio,
			@NotNull(message = "Ingresa la fecha de inicio.") LocalDate fechaInicio,
			@NotNull(message = "Ingresa la fecha de fin.") LocalDate fechaFin,
			@NotNull(message = "Elige el régimen de periodos.") Regimen regimen) {
	}

	/** Botón "Editar" de "CI-06 · Año escolar 2027 (planificado)": cambia las fechas y regenera los periodos. */
	public record SolicitudEdicionAnio(
			@NotNull(message = "Ingresa la fecha de inicio.") LocalDate fechaInicio,
			@NotNull(message = "Ingresa la fecha de fin.") LocalDate fechaFin) {
	}

	/** Figma "Modal - Cambiar régimen de periodos". */
	public record SolicitudRegimen(@NotNull(message = "Elige el régimen de periodos.") Regimen regimen) {
	}

	/** Figma "Modal - Editar periodo". */
	public record SolicitudPeriodo(
			@NotBlank(message = "Ingresa el nombre del periodo.") @Size(max = 30, message = "El nombre del periodo no puede pasar de 30 caracteres.") String nombre,
			@NotNull(message = "Ingresa la fecha de inicio.") LocalDate fechaInicio,
			@NotNull(message = "Ingresa la fecha de fin.") LocalDate fechaFin,
			@NotNull(message = "Ingresa la fecha límite de notas.") LocalDate fechaLimiteNotas) {
	}

	/** Fila del listado "CI-01 · Año escolar". */
	public record AnioEscolarResumen(UUID id, int anio, LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen,
			int cantidadPeriodos, int seccionesActivas, int estructurasHorarias, EstadoAnioEscolar estado) {
		static AnioEscolarResumen de(AnioListado a) {
			return new AnioEscolarResumen(a.id(), a.anio(), a.fechaInicio(), a.fechaFin(), a.regimen(),
					a.cantidadPeriodos(), a.seccionesActivas(), a.estructurasHorarias(), a.estado());
		}
	}

	/**
	 * Año con sus periodos: "CI-03 · Año escolar 2026 › Periodos". Las cantidades son las de las pestañas "Secciones
	 * (23)" y "Estructura horaria (2)".
	 */
	public record AnioEscolarDetalle(UUID id, int anio, LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen,
			EstadoAnioEscolar estado, int cantidadSecciones, int cantidadEstructuras, List<PeriodoDetalle> periodos) {
		static AnioEscolarDetalle de(AnioEscolar a, ConteosAnio conteos, LocalDate hoy) {
			return new AnioEscolarDetalle(a.getId(), a.getAnio(), a.getFechaInicio(), a.getFechaFin(), a.getRegimen(),
					a.getEstado(), conteos.seccionesActivas(), conteos.estructurasHorarias(),
					a.getPeriodos().stream().map(p -> PeriodoDetalle.de(p, a.getEstado(), hoy)).toList());
		}
	}

	/**
	 * Fila de la tabla de periodos. El estado no se guarda: se calcula con las fechas y el estado del año.
	 * {@code registroNotasAbierto} dice si los docentes pueden registrar notas hoy.
	 */
	public record PeriodoDetalle(UUID id, int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
			LocalDate fechaLimiteNotas, int duracionSemanas, EstadoPeriodo estado, boolean registroNotasAbierto) {
		static PeriodoDetalle de(PeriodoAcademico p, EstadoAnioEscolar estadoAnio, LocalDate hoy) {
			return new PeriodoDetalle(p.getId(), p.getNumero(), p.getNombre(), p.getFechaInicio(), p.getFechaFin(),
					p.getFechaLimiteNotas(), p.duracionSemanas(), p.estadoEn(hoy, estadoAnio),
					p.registroNotasAbierto(hoy, estadoAnio));
		}
	}

	/**
	 * Card del año en curso de "CI-01 · Año escolar". Entre dos periodos (por ejemplo, en las vacaciones) no hay
	 * {@code periodoEnCurso}; entonces la card muestra {@code periodoNotasAbiertas} (el periodo que ya terminó pero
	 * cuya
	 * fecha límite sigue vigente) y {@code proximoPeriodo}. Los tres son null cuando no aplican.
	 */
	public record AnioEscolarActual(UUID id, int anio, LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen,
			PeriodoEnCurso periodoEnCurso, PeriodoNotasAbiertas periodoNotasAbiertas, ProximoPeriodo proximoPeriodo,
			int seccionesActivas, int alumnosMatriculados, int estructurasHorarias, List<String> codigosEstructuras,
			int avance) {
		static AnioEscolarActual de(AnioEscolar a, ConteosAnio conteos, int alumnosMatriculados,
				List<String> codigosEstructuras, LocalDate hoy) {
			PeriodoEnCurso enCurso = a.periodoEnCurso(hoy).map(p -> PeriodoEnCurso.de(p, hoy)).orElse(null);
			PeriodoNotasAbiertas notasAbiertas = a.periodoNotasAbiertas(hoy).map(p -> PeriodoNotasAbiertas.de(p, hoy))
					.orElse(null);
			ProximoPeriodo proximo = a.proximoPeriodo(hoy).map(ProximoPeriodo::de).orElse(null);
			return new AnioEscolarActual(a.getId(), a.getAnio(), a.getFechaInicio(), a.getFechaFin(), a.getRegimen(),
					enCurso, notasAbiertas, proximo, conteos.seccionesActivas(), alumnosMatriculados,
					conteos.estructurasHorarias(), codigosEstructuras, a.avance(hoy));
		}
	}

	/** Periodo ya terminado cuyo registro de notas sigue abierto hasta su fecha límite. */
	public record PeriodoNotasAbiertas(UUID id, int numero, String nombre, LocalDate fechaFin,
			LocalDate fechaLimiteNotas, long diasParaLimiteNotas) {
		static PeriodoNotasAbiertas de(PeriodoAcademico p, LocalDate hoy) {
			return new PeriodoNotasAbiertas(p.getId(), p.getNumero(), p.getNombre(), p.getFechaFin(),
					p.getFechaLimiteNotas(), ChronoUnit.DAYS.between(hoy, p.getFechaLimiteNotas()));
		}
	}

	/** El siguiente periodo que aún no empieza. */
	public record ProximoPeriodo(UUID id, int numero, String nombre, LocalDate fechaInicio) {
		static ProximoPeriodo de(PeriodoAcademico p) {
			return new ProximoPeriodo(p.getId(), p.getNumero(), p.getNombre(), p.getFechaInicio());
		}
	}

	/** {@code diasParaLimiteNotas}: días que faltan hasta la fecha límite de notas (0 el mismo día). */
	public record PeriodoEnCurso(UUID id, int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
			LocalDate fechaLimiteNotas, long diasParaLimiteNotas) {
		static PeriodoEnCurso de(PeriodoAcademico p, LocalDate hoy) {
			return new PeriodoEnCurso(p.getId(), p.getNumero(), p.getNombre(), p.getFechaInicio(), p.getFechaFin(),
					p.getFechaLimiteNotas(), ChronoUnit.DAYS.between(hoy, p.getFechaLimiteNotas()));
		}
	}

	/**
	 * Card "Antes de activar el año" de "CI-06": 3 requisitos obligatorios y 2 recomendados. {@code puedeActivar} es
	 * true cuando se cumplen los obligatorios.
	 */
	public record RequisitosAnio(boolean puedeActivar, List<Requisito> obligatorios, List<Requisito> recomendados) {
	}

	/**
	 * {@code texto} y {@code detalle} van como en Figma ("Secciones creadas" · "23 secciones activas en 11 grados").
	 */
	public record Requisito(String codigo, String texto, boolean cumplido, String detalle) {
	}

	/**
	 * Botón "Confirmar tarifario" de "P19 · Configuración › Tarifario y calendario" (HU-34). Los campos vacíos los
	 * rechaza el dominio con un mensaje propio (400).
	 */
	public record SolicitudTarifario(BigDecimal montoCuotaIngreso, BigDecimal montoMatricula, BigDecimal montoPension,
			Integer diasGracia, BigDecimal recargoPorcentaje, BigDecimal topeRecargoPorcentaje) {
		DatosTarifario datos() {
			return new DatosTarifario(montoCuotaIngreso, montoMatricula, montoPension, diasGracia, recargoPorcentaje,
					topeRecargoPorcentaje);
		}
	}

	/**
	 * Pantalla P19 de un año. Si {@code existe} es false es una propuesta que todavía no se guardó: los montos y la
	 * morosidad son los del año anterior (o vienen vacíos) y el calendario sale de las fechas del año. {@code estado}
	 * es EN_CONFIGURACION mientras no exista; confirmar lo crea ya CONFIRMADO.
	 *
	 * <p>
	 * {@code soloLectura} no se guarda: es verdadero si el año está CERRADO (RF-73) o si el tarifario CONFIRMADO ya
	 * tiene compromisos de pago; {@code motivoSoloLectura} dice cuál. {@code calendarioDesactualizado} es verdadero si
	 * el calendario guardado ya no coincide con las fechas del año (cambiaron cuando había compromisos).
	 */
	public record TarifarioVista(UUID anioEscolarId, int anio, boolean existe, EstadoTarifario estado,
			boolean soloLectura, String motivoSoloLectura, boolean calendarioDesactualizado,
			ConceptosTarifario conceptos, MorosidadTarifario morosidad, CalendarioVista calendario) {
	}

	/** Tabla "Conceptos y montos" de P19. */
	public record ConceptosTarifario(BigDecimal montoCuotaIngreso, BigDecimal montoMatricula,
			LocalDate vencimientoMatricula, BigDecimal montoPension, int cantidadCuotas) {
	}

	/** "Política de morosidad" de P19. */
	public record MorosidadTarifario(Integer diasGracia, BigDecimal recargoPorcentaje,
			BigDecimal topeRecargoPorcentaje) {
	}

	/** "Calendario de cuotas" de P19; {@code totalPensiones} es null mientras no haya monto de pensión. */
	public record CalendarioVista(List<CuotaVista> cuotas, BigDecimal totalPensiones) {
	}

	public record CuotaVista(int numero, String nombre, LocalDate fechaVencimiento, BigDecimal monto) {
	}

	/** Para otros módulos (HU-23, Pagos): los montos y la morosidad de un tarifario CONFIRMADO. */
	public record TarifarioVigente(BigDecimal montoCuotaIngreso, BigDecimal montoMatricula,
			LocalDate vencimientoMatricula, BigDecimal montoPension, int diasGracia, BigDecimal recargoPorcentaje,
			BigDecimal topeRecargoPorcentaje) {
	}

	/** Para Pagos: las cuotas de pensión de un tarifario CONFIRMADO; todas valen {@code montoPension}. */
	public record CalendarioPension(BigDecimal montoPension, List<CuotaCalendario> cuotas) {
	}

	public record CuotaCalendario(int numero, String nombre, LocalDate fechaVencimiento) {
	}
}
