package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.aplicacion.UsoDeEstructura;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.BloqueOcupado;
import com.galiledu.horarios.aplicacion.BloqueSolicitado;
import com.galiledu.horarios.aplicacion.CeldaHorario;
import com.galiledu.horarios.aplicacion.ConversorDeBloques;
import com.galiledu.horarios.aplicacion.OcupacionHorarios;
import com.galiledu.horarios.aplicacion.ServicioHorarios;
import com.galiledu.horarios.aplicacion.ValidadorCruces;
import com.galiledu.horarios.dominio.HorarioBloque;
import com.galiledu.horarios.dominio.HorarioSeccion;
import com.galiledu.horarios.infraestructura.HorarioBloqueRepositorio;
import com.galiledu.horarios.infraestructura.HorarioSeccionRepositorio;

/**
 * HU-45, Fase 0: las entidades de Horarios calzan con V10 y los stubs del contrato se inyectan. Usa la sección 1.° A de
 * 2026, su estructura principal, un curso de su plan y su aula.
 */
class HorarioRepositorioIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");

	@Autowired
	private HorarioSeccionRepositorio horarios;
	@Autowired
	private HorarioBloqueRepositorio bloques;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private ValidadorCruces validadorCruces;
	@Autowired
	private OcupacionHorarios ocupacionHorarios;
	@Autowired
	private ConversorDeBloques conversorDeBloques;
	@Autowired
	private ServicioHorarios servicioHorarios;
	@Autowired
	private UsoDeEstructura usoDeEstructura;

	private record Datos(UUID seccionId, UUID estructuraId, UUID planCursoId, UUID ambienteId) {
	}

	private Datos datosDe1A() {
		return jdbc
				.sql("""
						SELECT s.id AS seccion_id, eh.id AS estructura_id, pc.id AS plan_id, s.ambiente_id
						FROM configuracion.seccion s
						JOIN configuracion.estructura_horaria eh
						  ON eh.anio_escolar_id = s.anio_escolar_id AND eh.nivel = 'SECUNDARIA' AND eh.es_principal
						JOIN academico.plan_curso pc ON pc.anio_escolar_id = s.anio_escolar_id AND pc.grado_codigo = s.grado_codigo
						WHERE s.anio_escolar_id = :anio AND s.grado_codigo = 'SEC-1' AND s.nombre = 'A'
						LIMIT 1""")
				.param("anio", ANIO_2026)
				.query((r, fila) -> new Datos(r.getObject("seccion_id", UUID.class),
						r.getObject("estructura_id", UUID.class),
						r.getObject("plan_id", UUID.class), r.getObject("ambiente_id", UUID.class)))
				.single();
	}

	@Test
	void guardaYLeeUnHorarioConUnBloque() {
		Datos d = datosDe1A();

		HorarioSeccion horario = horarios.saveAndFlush(HorarioSeccion.crear(d.seccionId(), d.estructuraId()));
		bloques.saveAndFlush(HorarioBloque.crear(horario.getId(), DiaSemana.MIE, 3, d.planCursoId(), d.ambienteId()));
		entityManager.clear();

		HorarioSeccion leido = horarios.findBySeccionId(d.seccionId()).orElseThrow();
		assertThat(leido.getId()).isEqualTo(horario.getId());
		assertThat(leido.getEstructuraHorariaId()).isEqualTo(d.estructuraId());
		assertThat(leido.getVersion()).isZero();
		assertThat(leido.getCreadoEn()).isNotNull();
		assertThat(leido.getActualizadoEn()).isNotNull();

		List<HorarioBloque> delHorario = bloques.findByHorarioSeccionId(leido.getId());
		assertThat(delHorario).singleElement().satisfies(b -> {
			assertThat(b.getDia()).isEqualTo(DiaSemana.MIE);
			assertThat(b.getNumeroBloque()).isEqualTo(3);
			assertThat(b.getPlanCursoId()).isEqualTo(d.planCursoId());
			assertThat(b.getAmbienteId()).isEqualTo(d.ambienteId());
		});
		// Lo que se guardó es lo que ve la BD (nombres de columna y texto del día).
		assertThat(jdbc.sql("SELECT dia FROM horarios.horario_bloque WHERE horario_seccion_id = :id")
				.param("id", leido.getId()).query(String.class).single()).isEqualTo("MIE");
	}

	@Test
	void lasEntidadesSonLasDeLaSeccionYNoOtras() {
		Datos d = datosDe1A();
		horarios.saveAndFlush(HorarioSeccion.crear(d.seccionId(), d.estructuraId()));
		entityManager.clear();

		assertThat(horarios.findBySeccionId(UUID.randomUUID())).isEmpty();
		assertThat(horarios.findBySeccionId(d.seccionId())).isPresent();
	}

	@Test
	void losStubsDelContratoSeInyectanYNoHacenNada() {
		UUID id = UUID.randomUUID();
		assertThat(validadorCruces.buscarCruces(id, id, List.<BloqueSolicitado>of())).isEmpty();
		assertThat(validadorCruces.compararCruces(List.<BloqueSolicitado>of(), List.<BloqueOcupado>of())).isEmpty();
		assertThat(ocupacionHorarios.bloquesOcupados(id, id)).isEmpty();
		assertThat(conversorDeBloques.aBloquesSolicitados(id, List.<CeldaHorario>of())).isEmpty();
		assertThat(servicioHorarios.bloquesDeCurso(id, id)).isZero();
		assertThat(usoDeEstructura.usoPorDia(id)).isEqualTo(Map.of());
	}
}
