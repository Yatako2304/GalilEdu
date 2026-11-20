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

/** HU-65: reglas de la marca y de la corrección del registro diario. */
class RegistroAsistenciaTests {
	private static final UUID TUTORA = UUID.randomUUID();
	private static final UUID ANA = UUID.randomUUID();
	private static final UUID LUIS = UUID.randomUUID();
	private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-15T08:05:00-05:00");

	@DisplayName("La tardanza exige hora y los demas estados la descartan")
	@Test
	void laTardanzaExigeHoraYLosDemasEstadosLaDescartan() {
		assertThatThrownBy(() -> new MarcaAsistencia(ANA, EstadoAsistencia.TARDANZA, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Indica la hora de llegada de cada tardanza.");
		assertThat(new MarcaAsistencia(ANA, EstadoAsistencia.PRESENTE, LocalTime.of(8, 0)).horaLlegada()).isNull();
		assertThat(new MarcaAsistencia(ANA, EstadoAsistencia.TARDANZA, LocalTime.of(8, 10, 45)).horaLlegada())
				.isEqualTo(LocalTime.of(8, 10));
	}

	@DisplayName("Sin estado no se puede marcar")
	@Test
	void sinEstadoNoSePuedeMarcar() {
		assertThatThrownBy(() -> new MarcaAsistencia(ANA, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Marca a cada estudiante como presente, tardanza o falta.");
	}

	@DisplayName("Corregir solo registra los cambios y quien los hizo")
	@Test
	void corregirSoloRegistraLosCambiosYQuienLosHizo() {
		RegistroAsistencia registro = RegistroAsistencia.registrar(UUID.randomUUID(), LocalDate.of(2026, 9, 15),
				List.of(new MarcaAsistencia(ANA, EstadoAsistencia.PRESENTE, null),
						new MarcaAsistencia(LUIS, EstadoAsistencia.FALTA, null)),
				TUTORA, AHORA);

		List<CorreccionAsistencia> cambios = registro.corregir(
				List.of(new MarcaAsistencia(ANA, EstadoAsistencia.PRESENTE, null),
						new MarcaAsistencia(LUIS, EstadoAsistencia.TARDANZA, LocalTime.of(9, 10))),
				TUTORA, AHORA.plusHours(1));

		assertThat(cambios).singleElement().satisfies(c -> {
			assertThat(c.getEstadoAnterior()).isEqualTo(EstadoAsistencia.FALTA);
			assertThat(c.getEstadoNuevo()).isEqualTo(EstadoAsistencia.TARDANZA);
			assertThat(c.getHoraNueva()).isEqualTo(LocalTime.of(9, 10));
		});
		assertThat(registro.getCorregidoPor()).isEqualTo(TUTORA);
		assertThat(registro.getCorregidoEn()).isEqualTo(AHORA.plusHours(1));
	}

	@DisplayName("Cambiar solo la hora de la tardanza también es una corrección")
	@Test
	void cambiarSoloLaHoraDeLaTardanzaTambienEsUnaCorreccion() {
		RegistroAsistencia registro = RegistroAsistencia.registrar(UUID.randomUUID(), LocalDate.of(2026, 9, 15),
				List.of(new MarcaAsistencia(ANA, EstadoAsistencia.TARDANZA, LocalTime.of(8, 10))), TUTORA, AHORA);

		assertThat(registro.corregir(List.of(new MarcaAsistencia(ANA, EstadoAsistencia.TARDANZA, LocalTime.of(8, 20))),
				TUTORA, AHORA)).hasSize(1);
	}

	@DisplayName("Sin cambios no se marca como corregido")
	@Test
	void sinCambiosNoSeMarcaComoCorregido() {
		RegistroAsistencia registro = RegistroAsistencia.registrar(UUID.randomUUID(), LocalDate.of(2026, 9, 15),
				List.of(new MarcaAsistencia(ANA, EstadoAsistencia.PRESENTE, null)), TUTORA, AHORA);

		assertThat(registro.corregir(List.of(new MarcaAsistencia(ANA, EstadoAsistencia.PRESENTE, null)), TUTORA,
				AHORA)).isEmpty();
		assertThat(registro.getCorregidoEn()).isNull();
	}
}
