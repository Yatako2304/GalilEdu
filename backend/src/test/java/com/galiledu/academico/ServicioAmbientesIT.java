package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.academico.aplicacion.DtosAmbientes.AmbienteResumen;
import com.galiledu.academico.aplicacion.DtosAmbientes.SolicitudEdicionAmbiente;
import com.galiledu.academico.aplicacion.ServicioAmbientes;
import com.galiledu.academico.dominio.EstadoAmbiente;
import com.galiledu.academico.dominio.TipoAmbiente;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * HU-43: consultas y cambios de {@link ServicioAmbientes} con los datos de prueba (A-101 en uso por 1.° A, A-205
 * inactiva, AUD-01 de 180). Todo corre dentro de la transacción de la prueba, que se deshace.
 * <p>
 * Lo que no entra aquí es {@code crear}: cada intento confirma en su propia transacción y esta prueba no puede
 * deshacerlo; está en {@link ServicioAmbientesCrearIT}.
 * <p>
 * Memoria, transacción y confirmado: {@code actualizar}, {@code desactivar} y {@code reactivar} modifican la entidad
 * <b>en memoria</b> (la respuesta ya sale de ahí) y Hibernate la escribe en la <b>transacción</b> recién en el flush;
 * como la prueba nunca hace commit, <b>nada se confirma</b>. Por eso estas pruebas llaman a {@code em.flush()} antes
 * de leer la fila con JDBC.
 */
class ServicioAmbientesIT extends PruebaIntegracion {
	private static final String ANIO_2026_ACTIVO = "30000000-0000-0000-0000-000000002026";

	@Autowired
	private ServicioAmbientes servicio;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager em;

	private int contador = 0;

	// ---------- Apoyo ----------

	private UUID idDe(String codigo) {
		return jdbc.sql("SELECT id FROM configuracion.ambiente WHERE codigo = :codigo").param("codigo", codigo)
				.query(UUID.class).single();
	}

	/** La fila tal como está en la base (después de un flush, es lo que Hibernate escribió). */
	private Map<String, Object> fila(String codigo) {
		return jdbc.sql("""
				SELECT codigo, nombre, tipo, aforo_maximo, array_to_string(recursos, '|') AS recursos, estado
				FROM configuracion.ambiente WHERE codigo = :codigo""").param("codigo", codigo).query().singleRow();
	}

	private static SolicitudEdicionAmbiente edicion(String nombre, Integer aforo, String... recursos) {
		return new SolicitudEdicionAmbiente(nombre, aforo, List.of(recursos));
	}

	private static void rechazaCon(HttpStatus estado, String mensaje, ThrowingCallable operacion) {
		assertThatThrownBy(operacion).isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
			assertThat(e.estado()).isEqualTo(estado);
			assertThat(e.getMessage()).isEqualTo(mensaje);
		});
	}

	private void insertarAmbiente(String codigo, String nombre, String tipo, int aforo, String estado) {
		jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo, estado)
				VALUES (:codigo, :nombre, :tipo, :aforo, :estado)""").param("codigo", codigo).param("nombre", nombre)
				.param("tipo", tipo).param("aforo", aforo).param("estado", estado).update();
	}

	private UUID seccionEnAula(UUID ambienteId, String nombre, String estado) {
		return jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima, estado,
				                                   ambiente_id)
				VALUES (:anio::uuid, 'SEC-1', :nombre, 40, :estado, :ambiente) RETURNING id""")
				.param("anio", ANIO_2026_ACTIVO).param("nombre", nombre).param("estado", estado)
				.param("ambiente", ambienteId).query(UUID.class).single();
	}

	private void matricular(UUID seccionId, String... estados) {
		for (String estado : estados) {
			int n = ++contador;
			UUID persona = jdbc.sql("""
					INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, nombres)
					VALUES ('DNI', :documento, 'Prueba', 'Alumno') RETURNING id""")
					.param("documento", "9903%04d".formatted(n)).query(UUID.class).single();
			UUID estudiante = jdbc.sql("""
					INSERT INTO estudiantes.estudiante (persona_id, anio_academico, grado_codigo)
					VALUES (:persona, 2026, 'SEC-1') RETURNING id""").param("persona", persona).query(UUID.class)
					.single();
			// Código MAT-AAAA-NNNN (V7); RETIRADO exige fecha de retiro.
			jdbc.sql("""
					INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id,
					                                 tipo_estudiante, estado, codigo, fecha_retiro)
					VALUES (:estudiante, :anio::uuid, 'SEC-1', :seccion, 'NUEVO', :estado, :codigo,
					        CASE WHEN CAST(:estado AS text) = 'RETIRADO' THEN current_date END)""")
					.param("estudiante", estudiante).param("anio", ANIO_2026_ACTIVO).param("seccion", seccionId)
					.param("estado", estado).param("codigo", "MAT-2098-%04d".formatted(n)).update();
		}
	}

	/** Matriculados de 1.° A que usan A-101 en los datos de prueba (los que ocupan vacante). */
	private int matriculadosDeA101() {
		return jdbc.sql("""
				SELECT count(*)::int FROM matricula.matricula m
				JOIN configuracion.seccion s ON s.id = m.seccion_id
				JOIN configuracion.ambiente a ON a.id = s.ambiente_id
				WHERE a.codigo = 'A-101' AND m.estado NOT IN ('RETIRADO', 'ANULADA')""").query(Integer.class).single();
	}

	// ---------- Resumen (tarjetas) ----------

	@DisplayName("El resumen cuenta solo los activos y el total incluye los inactivos")
	@Test
	void elResumenCuentaSoloLosActivosYElTotalIncluyeLosInactivos() {
		var esperado = jdbc.sql("""
				SELECT count(*) FILTER (WHERE estado = 'ACTIVO') AS activos, count(*) AS total,
				       count(*) FILTER (WHERE estado = 'ACTIVO' AND tipo = 'AULA_REGULAR') AS aulas,
				       count(*) FILTER (WHERE estado = 'ACTIVO' AND tipo = 'ESPACIO_ESPECIAL') AS espacios,
				       coalesce(sum(aforo_maximo) FILTER (WHERE estado = 'ACTIVO'), 0) AS aforo
				FROM configuracion.ambiente""").query().singleRow();

		var resumen = servicio.resumen();

		assertThat(resumen.activos()).isEqualTo(((Number) esperado.get("activos")).longValue());
		assertThat(resumen.total()).isEqualTo(((Number) esperado.get("total")).longValue());
		assertThat(resumen.aulasRegulares()).isEqualTo(((Number) esperado.get("aulas")).longValue());
		assertThat(resumen.espaciosEspeciales()).isEqualTo(((Number) esperado.get("espacios")).longValue());
		assertThat(resumen.aforoTotal()).isEqualTo(((Number) esperado.get("aforo")).longValue());
		// Los datos de prueba tienen un ambiente inactivo (A-205): no cuenta como activo pero sí como registrado.
		assertThat(resumen.total()).isGreaterThan(resumen.activos());
	}

	@DisplayName("Un ambiente nuevo suma a su tarjeta y uno inactivo solo al total")
	@Test
	void unAmbienteNuevoSumaASuTarjetaYUnoInactivoSoloAlTotal() {
		var antes = servicio.resumen();

		insertarAmbiente("A-901", "Aula nueva activa", "AULA_REGULAR", 20, "ACTIVO");
		insertarAmbiente("E-901", "Patio activo", "ESPACIO_ESPECIAL", 50, "ACTIVO");
		insertarAmbiente("E-902", "Taller cerrado", "ESPACIO_ESPECIAL", 70, "INACTIVO");
		var despues = servicio.resumen();

		assertThat(despues.activos()).isEqualTo(antes.activos() + 2);
		assertThat(despues.total()).isEqualTo(antes.total() + 3);
		assertThat(despues.aulasRegulares()).isEqualTo(antes.aulasRegulares() + 1);
		assertThat(despues.espaciosEspeciales()).isEqualTo(antes.espaciosEspeciales() + 1);
		assertThat(despues.aforoTotal()).isEqualTo(antes.aforoTotal() + 20 + 50); // los 70 del inactivo no suman
	}

	@DisplayName("El resumen no depende de los filtros de la lista")
	@Test
	void elResumenNoDependeDeLosFiltrosDeLaLista() {
		var antes = servicio.resumen();

		servicio.listar(TipoAmbiente.AULA_REGULAR, EstadoAmbiente.INACTIVO, "aula");
		servicio.listar(TipoAmbiente.ESPACIO_ESPECIAL, null, "nada que coincida");

		assertThat(servicio.resumen()).isEqualTo(antes);
	}

	// ---------- Listar ----------

	@DisplayName("Sin filtros lista todos por nombre con su código de solo lectura")
	@Test
	void sinFiltrosListaTodosPorNombreConSuCodigoDeSoloLectura() {
		var lista = servicio.listar(null, null, null);

		assertThat(lista).extracting(AmbienteResumen::nombre).containsSubsequence("Aula 101", "Aula 102", "Aula 201",
				"Aula 205");
		var a101 = lista.stream().filter(a -> a.nombre().equals("Aula 101")).findFirst().orElseThrow();
		assertThat(a101.codigo()).isEqualTo("A-101");
		assertThat(a101.tipo()).isEqualTo(TipoAmbiente.AULA_REGULAR);
		assertThat(a101.aforoMaximo()).isEqualTo(30);
		assertThat(a101.recursos()).containsExactly("Pizarra acrílica", "Proyector");
		assertThat(a101.estado()).isEqualTo(EstadoAmbiente.ACTIVO);
	}

	@DisplayName("Filtra por tipo y por estado")
	@Test
	void filtraPorTipoYPorEstado() {
		assertThat(servicio.listar(TipoAmbiente.AULA_REGULAR, null, null)).extracting(AmbienteResumen::tipo)
				.containsOnly(TipoAmbiente.AULA_REGULAR);
		assertThat(servicio.listar(TipoAmbiente.ESPACIO_ESPECIAL, null, null)).extracting(AmbienteResumen::nombre)
				.contains("Auditorio", "Biblioteca").doesNotContain("Aula 101");
		assertThat(servicio.listar(null, EstadoAmbiente.INACTIVO, null)).extracting(AmbienteResumen::nombre)
				.contains("Aula 205").doesNotContain("Aula 101");
		assertThat(servicio.listar(null, EstadoAmbiente.ACTIVO, null)).extracting(AmbienteResumen::nombre)
				.contains("Aula 101").doesNotContain("Aula 205");
		assertThat(servicio.listar(TipoAmbiente.AULA_REGULAR, EstadoAmbiente.INACTIVO, null))
				.extracting(AmbienteResumen::nombre).contains("Aula 205").doesNotContain("Aula 101", "Auditorio");
	}

	@DisplayName("La búsqueda es por nombre sin distinguir mayúsculas ni espacios sobrantes")
	@Test
	void laBusquedaEsPorNombreSinDistinguirMayusculasNiEspaciosSobrantes() {
		assertThat(servicio.listar(null, null, "AULA 10")).extracting(AmbienteResumen::nombre)
				.containsExactly("Aula 101", "Aula 102");
		assertThat(servicio.listar(null, null, "  aula    10  ")).extracting(AmbienteResumen::nombre)
				.containsExactly("Aula 101", "Aula 102");
		assertThat(servicio.listar(null, null, "laboratorio")).extracting(AmbienteResumen::nombre)
				.contains("Laboratorio de ciencias", "Laboratorio de cómputo").doesNotContain("Aula 101");
	}

	@DisplayName("La búsqueda no mira el código ni los recursos")
	@Test
	void laBusquedaNoMiraElCodigoNiLosRecursos() {
		assertThat(servicio.listar(null, null, "A-101")).isEmpty();
		assertThat(servicio.listar(null, null, "proyector")).isEmpty(); // recurso de A-101, no parte de un nombre
	}

	@DisplayName("La búsqueda no iguala tildes distintas")
	@Test
	void laBusquedaNoIgualaTildesDistintas() {
		assertThat(servicio.listar(null, null, "CÓMPUTO")).extracting(AmbienteResumen::nombre)
				.containsExactly("Laboratorio de cómputo");
		assertThat(servicio.listar(null, null, "computo")).isEmpty();
	}

	@DisplayName("Una búsqueda en blanco no filtra")
	@Test
	void unaBusquedaEnBlancoNoFiltra() {
		assertThat(servicio.listar(null, null, "   ")).hasSameSizeAs(servicio.listar(null, null, null));
	}

	@DisplayName("Los filtros se combinan")
	@Test
	void losFiltrosSeCombinan() {
		assertThat(servicio.listar(TipoAmbiente.AULA_REGULAR, EstadoAmbiente.ACTIVO, "aula 10"))
				.extracting(AmbienteResumen::nombre).containsExactly("Aula 101", "Aula 102");
		assertThat(servicio.listar(TipoAmbiente.ESPACIO_ESPECIAL, EstadoAmbiente.ACTIVO, "aula 10")).isEmpty();
	}

	// ---------- Actualizar ----------

	@DisplayName("Actualiza nombre aforo y recursos y no toca código tipo ni estado")
	@Test
	void actualizaNombreAforoYRecursosYNoTocaCodigoTipoNiEstado() {
		UUID id = idDe("A-101");

		var respuesta = servicio.actualizar(id,
				edicion("  Aula   101 renovada ", 40, " Pizarra digital ", "", "Proyector",
						"proyector"));

		// En memoria: la respuesta ya sale de la entidad modificada.
		assertThat(respuesta.nombre()).isEqualTo("Aula 101 renovada");
		assertThat(respuesta.aforoMaximo()).isEqualTo(40);
		assertThat(respuesta.recursos()).containsExactly("Pizarra digital", "Proyector");
		assertThat(respuesta.codigo()).isEqualTo("A-101");
		assertThat(respuesta.tipo()).isEqualTo(TipoAmbiente.AULA_REGULAR);
		assertThat(respuesta.estado()).isEqualTo(EstadoAmbiente.ACTIVO);
		// En la transacción pero todavía sin escribir: la fila aún tiene los valores anteriores.
		assertThat(fila("A-101")).containsEntry("nombre", "Aula 101").containsEntry("aforo_maximo", 30);
		// Tras el flush, Hibernate la escribió (la base la ve dentro de esta transacción; nunca se confirma).
		em.flush();
		assertThat(fila("A-101")).containsEntry("nombre", "Aula 101 renovada").containsEntry("aforo_maximo", 40)
				.containsEntry("recursos", "Pizarra digital|Proyector").containsEntry("codigo", "A-101")
				.containsEntry("tipo", "AULA_REGULAR").containsEntry("estado", "ACTIVO");
	}

	@DisplayName("Puede conservar su propio nombre aunque cambie la capitalización")
	@Test
	void puedeConservarSuPropioNombreAunqueCambieLaCapitalizacion() {
		var respuesta = servicio.actualizar(idDe("A-101"), edicion("AULA 101", 30));

		assertThat(respuesta.nombre()).isEqualTo("AULA 101");
		assertThat(respuesta.recursos()).isEmpty(); // sin recursos en la solicitud: quedan ninguno
	}

	@DisplayName("Un espacio especial puede superar45")
	@Test
	void unEspacioEspecialPuedeSuperar45() {
		var respuesta = servicio.actualizar(idDe("AUD-01"), edicion("Auditorio", 300, "Sonido"));

		assertThat(respuesta.aforoMaximo()).isEqualTo(300);
		assertThat(respuesta.tipo()).isEqualTo(TipoAmbiente.ESPACIO_ESPECIAL);
	}

	@DisplayName("Rechaza el nombre de otro ambiente sin distinguir mayúsculas ni espacios")
	@Test
	void rechazaElNombreDeOtroAmbienteSinDistinguirMayusculasNiEspacios() {
		UUID id = idDe("A-101");

		// El mensaje cita el nombre que escribió el usuario, ya sin espacios sobrantes.
		rechazaCon(HttpStatus.CONFLICT, "Ya existe un ambiente llamado \"aula 102\".",
				() -> servicio.actualizar(id, edicion("  aula   102 ", 30)));
		// También cuenta un ambiente inactivo (A-205): se puede reactivar.
		rechazaCon(HttpStatus.CONFLICT, "Ya existe un ambiente llamado \"AULA 205\".",
				() -> servicio.actualizar(id, edicion("AULA 205", 30)));
	}

	@DisplayName("Los datos invalidos dan400 con el mensaje del dominio")
	@Test
	void losDatosInvalidosDan400ConElMensajeDelDominio() {
		UUID id = idDe("A-101");

		rechazaCon(HttpStatus.BAD_REQUEST, "Ingresa el nombre del ambiente.",
				() -> servicio.actualizar(id, edicion("   ", 30)));
		rechazaCon(HttpStatus.BAD_REQUEST, "El aforo debe ser mayor que 0.",
				() -> servicio.actualizar(id, edicion("Aula", 0)));
		rechazaCon(HttpStatus.BAD_REQUEST, "El aforo debe ser mayor que 0.",
				() -> servicio.actualizar(id, edicion("Aula", -5)));
		rechazaCon(HttpStatus.BAD_REQUEST, "Un aula regular admite como máximo 45.",
				() -> servicio.actualizar(id, edicion("Aula", 46)));
		rechazaCon(HttpStatus.BAD_REQUEST, "Cada recurso admite como máximo 60 caracteres.",
				() -> servicio.actualizar(id, edicion("Aula", 30, "r".repeat(61))));
		rechazaCon(HttpStatus.BAD_REQUEST, "El nombre admite como máximo 80 caracteres.",
				() -> servicio.actualizar(id, edicion("a".repeat(81), 30)));
		rechazaCon(HttpStatus.BAD_REQUEST, "Ingresa el aforo máximo.",
				() -> servicio.actualizar(id, edicion("Aula", null)));
	}

	@DisplayName("Un dato inválido no deja nada modificado ni siquiera lo que si era válido")
	@Test
	void unDatoInvalidoNoDejaNadaModificadoNiSiquieraLoQueSiEraValido() {
		UUID id = idDe("A-101");

		// Nombre válido y nuevo, aforo inválido: no puede quedar el nombre cambiado en la entidad.
		assertThatThrownBy(() -> servicio.actualizar(id, edicion("Nombre nuevo", 46, "Parlantes")))
				.isInstanceOf(ExcepcionNegocio.class);
		em.flush(); // si la entidad hubiera cambiado, aquí se escribiría
		assertThat(fila("A-101")).containsEntry("nombre", "Aula 101").containsEntry("aforo_maximo", 30)
				.containsEntry("recursos", "Pizarra acrílica|Proyector");

		// Y con un nombre repetido (409) tampoco cambian el aforo ni los recursos.
		assertThatThrownBy(() -> servicio.actualizar(id, edicion("Aula 102", 20, "Parlantes")))
				.isInstanceOf(ExcepcionNegocio.class);
		em.flush();
		assertThat(fila("A-101")).containsEntry("nombre", "Aula 101").containsEntry("aforo_maximo", 30)
				.containsEntry("recursos", "Pizarra acrílica|Proyector");
	}

	@DisplayName("Un ambiente que no existe es404")
	@Test
	void unAmbienteQueNoExisteEs404() {
		rechazaCon(HttpStatus.NOT_FOUND, "El ambiente no existe.",
				() -> servicio.actualizar(UUID.randomUUID(), edicion("Aula", 30)));
		rechazaCon(HttpStatus.NOT_FOUND, "El ambiente no existe.", () -> servicio.desactivar(UUID.randomUUID()));
		rechazaCon(HttpStatus.NOT_FOUND, "El ambiente no existe.", () -> servicio.reactivar(UUID.randomUUID()));
	}

	// ---------- Actualizar: RF-111 (aforo contra la matrícula) ----------

	@DisplayName("No baja el aforo por debajo de la matrícula de una sección que usa el aula")
	@Test
	void noBajaElAforoPorDebajoDeLaMatriculaDeUnaSeccionQueUsaElAula() {
		UUID id = idDe("A-101");
		int matriculados = matriculadosDeA101();
		assertThat(matriculados).as("1.° A tiene alumnos en los datos de prueba").isPositive();

		rechazaCon(HttpStatus.CONFLICT,
				"No puedes bajar el aforo a %d: la sección A de 1.° de secundaria tiene %d estudiantes matriculados."
						.formatted(matriculados - 1, matriculados),
				() -> servicio.actualizar(id, edicion("Aula 101", matriculados - 1)));

		em.flush();
		assertThat(fila("A-101")).containsEntry("aforo_maximo", 30); // el rechazo no modificó nada
	}

	@DisplayName("Permite bajar el aforo hasta exactamente la matrícula")
	@Test
	void permiteBajarElAforoHastaExactamenteLaMatricula() {
		UUID id = idDe("A-101");
		int matriculados = matriculadosDeA101();

		assertThat(servicio.actualizar(id, edicion("Aula 101", matriculados)).aforoMaximo()).isEqualTo(matriculados);
	}

	@DisplayName("Subir el aforo no consulta la matrícula")
	@Test
	void subirElAforoNoConsultaLaMatricula() {
		assertThat(servicio.actualizar(idDe("A-101"), edicion("Aula 101", 45)).aforoMaximo()).isEqualTo(45);
	}

	@DisplayName("Retirados y anulados no impiden bajar el aforo")
	@Test
	void retiradosYAnuladosNoImpidenBajarElAforo() {
		insertarAmbiente("A-901", "Aula con retiros", "AULA_REGULAR", 40, "ACTIVO");
		UUID id = idDe("A-901");
		UUID seccion = seccionEnAula(id, "T1", "ACTIVA");
		matricular(seccion, "MATRICULADO", "PENDIENTE_PAGO", "SECCION_RESERVADA", "RETIRADO", "ANULADA");

		// Ocupan vacante 3 de las 5 matrículas.
		rechazaCon(HttpStatus.CONFLICT,
				"No puedes bajar el aforo a 2: la sección T1 de 1.° de secundaria tiene 3 estudiantes matriculados.",
				() -> servicio.actualizar(id, edicion("Aula con retiros", 2)));
		assertThat(servicio.actualizar(id, edicion("Aula con retiros", 3)).aforoMaximo()).isEqualTo(3);
	}

	@DisplayName("Una sección inactiva no impide bajar el aforo")
	@Test
	void unaSeccionInactivaNoImpideBajarElAforo() {
		insertarAmbiente("A-901", "Aula sin secciones activas", "AULA_REGULAR", 40, "ACTIVO");
		UUID id = idDe("A-901");
		matricular(seccionEnAula(id, "T1", "INACTIVA"), "MATRICULADO", "MATRICULADO", "MATRICULADO");

		assertThat(servicio.actualizar(id, edicion("Aula sin secciones activas", 1)).aforoMaximo()).isEqualTo(1);
	}

	@DisplayName("Un aula sin secciones baja el aforo libremente")
	@Test
	void unAulaSinSeccionesBajaElAforoLibremente() {
		assertThat(servicio.actualizar(idDe("A-205"), edicion("Aula 205", 1)).aforoMaximo()).isEqualTo(1);
	}

	// ---------- Desactivar y reactivar ----------

	@DisplayName("Desactivar se permite aunque el ambiente este en uso y reactivar lo devuelve")
	@Test
	void desactivarSePermiteAunqueElAmbienteEsteEnUsoYReactivarLoDevuelve() {
		UUID id = idDe("A-101"); // lo usa 1.° A, con alumnos

		assertThat(servicio.desactivar(id).estado()).isEqualTo(EstadoAmbiente.INACTIVO);
		em.flush();
		assertThat(fila("A-101")).containsEntry("estado", "INACTIVO");

		assertThat(servicio.reactivar(id).estado()).isEqualTo(EstadoAmbiente.ACTIVO);
		em.flush();
		assertThat(fila("A-101")).containsEntry("estado", "ACTIVO");
	}

	@DisplayName("Desactivar y reactivar son idempotentes")
	@Test
	void desactivarYReactivarSonIdempotentes() {
		UUID activo = idDe("A-101");
		UUID inactivo = idDe("A-205");

		assertThatCode(() -> servicio.reactivar(activo)).doesNotThrowAnyException(); // ya estaba activo
		assertThat(servicio.reactivar(activo).estado()).isEqualTo(EstadoAmbiente.ACTIVO);
		assertThatCode(() -> servicio.desactivar(inactivo)).doesNotThrowAnyException(); // ya estaba inactivo
		assertThat(servicio.desactivar(inactivo).estado()).isEqualTo(EstadoAmbiente.INACTIVO);
	}

	@DisplayName("Desactivar no cambia los demas datos")
	@Test
	void desactivarNoCambiaLosDemasDatos() {
		var respuesta = servicio.desactivar(idDe("A-101"));

		assertThat(respuesta.nombre()).isEqualTo("Aula 101");
		assertThat(respuesta.aforoMaximo()).isEqualTo(30);
		assertThat(respuesta.recursos()).containsExactly("Pizarra acrílica", "Proyector");
	}

	@DisplayName("Un ambiente desactivado sigue en la lista y sal de las tarjetas activas")
	@Test
	void unAmbienteDesactivadoSigueEnLaListaYSalDeLasTarjetasActivas() {
		var antes = servicio.resumen();

		servicio.desactivar(idDe("A-101"));
		em.flush(); // el resumen lee con Hibernate, que ya escribió el cambio

		assertThat(servicio.listar(null, EstadoAmbiente.INACTIVO, "aula 101")).hasSize(1);
		var despues = servicio.resumen();
		assertThat(despues.activos()).isEqualTo(antes.activos() - 1);
		assertThat(despues.total()).isEqualTo(antes.total());
		assertThat(despues.aforoTotal()).isEqualTo(antes.aforoTotal() - 30);
	}
}
