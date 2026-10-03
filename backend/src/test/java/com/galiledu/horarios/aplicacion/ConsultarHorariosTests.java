package com.galiledu.horarios.aplicacion;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.puertos.ConsultaHorarioSemanal;
import com.galiledu.horarios.aplicacion.puertos.ConsultaHorarioSemanal.EntradaHorario;
import com.galiledu.horarios.dominio.DiaSemana;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarHorariosTests {
	private final EntradaHorario entrada = new EntradaHorario(UUID.randomUUID(), DiaSemana.LUNES, 1,
		LocalTime.of(8, 0), LocalTime.of(8, 45), "Quinto A", "Historia", UUID.randomUUID(), null, null, "TEORIA");
	private final UUID periodo = UUID.randomUUID();

	@Test
	void delegaCadaVistaConSusIdentificadores() {
		var id = UUID.randomUUID();
		var consultar = new ConsultarHorarios(new ConsultaHorarioSemanal() {
			@Override
			public List<EntradaHorario> porDocente(UUID docenteId, UUID periodoId) {
				return docenteId.equals(id) && periodoId.equals(periodo) ? List.of(entrada) : List.of();
			}

			@Override
			public List<EntradaHorario> porSeccion(UUID seccionId, UUID periodoId) {
				return seccionId.equals(id) ? List.of(entrada) : List.of();
			}

			@Override
			public List<EntradaHorario> porMatricula(UUID matriculaId, UUID periodoId) {
				return matriculaId.equals(id) ? List.of(entrada) : List.of();
			}
		});

		assertThat(consultar.porDocente(id, periodo)).containsExactly(entrada);
		assertThat(consultar.porSeccion(id, periodo)).containsExactly(entrada);
		assertThat(consultar.porMatricula(id, periodo)).containsExactly(entrada);
		assertThat(consultar.porDocente(UUID.randomUUID(), periodo)).isEmpty();
	}

	@Test
	void exigeIdentificadores() {
		var consultar = new ConsultarHorarios(new ConsultaHorarioSemanal() {
			@Override
			public List<EntradaHorario> porDocente(UUID docenteId, UUID periodoId) {
				return List.of();
			}

			@Override
			public List<EntradaHorario> porSeccion(UUID seccionId, UUID periodoId) {
				return List.of();
			}

			@Override
			public List<EntradaHorario> porMatricula(UUID matriculaId, UUID periodoId) {
				return List.of();
			}
		});

		assertThatThrownBy(() -> consultar.porDocente(null, periodo)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> consultar.porMatricula(UUID.randomUUID(), null))
			.isInstanceOf(NullPointerException.class);
	}
}
