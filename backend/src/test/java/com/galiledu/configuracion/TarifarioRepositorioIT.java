package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.dominio.CuotaTarifario;
import com.galiledu.configuracion.dominio.DatosTarifario;
import com.galiledu.configuracion.dominio.EstadoTarifario;
import com.galiledu.configuracion.dominio.Tarifario;
import com.galiledu.configuracion.infraestructura.ConsultasTarifario;
import com.galiledu.configuracion.infraestructura.TarifarioRepositorio;

/** HU-34: el mapeo JPA de pagos.tarifario y pagos.cuota_tarifario con los datos de prueba (2026 confirmado y 2027). */
class TarifarioRepositorioIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");

	@Autowired
	private TarifarioRepositorio tarifarios;
	@Autowired
	private ConsultasTarifario consultas;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcClient jdbc;

	private static DatosTarifario datos() {
		return new DatosTarifario(new BigDecimal("500"), new BigDecimal("450"), new BigDecimal("620"), 5,
				new BigDecimal("2"), new BigDecimal("10"));
	}

	@Test
	void leeElTarifario2026ConfirmadoConSuCalendario() {
		Tarifario t = tarifarios.findByAnioEscolarId(ANIO_2026).orElseThrow();

		assertThat(t.getEstado()).isEqualTo(EstadoTarifario.CONFIRMADO);
		assertThat(t.getMontoCuotaIngreso()).isEqualByComparingTo("500");
		assertThat(t.getMontoMatricula()).isEqualByComparingTo("420");
		assertThat(t.getVencimientoMatricula()).isEqualTo(LocalDate.of(2026, 2, 28));
		assertThat(t.getMontoPension()).isEqualByComparingTo("600");
		assertThat(t.getCantidadCuotas()).isEqualTo(10);
		assertThat(t.getDiasGracia()).isEqualTo(5);
		assertThat(t.getRecargoPorcentaje()).isEqualByComparingTo("2");
		assertThat(t.getTopeRecargoPorcentaje()).isEqualByComparingTo("10");
		assertThat(t.getCuotas()).extracting(CuotaTarifario::getNumero).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		assertThat(t.getCuotas().getFirst().getNombre()).isEqualTo("Pensión de marzo");
		assertThat(t.getCuotas().getLast().getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 12, 15));
		assertThat(t.totalPensiones()).isEqualByComparingTo("6000");
	}

	@Test
	void leeElTarifario2027EnConfiguracion() {
		Tarifario t = tarifarios.findByAnioEscolarId(ANIO_2027).orElseThrow();

		assertThat(t.getEstado()).isEqualTo(EstadoTarifario.EN_CONFIGURACION);
		assertThat(t.getMontoPension()).isEqualByComparingTo("620");
		assertThat(t.getCuotas()).hasSize(10);
	}

	@Test
	void buscaElTarifarioPorElNumeroDelAnio() {
		assertThat(tarifarios.findByAnio(2026)).get().extracting(Tarifario::getAnioEscolarId)
				.isEqualTo(ANIO_2026);
		assertThat(tarifarios.findByAnio(2025)).isEmpty();
	}

	@Test
	void soloElTarifarioConCompromisosLosTiene() {
		Tarifario t2026 = tarifarios.findByAnioEscolarId(ANIO_2026).orElseThrow();
		Tarifario t2027 = tarifarios.findByAnioEscolarId(ANIO_2027).orElseThrow();

		assertThat(consultas.tieneCompromisos(t2026.getId())).isTrue(); // el de Sofía
		assertThat(consultas.tieneCompromisos(t2027.getId())).isFalse();
		assertThat(consultas.tieneCompromisos(UUID.randomUUID())).isFalse();
	}

	@Test
	void guardaUnTarifarioNuevoYRegeneraSuCalendarioSinChocarConLosUnique() {
		UUID anio = UUID.randomUUID();
		jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (:id, 2028, '2028-03-01', '2028-12-15', 'TRIMESTRAL', 'PLANIFICADO')""").param("id", anio)
				.update();
		Tarifario t = Tarifario.nuevo(anio);
		t.confirmar(datos(), LocalDate.of(2028, 3, 1), LocalDate.of(2028, 12, 15));
		UUID id = tarifarios.saveAndFlush(t).getId();
		assertThat(contarCuotas(id)).isEqualTo(10);

		t.quitarCuotas();
		tarifarios.flush();
		t.regenerarCalendario(LocalDate.of(2028, 4, 3), LocalDate.of(2028, 11, 30));
		tarifarios.flush();

		assertThat(contarCuotas(id)).isEqualTo(8);
		assertThat(jdbc.sql("SELECT cantidad_cuotas FROM pagos.tarifario WHERE id = ?").param(id).query(Integer.class)
				.single()).isEqualTo(8);
		assertThat(jdbc.sql("SELECT vencimiento_matricula FROM pagos.tarifario WHERE id = ?").param(id)
				.query(LocalDate.class).single()).isEqualTo(LocalDate.of(2028, 3, 31));
		entityManager.clear();
		assertThat(tarifarios.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoTarifario.CONFIRMADO);
	}

	private int contarCuotas(UUID tarifarioId) {
		return jdbc.sql("SELECT count(*) FROM pagos.cuota_tarifario WHERE tarifario_id = ?").param(tarifarioId)
				.query(Integer.class).single();
	}
}
