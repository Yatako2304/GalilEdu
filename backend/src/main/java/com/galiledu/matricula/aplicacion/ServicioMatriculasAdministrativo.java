package com.galiledu.matricula.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.matricula.aplicacion.DtosMatricula.MatriculaAdministrativa;
import com.galiledu.matricula.infraestructura.ConsultasMatricula;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.MatriculaAdministrativaFila;

/** Lecturas administrativas necesarias para navegar hasta la cuota de matrícula (HU-36). */
@Service
@Transactional(readOnly = true)
public class ServicioMatriculasAdministrativo {
	private final ConsultasMatricula consultas;

	public ServicioMatriculasAdministrativo(ConsultasMatricula consultas) {
		this.consultas = consultas;
	}

	public List<MatriculaAdministrativa> listar(UUID periodoId) {
		return consultas.matriculasDelPeriodo(periodoId).stream().map(this::respuesta).toList();
	}

	public MatriculaAdministrativa detalle(UUID matriculaId) {
		return consultas.matriculaAdministrativa(matriculaId)
				.map(this::respuesta)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe la matrícula seleccionada."));
	}

	private MatriculaAdministrativa respuesta(MatriculaAdministrativaFila fila) {
		return new MatriculaAdministrativa(
				fila.id(), fila.estudianteId(), fila.nombre(), fila.numeroDocumento(), fila.tipo(), fila.grado(),
				fila.anio(), fila.seccion(), fila.tutor(), fila.estado(), fila.codigo(), fila.apoderado(),
				fila.contacto(),
				fila.contactoEmergencia(), fila.informacionMedica());
	}
}
