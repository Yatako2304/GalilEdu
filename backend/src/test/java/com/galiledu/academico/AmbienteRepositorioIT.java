package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.academico.dominio.Ambiente;
import com.galiledu.academico.dominio.CodigoAmbiente;
import com.galiledu.academico.dominio.TipoAmbiente;
import com.galiledu.academico.infraestructura.AmbienteRepositorio;

/**
 * Consultas de {@link AmbienteRepositorio} sobre configuracion.ambiente (V6) con los datos de prueba (A-101, A-102,
 * A-201, A-205 inactiva, LAB-01, LAB-02, BIB-01 y AUD-01).
 * <p>
 * Tres niveles: lo que está <b>en memoria</b> (una entidad con {@code persist} que Hibernate aún no escribió), lo que
 * está <b>en la transacción</b> (ya se hizo INSERT, pero sin commit) y lo <b>confirmado</b> (commit, que la prueba
 * nunca hace). Ver {@link #memoriaTransaccionYConfirmado()}.
 */
class AmbienteRepositorioIT extends PruebaIntegracion {
	private static final List<String> CODIGOS_SEMILLA = List.of("A-101", "A-102", "A-201", "A-205", "LAB-01", "LAB-02",
			"BIB-01", "AUD-01");

	@Autowired
	private AmbienteRepositorio repositorio;
	@Autowired
	private EntityManager em;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private DataSource dataSource;

	private static Ambiente aula(String codigo, String nombre) {
		return Ambiente.crear(TipoAmbiente.AULA_REGULAR, new CodigoAmbiente(codigo), nombre, 30, List.of());
	}

	// ---------- findAllByOrderByNombreAsc ----------

	@Test
	void listaTodosLosAmbientesActivosEInactivosPorNombre() {
		var nombres = repositorio.findAllByOrderByNombreAsc().stream().map(Ambiente::getNombre).toList();

		assertThat(nombres).hasSizeGreaterThanOrEqualTo(8).contains("Aula 205", "Auditorio", "Biblioteca");
		assertThat(nombres).containsSubsequence("Aula 101", "Aula 102", "Aula 201", "Aula 205");
	}

	// ---------- Nombre repetido ----------

	@Test
	void elNombreSeComparaSinDistinguirMayusculas() {
		assertThat(repositorio.existsByNombreIgnoreCase("Aula 101")).isTrue();
		assertThat(repositorio.existsByNombreIgnoreCase("aula 101")).isTrue();
		assertThat(repositorio.existsByNombreIgnoreCase("AULA 101")).isTrue();
		assertThat(repositorio.existsByNombreIgnoreCase("Aula 103")).isFalse();
	}

	@Test
	void elNombreRepetidoIncluyeLosAmbientesInactivos() {
		// A-205 ("Aula 205") está INACTIVO y se puede reactivar: su nombre sigue ocupado.
		assertThat(repositorio.existsByNombreIgnoreCase("aula 205")).isTrue();
	}

	@Test
	void conTildesSoloSeIgualanLasMayusculasQueLaBaseSabeConvertir() {
		// La tilde en sí debe coincidir: "Computo" y "Cómputo" son nombres distintos (decisión: sin normalizar tildes).
		assertThat(repositorio.existsByNombreIgnoreCase("laboratorio de cómputo")).isTrue();
		assertThat(repositorio.existsByNombreIgnoreCase("Laboratorio de computo")).isFalse();
	}

	@Test
	void ignorarAlPropioAmbienteDejaEditarSinCambiarElNombre() {
		Ambiente a101 = repositorio.findAllByOrderByNombreAsc().stream().filter(a -> a.getCodigo().equals("A-101"))
				.findFirst().orElseThrow();
		Ambiente a102 = repositorio.findAllByOrderByNombreAsc().stream().filter(a -> a.getCodigo().equals("A-102"))
				.findFirst().orElseThrow();

		assertThat(repositorio.existsByNombreIgnoreCaseAndIdNot("aula 101", a101.getId())).isFalse();
		assertThat(repositorio.existsByNombreIgnoreCaseAndIdNot("aula 101", a102.getId())).isTrue();
		assertThat(repositorio.existsByNombreIgnoreCaseAndIdNot("Aula 103", a102.getId())).isFalse();
	}

	@Test
	void laBaseNoImpideDosAmbientesConElMismoNombre() {
		// Documenta la decisión 2: la unicidad del nombre es del servicio. Si se agrega el UNIQUE INDEX
		// (lower(btrim(nombre))), esta prueba pasa a exigir el rechazo por esa restricción.
		repositorio.saveAndFlush(aula("A-901", "Aula repetida"));

		assertThatCode(() -> repositorio.saveAndFlush(aula("A-902", "aula repetida"))).doesNotThrowAnyException();
	}

	// ---------- codigosConPrefijo ----------

	@Test
	void losCodigosDelPrefijoANoIncluyenLosQueEmpiezanConOtrasLetras() {
		var codigos = repositorio.codigosConPrefijo("A");

		assertThat(codigos).contains("A-101", "A-102", "A-201", "A-205").doesNotContain("AUD-01", "LAB-01", "BIB-01");
	}

	@Test
	void conLosCodigosDeLaSemillaElSiguienteDeAulaEsA206YElDeEspacioEspecialE001() {
		// Solo se miran los códigos de la semilla: esta prueba no depende de que A-206 o E-001 estén libres en la
		// base (ServicioAmbientesCrearIT los usa, confirma y limpia).
		var delAula = repositorio.codigosConPrefijo("A").stream().filter(CODIGOS_SEMILLA::contains).toList();
		var delEspacio = repositorio.codigosConPrefijo("E").stream().filter(CODIGOS_SEMILLA::contains).toList();

		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, delAula).valor()).isEqualTo("A-206");
		assertThat(delEspacio).isEmpty();
		assertThat(CodigoAmbiente.siguiente(TipoAmbiente.ESPACIO_ESPECIAL, delEspacio).valor()).isEqualTo("E-001");
	}

	@Test
	void unCodigoNuevoEntraEnLaSiguienteConsultaDeLaMismaTransaccion() {
		repositorio.saveAndFlush(aula("A-300", "Aula 300"));

		var siguiente = CodigoAmbiente.siguiente(TipoAmbiente.AULA_REGULAR, repositorio.codigosConPrefijo("A"));

		assertThat(siguiente.valor()).isEqualTo("A-301");
	}

	// ---------- Código repetido ----------

	@Test
	void laBaseRechazaUnCodigoRepetidoConLaRestriccionDelCodigo() {
		repositorio.saveAndFlush(aula("A-901", "Aula nueva"));

		// El servicio reintenta solo cuando falla esta restricción (ambiente_codigo_key) y no otra.
		rechazaPor("ambiente_codigo_key", () -> repositorio.saveAndFlush(aula("A-901", "Otra aula")));
	}

	@Test
	void guardarNoEscribeEnLaBaseHastaElFlush() {
		// persist solo deja la entidad en memoria: el choque de código aparece en el flush, no en persist. Por eso
		// el servicio usa saveAndFlush: ahí se entera del choque y puede reintentar.
		repositorio.save(aula("A-101", "Aula que choca con la semilla"));

		rechazaPor("ambiente_codigo_key", () -> repositorio.flush());
	}

	// ---------- Memoria, transacción y confirmado ----------

	@Test
	void memoriaTransaccionYConfirmado() throws SQLException {
		// 1) En memoria: persist deja la entidad en el contexto de Hibernate. Todavía no hay INSERT.
		var nuevo = aula("A-950", "Aula en tres niveles");
		em.persist(nuevo);
		assertThat(contarConJdbc()).as("JDBC directo no ve lo que sigue solo en memoria").isZero();

		// Una consulta de Hibernate sí lo ve: antes de ejecutarse, escribe lo pendiente (flush automático). Por eso
		// existsByNombreIgnoreCase ve un ambiente recién guardado en la misma transacción.
		assertThat(repositorio.existsByNombreIgnoreCase("aula en tres niveles")).isTrue();

		// 2) En la transacción: ya se hizo el INSERT, sin commit. La misma conexión lo ve...
		assertThat(contarConJdbc()).isEqualTo(1);
		// 3) ...pero no está confirmado: otra conexión no lo ve, y no lo verá nunca porque la prueba se deshace.
		assertThat(contarEnOtraConexion()).isZero();
	}

	private int contarConJdbc() {
		return jdbc.sql("SELECT count(*)::int FROM configuracion.ambiente WHERE codigo = 'A-950'")
				.query(Integer.class).single();
	}

	private long contarEnOtraConexion() throws SQLException {
		try (Connection otra = dataSource.getConnection();
				PreparedStatement consulta = otra
						.prepareStatement("SELECT count(*) FROM configuracion.ambiente WHERE codigo = 'A-950'");
				ResultSet resultado = consulta.executeQuery()) {
			resultado.next();
			return resultado.getLong(1);
		}
	}
}
