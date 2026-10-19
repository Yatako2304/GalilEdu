import {
  BookOpenCheck,
  Building2,
  CalendarCheck,
  CalendarDays,
  ClipboardList,
  GraduationCap,
  Contact,
  type LucideIcon,
  ShieldCheck,
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
};

export type NavSection = {
  title: string;
  items: NavItem[];
};

// Menú lateral por rol, copiado de las instancias "Sidebar" de cada página de Figma.
export const NAVIGATION: Record<ROLES, NavSection[]> = {
  [ROLES.ADMINISTRADOR]: [
    {
      title: 'Seguridad',
      items: [
        {
          label: 'Usuarios',
          href: paths.app.admin.usuarios.getHref(),
          icon: ShieldCheck,
        },
        {
          label: 'Estudiantes',
          href: paths.app.admin.estudiantes.getHref(),
          icon: Users,
        },
      ],
    },
  ],
  [ROLES.PERSONAL_ADMINISTRATIVO]: [
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
          label: 'Ambientes físicos',
          href: paths.app.coordinador.ambientes.getHref(),
          icon: Building2,
        },
      ],
    },
  ],
  [ROLES.DOCENTE]: [
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
