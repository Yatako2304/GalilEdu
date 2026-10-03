package com.galiledu.matricula.aplicacion;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.SeccionesParaMatricula;
import com.galiledu.matricula.aplicacion.puertos.RepositorioReservasMatricula;
import com.galiledu.matricula.dominio.SeleccionSeccion;
import com.galiledu.usuarios.aplicacion.puertos.ConsultaEstudianteActivo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionReservasMatricula {
	private final ConsultaEstudianteActivo estudiantes;
	private final SeccionesParaMatricula secciones;
	private final RepositorioReservasMatricula reservas;

	public GestionReservasMatricula(ConsultaEstudianteActivo estudiantes,
		SeccionesParaMatricula secciones, RepositorioReservasMatricula reservas) {
		this.estudiantes = estudiantes;
		this.secciones = secciones;
		this.reservas = reservas;
	}

	@Transactional
	public RepositorioReservasMatricula.Reserva reservar(UUID estudianteId,
		UUID anioEscolarId, UUID seccionSolicitadaId) {
		if (estudianteId == null || anioEscolarId == null || seccionSolicitadaId == null) {
			throw new IllegalArgumentException("Estudiante, año escolar y sección son obligatorios");
		}
		if (!estudiantes.existe(estudianteId)) {
			throw new NoSuchElementException("Estudiante activo no encontrado");
		}
		// Lock every candidate section in stable order before counting occupied seats.
		var candidatas = secciones.bloquearOpciones(anioEscolarId, seccionSolicitadaId);
		if (candidatas.isEmpty()) {
			throw new NoSuchElementException("Sección no disponible para el año escolar");
		}
		if (reservas.existe(estudianteId, anioEscolarId)) {
			throw new IllegalStateException("El estudiante ya tiene una matrícula en ese año");
		}
		var opciones = candidatas.stream().map(s -> new SeleccionSeccion.Opcion(
			s.id(), s.gradoId(), s.nombre(), s.capacidadMaxima(), reservas.ocupados(s.id()))).toList();
		UUID elegida = SeleccionSeccion.elegir(seccionSolicitadaId, opciones);
		UUID reservaId = reservas.reservar(estudianteId, anioEscolarId, elegida,
			LocalDate.now(ZoneId.of("America/Lima")));
		return consultar(reservaId);
	}

	@Transactional(readOnly = true)
	public RepositorioReservasMatricula.Reserva consultar(UUID id) {
		return reservas.buscar(id)
			.orElseThrow(() -> new NoSuchElementException("Reserva de matrícula no encontrada"));
	}
}
