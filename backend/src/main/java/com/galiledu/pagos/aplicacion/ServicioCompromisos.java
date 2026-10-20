package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoAnio;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoDetalle;
import com.galiledu.pagos.aplicacion.DtosPagos.CuotaDetalle;
import com.galiledu.pagos.aplicacion.DtosPagos.EstudianteCompromiso;
import com.galiledu.pagos.aplicacion.DtosPagos.ResumenCompromiso;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.dominio.Cuota;
import com.galiledu.pagos.dominio.EstadoCuota;
import com.galiledu.pagos.infraestructura.CompromisoPagoRepositorio;

/** Compromiso de pago y cronograma de cuotas (HU-37). Solo consulta. */
@Service
public class ServicioCompromisos {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");

	private final CompromisoPagoRepositorio compromisos;

	public ServicioCompromisos(CompromisoPagoRepositorio compromisos) {
		this.compromisos = compromisos;
	}

	/** Compromisos del estudiante, del año más reciente al más antiguo (selector de Figma P8). */
	@Transactional(readOnly = true)
	public List<CompromisoAnio> listarPorEstudiante(UUID estudianteId) {
		return compromisos.findByEstudianteId(estudianteId).stream()
				.sorted(Comparator.comparingInt(CompromisoPago::getAnio).reversed())
				.map(c -> new CompromisoAnio(c.getId(), c.getAnio(), c.getEstado()))
				.toList();
	}

	@Transactional(readOnly = true)
	public CompromisoDetalle detalle(UUID compromisoId) {
		CompromisoPago c = compromisos.findById(compromisoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el compromiso de pago."));
		LocalDate hoy = LocalDate.now(LIMA);
		int gracia = c.getDiasGracia();

		List<Cuota> activas = c.getCuotas().stream().filter(Cuota::estaActiva).toList();
		BigDecimal total = sumar(activas, Cuota::getMontoFinal);
		BigDecimal pagado = sumar(activas, Cuota::getMontoPagado);
		int pensiones = (int) activas.stream().filter(Cuota::esPension).count();
		int pensionesPagadas = (int) activas.stream()
				.filter(q -> q.esPension() && q.getEstado() == EstadoCuota.PAGADA).count();
		int vencidas = (int) activas.stream().filter(q -> q.estaVencida(hoy, gracia)).count();

		ResumenCompromiso resumen = new ResumenCompromiso(total, activas.size(), pagado, pensionesPagadas, pensiones,
				c.getDescuentoPorcentaje(), c.getDescuentoNombre(), c.getMontoPension(), total.subtract(pagado),
				vencidas);

		List<CuotaDetalle> cuotas = c.getCuotas().stream()
				.map(q -> new CuotaDetalle(q.getId(), q.getConceptoCodigo(), q.getNumero(), q.getNombre(),
						q.getFechaVencimiento(), q.getMontoBase(), q.getMontoDescuento(), q.getPorcentajeDescuento(),
						q.getMontoFinal(), q.getMontoPagado(), q.getEstado(), q.estaVencida(hoy, gracia),
						q.getFechaPago()))
				.toList();

		EstudianteCompromiso estudiante = new EstudianteCompromiso(c.getEstudianteId(), c.getEstudianteNombre(),
				c.getEstudianteDocumento(), c.getApoderadoNombre(), c.getSeccion());

		return new CompromisoDetalle(c.getId(), c.getAnio(), c.getEstado(), estudiante, resumen, cuotas);
	}

	private static BigDecimal sumar(List<Cuota> cuotas, java.util.function.Function<Cuota, BigDecimal> campo) {
		return cuotas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
