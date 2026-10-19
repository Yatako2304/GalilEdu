package com.galiledu.personas.aplicacion;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.personas.dominio.Persona;
import com.galiledu.personas.dominio.TipoDocumento;
import com.galiledu.personas.infraestructura.PersonaRepositorio;

/** Alta, edición y búsqueda de personas con las validaciones de RF-12, RF-14 y RF-23. */
@Service
public class ServicioPersonas {
	private static final Pattern DNI = Pattern.compile("[0-9]{8}");
	private static final Pattern CARNET = Pattern.compile("[A-Za-z0-9]{1,12}");

	private final PersonaRepositorio personas;

	public ServicioPersonas(PersonaRepositorio personas) {
		this.personas = personas;
	}

	@Transactional
	public Persona crear(DatosPersona datos) {
		validarFormato(datos);
		validarDuplicados(datos, null);
		Persona persona = new Persona();
		datos.aplicarA(persona);
		return personas.save(persona);
	}

	@Transactional
	public Persona actualizar(Persona persona, DatosPersona datos) {
		validarFormato(datos);
		validarDuplicados(datos, persona.getId());
		datos.aplicarA(persona);
		return personas.save(persona);
	}

	@Transactional(readOnly = true)
	public Optional<Persona> buscarPorDocumento(String numeroDocumento) {
		return personas.findByNumeroDocumento(numeroDocumento.strip().toUpperCase());
	}

	public Persona obtener(UUID id) {
		return personas.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("La persona no existe"));
	}

	private void validarFormato(DatosPersona datos) {
		String numero = datos.numeroDocumento().strip();
		boolean valido = datos.tipoDocumento() == TipoDocumento.DNI
			? DNI.matcher(numero).matches()
			: CARNET.matcher(numero).matches();
		if (!valido) {
			throw ExcepcionNegocio.invalido(datos.tipoDocumento() == TipoDocumento.DNI
				? "El DNI debe tener 8 dígitos"
				: "El carné de extranjería debe tener hasta 12 caracteres alfanuméricos");
		}
	}

	private void validarDuplicados(DatosPersona datos, UUID idActual) {
		personas.findByNumeroDocumento(datos.numeroDocumento().strip().toUpperCase())
			.filter(p -> !p.getId().equals(idActual))
			.ifPresent(p -> {
				throw ExcepcionNegocio.conflicto("Ya existe una persona registrada con el documento " + p.getNumeroDocumento());
			});
		if (datos.correo() != null && !datos.correo().isBlank()) {
			personas.buscarPorCorreo(datos.correo().strip())
				.filter(p -> !p.getId().equals(idActual))
				.ifPresent(p -> {
					throw ExcepcionNegocio.conflicto("El correo " + p.getCorreo() + " ya está registrado");
				});
		}
	}
}
