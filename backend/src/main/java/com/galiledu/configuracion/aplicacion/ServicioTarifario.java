package com.galiledu.configuracion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.CalendarioPension;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.CalendarioVista;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.ConceptosTarifario;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.CuotaCalendario;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.CuotaVista;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.MorosidadTarifario;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudTarifario;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.TarifarioVigente;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.TarifarioVista;
import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.CalendarioCuotas;
import com.galiledu.configuracion.dominio.CalendarioCuotas.Cuota;
import com.galiledu.configuracion.dominio.CuotaTarifario;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoTarifario;
import com.galiledu.configuracion.dominio.Tarifario;
import com.galiledu.configuracion.infraestructura.AnioEscolarRepositorio;
import com.galiledu.configuracion.infraestructura.ConsultasTarifario;
import com.galiledu.configuracion.infraestructura.TarifarioRepositorio;

/**
 * HU-34: tarifario del año escolar (P19). Es la tabla maestra de la que Pagos genera los compromisos; este servicio no
 * crea compromisos. El dominio lanza IllegalStateException (409) e IllegalArgumentException (400) con el mensaje que
 * se muestra.
 *
 * <p>
 * Para otros módulos, sin endpoint: {@link #vigente} y {@link #calendario}.
 */
@Service
public class ServicioTarifario {
	private final TarifarioRepositorio tarifarios;
	private final AnioEscolarRepositorio anios;
	private final ConsultasTarifario consultas;

	public ServicioTarifario(TarifarioRepositorio tarifarios, AnioEscolarRepositorio anios,
			ConsultasTarifario consultas) {
		this.tarifarios = tarifarios;
		this.anios = anios;
		this.consultas = consultas;
	}

	/** P19 del año: el tarifario guardado o, si no hay, una propuesta (existe = false) que no se guarda. */
	@Transactional(readOnly = true)
	public TarifarioVista ver(UUID anioEscolarId) {
		AnioEscolar anio = buscarAnio(anioEscolarId);
		return vista(anio, tarifarios.findByAnioEscolarId(anioEscolarId).orElse(null));
	}

	/**
	 * Botón "Confirmar tarifario": guarda los montos y la morosidad, regenera el calendario con las fechas del año y
	 * deja el tarifario CONFIRMADO (lo crea si no existía). Se puede repetir mientras no sea de solo lectura. Sin el
	 * flush entre quitar y volver a crear las cuotas, Hibernate inserta antes de borrar y choca con los UNIQUE de
	 * pagos.cuota_tarifario.
	 */
	@Transactional
	public TarifarioVista confirmar(UUID anioEscolarId, SolicitudTarifario solicitud) {
		AnioEscolar anio = buscarAnio(anioEscolarId);
		Tarifario tarifario = tarifarios.findByAnioEscolarId(anioEscolarId)
				.orElseGet(() -> Tarifario.nuevo(anioEscolarId));
		motivoSoloLectura(anio, tarifario).ifPresent(motivo -> {
			throw ExcepcionNegocio.conflicto(motivo);
		});
		// Primero se valida todo sin tocar nada: un 400 no debe dejar el calendario borrado.
		traducir(() -> Tarifario.validar(solicitud.datos(), anio.getFechaInicio(), anio.getFechaFin()));
		tarifario.quitarCuotas();
		tarifarios.flush();
		traducir(() -> tarifario.confirmar(solicitud.datos(), anio.getFechaInicio(), anio.getFechaFin()));
		tarifarios.saveAndFlush(tarifario);
		return vista(anio, tarifario);
	}

	/**
	 * Para otros módulos (HU-23, Pagos): los montos y la morosidad del año. Vacío si el año no tiene un tarifario
	 * CONFIRMADO.
	 */
	@Transactional(readOnly = true)
	public Optional<TarifarioVigente> vigente(UUID anioEscolarId) {
		return confirmado(anioEscolarId).map(t -> new TarifarioVigente(t.getId(), t.getMontoCuotaIngreso(),
				t.getMontoMatricula(), t.getVencimientoMatricula(), t.getMontoPension(), t.getDiasGracia(),
				t.getRecargoPorcentaje(), t.getTopeRecargoPorcentaje()));
	}

	/**
	 * Para Pagos: las cuotas de pensión (número, nombre y vencimiento) y su monto. Vacío si el año no tiene un
	 * tarifario CONFIRMADO.
	 */
	@Transactional(readOnly = true)
	public Optional<CalendarioPension> calendario(UUID anioEscolarId) {
		return confirmado(anioEscolarId)
				.map(t -> new CalendarioPension(t.getMontoPension(), t.getCuotas().stream()
						.map(c -> new CuotaCalendario(c.getNumero(), c.getNombre(), c.getFechaVencimiento()))
						.toList()));
	}

	private Optional<Tarifario> confirmado(UUID anioEscolarId) {
		return tarifarios.findByAnioEscolarId(anioEscolarId).filter(Tarifario::estaConfirmado);
	}

	private AnioEscolar buscarAnio(UUID anioEscolarId) {
		return anios.findById(anioEscolarId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe."));
	}

	/**
	 * RF-73: un año CERRADO conserva su tarifario como registro histórico. Un tarifario CONFIRMADO con compromisos
	 * tampoco se modifica; uno EN_CONFIGURACION siempre se puede confirmar, aunque Pagos ya haya generado una cuota de
	 * matrícula (el caso se cierra cuando Pagos exija CONFIRMADO).
	 */
	private Optional<String> motivoSoloLectura(AnioEscolar anio, Tarifario tarifario) {
		if (anio.getEstado() == EstadoAnioEscolar.CERRADO) {
			return Optional.of("Año escolar cerrado: el tarifario se conserva como registro histórico.");
		}
		if (tarifario != null && tarifario.estaConfirmado() && consultas.tieneCompromisos(tarifario.getId())) {
			return Optional.of("Ya se generaron compromisos de pago " + anio.getAnio()
					+ ": los montos y el calendario no se pueden modificar.");
		}
		return Optional.empty();
	}

	/** Los montos y la morosidad de P19, guardados o propuestos (vacíos si no hay año anterior). */
	private record Valores(BigDecimal ingreso, BigDecimal matricula, BigDecimal pension, Integer gracia,
			BigDecimal recargo, BigDecimal tope) {
		static final Valores VACIOS = new Valores(null, null, null, null, null, null);

		static Valores de(Tarifario t) {
			return new Valores(t.getMontoCuotaIngreso(), t.getMontoMatricula(), t.getMontoPension(),
					t.getDiasGracia(), t.getRecargoPorcentaje(), t.getTopeRecargoPorcentaje());
		}
	}

	/**
	 * Con tarifario guardado muestra lo guardado; sin él, una propuesta con los valores del año anterior (si hay) y el
	 * calendario generado con las fechas del año.
	 */
	private TarifarioVista vista(AnioEscolar anio, Tarifario guardado) {
		boolean existe = guardado != null;
		Valores v = existe
				? Valores.de(guardado)
				: tarifarios.findByAnio(anio.getAnio() - 1).map(Valores::de).orElse(Valores.VACIOS);
		List<Cuota> generado = CalendarioCuotas.generar(anio.getFechaInicio(), anio.getFechaFin());
		LocalDate matricula = CalendarioCuotas.vencimientoMatricula(anio.getFechaInicio());
		List<Cuota> cuotas = existe ? delTarifario(guardado) : generado;

		BigDecimal total = v.pension() == null ? null : v.pension().multiply(BigDecimal.valueOf(cuotas.size()));
		CalendarioVista calendario = new CalendarioVista(cuotas.stream()
				.map(c -> new CuotaVista(c.numero(), c.nombre(), c.fechaVencimiento(), v.pension())).toList(), total);
		boolean desactualizado = existe
				&& !(cuotas.equals(generado) && guardado.getVencimientoMatricula().equals(matricula));
		Optional<String> motivo = motivoSoloLectura(anio, guardado);
		return new TarifarioVista(anio.getId(), anio.getAnio(), existe,
				existe ? guardado.getEstado() : EstadoTarifario.EN_CONFIGURACION, motivo.isPresent(),
				motivo.orElse(null), desactualizado,
				new ConceptosTarifario(v.ingreso(), v.matricula(),
						existe ? guardado.getVencimientoMatricula() : matricula, v.pension(), cuotas.size()),
				new MorosidadTarifario(v.gracia(), v.recargo(), v.tope()), calendario);
	}

	private static List<Cuota> delTarifario(Tarifario t) {
		return t.getCuotas().stream()
				.map((CuotaTarifario c) -> new Cuota(c.getNumero(), c.getNombre(), c.getFechaVencimiento())).toList();
	}

	/** Traduce las excepciones del dominio conservando el mensaje: estado no permitido es 409; dato inválido, 400. */
	private static void traducir(Runnable operacion) {
		try {
			operacion.run();
		} catch (IllegalStateException e) {
			throw ExcepcionNegocio.conflicto(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw ExcepcionNegocio.invalido(e.getMessage());
		}
	}
}
