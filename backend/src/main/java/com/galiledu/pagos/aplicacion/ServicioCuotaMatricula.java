package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosPagos.CuotaMatriculaDetalle;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.dominio.Cuota;
import com.galiledu.pagos.dominio.EstadoCompromiso;
import com.galiledu.pagos.dominio.EstadoCuota;
import com.galiledu.pagos.infraestructura.CompromisoPagoRepositorio;
import com.galiledu.pagos.infraestructura.ConsultasPagos;
import com.galiledu.pagos.infraestructura.ConsultasPagos.DescuentoActivo;
import com.galiledu.pagos.infraestructura.ConsultasPagos.FilaCuotaMatricula;
import com.galiledu.pagos.infraestructura.ConsultasPagos.MatriculaParaPagos;
import com.galiledu.pagos.infraestructura.ConsultasPagos.Tarifario;

/**
 * HU-36: genera (RF-78, RF-79) y consulta (RF-80) la cuota de matrícula. Matrícula debe llamar a
 * {@link #generar} cuando el colegio aprueba la documentación (HU-29/HU-72) y usar {@link #consultar} para saber si
 * ya se pagó (HU-31).
 */
@Service
public class ServicioCuotaMatricula {
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);
	/** Estados de la matrícula en los que el proceso de confirmación ya terminó o se canceló. */
	private static final Set<String> CERRADOS = Set.of("MATRICULADO", "RETIRADO", "ANULADA");

	private final CompromisoPagoRepositorio compromisos;
	private final ConsultasPagos consultas;

	public ServicioCuotaMatricula(CompromisoPagoRepositorio compromisos, ConsultasPagos consultas) {
		this.compromisos = compromisos;
		this.consultas = consultas;
	}

	@Transactional
	public CuotaMatriculaDetalle generar(UUID matriculaId, UUID usuarioId) {
		MatriculaParaPagos m = consultas.matricula(matriculaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe la matrícula."));
		if (CERRADOS.contains(m.estado())) {
			throw ExcepcionNegocio.conflicto(
					"La cuota de matrícula se genera mientras la matrícula está en proceso de confirmación.");
		}
		// RF-78: monto y vencimiento del tarifario del año escolar de la matrícula.
		Tarifario tarifario = consultas.tarifario(m.anioEscolarId())
				.orElseThrow(() -> ExcepcionNegocio.conflicto("No hay tarifario para el año " + m.anio() + "."));

		CompromisoPago compromiso = compromisos.findByMatriculaId(matriculaId)
				.or(() -> compromisos.findByEstudianteIdAndAnioEscolarId(m.estudianteId(), m.anioEscolarId()))
				.orElseGet(() -> CompromisoPago.nuevo(m.estudianteId(), m.anioEscolarId(), matriculaId,
						tarifario.id()));
		if (compromiso.getEstado() == EstadoCompromiso.ANULADO) {
			throw ExcepcionNegocio.conflicto("El compromiso de pago " + m.anio() + " del estudiante está anulado.");
		}
		// RF-79: una sola cuota de matrícula por estudiante y año.
		if (compromiso.tieneCuotaMatricula()) {
			throw ExcepcionNegocio.conflicto(
					"El estudiante ya tiene una cuota de matrícula registrada para " + m.anio() + ".");
		}
		compromiso.vincularMatricula(matriculaId);

		Optional<DescuentoActivo> descuento = consultas.descuentoPara(m.estudianteId(), Cuota.CONCEPTO_MATRICULA)
				.filter(d -> d.vigenteEn(tarifario.vencimientoMatricula()));
		BigDecimal rebaja = descuento
				.map(d -> tarifario.montoMatricula().multiply(d.porcentaje()).divide(CIEN, 2, RoundingMode.HALF_UP))
				.orElse(BigDecimal.ZERO);
		compromiso.agregarCuota(Cuota.matricula(compromiso, "Matrícula " + m.anio(), tarifario.montoMatricula(),
				tarifario.vencimientoMatricula(), rebaja, descuento.map(DescuentoActivo::id).orElse(null),
				usuarioId));
		compromisos.saveAndFlush(compromiso);
		return consultar(matriculaId);
	}

	/** RF-80: estado de la cuota de matrícula; 404 si todavía no se generó. */
	@Transactional(readOnly = true)
	public CuotaMatriculaDetalle consultar(UUID matriculaId) {
		FilaCuotaMatricula f = consultas.cuotaMatricula(matriculaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La matrícula aún no tiene cuota de matrícula."));
		return new CuotaMatriculaDetalle(f.cuotaId(), f.compromisoId(), f.estudianteId(), f.anio(), f.concepto(),
				f.montoBase(), f.montoDescuento(), f.montoFinal(), f.montoPagado(), f.fechaVencimiento(),
				EstadoCuota.valueOf(f.estado()), f.fechaPago(), f.responsable());
	}
}
