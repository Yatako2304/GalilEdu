package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosPagos.CronogramaGenerado;
import com.galiledu.pagos.aplicacion.DtosPagos.RetiroRegistrado;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.dominio.Cuota;
import com.galiledu.pagos.dominio.EstadoCompromiso;
import com.galiledu.pagos.infraestructura.CompromisoPagoRepositorio;
import com.galiledu.pagos.infraestructura.ConsultasPagos;
import com.galiledu.pagos.infraestructura.ConsultasPagos.CuotaTarifario;
import com.galiledu.pagos.infraestructura.ConsultasPagos.DescuentoActivo;
import com.galiledu.pagos.infraestructura.ConsultasPagos.MatriculaParaPagos;
import com.galiledu.pagos.infraestructura.ConsultasPagos.Tarifario;

/**
 * HU-37: genera el cronograma de pensiones de una matrícula formalizada (RF-81, RF-82, RF-83) y anula las pensiones
 * futuras por retiro (RF-85). Matrícula debe llamar a {@link #generarPensiones} al confirmarse el pago (HU-31).
 */
@Service
public class ServicioCronograma {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final CompromisoPagoRepositorio compromisos;
	private final ConsultasPagos consultas;

	public ServicioCronograma(CompromisoPagoRepositorio compromisos, ConsultasPagos consultas) {
		this.compromisos = compromisos;
		this.consultas = consultas;
	}

	@Transactional
	public CronogramaGenerado generarPensiones(UUID matriculaId, UUID usuarioId) {
		MatriculaParaPagos m = consultas.matricula(matriculaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe la matrícula."));
		if (!"MATRICULADO".equals(m.estado())) {
			throw ExcepcionNegocio.conflicto(
					"El cronograma de pensiones se genera cuando la matrícula está formalizada (matriculado).");
		}
		Tarifario tarifario = consultas.tarifario(m.anioEscolarId())
				.orElseThrow(() -> ExcepcionNegocio.conflicto("No hay tarifario para el año " + m.anio() + "."));
		if (!"CONFIRMADO".equals(tarifario.estado())) {
			throw ExcepcionNegocio.conflicto("El tarifario " + m.anio() + " aún no está confirmado.");
		}

		CompromisoPago compromiso = compromisos.findByMatriculaId(matriculaId)
				.or(() -> compromisos.findByEstudianteIdAndAnioEscolarId(m.estudianteId(), m.anioEscolarId()))
				.orElseGet(() -> CompromisoPago.nuevo(m.estudianteId(), m.anioEscolarId(), matriculaId,
						tarifario.id()));
		if (compromiso.getEstado() == EstadoCompromiso.ANULADO) {
			throw ExcepcionNegocio.conflicto("El compromiso de pago " + m.anio() + " del estudiante está anulado.");
		}
		// RF-83: no se duplica el cronograma ante un reintento.
		if (compromiso.tienePensiones()) {
			throw ExcepcionNegocio.conflicto(
					"El estudiante ya cuenta con un cronograma de pensiones generado para " + m.anio() + ".");
		}
		compromiso.vincularMatricula(matriculaId);

		// RF-82: solo las pensiones que vencen después de la fecha de matrícula (sin prorratear el mes de ingreso).
		LocalDate fechaMatricula = m.confirmadoEn() != null
				? m.confirmadoEn().atZoneSameInstant(LIMA).toLocalDate()
				: LocalDate.now(LIMA);
		List<CuotaTarifario> pendientes = consultas.cuotasTarifario(tarifario.id()).stream()
				.filter(ct -> ct.fechaVencimiento().isAfter(fechaMatricula))
				.toList();
		if (pendientes.isEmpty()) {
			throw ExcepcionNegocio.conflicto("No quedan pensiones por vencer en el año " + m.anio() + ".");
		}

		Optional<DescuentoActivo> descuento = consultas.descuentoPara(m.estudianteId(), Cuota.CONCEPTO_PENSION);
		for (CuotaTarifario ct : pendientes) {
			BigDecimal rebaja = descuento.filter(d -> d.vigenteEn(ct.fechaVencimiento()))
					.map(d -> tarifario.montoPension().multiply(d.porcentaje()).divide(CIEN, 2, RoundingMode.HALF_UP))
					.orElse(BigDecimal.ZERO);
			compromiso.agregarCuota(Cuota.pension(compromiso, ct.numero(), ct.nombre(), tarifario.montoPension(),
					ct.fechaVencimiento(), rebaja, descuento.map(DescuentoActivo::id).orElse(null), usuarioId));
		}
		CompromisoPago guardado = compromisos.save(compromiso);
		return new CronogramaGenerado(guardado.getId(), m.anio(), pendientes.size());
	}

	/** RF-85: anula las pensiones pendientes que vencen después del retiro; las vencidas y parciales se mantienen. */
	@Transactional
	public RetiroRegistrado anularPorRetiro(UUID compromisoId, LocalDate fechaRetiro) {
		CompromisoPago compromiso = compromisos.findById(compromisoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el compromiso de pago."));
		int anuladas = 0;
		for (Cuota cuota : compromiso.getCuotas()) {
			if (cuota.anularPorRetiro(fechaRetiro)) {
				anuladas++;
			}
		}
		return new RetiroRegistrado(anuladas);
	}
}
