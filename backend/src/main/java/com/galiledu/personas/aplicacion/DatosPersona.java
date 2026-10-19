package com.galiledu.personas.aplicacion;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.galiledu.personas.dominio.Persona;
import com.galiledu.personas.dominio.Sexo;
import com.galiledu.personas.dominio.TipoDocumento;

/**
 * Datos personales del formulario "Nuevo usuario" de Figma (también usados por padres y apoderados
 * en la inscripción). Solo documento, apellido paterno y nombres son obligatorios aquí; cada caso de uso
 * exige lo demás que necesite (por ejemplo, el usuario necesita correo y celular).
 */
public record DatosPersona(
		@NotNull(message = "Selecciona el tipo de documento") TipoDocumento tipoDocumento,
		@NotBlank(message = "Ingresa el número de documento") String numeroDocumento,
		@NotBlank(message = "Ingresa el apellido paterno") @Size(max = 80) @Pattern(regexp = SIN_NUMEROS, message = "El apellido no puede contener números") String apellidoPaterno,
		@Size(max = 80) @Pattern(regexp = SIN_NUMEROS, message = "El apellido no puede contener números") String apellidoMaterno,
		@NotBlank(message = "Ingresa los nombres") @Size(max = 120) @Pattern(regexp = SIN_NUMEROS, message = "Los nombres no pueden contener números") String nombres,
		@Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
		@Size(max = 60) String nacionalidad,
		@Size(max = 120) String lugarNacimiento,
		Sexo sexo,
		@Size(max = 20) String estadoCivil,
		@Size(max = 40) String gradoInstruccion,
		@Pattern(regexp = "^$|^[0-9]{9}$", message = "El celular debe tener 9 dígitos") String celular,
		@Email(message = "Ingresa un correo válido") @Size(max = 254) String correo,
		@Size(max = 15) String telefonoCasa,
		@Size(max = 120) String profesion,
		@Size(max = 120) String centroTrabajo,
		@Size(max = 120) String cargo,
		@Size(max = 80) String distrito,
		@Size(max = 200) String direccion,
		Boolean esExalumno) {
	static final String SIN_NUMEROS = "^[^0-9]*$";

	/** Copia los datos a la entidad, normalizando textos vacíos a null. */
	public void aplicarA(Persona p) {
		p.setTipoDocumento(tipoDocumento);
		p.setNumeroDocumento(texto(numeroDocumento).toUpperCase());
		p.setApellidoPaterno(texto(apellidoPaterno));
		p.setApellidoMaterno(texto(apellidoMaterno));
		p.setNombres(texto(nombres));
		p.setFechaNacimiento(fechaNacimiento);
		p.setNacionalidad(texto(nacionalidad));
		p.setLugarNacimiento(texto(lugarNacimiento));
		p.setSexo(sexo);
		p.setEstadoCivil(texto(estadoCivil));
		p.setGradoInstruccion(texto(gradoInstruccion));
		p.setCelular(texto(celular));
		p.setCorreo(texto(correo) == null ? null : texto(correo).toLowerCase());
		p.setTelefonoCasa(texto(telefonoCasa));
		p.setProfesion(texto(profesion));
		p.setCentroTrabajo(texto(centroTrabajo));
		p.setCargo(texto(cargo));
		p.setDistrito(texto(distrito));
		p.setDireccion(texto(direccion));
		p.setEsExalumno(Boolean.TRUE.equals(esExalumno));
	}

	public static DatosPersona de(Persona p) {
		return new DatosPersona(p.getTipoDocumento(), p.getNumeroDocumento(), p.getApellidoPaterno(),
				p.getApellidoMaterno(), p.getNombres(), p.getFechaNacimiento(), p.getNacionalidad(),
				p.getLugarNacimiento(),
				p.getSexo(), p.getEstadoCivil(), p.getGradoInstruccion(), p.getCelular(), p.getCorreo(),
				p.getTelefonoCasa(),
				p.getProfesion(), p.getCentroTrabajo(), p.getCargo(), p.getDistrito(), p.getDireccion(),
				p.isEsExalumno());
	}

	private static String texto(String valor) {
		return valor == null || valor.isBlank() ? null : valor.strip();
	}
}
