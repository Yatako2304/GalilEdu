package com.galiledu.horarios.dominio;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Un bloque asignado del horario de una sección (tabla horarios.horario_bloque, V10). HU-45.
 * <p>
 * Dice: "en este día y número de bloque, la sección dicta este curso del plan en este ambiente". El bloque no es una
 * fila de catálogo: su hora se calcula con la estructura horaria. El docente no se guarda: es el de
 * academico.docente_seccion para (plan_curso_id, seccion_id). El plan de evaluación y el ambiente son de otros
 * módulos: solo se guardan sus ids.
 */
@Entity
@Table(schema = "horarios", name = "horario_bloque")
public class HorarioBloque {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "horario_seccion_id", nullable = false, updatable = false)
	private UUID horarioSeccionId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DiaSemana dia;
	@Column(name = "numero_bloque", nullable = false)
	private short numeroBloque;
	@Column(name = "plan_curso_id", nullable = false)
	private UUID planCursoId;
	@Column(name = "ambiente_id", nullable = false)
	private UUID ambienteId;

	protected HorarioBloque() {
	}

	public static HorarioBloque crear(UUID horarioSeccionId, DiaSemana dia, int numeroBloque, UUID planCursoId,
			UUID ambienteId) {
		HorarioBloque b = new HorarioBloque();
		b.horarioSeccionId = horarioSeccionId;
		b.dia = dia;
		b.numeroBloque = (short) numeroBloque;
		b.planCursoId = planCursoId;
		b.ambienteId = ambienteId;
		return b;
	}

	public UUID getId() {
		return id;
	}
	public UUID getHorarioSeccionId() {
		return horarioSeccionId;
	}
	public DiaSemana getDia() {
		return dia;
	}
	public int getNumeroBloque() {
		return numeroBloque;
	}
	public UUID getPlanCursoId() {
		return planCursoId;
	}
	public UUID getAmbienteId() {
		return ambienteId;
	}
}
