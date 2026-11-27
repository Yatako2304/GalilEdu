package com.galiledu.matricula.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.estudiantes.aplicacion.ServicioEstudiantes;
import com.galiledu.matricula.aplicacion.DtosMatricula.RetiroRegistrado;
import com.galiledu.matricula.infraestructura.ConsultasMatricula;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.MatriculaDetalleFila;
import com.galiledu.matricula.infraestructura.EscriturasMatricula;
import com.galiledu.pagos.aplicacion.ServicioCronograma;

/**
 * Retiro del alumno del colegio (fila 36 del plan de pruebas, «Registrar retiro de alumno del colegio»). Una sola
 * acción, la misma desde Estudiantes, el detalle de la matrícula y Pensiones:
 * <ol>
 * <li>la matrícula MATRICULADO pasa a RETIRADO con su fecha y motivo: libera la vacante y sale de asistencia y del
 * registro de notas (sus notas se siguen viendo en la libreta, pero no se pueden modificar);</li>
 * <li>se anulan las pensiones pendientes que vencen después del retiro (RF-85); las vencidas se siguen debiendo;</li>
 * <li>el estudiante queda RETIRADO (inactivo).</li>
 * </ol>
 * No cambia tablas ni columnas: usa {@code fecha_retiro} y {@code motivo_retiro} de V5. No toca las cuentas.
 */
@Service
public class ServicioRetiroAlumno {
	private static final DateTimeFormatter DD_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final ConsultasMatricula consultas;
	private final EscriturasMatricula escrituras;
	private final ServicioCronograma cronograma;
	private final ServicioEstudiantes estudiantes;
	private final Clock reloj;

	public ServicioRetiroAlumno(ConsultasMatricula consultas, EscriturasMatricula escrituras,
			ServicioCronograma cronograma, ServicioEstudiantes estudiantes, Clock reloj) {
		this.consultas = consultas;
		this.escrituras = escrituras;
		this.cronograma = cronograma;
		this.estudiantes = estudiantes;
		this.reloj = reloj;
	}

	@Transactional
	public RetiroRegistrado retirar(UUID estudianteId, LocalDate fechaRetiro, String motivo) {
		UUID matriculaId = consultas.matriculaParaRetiro(estudianteId)
				.orElseThrow(() -> ExcepcionNegocio.conflicto(
						"El estudiante no tiene una matrícula vigente (MATRICULADO) que retirar."));
		MatriculaDetalleFila m = consultas.matriculaDetalle(matriculaId).orElseThrow();
		if (fechaRetiro.isAfter(LocalDate.now(reloj))) {
			throw ExcepcionNegocio.invalido("La fecha de retiro no puede ser posterior a hoy.");
		}
		if (m.fechaConfirmacion() != null && fechaRetiro.isBefore(m.fechaConfirmacion())) {
			throw ExcepcionNegocio.invalido("La fecha de retiro no puede ser anterior a la matrícula ("
					+ m.fechaConfirmacion().format(DD_MM_AAAA) + ").");
		}
		if (!escrituras.retirar(matriculaId, fechaRetiro, motivo.trim())) {
			throw ExcepcionNegocio.conflicto("La matrícula ya no está vigente.");
		}
		int anuladas = cronograma.anularPorRetiroDeMatricula(matriculaId, fechaRetiro);
		estudiantes.marcarRetirado(estudianteId);
		return new RetiroRegistrado(matriculaId, m.codigo(), fechaRetiro, anuladas);
	}
}
