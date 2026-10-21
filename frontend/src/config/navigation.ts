import {
  BookOpenCheck,
  Building2,
  CalendarCheck,
  CalendarDays,
  ClipboardList,
  GraduationCap,
  Contact,
  Home,
  type LucideIcon,
  Star,
  Users,
  Wallet,
} from 'lucide-react';

import { paths } from './paths';
import { ROLES } from './roles';

export type NavItem = {
  label: string;
  href: string;
  icon: LucideIcon;
  /** Solo se marca activo en la ruta exacta (para "Inicio"). */
  exact?: boolean;
};

export type NavSection = {
  title: string;
  items: NavItem[];
};

const inicioItem = (href: string): NavItem => ({
  label: 'Inicio',
  href,
  icon: Home,
  exact: true,
});

/** Sección "General" con Inicio (y otros accesos generales del rol). */
const inicio = (href: string, extra: NavItem[] = []): NavSection => ({
  title: 'General',
  items: [inicioItem(href), ...extra],
});

// Menú lateral por rol, copiado de las instancias "Sidebar" de cada página de Figma.
export const NAVIGATION: Record<ROLES, NavSection[]> = {
  [ROLES.ADMINISTRADOR]: [
    inicio(paths.app.admin.root.getHref()),
    {
      title: 'Seguridad',
      items: [
        {
          label: 'Usuarios',
          href: paths.app.admin.usuarios.getHref(),
          icon: Users,
        },
      ],
    },
  ],
  [ROLES.PERSONAL_ADMINISTRATIVO]: [
    inicio(paths.app.administrativo.root.getHref(), [
      {
        label: 'Estudiantes',
        href: paths.app.administrativo.estudiantes.getHref(),
        icon: GraduationCap,
      },
    ]),
    {
      title: 'Académico y operaciones',
      items: [
        {
          label: 'Matrícula',
          href: paths.app.administrativo.matricula.getHref(),
          icon: ClipboardList,
        },
        {
          label: 'Pensiones',
          href: paths.app.administrativo.pensiones.getHref(),
          icon: Wallet,
        },
      ],
    },
    {
      title: 'Configuraciones',
      items: [
        {
          label: 'Año escolar',
          href: paths.app.administrativo.anioEscolar.getHref(),
          icon: CalendarDays,
        },
        {
          label: 'Competencias',
          href: paths.app.administrativo.competencias.getHref(),
          icon: BookOpenCheck,
        },
        {
          label: 'Ambientes físicos',
          href: paths.app.administrativo.ambientes.getHref(),
          icon: Building2,
        },
      ],
    },
  ],
  [ROLES.COORDINADOR]: [
    inicio(paths.app.coordinador.root.getHref()),
    {
      title: 'Académico y operaciones',
      items: [
        {
          label: 'Horarios',
          href: paths.app.coordinador.horarios.getHref(),
          icon: CalendarDays,
        },
        {
          label: 'Calificaciones',
          href: paths.app.coordinador.calificaciones.getHref(),
          icon: Star,
        },
        {
          label: 'Plan de evaluación',
          href: paths.app.coordinador.planEvaluacion.getHref(),
          icon: BookOpenCheck,
        },
        {
          label: 'Ambientes físicos',
          href: paths.app.coordinador.ambientes.getHref(),
          icon: Building2,
        },
      ],
    },
  ],
  [ROLES.DOCENTE]: [
    inicio(paths.app.docente.root.getHref()),
    {
      title: 'Académico',
      items: [
        {
          label: 'Horario',
          href: paths.app.docente.horario.getHref(),
          icon: CalendarDays,
        },
        {
          label: 'Calificaciones',
          href: paths.app.docente.calificaciones.getHref(),
          icon: Star,
        },
        {
          label: 'Asistencia',
          href: paths.app.docente.asistencia.getHref(),
          icon: CalendarCheck,
        },
      ],
    },
  ],
  [ROLES.TUTOR]: [
    inicio(paths.app.tutor.root.getHref()),
    {
      title: 'Académico',
      items: [
        {
          label: 'Horario',
          href: paths.app.tutor.horario.getHref(),
          icon: CalendarDays,
        },
        {
          label: 'Calificaciones',
          href: paths.app.tutor.calificaciones.getHref(),
          icon: Star,
        },
        {
          label: 'Asistencia',
          href: paths.app.tutor.asistencia.getHref(),
          icon: CalendarCheck,
        },
      ],
    },
  ],
  [ROLES.APODERADO]: [
    {
      title: 'General',
      items: [
        inicioItem(paths.app.apoderado.root.getHref()),
        {
          label: 'Datos del Alumno',
          href: paths.app.apoderado.datosAlumno.getHref(),
          icon: Contact,
        },
      ],
    },
    {
      title: 'Académico',
      items: [
        {
          label: 'Horarios',
          href: paths.app.apoderado.horarios.getHref(),
          icon: CalendarDays,
        },
        {
          label: 'Calificaciones',
          href: paths.app.apoderado.calificaciones.getHref(),
          icon: Star,
        },
      ],
    },
    {
      title: 'Matrícula y pensiones',
      items: [
        {
          label: 'Matrícula',
          href: paths.app.apoderado.matricula.getHref(),
          icon: GraduationCap,
        },
        {
          label: 'Pensiones',
          href: paths.app.apoderado.pensiones.getHref(),
          icon: Wallet,
        },
      ],
    },
  ],
  [ROLES.ESTUDIANTE]: [
    {
      title: 'General',
      items: [
        inicioItem(paths.app.estudiante.root.getHref()),
        {
          label: 'Datos del Alumno',
          href: paths.app.estudiante.datosAlumno.getHref(),
          icon: Contact,
        },
      ],
    },
    {
      title: 'Académico',
      items: [
        {
          label: 'Horarios',
          href: paths.app.estudiante.horarios.getHref(),
          icon: CalendarDays,
        },
        {
          label: 'Calificaciones',
          href: paths.app.estudiante.calificaciones.getHref(),
          icon: Star,
        },
      ],
    },
  ],
};
