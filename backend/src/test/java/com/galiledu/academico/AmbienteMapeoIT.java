package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.academico.dominio.Ambiente;
import com.galiledu.academico.dominio.CodigoAmbiente;
import com.galiledu.academico.dominio.EstadoAmbiente;
import com.galiledu.academico.dominio.TipoAmbiente;

/**
 * Mapeo de {@link Ambiente} sobre configuracion.ambiente (V6): sobre todo la columna {@code recursos VARCHAR(60)[]}.
 * Va con la capa de dominio para confirmar que la entidad lee y escribe la tabla real antes de construir encima.
 */
class AmbienteMapeoIT extends PruebaIntegracion {
	@Autowired
	private EntityManager em;
	@Autowired
	private JdbcClient jdbc;

	private Ambiente recargar(Ambiente ambiente) {
		em.flush();
		em.clear(); // fuerza a leer de la BD y no de la memoria de la transacción
		return em.find(Ambiente.class, ambiente.getId());
	}

	@DisplayName("Guarda y lee los recursos como arreglo de postgre sql")
	@Test
	void guardaYLeeLosRecursosComoArregloDePostgreSql() {
		var nuevo = Ambiente.crear(TipoAmbiente.AULA_REGULAR, new CodigoAmbiente("A-901"), "Aula de prueba", 30,
				List.of("Pizarra acrílica", "Proyector", "Estanterías con ñ"));
		em.persist(nuevo);

		var leido = recargar(nuevo);

		assertThat(leido).isNotSameAs(nuevo);
		assertThat(leido.getRecursos()).containsExactly("Pizarra acrílica", "Proyector", "Estanterías con ñ");
		// Y en la BD es un arreglo real de PostgreSQL, no un texto serializado.
		var fila = jdbc.sql("""
				SELECT cardinality(recursos) AS cantidad, recursos[1] AS primero, pg_typeof(recursos)::text AS tipo
				FROM configuracion.ambiente WHERE codigo = 'A-901'""").query().singleRow();
		assertThat(fila).containsEntry("cantidad", 3).containsEntry("primero", "Pizarra acrílica")
				.containsEntry("tipo", "character varying[]");
	}

	@DisplayName("Sin recursos guarda un arreglo vacio no nulo")
	@Test
	void sinRecursosGuardaUnArregloVacioNoNulo() {
		var nuevo = Ambiente.crear(TipoAmbiente.ESPACIO_ESPECIAL, new CodigoAmbiente("E-901"), "Patio cubierto", 100,
				List.of());
		em.persist(nuevo);

		var leido = recargar(nuevo);

		assertThat(leido.getRecursos()).isEmpty();
		var cantidad = jdbc.sql("SELECT cardinality(recursos) FROM configuracion.ambiente WHERE codigo = 'E-901'")
				.query(Integer.class).single();
		assertThat(cantidad).isZero();
	}

	@DisplayName("Lee los ambientes de los datos de prueba")
	@Test
	void leeLosAmbientesDeLosDatosDePrueba() {
		var a101 = em.createQuery("SELECT a FROM Ambiente a WHERE a.codigo = 'A-101'", Ambiente.class)
				.getSingleResult();
		assertThat(a101.getNombre()).isEqualTo("Aula 101");
		assertThat(a101.getTipo()).isEqualTo(TipoAmbiente.AULA_REGULAR);
		assertThat(a101.getAforoMaximo()).isEqualTo(30);
		assertThat(a101.getRecursos()).containsExactly("Pizarra acrílica", "Proyector");
		assertThat(a101.getEstado()).isEqualTo(EstadoAmbiente.ACTIVO);

		var a205 = em.createQuery("SELECT a FROM Ambiente a WHERE a.codigo = 'A-205'", Ambiente.class)
				.getSingleResult();
		assertThat(a205.getEstado()).isEqualTo(EstadoAmbiente.INACTIVO);

		var auditorio = em.createQuery("SELECT a FROM Ambiente a WHERE a.codigo = 'AUD-01'", Ambiente.class)
				.getSingleResult();
		assertThat(auditorio.getTipo()).isEqualTo(TipoAmbiente.ESPACIO_ESPECIAL);
		assertThat(auditorio.getAforoMaximo()).isEqualTo(180);
		assertThat(auditorio.getRecursos()).containsExactly("Sonido", "Escenario");
	}

	@DisplayName("Editar y guardar actualiza el arreglo y no toca código ni tipo")
	@Test
	void editarYGuardarActualizaElArregloYNoTocaCodigoNiTipo() {
		var existente = em.createQuery("SELECT a FROM Ambiente a WHERE a.codigo = 'A-102'", Ambiente.class)
				.getSingleResult();
		var id = existente.getId();

		existente.editar("Aula 102 renovada", 35, List.of("Pizarra digital"));
		existente.desactivar();
		var leido = recargar(existente);

		assertThat(leido.getId()).isEqualTo(id);
		assertThat(leido.getCodigo()).isEqualTo("A-102");
		assertThat(leido.getTipo()).isEqualTo(TipoAmbiente.AULA_REGULAR);
		assertThat(leido.getNombre()).isEqualTo("Aula 102 renovada");
		assertThat(leido.getAforoMaximo()).isEqualTo(35);
		assertThat(leido.getRecursos()).containsExactly("Pizarra digital");
		assertThat(leido.getEstado()).isEqualTo(EstadoAmbiente.INACTIVO);
	}

	@DisplayName("Llena el id y las fechas al guardar")
	@Test
	void llenaElIdYLasFechasAlGuardar() {
		var nuevo = Ambiente.crear(TipoAmbiente.AULA_REGULAR, new CodigoAmbiente("A-902"), "Aula con fechas", 20,
				List.of());
		em.persist(nuevo);
		em.flush();

		assertThat(nuevo.getId()).isNotNull();
		var fila = jdbc.sql("""
				SELECT creado_en IS NOT NULL AS creado, actualizado_en IS NOT NULL AS actualizado, estado
				FROM configuracion.ambiente WHERE codigo = 'A-902'""").query().singleRow();
		assertThat(fila).containsEntry("creado", true).containsEntry("actualizado", true)
				.containsEntry("estado", "ACTIVO");
	}
}
