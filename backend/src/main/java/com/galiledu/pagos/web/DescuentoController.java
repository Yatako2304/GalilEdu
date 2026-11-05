package com.galiledu.pagos.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.pagos.aplicacion.DtosDescuentos.ConfiguracionDescuentos;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentoDelCompromiso;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentoVista;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentosAplicables;
import com.galiledu.pagos.aplicacion.DtosDescuentos.ResultadoDescuento;
import com.galiledu.pagos.aplicacion.DtosDescuentos.SolicitudAplicarDescuento;
import com.galiledu.pagos.aplicacion.DtosDescuentos.SolicitudConfiguracionDescuentos;
import com.galiledu.pagos.aplicacion.DtosDescuentos.SolicitudDescuento;
import com.galiledu.pagos.aplicacion.DtosDescuentos.SolicitudRevocarDescuento;
import com.galiledu.pagos.aplicacion.ServicioCatalogoDescuentos;
import com.galiledu.pagos.aplicacion.ServicioDescuentosAplicados;
import com.galiledu.pagos.dominio.EstadoDescuento;

/**
 * Figma > Personal Administrativo > Pensiones: "P21 · Catálogo de descuentos" (HU-35) y "P10 · Descuento aplicado"
 * (HU-38).
 */
@RestController
@RequestMapping("/pagos")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class DescuentoController {
	private final ServicioCatalogoDescuentos catalogo;
	private final ServicioDescuentosAplicados aplicados;

	public DescuentoController(ServicioCatalogoDescuentos catalogo, ServicioDescuentosAplicados aplicados) {
		this.catalogo = catalogo;
		this.aplicados = aplicados;
	}

	// ---------- HU-35 ----------

	/** GET /api/pagos/descuentos · RF-76: catálogo ordenado por denominación. */
	@GetMapping("/descuentos")
	public List<DescuentoVista> listar() {
		return catalogo.listar();
	}

	/** GET /api/pagos/descuentos/configuracion · RF-77: límite de descuento. */
	@GetMapping("/descuentos/configuracion")
	public ConfiguracionDescuentos configuracion() {
		return catalogo.configuracion();
	}

	/** PUT /api/pagos/descuentos/configuracion · RF-77. */
	@PutMapping("/descuentos/configuracion")
	public ConfiguracionDescuentos cambiarLimite(@Valid @RequestBody SolicitudConfiguracionDescuentos solicitud) {
		return catalogo.cambiarLimite(solicitud.porcentajeMaximo());
	}

	@GetMapping("/descuentos/{id}")
	public DescuentoVista ver(@PathVariable UUID id) {
		return catalogo.ver(id);
	}

	/** POST /api/pagos/descuentos · RF-74, RF-75. */
	@PostMapping("/descuentos")
	@ResponseStatus(HttpStatus.CREATED)
	public DescuentoVista crear(@Valid @RequestBody SolicitudDescuento solicitud) {
		return catalogo.crear(solicitud);
	}

	/** PUT /api/pagos/descuentos/{id} · RF-76. */
	@PutMapping("/descuentos/{id}")
	public DescuentoVista editar(@PathVariable UUID id, @Valid @RequestBody SolicitudDescuento solicitud) {
		return catalogo.editar(id, solicitud);
	}

	/** POST /api/pagos/descuentos/{id}/desactivacion · RF-76. */
	@PostMapping("/descuentos/{id}/desactivacion")
	public DescuentoVista desactivar(@PathVariable UUID id) {
		return catalogo.cambiarEstado(id, EstadoDescuento.INACTIVO);
	}

	@PostMapping("/descuentos/{id}/activacion")
	public DescuentoVista activar(@PathVariable UUID id) {
		return catalogo.cambiarEstado(id, EstadoDescuento.ACTIVO);
	}

	// ---------- HU-38 ----------

	/** GET /api/pagos/compromisos/{id}/descuentos-aplicables · RF-86 (botón "Añadir descuento"). */
	@GetMapping("/compromisos/{id}/descuentos-aplicables")
	public DescuentosAplicables aplicables(@PathVariable UUID id) {
		return aplicados.aplicables(id);
	}

	/** GET /api/pagos/compromisos/{id}/descuento · pestaña "Descuento" de P10. */
	@GetMapping("/compromisos/{id}/descuento")
	public DescuentoDelCompromiso descuento(@PathVariable UUID id) {
		return aplicados.delCompromiso(id);
	}

	/** POST /api/pagos/compromisos/{id}/descuentos · RF-87, RF-88, RF-89. */
	@PostMapping("/compromisos/{id}/descuentos")
	@ResponseStatus(HttpStatus.CREATED)
	public ResultadoDescuento aplicar(@PathVariable UUID id, @Valid @RequestBody SolicitudAplicarDescuento solicitud,
			Authentication autenticacion) {
		return aplicados.aplicar(id, solicitud, ServicioJwt.usuarioActual(autenticacion));
	}

	/** POST /api/pagos/descuentos-aplicados/{id}/revocacion · RF-90 ("Revocar descuento"). */
	@PostMapping("/descuentos-aplicados/{id}/revocacion")
	public ResultadoDescuento revocar(@PathVariable UUID id, @Valid @RequestBody SolicitudRevocarDescuento solicitud,
			Authentication autenticacion) {
		return aplicados.revocar(id, solicitud.motivo(), ServicioJwt.usuarioActual(autenticacion));
	}
}
