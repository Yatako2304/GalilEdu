package com.galiledu.matricula.aplicacion;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.ConsultaAnioEscolar;
import com.galiledu.matricula.aplicacion.puertos.ConsultaPeriodoMatricula;
import com.galiledu.matricula.aplicacion.puertos.RegistroPeriodoMatricula;
import com.galiledu.matricula.dominio.PeriodoMatricula;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionPeriodosMatriculaTests {
	private final ConsultaAnioEscolar anios = mock(ConsultaAnioEscolar.class);
	private final ConsultaPeriodoMatricula consulta = mock(ConsultaPeriodoMatricula.class);
	private final RegistroPeriodoMatricula registro = mock(RegistroPeriodoMatricula.class);
	private final GestionPeriodosMatricula gestion = new GestionPeriodosMatricula(anios, consulta, registro);
	private final UUID anioId = UUID.randomUUID();
	private final PeriodoMatricula periodo = new PeriodoMatricula(anioId,
		LocalDate.of(2027, 1, 10), LocalDate.of(2027, 2, 20));

	@Test
	void registraUnRangoPropioYLoConsulta() {
		when(anios.buscar(anioId)).thenReturn(Optional.of(new ConsultaAnioEscolar.AnioEscolar(
			anioId, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 12, 31), "PLANIFICADO")));
		assertThat(gestion.guardar(periodo)).isEqualTo(periodo);
		verify(registro).guardar(periodo);
		when(consulta.buscarPorAnio(anioId)).thenReturn(Optional.of(periodo));
		assertThat(gestion.consultar(anioId, LocalDate.of(2027, 2, 1)).habilitado()).isTrue();
	}

	@Test
	void noInventaUnPeriodoSiElAnioNoExiste() {
		assertThatThrownBy(() -> gestion.guardar(periodo))
			.isInstanceOf(java.util.NoSuchElementException.class);
	}
}
