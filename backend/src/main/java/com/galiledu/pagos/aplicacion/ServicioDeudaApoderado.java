package com.galiledu.pagos.aplicacion;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoAnio;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoDetalle;
import com.galiledu.pagos.aplicacion.DtosPagos.HijoConDeuda;
import com.galiledu.pagos.aplicacion.DtosPagos.PagoRealizado;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.infraestructura.CompromisoPagoRepositorio;
import com.galiledu.pagos.infraestructura.ConsultasPagos;

/**
 * HU-40 · Figma Apoderado › "Pensiones": estado de deuda de los hijos (RF-95), cronograma con recargo (RF-96) e
 * historial de pagos (RF-97). Cualquier padre, madre o apoderado vinculado al estudiante puede consultarlo, no solo
 * el responsable de matrícula (corrección 2 de la iteración 1). Un estudiante o compromiso ajeno responde 404.
 */
@Service
public class ServicioDeudaApoderado {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");

	private final ConsultasPagos consultas;
	private final CompromisoPagoRepositorio compromisos;
	private final ServicioCompromisos servicio;

	public ServicioDeudaApoderado(ConsultasPagos consultas, CompromisoPagoRepositorio compromisos,
			ServicioCompromisos servicio) {
		this.consultas = consultas;
		this.compromisos = compromisos;
		this.servicio = servicio;
	}

	/** RF-95: hijos a cargo con su saldo y pensiones vencidas, ordenados por apellido. */
	@Transactional(readOnly = true)
	public List<HijoConDeuda> hijos(UUID usuarioId) {
		return consultas.hijosConDeuda(usuarioId, LocalDate.now(LIMA)).stream()
				.map(f -> new HijoConDeuda(f.estudianteId(), f.codigo(), f.nombres(), f.apellidos(), f.grado(),
						f.seccion(), f.saldo(), f.cuotasVencidas()))
				.toList();
	}

	/** Selector "Compromiso 2026" del apoderado. */
	@Transactional(readOnly = true)
	public List<CompromisoAnio> compromisos(UUID usuarioId, UUID estudianteId) {
		if (!consultas.esHijoDelUsuario(usuarioId, estudianteId)) {
			throw ExcepcionNegocio.noEncontrado("No tienes a este estudiante a tu cargo.");
		}
		return servicio.listarPorEstudiante(estudianteId);
	}

	/**
	 * RF-96: cronograma completo con estado y recargo por cuota. El monto ya llega rebajado si tiene descuento; el
	 * motivo del beneficio no se expone.
	 */
	@Transactional(readOnly = true)
	public CompromisoDetalle detalle(UUID usuarioId, UUID compromisoId) {
		verificar(usuarioId, compromisoId);
		return servicio.detalle(compromisoId);
	}

	/** RF-97: pagos del compromiso, del más reciente al más antiguo. */
	@Transactional(readOnly = true)
	public List<PagoRealizado> pagos(UUID usuarioId, UUID compromisoId) {
		verificar(usuarioId, compromisoId);
		return servicio.pagos(compromisoId);
	}

	private void verificar(UUID usuarioId, UUID compromisoId) {
		boolean suyo = compromisos.findById(compromisoId).map(CompromisoPago::getEstudianteId)
				.filter(estudianteId -> consultas.esHijoDelUsuario(usuarioId, estudianteId))
				.isPresent();
		if (!suyo) {
			throw ExcepcionNegocio.noEncontrado("No existe el compromiso de pago.");
		}
	}
}
