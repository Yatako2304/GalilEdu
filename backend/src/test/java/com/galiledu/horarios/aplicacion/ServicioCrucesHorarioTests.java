package com.galiledu.horarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.DatosDeSeccion.DatosSeccion;
import com.galiledu.horarios.aplicacion.DtosHorarios.SolicitudCelda;

/** HU-46: el aviso de cruces antes de guardar, sin Spring ni BD. */
class ServicioCrucesHorarioTests {
	private static final UUID ANIO = UUID.randomUUID();
	private static final UUID SECCION = UUID.randomUUID();
	private static final UUID CURSO = UUID.randomUUID();
	private static final UUID AMBIENTE = UUID.randomUUID();

	private final DatosDeSeccion secciones = mock(DatosDeSeccion.class);
	private final ConversorDeBloques conversor = mock(ConversorDeBloques.class);
	private final ValidadorCruces validador = mock(ValidadorCruces.class);
	private final ServicioCrucesHorario servicio = new ServicioCrucesHorario(secciones, conversor, validador);

	private static SolicitudCruces solicitud() {
		return new SolicitudCruces(List.of(new SolicitudCelda(DiaSemana.LUN, 1, CURSO, AMBIENTE)));
	}

	private static DatosSeccion seccion() {
		return new DatosSeccion(SECCION, "5.° B", "5P", "5.° de primaria", "PRIMARIA", ANIO, "ACTIVO", null, null,
				null);
	}

	@Test
	void sinSeccionResponde404SinConsultarNada() {
		when(secciones.buscar(SECCION)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.buscarCruces(SECCION, solicitud())).isInstanceOfSatisfying(
				ExcepcionNegocio.class, e -> {
					assertThat(e.estado()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(e.getMessage()).isEqualTo("La sección no existe.");
				});
		verifyNoInteractions(conversor, validador);
	}

	@Test
	void buscaLosCrucesConElAnioYLaSeccionCorrectosYDevuelveLoDelValidador() {
		var solicitados = List.of(new BloqueSolicitado(DiaSemana.LUN, 1, LocalTime.of(8, 0), LocalTime.of(8, 45),
				CURSO, UUID.randomUUID(), AMBIENTE));
		var cruce = new CruceDetectado(TipoCruce.DOCENTE, DiaSemana.LUN, 1, "El docente ya dicta clase.", "5.° A",
				"Álgebra");
		when(secciones.buscar(SECCION)).thenReturn(Optional.of(seccion()));
		when(conversor.aBloquesSolicitados(SECCION, List.of(new CeldaHorario(DiaSemana.LUN, 1, CURSO, AMBIENTE))))
				.thenReturn(solicitados);
		when(validador.buscarCruces(ANIO, SECCION, solicitados)).thenReturn(List.of(cruce));

		assertThat(servicio.buscarCruces(SECCION, solicitud())).containsExactly(cruce);
		verify(validador).buscarCruces(ANIO, SECCION, solicitados);
	}

	@Test
	void siElConversorOmiteTodoDevuelveLoQueDiceElValidadorSinFallar() {
		when(secciones.buscar(SECCION)).thenReturn(Optional.of(seccion()));
		when(conversor.aBloquesSolicitados(any(), any())).thenReturn(List.of());
		when(validador.buscarCruces(ANIO, SECCION, List.of())).thenReturn(List.of());

		assertThat(servicio.buscarCruces(SECCION, solicitud())).isEmpty();
	}
}
