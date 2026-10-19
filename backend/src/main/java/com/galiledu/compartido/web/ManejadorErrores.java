package com.galiledu.compartido.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Todas las respuestas de error tienen la forma { "message": "...", "errores": { campo: mensaje } }.
 * El frontend muestra "message" en la notificación o debajo del formulario.
 */
@RestControllerAdvice
public class ManejadorErrores {
	private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

	public record ErrorApi(String message, Map<String, String> errores) {
		static ErrorApi de(String mensaje) {
			return new ErrorApi(mensaje, Map.of());
		}
	}

	@ExceptionHandler(ExcepcionNegocio.class)
	ResponseEntity<ErrorApi> negocio(ExcepcionNegocio e) {
		return ResponseEntity.status(e.estado()).body(ErrorApi.de(e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException e) {
		Map<String, String> errores = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors().forEach(f -> errores.putIfAbsent(f.getField(), f.getDefaultMessage()));
		String primero = errores.values().stream().findFirst().orElse("Datos inválidos");
		return ResponseEntity.badRequest().body(new ErrorApi(primero, errores));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorApi> cuerpoIlegible(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(ErrorApi.de("La solicitud tiene un formato inválido"));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ErrorApi> accesoDenegado(AccessDeniedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorApi.de("No tienes permiso para realizar esta acción"));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorApi> inesperado(Exception e) {
		log.error("Error no controlado", e);
		return ResponseEntity.internalServerError()
				.body(ErrorApi.de("Ocurrió un error inesperado. Inténtalo nuevamente."));
	}
}
