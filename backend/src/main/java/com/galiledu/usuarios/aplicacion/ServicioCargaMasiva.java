package com.galiledu.usuarios.aplicacion;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.personas.aplicacion.DatosPersona;
import com.galiledu.personas.dominio.Sexo;
import com.galiledu.personas.dominio.TipoDocumento;
import com.galiledu.personas.infraestructura.PersonaRepositorio;
import com.galiledu.usuarios.aplicacion.Dtos.ErrorFila;
import com.galiledu.usuarios.aplicacion.Dtos.ResultadoCargaMasiva;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudUsuario;
import com.galiledu.usuarios.dominio.Rol;
import com.galiledu.usuarios.infraestructura.ArchivoUsuarios;
import com.galiledu.usuarios.infraestructura.ArchivoUsuarios.Fila;

/**
 * HU-8: carga masiva de usuarios desde Excel o CSV (Figma > "Carga masiva de usuarios", 3 pasos). Cada fila se valida
 * con las mismas reglas que el registro individual (RF-12 a RF-14) y no detiene a las demás (RF-20).
 */
@Service
public class ServicioCargaMasiva {
	private static final long TAMANO_MAXIMO = 10L * 1024 * 1024;
	private static final Pattern DNI = Pattern.compile("[0-9]{8}");
	private static final Pattern CARNET = Pattern.compile("[A-Za-z0-9]{1,12}");
	private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(DateTimeFormatter.ofPattern("d/M/yyyy"),
			DateTimeFormatter.ofPattern("d-M-yyyy"), DateTimeFormatter.ISO_LOCAL_DATE);

	private final ArchivoUsuarios archivos;
	private final PersonaRepositorio personas;
	private final ServicioUsuarios usuarios;
	private final Validator validador;

	public ServicioCargaMasiva(ArchivoUsuarios archivos, PersonaRepositorio personas, ServicioUsuarios usuarios,
			Validator validador) {
		this.archivos = archivos;
		this.personas = personas;
		this.usuarios = usuarios;
		this.validador = validador;
	}

	private record FilaRevisada(int numero, SolicitudUsuario solicitud, List<String> errores) {
	}

	/** Paso 2: revisa el archivo sin crear nada. */
	public ResultadoCargaMasiva validar(MultipartFile archivo) {
		List<FilaRevisada> filas = revisar(archivo);
		List<ErrorFila> errores = errores(filas);
		return new ResultadoCargaMasiva(archivo.getOriginalFilename(), filas.size(), filas.size() - errores.size(),
				0, errores);
	}

	/** Paso 3: crea las filas válidas (cada una en su propia transacción) y devuelve el resumen. */
	public ResultadoCargaMasiva cargar(MultipartFile archivo) {
		List<FilaRevisada> filas = revisar(archivo);
		List<ErrorFila> errores = new ArrayList<>(errores(filas));
		int creados = 0;
		for (FilaRevisada fila : filas) {
			if (!fila.errores().isEmpty()) {
				continue;
			}
			try {
				usuarios.crear(fila.solicitud());
				creados++;
			} catch (ExcepcionNegocio e) {
				errores.add(new ErrorFila(fila.numero(), e.getMessage()));
			}
		}
		errores.sort((a, b) -> Integer.compare(a.fila(), b.fila()));
		return new ResultadoCargaMasiva(archivo.getOriginalFilename(), filas.size(), filas.size() - errores.size(),
				creados, errores);
	}

	public byte[] plantilla() {
		return archivos.plantilla();
	}

	private List<FilaRevisada> revisar(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			throw ExcepcionNegocio.invalido("Selecciona un archivo");
		}
		if (archivo.getSize() > TAMANO_MAXIMO) {
			throw ExcepcionNegocio.invalido("El archivo no puede pesar más de 10 MB");
		}
		List<Fila> filas;
		try (InputStream contenido = archivo.getInputStream()) {
			filas = archivos.leer(archivo.getOriginalFilename(), contenido);
		} catch (IOException e) {
			throw ExcepcionNegocio.invalido("No se pudo leer el archivo");
		}

		// Para detectar documentos y correos repetidos dentro del mismo archivo.
		Map<String, Integer> documentos = new HashMap<>();
		Map<String, Integer> correos = new HashMap<>();
		List<FilaRevisada> revisadas = new ArrayList<>();
		for (Fila fila : filas) {
			List<String> errores = new ArrayList<>();
			SolicitudUsuario solicitud = convertir(fila, errores);
			if (solicitud != null) {
				validarPersona(solicitud.persona(), errores);
				String documento = solicitud.persona().numeroDocumento();
				String correo = solicitud.persona().correo() == null
						? null
						: solicitud.persona().correo().toLowerCase();
				Integer filaDocumento = documento == null ? null : documentos.putIfAbsent(documento, fila.numero());
				Integer filaCorreo = correo == null ? null : correos.putIfAbsent(correo, fila.numero());
				if (filaDocumento != null) {
					errores.add("Documento repetido en la fila " + filaDocumento + " del archivo");
				} else if (documento != null && personas.findByNumeroDocumento(documento.toUpperCase()).isPresent()) {
					errores.add(etiqueta(solicitud.persona().tipoDocumento()) + " duplicado: " + documento
							+ " ya está registrado");
				}
				if (filaCorreo != null) {
					errores.add("Correo repetido en la fila " + filaCorreo + " del archivo");
				} else if (correo != null && personas.buscarPorCorreo(correo).isPresent()) {
					errores.add("Correo duplicado: " + correo + " ya está registrado");
				}
			}
			revisadas.add(new FilaRevisada(fila.numero(), solicitud, errores));
		}
		return revisadas;
	}

	private static List<ErrorFila> errores(List<FilaRevisada> filas) {
		return filas.stream()
				.filter(f -> !f.errores().isEmpty())
				.map(f -> new ErrorFila(f.numero(), String.join("; ", f.errores())))
				.toList();
	}

	/** Convierte los textos de la fila a la solicitud del alta individual; anota los errores de formato. */
	private SolicitudUsuario convertir(Fila f, List<String> errores) {
		TipoDocumento tipo = tipoDocumento(f.valor("tipoDocumento"), errores);
		String numero = f.valor("numeroDocumento");
		if (numero != null && tipo == TipoDocumento.DNI && numero.matches("[0-9]{1,7}")) {
			// Excel suele quitar los ceros iniciales del DNI.
			numero = "0".repeat(8 - numero.length()) + numero;
		}
		DatosPersona persona = new DatosPersona(tipo, numero, f.valor("apellidoPaterno"), f.valor("apellidoMaterno"),
				f.valor("nombres"), fecha(f.valor("fechaNacimiento"), errores), f.valor("nacionalidad"),
				f.valor("lugarNacimiento"), sexo(f.valor("sexo"), errores), f.valor("estadoCivil"),
				f.valor("gradoInstruccion"), sinEspacios(f.valor("celular")), f.valor("correo"),
				f.valor("telefonoCasa"), f.valor("profesion"), f.valor("centroTrabajo"), f.valor("cargo"),
				f.valor("distrito"), f.valor("direccion"), siNo(f.valor("esExalumno")));
		Set<Rol> roles = roles(f.valor("roles"), errores);
		return roles == null ? new SolicitudUsuario(persona, Set.of()) : new SolicitudUsuario(persona, roles);
	}

	private void validarPersona(DatosPersona p, List<String> errores) {
		for (ConstraintViolation<DatosPersona> v : validador.validate(p)) {
			errores.add(v.getMessage());
		}
		if (p.tipoDocumento() != null && p.numeroDocumento() != null) {
			boolean valido = p.tipoDocumento() == TipoDocumento.DNI
					? DNI.matcher(p.numeroDocumento()).matches()
					: CARNET.matcher(p.numeroDocumento()).matches();
			if (!valido) {
				errores.add(p.tipoDocumento() == TipoDocumento.DNI
						? "El DNI debe tener 8 dígitos"
						: "El carné de extranjería debe tener hasta 12 caracteres alfanuméricos");
			}
		}
		if (p.correo() == null) {
			errores.add("Ingresa el correo electrónico");
		}
		if (p.celular() == null) {
			errores.add("Ingresa el celular");
		}
	}

	private static TipoDocumento tipoDocumento(String valor, List<String> errores) {
		String v = normalizar(valor);
		if (v.isEmpty()) {
			return null; // el validador avisa "Selecciona el tipo de documento"
		}
		if (v.equals("dni")) {
			return TipoDocumento.DNI;
		}
		if (v.equals("ce") || v.contains("carne") || v.contains("extranjeria")) {
			return TipoDocumento.CARNET_EXTRANJERIA;
		}
		errores.add("Tipo de documento no válido: " + valor + " (usa DNI o Carné de extranjería)");
		return null;
	}

	private static LocalDate fecha(String valor, List<String> errores) {
		if (valor == null) {
			return null;
		}
		for (DateTimeFormatter formato : FORMATOS_FECHA) {
			try {
				return LocalDate.parse(valor, formato);
			} catch (DateTimeParseException e) {
				// se prueba el siguiente formato
			}
		}
		errores.add("Fecha de nacimiento no válida: " + valor + " (usa dd/mm/aaaa)");
		return null;
	}

	private static Sexo sexo(String valor, List<String> errores) {
		String v = normalizar(valor);
		if (v.isEmpty()) {
			return null;
		}
		if (v.equals("m") || v.equals("masculino")) {
			return Sexo.MASCULINO;
		}
		if (v.equals("f") || v.equals("femenino")) {
			return Sexo.FEMENINO;
		}
		errores.add("Sexo no válido: " + valor + " (usa Masculino o Femenino)");
		return null;
	}

	private static Boolean siNo(String valor) {
		String v = normalizar(valor);
		return v.equals("si") || v.equals("s") || v.equals("true") || v.equals("1");
	}

	/** "Docente, Tutor" → {DOCENTE, TUTOR}. Acepta el nombre visible o el código del rol. */
	private static Set<Rol> roles(String valor, List<String> errores) {
		if (valor == null) {
			errores.add("Asigna al menos un rol");
			return null;
		}
		Set<Rol> roles = EnumSet.noneOf(Rol.class);
		Set<String> desconocidos = new LinkedHashSet<>();
		for (String parte : valor.split("[,;/]")) {
			String v = normalizar(parte);
			if (v.isEmpty()) {
				continue;
			}
			Rol rol = null;
			for (Rol r : Rol.values()) {
				if (normalizar(r.nombre()).equals(v) || normalizar(r.name()).equals(v)) {
					rol = r;
				}
			}
			if (rol == null || rol == Rol.ESTUDIANTE) {
				desconocidos.add(parte.strip());
			} else {
				roles.add(rol);
			}
		}
		if (!desconocidos.isEmpty()) {
			errores.add("Rol no válido: " + String.join(", ", desconocidos));
		} else if (roles.isEmpty()) {
			errores.add("Asigna al menos un rol");
		}
		return roles;
	}

	private static String etiqueta(TipoDocumento tipo) {
		return tipo == TipoDocumento.CARNET_EXTRANJERIA ? "Carné de extranjería" : "DNI";
	}

	private static String sinEspacios(String valor) {
		return valor == null ? null : valor.replaceAll("\\s", "");
	}

	private static String normalizar(String texto) {
		return texto == null
				? ""
				: java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
						.replaceAll("\\p{M}", "").toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}
