package com.galiledu.matricula.aplicacion;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.ConsultaAnioEscolar;
import com.galiledu.matricula.aplicacion.puertos.ConsultaPeriodoMatricula;
import com.galiledu.matricula.aplicacion.puertos.RegistroPeriodoMatricula;
import com.galiledu.matricula.dominio.PeriodoMatricula;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionPeriodosMatricula {
	private final ConsultaAnioEscolar anios;
	private final ConsultaPeriodoMatricula consulta;
	private final RegistroPeriodoMatricula registro;

	public GestionPeriodosMatricula(ConsultaAnioEscolar anios,
		ConsultaPeriodoMatricula consulta, RegistroPeriodoMatricula registro) {
		this.anios = anios;
		this.consulta = consulta;
		this.registro = registro;
	}

	@Transactional
	public PeriodoMatricula guardar(PeriodoMatricula periodo) {
		anios.buscar(periodo.anioEscolarId())
			.orElseThrow(() -> new NoSuchElementException("Año escolar no encontrado"));
		registro.guardar(periodo);
		return periodo;
	}

	@Transactional(readOnly = true)
	public ConsultarPeriodoMatricula.Resultado consultar(UUID anioEscolarId, LocalDate fecha) {
		return new ConsultarPeriodoMatricula(consulta).ejecutar(anioEscolarId, fecha)
			.orElseThrow(() -> new NoSuchElementException("Periodo de matrícula no configurado"));
	}
}
