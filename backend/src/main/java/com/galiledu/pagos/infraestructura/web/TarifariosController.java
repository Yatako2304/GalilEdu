package com.galiledu.pagos.infraestructura.web;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.pagos.aplicacion.GestionTarifarios;
import com.galiledu.pagos.aplicacion.puertos.RepositorioTarifarios;
import com.galiledu.pagos.dominio.ConfiguracionTarifario;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos/tarifarios")
public class TarifariosController {
	private final GestionTarifarios gestion;

	public TarifariosController(GestionTarifarios gestion) {
		this.gestion = gestion;
	}

	@PostMapping
	public ResponseEntity<RepositorioTarifarios.Tarifario> registrar(@RequestBody RegistrarTarifarioRequest solicitud) {
		var resultado = gestion.registrar(solicitud.aDominio());
		return ResponseEntity.created(URI.create("/api/pagos/tarifarios/" + resultado.anioEscolarId()))
			.body(resultado);
	}

	@GetMapping("/{anioEscolarId}")
	public RepositorioTarifarios.Tarifario consultar(@PathVariable UUID anioEscolarId) {
		return gestion.consultar(anioEscolarId);
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorRespuesta> noEncontrado(NoSuchElementException error) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorRespuesta> solicitudInvalida(IllegalArgumentException error) {
		return ResponseEntity.badRequest().body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
	public ResponseEntity<ErrorRespuesta> conflicto(RuntimeException error) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new ErrorRespuesta(error instanceof DataIntegrityViolationException
				? "El tarifario entra en conflicto con datos existentes" : error.getMessage()));
	}

	public record RegistrarTarifarioRequest(UUID anioEscolarId, BigDecimal montoMatricula,
		BigDecimal montoPension, int cantidadCuotas, LocalDate vencimientoMatricula,
		List<LocalDate> vencimientosPensiones,
		int diasGracia, BigDecimal tasaMensual, BigDecimal topePorcentaje) {
		ConfiguracionTarifario aDominio() {
			return new ConfiguracionTarifario(anioEscolarId, montoMatricula, montoPension,
				cantidadCuotas, vencimientoMatricula, vencimientosPensiones,
				diasGracia, tasaMensual, topePorcentaje);
		}
	}

	public record ErrorRespuesta(String mensaje) {}
}
