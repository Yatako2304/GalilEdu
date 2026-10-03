package com.galiledu.horarios.dominio;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModeloHorarioTests {
	@Test
	void unaAsignacionExigeAlMenosUnPeriodo() {
		assertThatThrownBy(() -> asignacion(Set.of(), "TEORIA"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> asignacion(null, "TEORIA"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void espacioSubgrupoYTipoDeSesionSonOpcionalesYSeNormalizan() {
		var asignacion = asignacion(Set.of(UUID.randomUUID()), "  ");

		assertThat(asignacion.espacioFisicoId()).isNull();
		assertThat(asignacion.subgrupoId()).isNull();
		assertThat(asignacion.tipoSesion()).isNull();
		assertThat(asignacion(Set.of(UUID.randomUUID()), " TEORIA ").tipoSesion()).isEqualTo("TEORIA");
	}

	@Test
	void respetaLaLongitudMaximaDelTipoDeSesion() {
		assertThatThrownBy(() -> asignacion(Set.of(UUID.randomUUID()), "x".repeat(41)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void desactivarConservaIdentidadYDatos() {
		var original = asignacion(Set.of(UUID.randomUUID()), "TEORIA");
		var desactivada = original.desactivar();

		assertThat(desactivada.activa()).isFalse();
		assertThat(original.activa()).isTrue();
		assertThat(desactivada.id()).isEqualTo(original.id());
		assertThat(desactivada.periodos()).isEqualTo(original.periodos());
	}

	@Test
	void unBorradorNaceActivoYConLaVersionIndicada() {
		var horario = HorarioSeccion.nuevoBorrador(UUID.randomUUID(), 3);

		assertThat(horario.estado()).isEqualTo(EstadoHorario.BORRADOR);
		assertThat(horario.activo()).isTrue();
		assertThat(horario.version()).isEqualTo(3);
		assertThat(horario.desactivar().activo()).isFalse();
		assertThatThrownBy(() -> HorarioSeccion.nuevoBorrador(UUID.randomUUID(), 0))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void elSubgrupoExigeNombreDeHasta40Caracteres() {
		assertThat(Subgrupo.nuevo(UUID.randomUUID(), "  Grupo 1 ").nombre()).isEqualTo("Grupo 1");
		assertThatThrownBy(() -> Subgrupo.nuevo(UUID.randomUUID(), " "))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Subgrupo.nuevo(UUID.randomUUID(), "x".repeat(41)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private static AsignacionHoraria asignacion(Set<UUID> periodos, String tipoSesion) {
		return AsignacionHoraria.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
			null, null, tipoSesion, DiaSemana.LUNES, periodos);
	}
}
