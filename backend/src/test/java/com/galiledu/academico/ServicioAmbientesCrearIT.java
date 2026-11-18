package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.PruebaIntegracion;
import com.galiledu.academico.aplicacion.DtosAmbientes.SolicitudAmbiente;
import com.galiledu.academico.aplicacion.ServicioAmbientes;
import com.galiledu.academico.dominio.Ambiente;
import com.galiledu.academico.dominio.EstadoAmbiente;
import com.galiledu.academico.dominio.TipoAmbiente;
import com.galiledu.academico.infraestructura.AmbienteRepositorio;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * HU-43: {@link ServicioAmbientes#crear}, que hace hasta 3 intentos y cada uno en su propia transacción
 * ({@code REQUIRES_NEW}) y con {@code saveAndFlush}.
 * <p>
 * <b>Esta clase no usa una transacción de prueba</b> ({@code NOT_SUPPORTED} anula la de {@link PruebaIntegracion}, que
 * no se modifica). Cada alta exitosa <b>se confirma de verdad</b> y la base de pruebas es compartida, así que la
 * limpieza es parte de la prueba:
 * <ul>
 * <li>Solo se borran las filas que la prueba creó: las que no estaban en la base al empezar cada prueba. Las de la
 * semilla nunca, y se comprueba que sigan las 8 antes y después.</li>
 * <li>Al empezar también se borran los restos de una ejecución interrumpida (nombres con el prefijo
 * {@value #PREFIJO}), para que A-206 y E-001 estén libres. Ninguna otra prueba depende de eso: las que usan esos
 * códigos los calculan solo sobre los códigos de la semilla.</li>
 * </ul>
 * <p>
 * El choque de código se simula con un espía del repositorio: el primer intento recibe una lista de códigos
 * <b>vieja</b> (como si otra petición hubiera confirmado un código entre la lectura y el INSERT), calcula un código
 * que ya existe y el INSERT lo rechaza PostgreSQL de verdad con {@code ambiente_codigo_key}.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ServicioAmbientesCrearIT extends PruebaIntegracion {
	/** Todos los nombres que crea esta clase empiezan así. */
	static final String PREFIJO = "ZZ-IT ";
	private static final List<String> CODIGOS_SEMILLA = List.of("A-101", "A-102", "A-201", "A-205", "LAB-01", "LAB-02",
			"BIB-01", "AUD-01");

	@Autowired
	private ServicioAmbientes servicio;
	@Autowired
	private JdbcClient jdbc;
	@MockitoSpyBean
	private AmbienteRepositorio repositorio;

	private Set<UUID> idsAlEmpezar;

	// ---------- Limpieza ----------

	@BeforeEach
	void empezarLimpio() {
		// Restos de una ejecución interrumpida. Por nombre, que ninguna fila de la semilla empieza así.
		jdbc.sql("DELETE FROM configuracion.ambiente WHERE nombre LIKE :prefijo").param("prefijo", PREFIJO + "%")
				.update();
		exigirSemillaIntacta();
		idsAlEmpezar = Set.copyOf(jdbc.sql("SELECT id FROM configuracion.ambiente").query(UUID.class).list());
	}

	@AfterEach
	void borrarLoQueCreoLaPrueba() {
		// Todo lo que no estaba al empezar: nunca una fila de la semilla, porque todas estaban.
		jdbc.sql("DELETE FROM configuracion.ambiente WHERE id NOT IN (:ids)").param("ids", idsAlEmpezar).update();
		exigirSemillaIntacta();
	}

	private void exigirSemillaIntacta() {
		int presentes = jdbc.sql("SELECT count(*)::int FROM configuracion.ambiente WHERE codigo IN (:codigos)")
				.param("codigos", CODIGOS_SEMILLA).query(Integer.class).single();
		assertThat(presentes).as("las 8 filas de la semilla").isEqualTo(CODIGOS_SEMILLA.size());
	}

	// ---------- Apoyo ----------

	private static SolicitudAmbiente aula(String nombre, Integer aforo, String... recursos) {
		return new SolicitudAmbiente(TipoAmbiente.AULA_REGULAR, nombre, aforo, List.of(recursos));
	}

	private static SolicitudAmbiente espacio(String nombre, Integer aforo, String... recursos) {
		return new SolicitudAmbiente(TipoAmbiente.ESPACIO_ESPECIAL, nombre, aforo, List.of(recursos));
	}

	private int contarAmbientes() {
		return jdbc.sql("SELECT count(*)::int FROM configuracion.ambiente").query(Integer.class).single();
	}

	private static void rechazaCon(HttpStatus estado, String mensaje, ThrowingCallable operacion) {
		assertThatThrownBy(operacion).isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
			assertThat(e.estado()).isEqualTo(estado);
			assertThat(e.getMessage()).isEqualTo(mensaje);
		});
	}

	/** Lista vieja de aulas: calcula A-101, que ya existe en la semilla, y hace chocar el INSERT. */
	private static final List<String> CODIGOS_VIEJOS_DE_AULAS = List.of("A-100");

	/**
	 * Simula que otra petición confirmó un código entre la lectura y el INSERT: las primeras {@code intentosViejos}
	 * lecturas del prefijo devuelven {@code viejos} y las siguientes llaman al repositorio real.
	 * <p>
	 * {@code doCallRealMethod} no sirve aquí: {@code codigosConPrefijo} es un método abstracto de la interfaz y
	 * Mockito no puede llamarlo "de verdad" sobre el espía. Lo que sí se puede es reutilizar la respuesta por defecto
	 * del espía, que es la que Spring configuró para que las llamadas sin stub lleguen al repositorio real: así las
	 * lecturas siguientes ejecutan la consulta real, dentro de la transacción del intento.
	 */
	private void codigosViejosEnLosPrimerosIntentos(String prefijo, int intentosViejos, List<String> viejos) {
		Answer<?> alRepositorioReal = mockingDetails(repositorio).getMockCreationSettings().getDefaultAnswer();
		AtomicInteger lecturas = new AtomicInteger();
		doAnswer(llamada -> lecturas.incrementAndGet() <= intentosViejos ? viejos : alRepositorioReal.answer(llamada))
				.when(repositorio).codigosConPrefijo(prefijo);
	}

	// ---------- Altas ----------

	@Test
	void creaUnAulaConElSiguienteCodigoYLosDatosNormalizados() {
		var creada = servicio
				.crear(aula("  " + PREFIJO + "Aula   nueva ", 30, " Pizarra ", "", "Proyector", "proyector"));

		assertThat(creada.id()).isNotNull();
		assertThat(creada.codigo()).isEqualTo("A-206"); // el mayor de la semilla es A-205
		assertThat(creada.nombre()).isEqualTo(PREFIJO + "Aula nueva");
		assertThat(creada.tipo()).isEqualTo(TipoAmbiente.AULA_REGULAR);
		assertThat(creada.aforoMaximo()).isEqualTo(30);
		assertThat(creada.recursos()).containsExactly("Pizarra", "Proyector");
		assertThat(creada.estado()).isEqualTo(EstadoAmbiente.ACTIVO);
	}

	@Test
	void elAltaQuedaConfirmadaEnLaBase() {
		var creada = servicio.crear(aula(PREFIJO + "Confirmada", 25));

		// La prueba no tiene transacción: JdbcClient lee con su propia conexión y solo ve lo confirmado.
		var fila = jdbc.sql("""
				SELECT codigo, nombre, tipo, aforo_maximo, estado FROM configuracion.ambiente WHERE id = :id""")
				.param("id", creada.id()).query().singleRow();
		assertThat(fila).containsEntry("codigo", "A-206").containsEntry("nombre", PREFIJO + "Confirmada")
				.containsEntry("tipo", "AULA_REGULAR").containsEntry("aforo_maximo", 25)
				.containsEntry("estado", "ACTIVO");
	}

	@Test
	void cadaTipoLlevaSuPropioPrefijoYCorrelativo() {
		assertThat(servicio.crear(espacio(PREFIJO + "Auditorio nuevo", 200)).codigo()).isEqualTo("E-001");
		assertThat(servicio.crear(aula(PREFIJO + "Aula 1", 30)).codigo()).isEqualTo("A-206");
		assertThat(servicio.crear(espacio(PREFIJO + "Taller", 15)).codigo()).isEqualTo("E-002");
		assertThat(servicio.crear(aula(PREFIJO + "Aula 2", 30)).codigo()).isEqualTo("A-207");
	}

	@Test
	void unEspacioEspecialAdmiteMasDe45YUnAulaRegularSoloHasta45() {
		assertThat(servicio.crear(espacio(PREFIJO + "Losa", 180)).aforoMaximo()).isEqualTo(180);
		assertThat(servicio.crear(aula(PREFIJO + "Aula de 45", 45)).aforoMaximo()).isEqualTo(45);
	}

	@Test
	void elCodigoNoDependeDeLoQueEscribaElUsuario() {
		// El contrato no tiene campo "codigo": lo único que decide el código es el tipo y lo que ya existe.
		assertThat(SolicitudAmbiente.class.getRecordComponents()).extracting(c -> c.getName())
				.containsExactly("tipo", "nombre", "aforoMaximo", "recursos");
	}

	// ---------- Nombre repetido ----------

	@Test
	void rechazaUnNombreRepetidoSinDistinguirMayusculasNiEspaciosSobrantes() {
		int antes = contarAmbientes();

		rechazaCon(HttpStatus.CONFLICT, "Ya existe un ambiente llamado \"aula 101\".",
				() -> servicio.crear(aula("aula 101", 30)));
		// El mensaje cita el nombre que escribió el usuario, ya sin espacios sobrantes.
		rechazaCon(HttpStatus.CONFLICT, "Ya existe un ambiente llamado \"AULA 101\".",
				() -> servicio.crear(aula("  AULA    101  ", 30)));
		assertThat(contarAmbientes()).isEqualTo(antes);
	}

	@Test
	void elNombreRepetidoIncluyeLosInactivosYLosDeOtroTipo() {
		int antes = contarAmbientes();

		rechazaCon(HttpStatus.CONFLICT, "Ya existe un ambiente llamado \"AULA 205\".", // A-205 está inactiva
				() -> servicio.crear(aula("AULA 205", 30)));
		rechazaCon(HttpStatus.CONFLICT, "Ya existe un ambiente llamado \"Auditorio\".", // AUD-01 es espacio
				() -> servicio.crear(aula("Auditorio", 30)));
		assertThat(contarAmbientes()).isEqualTo(antes);
	}

	@Test
	void unNombreRepetidoNoSeReintenta() {
		rechazaCon(HttpStatus.CONFLICT, "Ya existe un ambiente llamado \"Biblioteca\".",
				() -> servicio.crear(espacio("Biblioteca", 30)));

		verify(repositorio, times(1)).existsByNombreIgnoreCase("Biblioteca");
		verify(repositorio, times(0)).saveAndFlush(any());
	}

	@Test
	void tambienRechazaElNombreDeUnAmbienteCreadoAntes() {
		servicio.crear(aula(PREFIJO + "Única", 30));

		rechazaCon(HttpStatus.CONFLICT, "Ya existe un ambiente llamado \"" + PREFIJO + "ÚNICA\".",
				() -> servicio.crear(espacio(PREFIJO + "ÚNICA", 30)));
	}

	// ---------- Datos inválidos (nunca llegan a la base) ----------

	@Test
	void losDatosInvalidosDan400SinAbrirTransaccionNiCrearNada() {
		int antes = contarAmbientes();

		rechazaCon(HttpStatus.BAD_REQUEST, "Elige el tipo de ambiente.",
				() -> servicio.crear(new SolicitudAmbiente(null, PREFIJO + "x", 30, List.of())));
		rechazaCon(HttpStatus.BAD_REQUEST, "Ingresa el nombre del ambiente.", () -> servicio.crear(aula("   ", 30)));
		rechazaCon(HttpStatus.BAD_REQUEST, "Ingresa el nombre del ambiente.", () -> servicio.crear(aula(null, 30)));
		rechazaCon(HttpStatus.BAD_REQUEST, "El nombre admite como máximo 80 caracteres.",
				() -> servicio.crear(aula("a".repeat(81), 30)));
		rechazaCon(HttpStatus.BAD_REQUEST, "Ingresa el aforo máximo.", () -> servicio.crear(aula(PREFIJO + "x", null)));
		rechazaCon(HttpStatus.BAD_REQUEST, "El aforo debe ser mayor que 0.",
				() -> servicio.crear(aula(PREFIJO + "x", 0)));
		rechazaCon(HttpStatus.BAD_REQUEST, "El aforo debe ser mayor que 0.",
				() -> servicio.crear(aula(PREFIJO + "x", -3)));
		rechazaCon(HttpStatus.BAD_REQUEST, "Un aula regular admite como máximo 45.",
				() -> servicio.crear(aula(PREFIJO + "x", 46)));
		rechazaCon(HttpStatus.BAD_REQUEST, "Cada recurso admite como máximo 60 caracteres.",
				() -> servicio.crear(aula(PREFIJO + "x", 30, "r".repeat(61))));
		rechazaCon(HttpStatus.BAD_REQUEST, "El texto contiene caracteres no válidos.",
				() -> servicio.crear(aula(PREFIJO + "x\u0000", 30)));

		assertThat(contarAmbientes()).isEqualTo(antes);
		verify(repositorio, times(0)).saveAndFlush(any()); // ni siquiera se intentó guardar
		verify(repositorio, times(0)).codigosConPrefijo(any());
	}

	// ---------- Choque de código y reintento ----------

	@Test
	void siOtraPeticionSeLlevaElCodigoReintentaConUnaTransaccionNuevaYObtieneElSiguiente() {
		// 1.er intento: lista vieja -> A-101 (ya existe) -> el INSERT falla de verdad -> se deshace ese intento.
		// 2.º intento: lista real -> A-206.
		codigosViejosEnLosPrimerosIntentos("A", 1, CODIGOS_VIEJOS_DE_AULAS);

		var creada = servicio.crear(aula(PREFIJO + "Con choque", 30, "Pizarra"));

		assertThat(creada.codigo()).isEqualTo("A-206");
		verify(repositorio, times(2)).codigosConPrefijo("A");
		verify(repositorio, times(2)).saveAndFlush(any(Ambiente.class));
		// Del intento fallido no quedó nada: hay una sola fila nueva, y es la del segundo intento.
		var filas = jdbc.sql("SELECT codigo FROM configuracion.ambiente WHERE nombre = :nombre")
				.param("nombre", PREFIJO + "Con choque").query(String.class).list();
		assertThat(filas).containsExactly("A-206");
		assertThat(contarAmbientes()).isEqualTo(CODIGOS_SEMILLA.size() + 1);
	}

	@Test
	void tambienReintentaConUnEspacioEspecial() {
		servicio.crear(espacio(PREFIJO + "Primero", 20)); // E-001, ya confirmado
		// Lista vieja sin ningún E: calcula E-001, que ya existe.
		codigosViejosEnLosPrimerosIntentos("E", 1, List.of());

		var creada = servicio.crear(espacio(PREFIJO + "Segundo", 20));

		assertThat(creada.codigo()).isEqualTo("E-002");
	}

	@Test
	void conTresChoquesSeguidosDa409SinCrearNada() {
		int antes = contarAmbientes();
		codigosViejosEnLosPrimerosIntentos("A", Integer.MAX_VALUE, CODIGOS_VIEJOS_DE_AULAS); // siempre vieja

		rechazaCon(HttpStatus.CONFLICT, "No pudimos registrar el ambiente. Inténtalo de nuevo.",
				() -> servicio.crear(aula(PREFIJO + "Sin suerte", 30)));

		verify(repositorio, times(3)).saveAndFlush(any(Ambiente.class)); // 3 intentos, ni uno más
		assertThat(contarAmbientes()).isEqualTo(antes);
	}

	@Test
	void conDosChoquesYUnTerceroLimpioAlcanzaElTercerIntento() {
		codigosViejosEnLosPrimerosIntentos("A", 2, CODIGOS_VIEJOS_DE_AULAS);

		var creada = servicio.crear(aula(PREFIJO + "Al tercero", 30));

		assertThat(creada.codigo()).isEqualTo("A-206");
		verify(repositorio, times(3)).saveAndFlush(any(Ambiente.class));
	}

	@Test
	void otraViolacionDeLaBaseDa409SinReintentar() {
		int antes = contarAmbientes();
		var otraRestriccion = new DataIntegrityViolationException("no se pudo guardar",
				new RuntimeException("ERROR: new row violates check constraint \"ck_ambiente_aforo\""));
		doThrow(otraRestriccion).when(repositorio).saveAndFlush(any(Ambiente.class));

		rechazaCon(HttpStatus.CONFLICT, "No pudimos registrar el ambiente. Inténtalo de nuevo.",
				() -> servicio.crear(aula(PREFIJO + "Otra restricción", 30)));

		verify(repositorio, times(1)).saveAndFlush(any(Ambiente.class)); // no es el código: un solo intento
		assertThat(contarAmbientes()).isEqualTo(antes);
	}

	@Test
	void unaExcepcionQueNoEsDeLaBaseNoSeTraga() {
		doThrow(new IllegalStateException("falla inesperada")).when(repositorio).saveAndFlush(any(Ambiente.class));

		assertThatThrownBy(() -> servicio.crear(aula(PREFIJO + "Inesperada", 30)))
				.isInstanceOf(IllegalStateException.class).hasMessage("falla inesperada");
		verify(repositorio, times(1)).saveAndFlush(any(Ambiente.class));
	}
}
