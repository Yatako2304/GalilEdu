package com.galiledu.asistencia.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import com.galiledu.asistencia.aplicacion.puertos.AutorizacionAsistencia;
import com.galiledu.asistencia.aplicacion.puertos.RepositorioAsistenciaEstudiantes;
import com.galiledu.asistencia.dominio.EstadoAsistencia;

/** Corrige una marcación guardada solo durante el mismo día escolar (HU-65). */
public final class CorregirAsistenciaEstudiante {
	private final RepositorioAsistenciaEstudiantes registros;
	private final AutorizacionAsistencia autorizacion;
	private final Clock reloj;

	public CorregirAsistenciaEstudiante(RepositorioAsistenciaEstudiantes registros,
		AutorizacionAsistencia autorizacion, Clock reloj) {
		this.registros = Objects.requireNonNull(registros);
		this.autorizacion = Objects.requireNonNull(autorizacion);
		this.reloj = Objects.requireNonNull(reloj);
	}

	public Resultado ejecutar(String actor, String estudiante, LocalDate fecha,
		EstadoAsistencia nuevoEstado) {
		if (actor == null || actor.isBlank() || estudiante == null || estudiante.isBlank()) {
			throw new IllegalArgumentException("Actor y estudiante son obligatorios");
		}
		Objects.requireNonNull(fecha, "La fecha es obligatoria");
		Objects.requireNonNull(nuevoEstado, "El nuevo estado es obligatorio");
		var registro = registros.buscar(estudiante, fecha);
		if (registro.isEmpty() || !autorizacion.puedeGestionarSeccion(actor,
			registro.orElseThrow().seccion())) {
			return Resultado.NO_DISPONIBLE;
		}
		if (!fecha.equals(LocalDate.now(reloj))) {
			return Resultado.FUERA_DEL_MISMO_DIA;
		}
		registros.guardar(registro.orElseThrow().conEstado(nuevoEstado));
		return Resultado.CORREGIDO;
	}

	public enum Resultado {
		CORREGIDO,
		NO_DISPONIBLE,
		FUERA_DEL_MISMO_DIA
	}
}
