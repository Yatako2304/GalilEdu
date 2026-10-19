// Roles del sistema (Figma: una página por rol).
// El rol activo se deduce de la URL (/app/<segmento>/...), por eso cada rol tiene un segmento.
export enum ROLES {
  ADMINISTRADOR = 'ADMINISTRADOR',
  PERSONAL_ADMINISTRATIVO = 'PERSONAL_ADMINISTRATIVO',
  COORDINADOR = 'COORDINADOR',
  DOCENTE = 'DOCENTE',
  TUTOR = 'TUTOR',
  APODERADO = 'APODERADO',
  ESTUDIANTE = 'ESTUDIANTE',
}

export type RoleSegment =
  | 'admin'
  | 'administrativo'
  | 'coordinador'
  | 'docente'
  | 'tutor'
  | 'apoderado'
  | 'estudiante';

type RoleConfig = {
  label: string;
  segment: RoleSegment;
};

export const ROLE_CONFIG: Record<ROLES, RoleConfig> = {
  [ROLES.ADMINISTRADOR]: { label: 'Administrador', segment: 'admin' },
  [ROLES.PERSONAL_ADMINISTRATIVO]: {
    label: 'Personal Administrativo',
    segment: 'administrativo',
  },
  [ROLES.COORDINADOR]: { label: 'Coordinador', segment: 'coordinador' },
  [ROLES.DOCENTE]: { label: 'Docente', segment: 'docente' },
  [ROLES.TUTOR]: { label: 'Tutor', segment: 'tutor' },
  [ROLES.APODERADO]: { label: 'Apoderado', segment: 'apoderado' },
  [ROLES.ESTUDIANTE]: { label: 'Alumno', segment: 'estudiante' },
};

export const getRoleHomeHref = (role: ROLES) =>
  `/app/${ROLE_CONFIG[role].segment}`;

export const getRoleBySegment = (segment?: string) =>
  (Object.keys(ROLE_CONFIG) as ROLES[]).find(
    (role) => ROLE_CONFIG[role].segment === segment,
  );
