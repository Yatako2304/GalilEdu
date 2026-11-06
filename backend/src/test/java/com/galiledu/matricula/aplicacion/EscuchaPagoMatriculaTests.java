package com.galiledu.matricula.aplicacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.PagoEnLineaAprobado;

/** HU-31: qué hace Matrícula cuando Pagos avisa que se pagó una cuota de matrícula. */
class EscuchaPagoMatriculaTests {
	private final ServicioConfirmacionPago confirmacion = mock(ServicioConfirmacionPago.class);
	private final EscuchaPagoMatricula escucha = new EscuchaPagoMatricula(confirmacion);
	private final UUID usuario = UUID.randomUUID();

	private PagoEnLineaAprobado pago(UUID... matriculas) {
		return new PagoEnLineaAprobado(UUID.randomUUID(), "OP-2026-000001", usuario, List.of(), List.of(matriculas));
	}

	@Test
	void matriculaYGeneraLasPensiones() {
		UUID matricula = UUID.randomUUID();
		when(confirmacion.matricular(matricula)).thenReturn(Optional.of("MAT-2027-0001"));
		escucha.alAprobarsePago(pago(matricula));
		verify(confirmacion).generarPensiones(matricula, usuario);
	}

	@Test
	void siYaEstabaMatriculadoNoGeneraOtraVezLasPensiones() {
		UUID matricula = UUID.randomUUID();
		when(confirmacion.matricular(matricula)).thenReturn(Optional.empty());
		escucha.alAprobarsePago(pago(matricula));
		verify(confirmacion, never()).generarPensiones(any(), any());
	}

	@Test
	void unErrorEnUnaMatriculaNoDetieneLasDemas() {
		UUID primera = UUID.randomUUID();
		UUID segunda = UUID.randomUUID();
		when(confirmacion.matricular(any())).thenReturn(Optional.of("MAT-2027-0001"));
		doThrow(ExcepcionNegocio.conflicto("ya existe")).when(confirmacion).generarPensiones(primera, usuario);
		escucha.alAprobarsePago(pago(primera, segunda));
		verify(confirmacion).generarPensiones(segunda, usuario);
	}

	@Test
	void unPagoSinMatriculasNoHaceNada() {
		escucha.alAprobarsePago(pago());
		verify(confirmacion, never()).matricular(any());
	}
}
