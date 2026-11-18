package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.academico.infraestructura.ConsultasAmbientes;
import com.galiledu.academico.infraestructura.ConsultasAmbientes.SeccionOcupada;

/**
 * RF-111: la sección con más matriculados entre las que usan un aula. Cada prueba arma su propia aula, sus secciones y
 * sus matrículas (con SQL, como lo haría otro módulo); los datos de prueba solo se usan en el último caso.
 * <p>
 * Qué ve cada quien: lo que la prueba inserta con {@code JdbcClient} está <b>en la transacción</b> de la prueba, y
 * {@code ConsultasAmbientes} usa esa misma conexión, así que lo ve aunque no esté <b>confirmado</b>. Otra conexión
 * no lo ve hasta el commit, que nunca llega porque la prueba se deshace. No hay nada en <b>memoria</b> de Hibernate
 * aquí: todo se escribe y se lee con JDBC, sin entidades.
 */
class ConsultasAmbientesIT extends PruebaIntegracion {
	private static final String ANIO_2026_ACTIVO = "30000000-0000-0000-0000-000000002026";
	private static final String ANIO_2027_PLANIFICADO = "30000000-0000-0000-0000-000000002027";

	@Autowired
	private ConsultasAmbientes consultas;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private DataSource dataSource;

	private int contador = 0;

	// ---------- Datos propios ----------

	private UUID ambiente(String codigo) {
		return jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo)
				VALUES (:codigo, :nombre, 'AULA_REGULAR', 40) RETURNING id""")
				.param("codigo", codigo).param("nombre", "Aula " + codigo).query(UUID.class).single();
	}

	private UUID anioCerrado() {
		return jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (2040, DATE '2040-03-01', DATE '2040-12-15', 'BIMESTRAL', 'CERRADO') RETURNING id""")
				.query(UUID.class).single();
	}

	private UUID seccion(String anioId, String grado, String nombre, String estado, UUID ambienteId) {
		return jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima, estado,
				                                   ambiente_id)
				VALUES (:anio::uuid, :grado, :nombre, 40, :estado, :ambiente) RETURNING id""")
				.param("anio", anioId).param("grado", grado).param("nombre", nombre).param("estado", estado)
				.param("ambiente", ambienteId).query(UUID.class).single();
	}

	/** Una matrícula por estado, cada una de un estudiante nuevo (hay una matrícula por estudiante y año). */
	private void matricular(UUID seccionId, String anioId, String grado, String... estados) {
		for (String estado : estados) {
			int n = ++contador;
			UUID persona = jdbc.sql("""
					INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, nombres)
					VALUES ('DNI', :documento, 'Prueba', 'Alumno') RETURNING id""")
					.param("documento", "9902%04d".formatted(n)).query(UUID.class).single();
			UUID estudiante = jdbc.sql("""
					INSERT INTO estudiantes.estudiante (persona_id, anio_academico, grado_codigo)
					VALUES (:persona, 2026, :grado) RETURNING id""")
					.param("persona", persona).param("grado", grado).query(UUID.class).single();
			// RETIRADO exige fecha de retiro; RETIRADO y MATRICULADO exigen código, y el código tiene el formato
			// MAT-AAAA-NNNN (ck_matricula_retiro, ck_matricula_codigo y ck_matricula_formato_codigo de V7).
			jdbc.sql("""
					INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id,
					                                 tipo_estudiante, estado, codigo, fecha_retiro)
					VALUES (:estudiante, :anio::uuid, :grado, :seccion, 'NUEVO', :estado, :codigo,
					        CASE WHEN CAST(:estado AS text) = 'RETIRADO' THEN current_date END)""")
					.param("estudiante", estudiante).param("anio", anioId).param("grado", grado)
					.param("seccion", seccionId).param("estado", estado).param("codigo", "MAT-2099-%04d".formatted(n))
					.update();
		}
	}

	// ---------- Sin secciones ----------

	@Test
	void unAulaSinSeccionesNoTieneSeccionOcupada() {
		assertThat(consultas.seccionMasOcupada(ambiente("A-901"))).isEmpty();
	}

	@Test
	void unAmbienteQueNoExisteNoTieneSeccionOcupada() {
		assertThat(consultas.seccionMasOcupada(UUID.randomUUID())).isEmpty();
	}

	@Test
	void unaSeccionSinAulaNoCuentaParaNingunAmbiente() {
		UUID aula = ambiente("A-901");
		UUID sinAula = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", null);
		matricular(sinAula, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO");

		assertThat(consultas.seccionMasOcupada(aula)).isEmpty();
	}

	// ---------- Qué matrículas cuentan ----------

	@Test
	void unaSeccionSinMatriculasCuentaCeroYEsElResultado() {
		UUID aula = ambiente("A-901");
		seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", aula);

		assertThat(consultas.seccionMasOcupada(aula))
				.contains(new SeccionOcupada("1.° de secundaria", "T1", 0));
	}

	@Test
	void retiradoYAnuladaNoCuentan() {
		UUID aula = ambiente("A-901");
		UUID seccion = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", aula);
		matricular(seccion, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO", "RETIRADO", "ANULADA", "RETIRADO");

		assertThat(consultas.seccionMasOcupada(aula)).contains(new SeccionOcupada("1.° de secundaria", "T1", 1));
	}

	@Test
	void unaSeccionSoloConRetiradosYAnuladosCuentaCero() {
		UUID aula = ambiente("A-901");
		UUID seccion = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", aula);
		matricular(seccion, ANIO_2026_ACTIVO, "SEC-1", "RETIRADO", "ANULADA");

		assertThat(consultas.seccionMasOcupada(aula)).contains(new SeccionOcupada("1.° de secundaria", "T1", 0));
	}

	@Test
	void todosLosDemasEstadosOcupanVacante() {
		// La misma definición que Seccion.SQL_OCUPA_VACANTE: una reserva o un pago pendiente ya ocupa lugar.
		UUID aula = ambiente("A-901");
		UUID seccion = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", aula);
		matricular(seccion, ANIO_2026_ACTIVO, "SEC-1", "SECCION_RESERVADA", "EN_VALIDACION", "PENDIENTE_PAGO",
				"MATRICULADO");

		assertThat(consultas.seccionMasOcupada(aula))
				.hasValueSatisfying(o -> assertThat(o.matriculados()).isEqualTo(4));
	}

	@Test
	void lasMatriculasDeOtraSeccionNoSeMezclan() {
		UUID aula = ambiente("A-901");
		UUID otraAula = ambiente("A-902");
		UUID mia = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", aula);
		UUID deOtraAula = seccion(ANIO_2026_ACTIVO, "SEC-1", "T2", "ACTIVA", otraAula);
		matricular(mia, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO");
		matricular(deOtraAula, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO", "MATRICULADO", "MATRICULADO");

		assertThat(consultas.seccionMasOcupada(aula)).contains(new SeccionOcupada("1.° de secundaria", "T1", 1));
		assertThat(consultas.seccionMasOcupada(otraAula)).contains(new SeccionOcupada("1.° de secundaria", "T2", 3));
	}

	// ---------- Qué secciones cuentan ----------

	@Test
	void unaSeccionInactivaNoCuenta() {
		UUID aula = ambiente("A-901");
		UUID inactiva = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "INACTIVA", aula);
		matricular(inactiva, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO", "MATRICULADO", "MATRICULADO");

		assertThat(consultas.seccionMasOcupada(aula)).isEmpty();
	}

	@Test
	void conUnaInactivaMuyLlenaGanaLaActivaAunqueTengaMenos() {
		UUID aula = ambiente("A-901");
		UUID inactiva = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "INACTIVA", aula);
		UUID activa = seccion(ANIO_2026_ACTIVO, "SEC-1", "T2", "ACTIVA", aula);
		matricular(inactiva, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO", "MATRICULADO", "MATRICULADO");
		matricular(activa, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO");

		assertThat(consultas.seccionMasOcupada(aula)).contains(new SeccionOcupada("1.° de secundaria", "T2", 1));
	}

	@Test
	void unaSeccionDeUnAnioCerradoNoCuenta() {
		UUID aula = ambiente("A-901");
		String cerrado = anioCerrado().toString();
		UUID deCerrado = seccion(cerrado, "SEC-1", "T1", "ACTIVA", aula);
		matricular(deCerrado, cerrado, "SEC-1", "MATRICULADO", "MATRICULADO");

		assertThat(consultas.seccionMasOcupada(aula)).isEmpty();
	}

	@Test
	void conUnAnioCerradoMuyLlenoGanaLaSeccionDelAnioVigente() {
		UUID aula = ambiente("A-901");
		String cerrado = anioCerrado().toString();
		UUID deCerrado = seccion(cerrado, "SEC-1", "T1", "ACTIVA", aula);
		UUID vigente = seccion(ANIO_2026_ACTIVO, "SEC-1", "T2", "ACTIVA", aula);
		matricular(deCerrado, cerrado, "SEC-1", "MATRICULADO", "MATRICULADO", "MATRICULADO");
		matricular(vigente, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO");

		assertThat(consultas.seccionMasOcupada(aula)).contains(new SeccionOcupada("1.° de secundaria", "T2", 1));
	}

	@Test
	void unAnioPlanificadoYUnoActivoSiCuentan() {
		UUID aula = ambiente("A-901");
		UUID planificada = seccion(ANIO_2027_PLANIFICADO, "SEC-1", "T1", "ACTIVA", aula);
		UUID activa = seccion(ANIO_2026_ACTIVO, "SEC-1", "T2", "ACTIVA", aula);
		matricular(planificada, ANIO_2027_PLANIFICADO, "SEC-1", "SECCION_RESERVADA", "SECCION_RESERVADA");
		matricular(activa, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO");

		assertThat(consultas.seccionMasOcupada(aula)).contains(new SeccionOcupada("1.° de secundaria", "T1", 2));
	}

	// ---------- Varias secciones en la misma aula ----------

	@Test
	void conVariasSeccionesDevuelveLaDeMasMatriculadosYNoLaSuma() {
		UUID aula = ambiente("A-901");
		UUID t1 = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", aula);
		UUID t2 = seccion(ANIO_2026_ACTIVO, "SEC-2", "T2", "ACTIVA", aula);
		UUID t3 = seccion(ANIO_2027_PLANIFICADO, "SEC-3", "T3", "ACTIVA", aula);
		matricular(t1, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO", "MATRICULADO", "MATRICULADO");
		matricular(t2, ANIO_2026_ACTIVO, "SEC-2", "MATRICULADO", "PENDIENTE_PAGO", "MATRICULADO", "MATRICULADO",
				"MATRICULADO", "RETIRADO");
		matricular(t3, ANIO_2027_PLANIFICADO, "SEC-3", "SECCION_RESERVADA", "SECCION_RESERVADA");

		// 3 + 5 + 2 = 10 sumadas, pero la regla compara cada sección con el aforo: gana la de 5.
		assertThat(consultas.seccionMasOcupada(aula)).contains(new SeccionOcupada("2.° de secundaria", "T2", 5));
	}

	@Test
	void unEmpateSeResuelveSiempreIgualPorAnioGradoYNombre() {
		UUID aula = ambiente("A-901");
		UUID deSec2 = seccion(ANIO_2026_ACTIVO, "SEC-2", "T1", "ACTIVA", aula);
		UUID t2DeSec1 = seccion(ANIO_2026_ACTIVO, "SEC-1", "T2", "ACTIVA", aula);
		UUID t1DeSec1 = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", aula);
		UUID de2027 = seccion(ANIO_2027_PLANIFICADO, "SEC-1", "T0", "ACTIVA", aula);
		for (var par : new Object[][]{{deSec2, ANIO_2026_ACTIVO, "SEC-2"}, {t2DeSec1, ANIO_2026_ACTIVO, "SEC-1"},
				{t1DeSec1, ANIO_2026_ACTIVO, "SEC-1"}, {de2027, ANIO_2027_PLANIFICADO, "SEC-1"}}) {
			matricular((UUID) par[0], (String) par[1], (String) par[2], "MATRICULADO", "MATRICULADO");
		}

		// Todas con 2: gana la del año más antiguo (2026), de grado más bajo (1.°) y nombre menor (T1).
		assertThat(consultas.seccionMasOcupada(aula)).contains(new SeccionOcupada("1.° de secundaria", "T1", 2));
	}

	// ---------- Confirmado o no ----------

	@Test
	void lasFilasDeLaPruebaSoloLasVeLaTransaccionQueLasEscribio() throws SQLException {
		UUID aula = ambiente("A-901");
		UUID seccion = seccion(ANIO_2026_ACTIVO, "SEC-1", "T1", "ACTIVA", aula);
		matricular(seccion, ANIO_2026_ACTIVO, "SEC-1", "MATRICULADO");

		// En la transacción (la conexión de la prueba): ConsultasAmbientes lo ve sin que esté confirmado.
		assertThat(consultas.seccionMasOcupada(aula)).isPresent();
		// Confirmado (otra conexión, que solo ve lo que ya se hizo commit): todavía no existe.
		assertThat(contarAmbientesEnOtraConexion("A-901")).isZero();
	}

	private long contarAmbientesEnOtraConexion(String codigo) throws SQLException {
		try (Connection otra = dataSource.getConnection();
				PreparedStatement consulta = otra
						.prepareStatement("SELECT count(*) FROM configuracion.ambiente WHERE codigo = ?")) {
			consulta.setString(1, codigo);
			try (ResultSet resultado = consulta.executeQuery()) {
				resultado.next();
				return resultado.getLong(1);
			}
		}
	}

	// ---------- Datos de prueba ----------

	@Test
	void conLosDatosDePruebaElAulaA101EsDeLa1ASecundaria() {
		UUID a101 = jdbc.sql("SELECT id FROM configuracion.ambiente WHERE codigo = 'A-101'").query(UUID.class)
				.single();
		int esperados = jdbc.sql("""
				SELECT count(*)::int FROM matricula.matricula m
				JOIN configuracion.seccion s ON s.id = m.seccion_id
				WHERE s.ambiente_id = :ambiente AND m.estado NOT IN ('RETIRADO', 'ANULADA')""")
				.param("ambiente", a101).query(Integer.class).single();

		var ocupada = consultas.seccionMasOcupada(a101);

		assertThat(esperados).isPositive();
		assertThat(ocupada).contains(new SeccionOcupada("1.° de secundaria", "A", esperados));
	}

	@Test
	void unAulaInactivaSinSeccionesDeLosDatosDePruebaNoTieneSeccionOcupada() {
		UUID a205 = jdbc.sql("SELECT id FROM configuracion.ambiente WHERE codigo = 'A-205'").query(UUID.class)
				.single();
		assertThat(consultas.seccionMasOcupada(a205)).isEmpty();
	}
}
