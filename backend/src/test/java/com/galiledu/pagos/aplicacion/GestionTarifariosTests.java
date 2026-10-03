package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.ConsultaAnioEscolar;
import com.galiledu.pagos.aplicacion.puertos.RepositorioTarifarios;
import com.galiledu.pagos.dominio.ConfiguracionTarifario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionTarifariosTests {
	private final ConsultaAnioEscolar anios = mock(ConsultaAnioEscolar.class);
	private final RepositorioTarifarios repositorio = mock(RepositorioTarifarios.class);
	private final GestionTarifarios gestion = new GestionTarifarios(anios, repositorio);
	private final UUID anioId = UUID.randomUUID();

	@Test
	void rechazaFechasFueraDelAnioSinGuardar() {
		when(anios.buscar(anioId)).thenReturn(Optional.of(anio("PLANIFICADO")));
		assertThatThrownBy(() -> gestion.registrar(datos(LocalDate.of(2028, 1, 1))))
			.isInstanceOf(IllegalArgumentException.class);
		verify(repositorio, never()).crear(any());
	}

	@Test
	void noCreaTarifarioParaAnioCerrado() {
		when(anios.buscar(anioId)).thenReturn(Optional.of(anio("CERRADO")));
		assertThatThrownBy(() -> gestion.registrar(datos(LocalDate.of(2027, 4, 10))))
			.isInstanceOf(IllegalStateException.class);
		verify(repositorio, never()).crear(any());
	}

	@Test
	void noReemplazaTarifarioExistente() {
		when(anios.buscar(anioId)).thenReturn(Optional.of(anio("ACTIVO")));
		when(repositorio.buscarPorAnio(anioId)).thenReturn(Optional.of(new RepositorioTarifarios.Tarifario(
			UUID.randomUUID(), anioId, "VIGENTE", BigDecimal.ONE, BigDecimal.ONE,
			1, LocalDate.of(2027, 2, 10), List.of(LocalDate.of(2027, 4, 10)),
			0, BigDecimal.ZERO, BigDecimal.ZERO)));
		assertThatThrownBy(() -> gestion.registrar(datos(LocalDate.of(2027, 4, 10))))
			.isInstanceOf(IllegalStateException.class);
		verify(repositorio, never()).crear(any());
	}

	private ConsultaAnioEscolar.AnioEscolar anio(String estado) {
		return new ConsultaAnioEscolar.AnioEscolar(anioId,
			LocalDate.of(2027, 3, 1), LocalDate.of(2027, 12, 31), estado);
	}

	private ConfiguracionTarifario datos(LocalDate vencimiento) {
		return new ConfiguracionTarifario(anioId, new BigDecimal("850.00"),
			new BigDecimal("450.00"), 1, LocalDate.of(2027, 2, 10),
			List.of(vencimiento), 5,
			new BigDecimal("1.50"), new BigDecimal("10.00"));
	}
}
