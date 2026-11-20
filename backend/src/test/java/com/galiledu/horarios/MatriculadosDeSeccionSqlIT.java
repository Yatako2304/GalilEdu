package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.configuracion.dominio.Seccion;
import com.galiledu.horarios.aplicacion.MatriculadosDeSeccion;

/**
 * HU-48: cuántos alumnos tiene una sección, contados en la BD real. Cuentan todas las matrículas menos RETIRADO y
 * ANULADA. 4.° A de 2026 tiene una sola matrícula (la de Sofía, MATRICULADO) en los datos de prueba.
 */
class MatriculadosDeSeccionSqlIT extends PruebaReservas {
	private static final UUID MATRICULA_DE_SOFIA = UUID.fromString("50000000-0000-0000-0000-000000000001");

	@Autowired
	private MatriculadosDeSeccion matriculados;

	private void cambiarEstadoDeSofia(String estado) {
		jdbc.sql("UPDATE matricula.matricula SET estado = :estado, fecha_retiro = current_date WHERE id = :id")
				.param("estado", estado).param("id", MATRICULA_DE_SOFIA).update();
	}

	@DisplayName("Cuenta las matrículas de la sección")
	@Test
	void cuentaLasMatriculasDeLaSeccion() {
		assertThat(matriculados.de(seccion2026("SEC-4", "A"))).isEqualTo(1);
		assertThat(matriculados.de(seccion2026("SEC-1", "A"))).isEqualTo(3);
	}

	@DisplayName("Una sección sin matrículas tiene cero")
	@Test
	void unaSeccionSinMatriculasTieneCero() {
		assertThat(matriculados.de(seccion("SEC-1", "R1", null, null))).isZero();
		assertThat(matriculados.de(UUID.randomUUID())).isZero();
	}

	@DisplayName("Ocupan vacante todos los estados menos retirado y anulada")
	@Test
	void ocupanVacanteTodosLosEstadosMenosRetiradoYAnulada() {
		UUID cuartoA = seccion2026("SEC-4", "A");

		for (String estado : new String[]{"SECCION_RESERVADA", "EN_VALIDACION", "PENDIENTE_PAGO", "MATRICULADO"}) {
			cambiarEstadoDeSofia(estado);
			assertThat(matriculados.de(cuartoA)).as(estado).isEqualTo(1);
		}
		for (String estado : new String[]{"RETIRADO", "ANULADA"}) {
			cambiarEstadoDeSofia(estado);
			assertThat(matriculados.de(cuartoA)).as(estado).isZero();
		}
	}

	/** Horarios copia la condición de Configuración (no importa su entidad): si una cambia, esta prueba lo avisa. */
	@DisplayName("La condición de vacante es la misma que la de configuración")
	@Test
	void laCondicionDeVacanteEsLaMismaQueLaDeConfiguracion() throws Exception {
		var campo = Class.forName("com.galiledu.horarios.infraestructura.MatriculadosDeSeccionSql")
				.getDeclaredField("SQL_OCUPA_VACANTE");
		campo.setAccessible(true);

		assertThat(campo.get(null)).isEqualTo(Seccion.SQL_OCUPA_VACANTE);
	}
}
