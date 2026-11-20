package com.galiledu.usuarios.aplicacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.personas.aplicacion.DatosPersona;
import com.galiledu.personas.aplicacion.ServicioPersonas;
import com.galiledu.personas.dominio.Persona;
import com.galiledu.usuarios.aplicacion.Dtos.ContactoFamilia;
import com.galiledu.usuarios.aplicacion.Dtos.EstudianteFamilia;
import com.galiledu.usuarios.aplicacion.Dtos.FamiliaApoderado;
import com.galiledu.usuarios.aplicacion.Dtos.IntegranteFamiliar;
import com.galiledu.usuarios.aplicacion.VinculosUsuario.ContactoDeEmergencia;
import com.galiledu.usuarios.aplicacion.VinculosUsuario.EstudianteDeFamilia;
import com.galiledu.usuarios.aplicacion.VinculosUsuario.IntegranteFamilia;
import com.galiledu.usuarios.infraestructura.UsuarioRepositorio;

/**
 * Apoderado › «Datos de la Familia», de solo vista. La familia no es una tabla: sale de la inscripción de cada
 * estudiante a cargo (padre, madre, apoderado y contactos de emergencia). Los hermanos con el mismo padre, madre y
 * apoderado se muestran como una sola familia.
 */
@Service
public class ServicioMiFamilia {
	private static final List<String> PARENTESCOS = List.of("PADRE", "MADRE", "APODERADO");

	private final UsuarioRepositorio usuarios;
	private final VinculosUsuario vinculos;
	private final ServicioPersonas personas;

	public ServicioMiFamilia(UsuarioRepositorio usuarios, VinculosUsuario vinculos, ServicioPersonas personas) {
		this.usuarios = usuarios;
		this.vinculos = vinculos;
		this.personas = personas;
	}

	@Transactional(readOnly = true)
	public List<FamiliaApoderado> deUsuario(UUID usuarioId) {
		UUID yo = usuarios.findById(usuarioId).orElseThrow().getPersona().getId();
		List<EstudianteDeFamilia> estudiantes = vinculos.estudiantesDeFamilia(yo);
		Set<UUID> ids = estudiantes.stream().map(EstudianteDeFamilia::estudianteId)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<UUID, List<IntegranteFamilia>> integrantes = vinculos.integrantesDe(ids).stream()
				.collect(Collectors.groupingBy(IntegranteFamilia::estudianteId));
		Map<UUID, List<ContactoDeEmergencia>> contactos = vinculos.contactosDeEmergencia(ids).stream()
				.collect(Collectors.groupingBy(ContactoDeEmergencia::estudianteId));

		// Hermanos: mismo padre, madre y apoderado (o el mismo motivo de «no registra»).
		Map<String, List<EstudianteDeFamilia>> familias = new LinkedHashMap<>();
		for (EstudianteDeFamilia e : estudiantes) {
			familias.computeIfAbsent(clave(integrantes.getOrDefault(e.estudianteId(), List.of())),
					k -> new ArrayList<>()).add(e);
		}

		Map<UUID, Persona> cache = new LinkedHashMap<>();
		List<FamiliaApoderado> resultado = new ArrayList<>();
		for (List<EstudianteDeFamilia> hermanos : familias.values()) {
			EstudianteDeFamilia primero = hermanos.getFirst();
			List<IntegranteFamilia> deLaFamilia = integrantes.getOrDefault(primero.estudianteId(), List.of());
			resultado.add(new FamiliaApoderado(apellidos(primero),
					hermanos.stream().map(ServicioMiFamilia::estudiante).toList(),
					integrante(deLaFamilia, "PADRE", yo, cache), integrante(deLaFamilia, "MADRE", yo, cache),
					integrante(deLaFamilia, "APODERADO", yo, cache), contactosSinRepetir(hermanos, contactos)));
		}
		return resultado;
	}

	private static String clave(List<IntegranteFamilia> integrantes) {
		List<String> partes = new ArrayList<>();
		for (String parentesco : PARENTESCOS) {
			partes.add(integrantes.stream().filter(i -> i.parentesco().equals(parentesco))
					.map(ServicioMiFamilia::quien).sorted().collect(Collectors.joining(",")));
		}
		return String.join("|", partes);
	}

	private static String quien(IntegranteFamilia i) {
		return i.personaId() != null ? i.personaId().toString() : "sin:" + i.motivoNoRegistra();
	}

	private static String apellidos(EstudianteDeFamilia e) {
		return (e.apellidoPaterno() + " " + Objects.toString(e.apellidoMaterno(), "")).strip();
	}

	private static EstudianteFamilia estudiante(EstudianteDeFamilia e) {
		String nombre = (e.nombres() + " " + apellidos(e)).strip();
		return new EstudianteFamilia(e.estudianteId(), nombre, e.grado(), e.anioAcademico(), e.estado());
	}

	private IntegranteFamiliar integrante(List<IntegranteFamilia> integrantes, String parentesco, UUID yo,
			Map<UUID, Persona> cache) {
		for (IntegranteFamilia i : integrantes) {
			if (!i.parentesco().equals(parentesco)) {
				continue;
			}
			if (i.personaId() == null) {
				return new IntegranteFamiliar(null, false, i.motivoNoRegistra(), i.relacion(), false);
			}
			Persona p = cache.computeIfAbsent(i.personaId(), personas::obtener);
			boolean esUsted = p.getId().equals(yo);
			return new IntegranteFamiliar(DatosPersona.de(p), p.isFallecido(), null, i.relacion(), esUsted);
		}
		return null;
	}

	/** Los hermanos suelen compartir contactos de emergencia: se listan una vez (mismo nombre y celular). */
	private static List<ContactoFamilia> contactosSinRepetir(List<EstudianteDeFamilia> hermanos,
			Map<UUID, List<ContactoDeEmergencia>> contactos) {
		Map<String, ContactoFamilia> unicos = new LinkedHashMap<>();
		for (EstudianteDeFamilia e : hermanos) {
			for (ContactoDeEmergencia c : contactos.getOrDefault(e.estudianteId(), List.of())) {
				String clave = c.nombreCompleto().strip().toLowerCase() + "|" + Objects.toString(c.celular(), "");
				unicos.putIfAbsent(clave,
						new ContactoFamilia(c.nombreCompleto(), c.parentesco(), c.celular(), c.telefonoFijo()));
			}
		}
		return List.copyOf(unicos.values());
	}
}
