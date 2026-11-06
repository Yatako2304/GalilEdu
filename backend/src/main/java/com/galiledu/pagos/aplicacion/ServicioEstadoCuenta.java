package com.galiledu.pagos.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.pagos.aplicacion.DtosPagos.CuotaVencida;
import com.galiledu.pagos.infraestructura.ConsultasPagos;

/**
 * IEstadoCuenta (diagrama de componentes): lo que otros módulos pueden preguntarle a Pagos sobre la deuda.
 * Matrícula la usa para validar la deuda antes de matricular (HU-30, RF-64).
 */
@Service
public class ServicioEstadoCuenta {
	private final ConsultasPagos consultas;
	private final Clock reloj;

	public ServicioEstadoCuenta(ConsultasPagos consultas, Clock reloj) {
		this.consultas = consultas;
		this.reloj = reloj;
	}

	/** Pensiones vencidas e impagas de esos estudiantes (por ejemplo, todos los hijos de un apoderado). */
	@Transactional(readOnly = true)
	public List<CuotaVencida> cuotasVencidas(Collection<UUID> estudianteIds) {
		return consultas.cuotasVencidas(estudianteIds, LocalDate.now(reloj)).stream()
				.map(f -> new CuotaVencida(f.estudianteId(), f.estudiante(), f.concepto(), f.fechaVencimiento(),
						f.saldo()))
				.toList();
	}
}
