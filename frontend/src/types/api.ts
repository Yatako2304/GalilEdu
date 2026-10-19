// Tipos de los contratos con el backend.
// Lo ideal es que estos tipos se generen (o se mantengan sincronizados) con el backend.
// Cuando el back defina un DTO, actualízalo aquí y TypeScript te marcará qué romper.

import { ROLES } from '@/config/roles';

export type BaseEntity = {
  id: string;
  createdAt: number;
};

export type Entity<T> = {
  [K in keyof T]: T[K];
} & BaseEntity;

export type Meta = {
  page: number;
  total: number;
  totalPages: number;
};

// ---------- Auth / Usuarios ----------

export type User = Entity<{
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: ROLES[];
}>;

export type AuthResponse = {
  jwt: string;
  user: User;
};

// ---------- Horarios (HU-44 a HU-51) ----------

export type DiaSemana = 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES' | 'VIERNES';

export type BloqueHorario = {
  id: string;
  nombre: string; // "Bloque 1", "Recreo 1"
  tipo: 'CLASE' | 'RECREO';
  horaInicio: string; // "08:00"
  horaFin: string; // "08:45"
  orden: number;
};

export type CursoSesion = {
  curso: string;
  docente: string;
  aula: string;
};

export type SesionClase = {
  dia: DiaSemana;
  bloqueId: string;
  tipo: 'ASIGNADO' | 'SUBGRUPOS' | 'SIN_ASIGNAR';
  cursos: CursoSesion[];
};

export type HorarioSeccion = {
  anioAcademico: number;
  seccion: {
    id: string;
    grado: number;
    letra: string;
    tutor: string;
  };
  estudiante?: {
    id: string;
    nombreCompleto: string;
  };
  bloques: BloqueHorario[];
  sesiones: SesionClase[];
};
