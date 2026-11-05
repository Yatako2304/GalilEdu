package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosDescuentos.ConfiguracionDescuentos;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentoVista;
import com.galiledu.pagos.aplicacion.DtosDescuentos.SolicitudDescuento;
import com.galiledu.pagos.aplicacion.DtosDescuentos.TramoDescuento;
import com.galiledu.pagos.dominio.ConfiguracionPagos;
import com.galiledu.pagos.dominio.Descuento;
import com.galiledu.pagos.dominio.EstadoDescuento;
import com.galiledu.pagos.dominio.TipoDescuento;
import com.galiledu.pagos.infraestructura.ConfiguracionPagosRepositorio;
import com.galiledu.pagos.infraestructura.ConsultasPagos;
import com.galiledu.pagos.infraestructura.DescuentoRepositorio;

/**
 * HU-35 · Figma "P21 · Catálogo de descuentos": registro (RF-74, RF-75), consulta y edición (RF-76) y límite (RF-77).
 */
@Service
public class ServicioCatalogoDescuentos {
	private final DescuentoRepositorio descuentos;
	private final ConfiguracionPagosRepositorio configuracion;
	private final ConsultasPagos consultas;

	public ServicioCatalogoDescuentos(DescuentoRepositorio descuentos, ConfiguracionPagosRepositorio configuracion,
			ConsultasPagos consultas) {
		this.descuentos = descuentos;
		this.configuracion = configuracion;
		this.consultas = consultas;
	}

	/** RF-76: todo el catálogo (activos e inactivos), ordenado alfabéticamente por denominación. */
	@Transactional(readOnly = true)
	public List<DescuentoVista> listar() {
		return descuentos.findAllByOrderByNombreAsc().stream().map(ServicioCatalogoDescuentos::vista).toList();
	}

	@Transactional(readOnly = true)
	public DescuentoVista ver(UUID id) {
		return vista(buscar(id));
	}

	@Transactional
	public DescuentoVista crear(SolicitudDescuento s) {
		String nombre = s.nombre().trim();
		if (descuentos.existsByNombreIgnoreCase(nombre)) {
			throw ExcepcionNegocio.conflicto("Ya existe un descuento con la denominación \"" + nombre + "\".");
		}
		Set<String> conceptos = validar(s);
		Descuento d = Descuento.nuevo(nombre, limpio(s.descripcion()), s.tipo(), s.aplicacion(), s.porcentaje(),
				limpio(s.condicion()), conceptos);
		agregarTramos(d, s);
		return vista(descuentos.saveAndFlush(d));
	}

	/** RF-76: editar no cambia los descuentos ya aplicados (guardan su copia del nombre y del porcentaje). */
	@Transactional
	public DescuentoVista editar(UUID id, SolicitudDescuento s) {
		Descuento d = buscar(id);
		String nombre = s.nombre().trim();
		if (descuentos.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
			throw ExcepcionNegocio.conflicto("Ya existe un descuento con la denominación \"" + nombre + "\".");
		}
		Set<String> conceptos = validar(s);
		d.actualizar(nombre, limpio(s.descripcion()), s.tipo(), s.aplicacion(), s.porcentaje(), limpio(s.condicion()),
				conceptos);
		// Se borran los tramos antes de insertar los nuevos: (descuento_id, hermanos_desde) es único.
		d.quitarTramos();
		descuentos.flush();
		agregarTramos(d, s);
		return vista(descuentos.saveAndFlush(d));
	}

	/** RF-76: desactivarlo impide aplicarlo de nuevo, pero no afecta a los compromisos que ya lo tienen. */
	@Transactional
	public DescuentoVista cambiarEstado(UUID id, EstadoDescuento estado) {
		Descuento d = buscar(id);
		d.cambiarEstado(estado);
		return vista(d);
	}

	@Transactional(readOnly = true)
	public ConfiguracionDescuentos configuracion() {
		return new ConfiguracionDescuentos(fila().getPorcentajeMaximoDescuento());
	}

	/** RF-77: el límite rige para los descuentos que se registren o editen desde ahora. */
	@Transactional
	public ConfiguracionDescuentos cambiarLimite(BigDecimal porcentajeMaximo) {
		ConfiguracionPagos c = fila();
		c.cambiarPorcentajeMaximo(porcentajeMaximo);
		return new ConfiguracionDescuentos(c.getPorcentajeMaximoDescuento());
	}

	/** RF-74, RF-75, RF-77. Devuelve los conceptos sin repetir. */
	private Set<String> validar(SolicitudDescuento s) {
		BigDecimal limite = fila().getPorcentajeMaximoDescuento();
		Set<String> conceptos = new LinkedHashSet<>(s.conceptos() == null ? List.of() : s.conceptos());
		if (conceptos.isEmpty()) {
			throw ExcepcionNegocio.invalido("Asocia al menos un concepto de pago al descuento.");
		}
		if (consultas.conceptosExistentes(conceptos).size() != conceptos.size()) {
			throw ExcepcionNegocio.invalido("Alguno de los conceptos de pago no existe.");
		}
		if (s.tipo() == TipoDescuento.PORCENTAJE_UNICO) {
			validarPorcentaje(s.porcentaje(), limite, "El porcentaje");
			return conceptos;
		}
		List<TramoDescuento> tramos = new ArrayList<>(s.tramos() == null ? List.of() : s.tramos());
		if (tramos.size() < 2) {
			throw ExcepcionNegocio.invalido("El descuento por hermanos necesita al menos dos tramos.");
		}
		tramos.sort(Comparator.comparingInt(TramoDescuento::hermanosDesde));
		for (int i = 0; i < tramos.size(); i++) {
			TramoDescuento t = tramos.get(i);
			if (t.hermanosDesde() < 2) {
				throw ExcepcionNegocio.invalido("Cada tramo empieza en 2 hermanos o más.");
			}
			if (t.hermanosHasta() != null && t.hermanosHasta() < t.hermanosDesde()) {
				throw ExcepcionNegocio.invalido("En el tramo de " + t.hermanosDesde()
						+ " hermanos, el \"hasta\" no puede ser menor que el \"desde\".");
			}
			validarPorcentaje(t.porcentaje(), limite, "El porcentaje del tramo de " + t.hermanosDesde() + " hermanos");
			if (i > 0) {
				TramoDescuento anterior = tramos.get(i - 1);
				if (anterior.hermanosHasta() == null || anterior.hermanosHasta() >= t.hermanosDesde()) {
					throw ExcepcionNegocio.invalido("Los tramos de " + anterior.hermanosDesde() + " y "
							+ t.hermanosDesde() + " hermanos se superponen.");
				}
			}
		}
		return conceptos;
	}

	private static void validarPorcentaje(BigDecimal porcentaje, BigDecimal limite, String que) {
		if (porcentaje == null) {
			throw ExcepcionNegocio.invalido(que + " es obligatorio.");
		}
		if (porcentaje.compareTo(BigDecimal.ONE) < 0 || porcentaje.compareTo(limite) > 0) {
			throw ExcepcionNegocio
					.invalido(que + " debe estar entre 1 % y " + limite.stripTrailingZeros().toPlainString()
							+ " % (límite de la institución).");
		}
	}

	private static void agregarTramos(Descuento d, SolicitudDescuento s) {
		if (s.tipo() == TipoDescuento.POR_HERMANOS) {
			s.tramos().stream().sorted(Comparator.comparingInt(TramoDescuento::hermanosDesde))
					.forEach(t -> d.agregarTramo(t.hermanosDesde(), t.hermanosHasta(), t.porcentaje()));
		}
	}

	private Descuento buscar(UUID id) {
		return descuentos.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el descuento."));
	}

	private ConfiguracionPagos fila() {
		return configuracion.findById(ConfiguracionPagos.ID)
				.orElseThrow(() -> new IllegalStateException("Falta la fila de pagos.configuracion."));
	}

	private static String limpio(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

	static DescuentoVista vista(Descuento d) {
		return new DescuentoVista(d.getId(), d.getNombre(), d.getDescripcion(), d.getTipo(), d.getAplicacion(),
				d.getPorcentaje(),
				d.getTramos().stream()
						.map(t -> new TramoDescuento(t.getHermanosDesde(), t.getHermanosHasta(), t.getPorcentaje()))
						.toList(),
				d.getConceptos().stream().sorted().toList(), d.getCondicion(), d.getEstado());
	}
}
