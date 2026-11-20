package com.galiledu.asistencia.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-67 · RF-145: qué se puede justificar y con qué datos. */
class JustificacionTests {
	private static final UUID TUTORA = UUID.randomUUID();
	private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-15T10:00:00-05:00");

	private static AsistenciaEstudiante asistencia(EstadoAsistencia estado) {
		LocalTime hora = estado == EstadoAsistencia.TARDANZA ? LocalTime.of(8, 10) : null;
		return RegistroAsistencia.registrar(UUID.randomUUID(), LocalDate.of(2026, 9, 15),
				List.of(new MarcaAsistencia(UUID.randomUUID(), estado, hora)), TUTORA, AHORA).getAsistencias()
				.get(0);
	}

	@DisplayName("Una falta se justifica con motivo y detalle limpio")
	@Test
	void unaFaltaSeJustificaConMotivoYDetalleLimpio() {
		Justificacion j = Justificacion.de(asistencia(EstadoAsistencia.FALTA), MotivoJustificacion.CITA_MEDICA,
				"  Reposo médico  ", TUTORA, AHORA);

		assertThat(j.getMotivo()).isEqualTo(MotivoJustificacion.CITA_MEDICA);
		assertThat(j.getDetalle()).isEqualTo("Reposo médico");
		assertThat(j.getArchivoNombre()).isNull();
	}

	@DisplayName("Un detalle en blanco queda vacio")
	@Test
	void unDetalleEnBlancoQuedaVacio() {
		assertThat(Justificacion.de(asistencia(EstadoAsistencia.TARDANZA), MotivoJustificacion.ENFERMEDAD, "   ",
				TUTORA, AHORA).getDetalle()).isNull();
	}

	@DisplayName("Un presente no se justifica")
	@Test
	void unPresenteNoSeJustifica() {
		assertThatThrownBy(() -> Justificacion.de(asistencia(EstadoAsistencia.PRESENTE),
				MotivoJustificacion.ENFERMEDAD, null, TUTORA, AHORA))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Solo se justifica una falta o una tardanza.");
	}

	@DisplayName("Sin motivo o con otro sin detalle no se justifica")
	@Test
	void sinMotivoOConOtroSinDetalleNoSeJustifica() {
		assertThatThrownBy(() -> Justificacion.de(asistencia(EstadoAsistencia.FALTA), null, "x", TUTORA, AHORA))
				.hasMessage("Elige el motivo de la justificación.");
		assertThatThrownBy(() -> Justificacion.de(asistencia(EstadoAsistencia.FALTA), MotivoJustificacion.OTRO, null,
				TUTORA, AHORA))
				.hasMessage("Describe el motivo en el detalle.");
	}

	@DisplayName("El detalle tiene como maximo500 caracteres")
	@Test
	void elDetalleTieneComoMaximo500Caracteres() {
		assertThatThrownBy(() -> Justificacion.de(asistencia(EstadoAsistencia.FALTA), MotivoJustificacion.OTRO,
				"a".repeat(501), TUTORA, AHORA))
				.hasMessage("El detalle no puede pasar de 500 caracteres.");
	}

	@DisplayName("El adjunto guarda nombre tipo y tamano")
	@Test
	void elAdjuntoGuardaNombreTipoYTamano() {
		Justificacion j = Justificacion.de(asistencia(EstadoAsistencia.FALTA), MotivoJustificacion.ENFERMEDAD, null,
				TUTORA, AHORA);
		j.adjuntar("descanso.pdf", "application/pdf", new byte[]{1, 2, 3});

		assertThat(j.getArchivoNombre()).isEqualTo("descanso.pdf");
		assertThat(j.getArchivoTamano()).isEqualTo(3);
	}
}
