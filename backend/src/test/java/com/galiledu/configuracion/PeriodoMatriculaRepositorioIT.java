package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.dominio.DatosDocumento;
import com.galiledu.configuracion.dominio.DocumentoRequerido;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoPeriodoMatricula;
import com.galiledu.configuracion.dominio.PeriodoMatricula;
import com.galiledu.configuracion.infraestructura.ConsultasPeriodoMatricula;
import com.galiledu.configuracion.infraestructura.ConsultasPeriodoMatricula.AnioDisponible;
import com.galiledu.configuracion.infraestructura.PeriodoMatriculaRepositorio;

/**
 * HU-23: el mapeo JPA de matricula.periodo_matricula y matricula.documento_requerido y las consultas de SQL, con los
 * datos de prueba (2027 HABILITADO del 15/09 al 31/10/2026 y 2026 CERRADO, cada uno con 4 documentos y 10 secciones).
 */
class PeriodoMatriculaRepositorioIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");
	private static final UUID MATRICULA_2026 = UUID.fromString("50000000-0000-0000-0000-000000000001");
	private static final UUID MATRICULA_2027 = UUID.fromString("50000000-0000-0000-0000-000000000002");
	private static final Instant AHORA = Instant.parse("2026-10-22T14:00:00Z");

	@Autowired
	private PeriodoMatriculaRepositorio periodos;
	@Autowired
	private ConsultasPeriodoMatricula consultas;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;

	private UUID anioPorSql(int anio, String estado) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (:id, :anio, :inicio, :fin, 'TRIMESTRAL', :estado)""")
				.param("id", id).param("anio", anio).param("inicio", java.time.LocalDate.of(anio, 3, 1))
				.param("fin", java.time.LocalDate.of(anio, 12, 15)).param("estado", estado).update();
		return id;
	}

	private static DatosDocumento doc(String nombre) {
		return new DatosDocumento(null, nombre, null);
	}

	private PeriodoMatricula periodoNuevo(UUID anio, DatosDocumento... documentos) {
		return PeriodoMatricula.habilitar(anio, LocalDateTime.of(2028, 1, 10, 10, 0),
				LocalDateTime.of(2028, 2, 15, 10, 0),
				List.of(documentos), AHORA);
	}

	private PeriodoMatricula periodo(UUID anio) {
		return periodos.findByAnioEscolarId(anio).orElseThrow();
	}

	private UUID primerDocumento(UUID anio) {
		return periodo(anio).getDocumentos().getFirst().getId();
	}

	// --- Mapeo ---

	@DisplayName("Lee el periodo2027 habilitado con sus cuatro documentos")
	@Test
	void leeElPeriodo2027HabilitadoConSusCuatroDocumentos() {
		PeriodoMatricula p = periodo(ANIO_2027);

		assertThat(p.getEstado()).isEqualTo(EstadoPeriodoMatricula.HABILITADO);
		assertThat(p.getInicio()).isEqualTo(Instant.parse("2026-09-15T13:00:00Z")); // 08:00 en Lima
		assertThat(p.getCierre()).isEqualTo(Instant.parse("2026-11-01T04:59:00Z")); // 31/10 23:59 en Lima
		assertThat(p.isBloquearPorMorosidad()).isTrue();
		assertThat(p.isPermitirExtemporanea()).isTrue();
		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getNombre).containsExactly(
				"Ficha de matrícula firmada", "DNI del estudiante", "DNI del apoderado",
				"Declaración jurada del apoderado");
		assertThat(p.getDocumentos()).extracting(DocumentoRequerido::getOrden).containsExactly(1, 2, 3, 4);
		assertThat(p.getDocumentos().getFirst().getDescripcion()).isEqualTo("Descarga la ficha, fírmala y súbela.");
		assertThat(p.getDocumentos().getLast().getDescripcion()).isNull();
		assertThat(p.getDocumentos()).noneMatch(DocumentoRequerido::tienePlantilla);
	}

	@DisplayName("El periodo2026 está guardado como cerrado")
	@Test
	void elPeriodo2026EstaGuardadoComoCerrado() {
		assertThat(periodo(ANIO_2026).getEstado()).isEqualTo(EstadoPeriodoMatricula.CERRADO);
		assertThat(periodos.findByEstado(EstadoPeriodoMatricula.HABILITADO))
				.extracting(PeriodoMatricula::getAnioEscolarId)
				.containsExactly(ANIO_2027);
		assertThat(periodos.findAllByOrderByInicioDesc()).extracting(PeriodoMatricula::getAnioEscolarId)
				.containsExactly(ANIO_2027, ANIO_2026);
	}

	@DisplayName("Guarda un período con sus documentos y lo edita sin chocar con los unique")
	@Test
	void guardaUnPeriodoConSusDocumentosYLoEditaSinChocarConLosUnique() {
		UUID anio = anioPorSql(2028, "PLANIFICADO");
		PeriodoMatricula nuevo = periodos.saveAndFlush(periodoNuevo(anio, doc("Ficha"), doc("DNI")));
		assertThat(jdbc.sql("SELECT count(*) FROM matricula.documento_requerido WHERE periodo_matricula_id = ?")
				.param(nuevo.getId()).query(Integer.class).single()).isEqualTo(2);
		assertThat(jdbc.sql("SELECT estado FROM matricula.periodo_matricula WHERE id = ?").param(nuevo.getId())
				.query(String.class).single()).isEqualTo("HABILITADO");

		// se retira el "DNI" y se agrega otro con el mismo nombre: sin flush entre medias, Hibernate insertaría antes de
		// borrar y chocaría con uq_documento_requerido_nombre
		UUID ficha = nuevo.getDocumentos().getFirst().getId();
		List<DatosDocumento> lista = List.of(new DatosDocumento(ficha, "Ficha", "Con firma"), doc("DNI"));
		nuevo.retirarDocumentosAusentes(lista);
		periodos.flush();
		nuevo.editar(LocalDateTime.of(2028, 1, 12, 10, 0), LocalDateTime.of(2028, 2, 20, 10, 0), lista, AHORA);
		periodos.saveAndFlush(nuevo);

		entityManager.clear();
		PeriodoMatricula leido = periodo(anio);
		assertThat(leido.getDocumentos()).extracting(DocumentoRequerido::getNombre).containsExactly("Ficha", "DNI");
		assertThat(leido.getDocumentos().getFirst().getId()).isEqualTo(ficha);
		assertThat(leido.getDocumentos().getFirst().getDescripcion()).isEqualTo("Con firma");
		assertThat(leido.getCierre()).isEqualTo(Instant.parse("2028-02-20T15:00:00Z"));
	}

	@DisplayName("Un período sin documentos se guarda y se elimina con sus documentos")
	@Test
	void unPeriodoSinDocumentosSeGuardaYSeEliminaConSusDocumentos() {
		UUID anio = anioPorSql(2028, "PLANIFICADO");
		PeriodoMatricula nuevo = periodos.saveAndFlush(periodoNuevo(anio, doc("Ficha")));

		periodos.delete(nuevo);
		periodos.flush();

		assertThat(periodos.findByAnioEscolarId(anio)).isEmpty();
		assertThat(jdbc.sql("SELECT count(*) FROM matricula.documento_requerido WHERE periodo_matricula_id = ?")
				.param(nuevo.getId()).query(Integer.class).single()).isZero();
	}

	// --- Plantillas ---

	@DisplayName("Guarda lee reemplaza y quita la plantilla de un documento")
	@Test
	void guardaLeeReemplazaYQuitaLaPlantillaDeUnDocumento() {
		UUID documento = primerDocumento(ANIO_2027);
		byte[] contenido = "nombre,dni\nAna,1\n".getBytes();

		assertThat(consultas.plantilla(documento)).isEmpty();
		assertThat(consultas.guardarPlantilla(documento, "ficha.pdf", "application/pdf", contenido)).isTrue();

		var guardada = consultas.plantilla(documento).orElseThrow();
		assertThat(guardada.nombreArchivo()).isEqualTo("ficha.pdf");
		assertThat(guardada.tipoMime()).isEqualTo("application/pdf");
		assertThat(guardada.contenido()).isEqualTo(contenido);
		entityManager.clear();
		DocumentoRequerido leido = periodo(ANIO_2027).getDocumentos().getFirst();
		assertThat(leido.tienePlantilla()).isTrue();
		assertThat(leido.getPlantillaNombreArchivo()).isEqualTo("ficha.pdf");
		assertThat(leido.getPlantillaTipoMime()).isEqualTo("application/pdf");

		assertThat(consultas.guardarPlantilla(documento, "ficha.xls", "application/vnd.ms-excel", new byte[]{1, 2, 3}))
				.isTrue();
		assertThat(consultas.plantilla(documento).orElseThrow().contenido()).containsExactly(1, 2, 3);

		consultas.quitarPlantilla(documento);
		assertThat(consultas.plantilla(documento)).isEmpty();
		entityManager.clear();
		assertThat(periodo(ANIO_2027).getDocumentos().getFirst().tienePlantilla()).isFalse();
	}

	@DisplayName("Guardar la plantilla de un documento que no existe devuelve false")
	@Test
	void guardarLaPlantillaDeUnDocumentoQueNoExisteDevuelveFalse() {
		assertThat(consultas.guardarPlantilla(UUID.randomUUID(), "ficha.pdf", "application/pdf", new byte[]{1}))
				.isFalse();
	}

	@DisplayName("Editar un período no borra la plantilla de los documentos que se conservan")
	@Test
	void editarUnPeriodoNoBorraLaPlantillaDeLosDocumentosQueSeConservan() {
		UUID anio = anioPorSql(2028, "PLANIFICADO");
		PeriodoMatricula nuevo = periodos.saveAndFlush(periodoNuevo(anio, doc("Ficha"), doc("DNI")));
		UUID ficha = nuevo.getDocumentos().getFirst().getId();
		consultas.guardarPlantilla(ficha, "ficha.pdf", "application/pdf", new byte[]{1, 2});
		entityManager.clear();

		PeriodoMatricula p = periodo(anio);
		List<DatosDocumento> lista = List.of(new DatosDocumento(ficha, "Ficha de matrícula", null));
		p.retirarDocumentosAusentes(lista);
		periodos.flush();
		p.editar(LocalDateTime.of(2028, 1, 12, 10, 0), LocalDateTime.of(2028, 2, 20, 10, 0), lista, AHORA);
		periodos.saveAndFlush(p);

		assertThat(consultas.plantilla(ficha).orElseThrow().contenido()).containsExactly(1, 2);
		entityManager.clear();
		DocumentoRequerido leido = periodo(anio).getDocumentos().getFirst();
		assertThat(leido.getNombre()).isEqualTo("Ficha de matrícula");
		assertThat(leido.tienePlantilla()).isTrue();
		assertThat(periodo(anio).getDocumentos()).hasSize(1);
	}

	// --- Consultas ---

	@DisplayName("Cuenta las matrículas que ocupan vacante de cada año")
	@Test
	void cuentaLasMatriculasQueOcupanVacanteDeCadaAnio() {
		assertThat(consultas.matriculasDelAnio(ANIO_2026)).isEqualTo(contar(ANIO_2026));
		assertThat(consultas.matriculasDelAnio(ANIO_2027)).isEqualTo(1);
		assertThat(consultas.matriculasPorAnio()).containsEntry(ANIO_2027, 1).containsKey(ANIO_2026);
		assertThat(consultas.matriculasDelAnio(UUID.randomUUID())).isZero();
	}

	private int contar(UUID anio) {
		return jdbc.sql(
				"SELECT count(*) FROM matricula.matricula WHERE anio_escolar_id = ? AND estado NOT IN ('RETIRADO', 'ANULADA')")
				.param(anio).query(Integer.class).single();
	}

	@DisplayName("Las matrículas anuladas no ocupan vacante pero si impiden eliminar")
	@Test
	void lasMatriculasAnuladasNoOcupanVacantePeroSiImpidenEliminar() {
		jdbc.sql("UPDATE matricula.matricula SET estado = 'ANULADA' WHERE id = ?").param(MATRICULA_2027).update();

		assertThat(consultas.matriculasDelAnio(ANIO_2027)).isZero();
		assertThat(consultas.matriculasPorAnio()).doesNotContainKey(ANIO_2027);
		assertThat(consultas.tieneMatriculas(ANIO_2027)).isTrue();
	}

	@DisplayName("Un año sin matrículas no las tiene")
	@Test
	void unAnioSinMatriculasNoLasTiene() {
		assertThat(consultas.tieneMatriculas(anioPorSql(2028, "PLANIFICADO"))).isFalse();
		assertThat(consultas.tieneMatriculas(ANIO_2026)).isTrue();
	}

	@DisplayName("Detecta las entregas de documentos de un período")
	@Test
	void detectaLasEntregasDeDocumentosDeUnPeriodo() {
		PeriodoMatricula p2026 = periodo(ANIO_2026);
		PeriodoMatricula p2027 = periodo(ANIO_2027);
		assertThat(consultas.tieneEntregas(p2026.getId())).isFalse();
		assertThat(consultas.documentosConEntregas(p2026.getId())).isEmpty();

		UUID documento = p2026.getDocumentos().getFirst().getId();
		jdbc.sql("""
				INSERT INTO matricula.documento_matricula (matricula_id, documento_requerido_id, nombre_archivo,
				                                           tipo_mime, tamano_bytes, contenido)
				VALUES (?, ?, 'ficha.pdf', 'application/pdf', 3, '\\x010203'::bytea)""")
				.params(MATRICULA_2026, documento).update();

		assertThat(consultas.tieneEntregas(p2026.getId())).isTrue();
		assertThat(consultas.documentosConEntregas(p2026.getId())).containsExactly(documento);
		assertThat(consultas.tieneEntregas(p2027.getId())).isFalse();
	}

	@DisplayName("Lista los años planificados o activos que aún no tienen período")
	@Test
	void listaLosAniosPlanificadosOActivosQueAunNoTienenPeriodo() {
		assertThat(consultas.aniosSinPeriodo()).isEmpty(); // 2026 y 2027 ya tienen el suyo
		UUID id2028 = anioPorSql(2028, "PLANIFICADO");
		anioPorSql(2029, "CERRADO");
		UUID id2030 = anioPorSql(2030, "PLANIFICADO");
		periodos.saveAndFlush(periodoNuevo(id2030));

		assertThat(consultas.aniosSinPeriodo())
				.containsExactly(new AnioDisponible(id2028, 2028, EstadoAnioEscolar.PLANIFICADO));
	}
}
