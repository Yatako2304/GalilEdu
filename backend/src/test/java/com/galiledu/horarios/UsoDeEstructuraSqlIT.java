package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.configuracion.aplicacion.UsoDeEstructura;
import com.galiledu.configuracion.dominio.DiaSemana;

/** HU-45: lo que los horarios ya usan de una estructura, leído por SQL. Datos propios en 2027. */
class UsoDeEstructuraSqlIT extends PruebaHorarios {
	@Autowired
	private UsoDeEstructura uso;

	private UUID aula;
	private UUID plan;

	private void escenarioBase() {
		aula = ambiente("TMP-A1", "ACTIVO");
		plan = plan("Álgebra", 5);
	}

	@Test
	void devuelveElMayorBloqueDeCadaDiaSumandoTodasLasSeccionesQueUsanLaEstructura() {
		escenarioBase();
		UUID e1 = estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID a = cabecera(seccion("SEC-1", "T1", aula, null), e1);
		UUID b = cabecera(seccion("SEC-1", "T2", aula, null), e1);
		bloque(a, "LUN", 1, plan, aula);
		bloque(a, "LUN", 4, plan, aula);
		bloque(a, "MIE", 2, plan, aula);
		bloque(b, "LUN", 5, plan, aula);
		bloque(b, "MAR", 1, plan, aula);

		Map<DiaSemana, Integer> resultado = uso.usoPorDia(e1);

		assertThat(resultado).containsOnly(Map.entry(DiaSemana.LUN, 5), Map.entry(DiaSemana.MAR, 1),
				Map.entry(DiaSemana.MIE, 2));
		// Los días salen en el orden de la semana: así siempre se informa el mismo primer caso.
		assertThat(List.copyOf(resultado.keySet())).containsExactly(DiaSemana.LUN, DiaSemana.MAR, DiaSemana.MIE);
		assertThat(uso.tieneHorarios(e1)).isTrue();
	}

	@Test
	void ignoraLosHorariosDeOtrasEstructuras() {
		escenarioBase();
		UUID e1 = estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID e2 = estructura("EH-OTRA", "SECUNDARIA", false, 6);
		bloque(cabecera(seccion("SEC-1", "T1", aula, null), e1), "LUN", 2, plan, aula);
		bloque(cabecera(seccion("SEC-1", "T2", aula, null), e2), "VIE", 6, plan, aula);

		assertThat(uso.usoPorDia(e1)).containsOnly(Map.entry(DiaSemana.LUN, 2));
		assertThat(uso.usoPorDia(e2)).containsOnly(Map.entry(DiaSemana.VIE, 6));
	}

	@Test
	void unaCabeceraSinBloquesCuentaComoHorarioPeroNoComoUsoDeDias() {
		escenarioBase();
		UUID conCabecera = estructura("EH-T-27", "SECUNDARIA", true, 6);
		UUID sinCabecera = estructura("EH-OTRA", "SECUNDARIA", false, 6);
		cabecera(seccion("SEC-1", "T1", aula, null), conCabecera);

		assertThat(uso.usoPorDia(conCabecera)).isEmpty();
		assertThat(uso.tieneHorarios(conCabecera)).isTrue();
		assertThat(uso.usoPorDia(sinCabecera)).isEmpty();
		assertThat(uso.tieneHorarios(sinCabecera)).isFalse();
		assertThat(uso.usoPorDia(UUID.randomUUID())).isEmpty();
		assertThat(uso.tieneHorarios(UUID.randomUUID())).isFalse();
	}
}
