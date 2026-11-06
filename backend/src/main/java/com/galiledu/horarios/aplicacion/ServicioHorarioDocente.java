package com.galiledu.horarios.aplicacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.horarios.aplicacion.AniosYSecciones.AnioLeido;
import com.galiledu.horarios.aplicacion.ClasesDelDocente.AnioConClases;
import com.galiledu.horarios.aplicacion.ClasesDelDocente.ClaseDelDocente;
import com.galiledu.horarios.aplicacion.DtosHorarioDocente.AnioDelDocente;
import com.galiledu.horarios.aplicacion.DtosHorarioDocente.ClaseDictada;
import com.galiledu.horarios.aplicacion.DtosHorarioDocente.HorarioDocente;
import com.galiledu.horarios.aplicacion.DtosHorarioDocente.HorarioPorEstructura;
import com.galiledu.horarios.aplicacion.DtosHorarios.BloqueJornada;
import com.galiledu.horarios.aplicacion.DtosHorarios.JornadaDelDia;
import com.galiledu.horarios.aplicacion.DtosHorarios.RecesoJornada;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;

/**
 * HU-49: el horario de dictado del docente (y del tutor): solo las clases donde es titular en
 * {@code docente_seccion}, separadas por estructura horaria porque cada una tiene su propia jornada. Solo lectura.
 */
@Service
public class ServicioHorarioDocente {
	private static final String NIVEL_PRIMARIA = "PRIMARIA";

	private final ClasesDelDocente clases;
	private final JornadaDeSeccion jornadas;
	private final AniosYSecciones anios;

	public ServicioHorarioDocente(ClasesDelDocente clases, JornadaDeSeccion jornadas, AniosYSecciones anios) {
		this.clases = clases;
		this.jornadas = jornadas;
		this.anios = anios;
	}

	/** Los años en los que el docente tiene clases, del más reciente al más antiguo. */
	@Transactional(readOnly = true)
	public List<AnioDelDocente> aniosDelDocente(UUID docenteId) {
		return clases.aniosDe(docenteId).stream().map(a -> new AnioDelDocente(a.id(), a.anio(), a.estado())).toList();
	}

	/**
	 * El horario del docente en {@code anioId}. Sin {@code anioId} usa el año ACTIVO si el docente tiene clases en él
	 * y, si no, el más reciente con clases; si no tiene clases en ningún año, {@code anio} es null. 404 si el año no
	 * existe; un año sin clases devuelve {@code horarios} vacío.
	 */
	@Transactional(readOnly = true)
	public HorarioDocente horarioDelDocente(UUID docenteId, UUID anioId) {
		AnioDelDocente anio = anioId == null ? anioPorDefecto(docenteId) : anioExistente(anioId);
		if (anio == null) {
			return new HorarioDocente(null, List.of());
		}
		Map<UUID, List<ClaseDelDocente>> porEstructura = new LinkedHashMap<>();
		for (ClaseDelDocente clase : clases.clasesDe(docenteId, anio.id())) {
			porEstructura.computeIfAbsent(clase.estructuraId(), k -> new ArrayList<>()).add(clase);
		}

		List<Grupo> grupos = porEstructura.entrySet().stream()
				.map(e -> new Grupo(e.getValue().get(0).nivel(), jornadas.deEstructura(e.getKey()), e.getValue()))
				.sorted(Comparator.comparingInt((Grupo g) -> NIVEL_PRIMARIA.equals(g.nivel()) ? 0 : 1)
						.thenComparing(g -> g.jornada().codigo()))
				.toList();
		List<HorarioPorEstructura> horarios = grupos.stream().map(g -> new HorarioPorEstructura(g.nivel(),
				titulo(g, grupos), jornadaDe(g.jornada()),
				g.clases().stream()
						.map(c -> new ClaseDictada(c.dia(), c.numeroBloque(), c.curso(), c.seccion(), c.ambiente()))
						.toList()))
				.toList();
		return new HorarioDocente(anio, horarios);
	}

	private record Grupo(String nivel, JornadaEstructura jornada, List<ClaseDelDocente> clases) {
	}

	/** El año ACTIVO si el docente tiene clases en él; si no, el más reciente con clases; null si no hay ninguno. */
	private AnioDelDocente anioPorDefecto(UUID docenteId) {
		List<AnioConClases> conClases = clases.aniosDe(docenteId);
		return conClases.stream().filter(a -> "ACTIVO".equals(a.estado())).findFirst()
				.or(() -> conClases.stream().findFirst()).map(a -> new AnioDelDocente(a.id(), a.anio(), a.estado()))
				.orElse(null);
	}

	private AnioDelDocente anioExistente(UUID anioId) {
		AnioLeido anio = anios.anios().stream().filter(a -> a.id().equals(anioId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe."));
		return new AnioDelDocente(anio.id(), anio.anio(), anio.estado());
	}

	/** "Primaria" o "Secundaria"; con el código si el docente tiene dos estructuras del mismo nivel. */
	private static String titulo(Grupo grupo, List<Grupo> todos) {
		String nivel = grupo.nivel().charAt(0) + grupo.nivel().substring(1).toLowerCase(Locale.ROOT);
		long delMismoNivel = todos.stream().filter(g -> g.nivel().equals(grupo.nivel())).count();
		return delMismoNivel > 1 ? nivel + " · " + grupo.jornada().codigo() : nivel;
	}

	private static JornadaDelDia jornadaDe(JornadaEstructura j) {
		return new JornadaDelDia(j.dias(),
				j.bloques().stream().map(b -> new BloqueJornada(b.numero(), b.inicio().toString(), b.fin().toString()))
						.toList(),
				j.recesos().stream()
						.map(r -> new RecesoJornada(r.despuesDeBloque(), r.inicio().toString(), r.fin().toString()))
						.toList());
	}
}
