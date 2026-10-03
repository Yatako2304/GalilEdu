package com.galiledu.matricula.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.SeccionesParaMatricula;
import com.galiledu.matricula.aplicacion.puertos.RepositorioReservasMatricula;
import com.galiledu.usuarios.aplicacion.puertos.ConsultaEstudianteActivo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionReservasMatriculaTests {
	private final ConsultaEstudianteActivo estudiantes = mock(ConsultaEstudianteActivo.class);
	private final SeccionesParaMatricula secciones = mock(SeccionesParaMatricula.class);
	private final RepositorioReservasMatricula reservas = mock(RepositorioReservasMatricula.class);
	private final GestionReservasMatricula gestion = new GestionReservasMatricula(estudiantes, secciones, reservas);
	private final UUID estudianteId = UUID.randomUUID();
	private final UUID anioId = UUID.randomUUID();
	private final UUID solicitada = UUID.randomUUID();
	private final UUID alternativa = UUID.randomUUID();

	@Test
	void reservaAlternativaSinFormalizar() {
		UUID id = UUID.randomUUID();
		UUID gradoId = UUID.randomUUID();
		when(estudiantes.existe(estudianteId)).thenReturn(true);
		when(secciones.bloquearOpciones(anioId, solicitada)).thenReturn(List.of(
			new SeccionesParaMatricula.Seccion(alternativa, gradoId, "A", 2),
			new SeccionesParaMatricula.Seccion(solicitada, gradoId, "B", 1)));
		when(reservas.ocupados(solicitada)).thenReturn(1);
		when(reservas.reservar(any(), any(), any(), any())).thenReturn(id);
		when(reservas.buscar(id)).thenReturn(Optional.of(new RepositorioReservasMatricula.Reserva(
			id, estudianteId, anioId, alternativa, "SECCION_RESERVADA", "REGULAR", LocalDate.now())));

		var resultado = gestion.reservar(estudianteId, anioId, solicitada);
		assertThat(resultado.seccionId()).isEqualTo(alternativa);
		assertThat(resultado.estado()).isEqualTo("SECCION_RESERVADA");
		verify(reservas).reservar(any(), any(), org.mockito.ArgumentMatchers.eq(alternativa), any());
	}

	@Test
	void noReservaEstudianteInactivoNiDuplicaAnio() {
		assertThatThrownBy(() -> gestion.reservar(estudianteId, anioId, solicitada))
			.isInstanceOf(java.util.NoSuchElementException.class);
		verify(reservas, never()).reservar(any(), any(), any(), any());

		when(estudiantes.existe(estudianteId)).thenReturn(true);
		when(secciones.bloquearOpciones(anioId, solicitada)).thenReturn(List.of(
			new SeccionesParaMatricula.Seccion(solicitada, UUID.randomUUID(), "A", 1)));
		when(reservas.existe(estudianteId, anioId)).thenReturn(true);
		assertThatThrownBy(() -> gestion.reservar(estudianteId, anioId, solicitada))
			.isInstanceOf(IllegalStateException.class);
		verify(reservas, never()).reservar(any(), any(), any(), any());
	}
}
