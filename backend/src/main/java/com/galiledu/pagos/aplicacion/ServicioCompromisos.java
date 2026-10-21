package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosPagos.AlumnoEstadoCuenta;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoAnio;
import com.galiledu.pagos.aplicacion.DtosPagos.CompromisoDetalle;
import com.galiledu.pagos.aplicacion.DtosPagos.CuotaDetalle;
import com.galiledu.pagos.aplicacion.DtosPagos.EstadoCuentaGeneral;
import com.galiledu.pagos.aplicacion.DtosPagos.EstudianteCompromiso;
import com.galiledu.pagos.aplicacion.DtosPagos.ResumenCompromiso;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.dominio.Cuota;
import com.galiledu.pagos.dominio.EstadoCompromiso;
import com.galiledu.pagos.dominio.EstadoCuota;
import com.galiledu.pagos.infraestructura.CompromisoPagoRepositorio;
import com.galiledu.pagos.infraestructura.ConsultasPagos;

/** Compromiso de pago y cronograma de cuotas (HU-37). Solo consulta. */
@Service
public class ServicioCompromisos {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");

	private final CompromisoPagoRepositorio compromisos;
	private final ConsultasPagos consultas;

	public ServicioCompromisos(CompromisoPagoRepositorio compromisos, ConsultasPagos consultas) {
		this.compromisos = compromisos;
		this.consultas = consultas;
	}

	/**
	 * P2 · Estado de cuenta (HU-37): compromisos del año con su deuda vencida. Sin {@code anio} usa el año escolar
	 * ACTIVO. Las tarjetas suman solo los compromisos VIGENTES.
	 */
	@Transactional(readOnly = true)
	public EstadoCuentaGeneral estadoCuenta(Integer anio) {
		int anioConsulta = anio != null ? anio : consultas.anioActivo().orElse(LocalDate.now(LIMA).getYear());
		List<AlumnoEstadoCuenta> alumnos = consultas.estadoCuenta(anioConsulta, LocalDate.now(LIMA)).stream()
				.map(f -> new AlumnoEstadoCuenta(f.estudianteId(), f.compromisoId(), f.nombre(), f.numeroDocumento(),
						f.apoderado(), f.seccion(), f.antiguedad(), EstadoCompromiso.valueOf(f.estado()), f.total(),
						f.pagado(), f.deudaVencida(), f.cuotasVencidas()))
				.toList();
		List<AlumnoEstadoCuenta> vigentes = alumnos.stream()
				.filter(a -> a.estadoCompromiso() == EstadoCompromiso.VIGENTE).toList();
		BigDecimal comprometido = vigentes.stream().map(AlumnoEstadoCuenta::totalAnual)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal cobrado = vigentes.stream().map(AlumnoEstadoCuenta::capitalPagado)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal deuda = vigentes.stream().map(AlumnoEstadoCuenta::deudaVencida)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		int porcentaje = comprometido.signum() == 0
				? 0
				: cobrado.multiply(BigDecimal.valueOf(100)).divide(comprometido, 0, RoundingMode.HALF_UP).intValue();
		int cuotasVencidas = vigentes.stream().mapToInt(AlumnoEstadoCuenta::cuotasVencidas).sum();
		int alumnosConDeuda = (int) vigentes.stream().filter(a -> a.cuotasVencidas() > 0).count();
		return new EstadoCuentaGeneral(anioConsulta, comprometido, cobrado, porcentaje, deuda, cuotasVencidas,
				alumnosConDeuda, alumnos);
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
