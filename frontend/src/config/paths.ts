// Todas las rutas de la app en un solo lugar (patrón bulletproof-react).
// `path` es el segmento relativo usado en el router; `getHref` la URL absoluta para navegar.

const route = <TBase extends string, TSegment extends string>(
  base: TBase,
  segment: TSegment,
) => ({
  path: segment,
  getHref: () => `${base}/${segment}` as const,
});

const roleRoot = <TBase extends string>(base: TBase) => ({
  path: base.replace('/app/', ''),
  getHref: () => base,
});

const ADMIN = '/app/admin';
const ADMINISTRATIVO = '/app/administrativo';
const COORDINADOR = '/app/coordinador';
const DOCENTE = '/app/docente';
const TUTOR = '/app/tutor';
const APODERADO = '/app/apoderado';
const ESTUDIANTE = '/app/estudiante';

export const paths = {
  home: {
    path: '/',
    getHref: () => '/',
  },

  auth: {
    login: {
      path: '/auth/login',
      getHref: (redirectTo?: string | null | undefined) =>
        `/auth/login${redirectTo ? `?redirectTo=${encodeURIComponent(redirectTo)}` : ''}`,
    },
    recuperarContrasena: {
      path: '/auth/recuperar-contrasena',
      getHref: () => '/auth/recuperar-contrasena',
    },
  },

  app: {
    root: {
      path: '/app',
      getHref: () => '/app',
    },

    // Figma: 🌟 Administrador (sidebar: Inicio · Usuarios)
    admin: {
      root: roleRoot(ADMIN),
      usuarios: route(ADMIN, 'usuarios'),
    },

    // Figma: 🌟 Personal Administrativo
    administrativo: {
      root: roleRoot(ADMINISTRATIVO),
      estudiantes: route(ADMINISTRATIVO, 'estudiantes'),
      nuevaInscripcion: {
        path: 'estudiantes/nueva',
        getHref: () => `${ADMINISTRATIVO}/estudiantes/nueva`,
      },
      fichaEstudiante: {
        path: 'estudiantes/:estudianteId',
        getHref: (id: string) => `${ADMINISTRATIVO}/estudiantes/${id}`,
      },
      matricula: route(ADMINISTRATIVO, 'matricula'),
      // Periodos de matrícula (HU-23). El listado es la propia ruta `matricula`, sin `?matriculaId=`.
      periodoMatriculaNuevo: {
        path: 'matricula/periodos/nuevo',
        getHref: () => `${ADMINISTRATIVO}/matricula/periodos/nuevo`,
      },
      periodoMatricula: {
        path: 'matricula/periodos/:periodoId',
        getHref: (periodoId: string) =>
          `${ADMINISTRATIVO}/matricula/periodos/${periodoId}`,
      },
      periodoMatriculaEditar: {
        path: 'matricula/periodos/:periodoId/editar',
        getHref: (periodoId: string) =>
          `${ADMINISTRATIVO}/matricula/periodos/${periodoId}/editar`,
      },
      // A3 de Matrícula (HU-24, HU-71): aún no existe ni está registrada en el router.
      matriculasDelPeriodo: {
        path: 'matricula/periodos/:periodoId/matriculas',
        getHref: (periodoId: string) =>
          `${ADMINISTRATIVO}/matricula/periodos/${periodoId}/matriculas`,
      },
      pensiones: route(ADMINISTRATIVO, 'pensiones'),
      // P19 · Tarifario (HU-34): aún no existe ni está registrada en el router.
      tarifario: route(ADMINISTRATIVO, 'pensiones/configuracion'),
      compromisoPago: {
        path: 'pensiones/:estudianteId',
        getHref: (estudianteId: string) =>
          `${ADMINISTRATIVO}/pensiones/${estudianteId}`,
      },
      anioEscolar: route(ADMINISTRATIVO, 'anio-escolar'),
      anioEscolarDetalle: {
        path: 'anio-escolar/:anioId',
        getHref: (anioId: string) => `${ADMINISTRATIVO}/anio-escolar/${anioId}`,
      },
      competencias: route(ADMINISTRATIVO, 'competencias'),
      ambientes: route(ADMINISTRATIVO, 'ambientes'),
    },

    // Figma: 🌟 Coordinador
    coordinador: {
      root: roleRoot(COORDINADOR),
      horarios: route(COORDINADOR, 'horarios'),
      calificaciones: route(COORDINADOR, 'calificaciones'),
      planEvaluacion: route(COORDINADOR, 'plan-evaluacion'),
      ambientes: route(COORDINADOR, 'ambientes'),
    },

    // Figma: 🌟 Docente
    docente: {
      root: roleRoot(DOCENTE),
      horario: route(DOCENTE, 'horario'),
      calificaciones: route(DOCENTE, 'calificaciones'),
      asistencia: route(DOCENTE, 'asistencia'),
    },

    // Figma: 🌟 Tutor
    tutor: {
      root: roleRoot(TUTOR),
      horario: route(TUTOR, 'horario'),
      calificaciones: route(TUTOR, 'calificaciones'),
      asistencia: route(TUTOR, 'asistencia'),
    },

    // Figma: 🌟 Apoderado
    apoderado: {
      root: roleRoot(APODERADO),
      datosAlumno: route(APODERADO, 'datos-alumno'),
      horarios: route(APODERADO, 'horarios'),
      calificaciones: route(APODERADO, 'calificaciones'),
      matricula: route(APODERADO, 'matricula'),
      pensiones: route(APODERADO, 'pensiones'),
    },

    // Figma: 🌟 Estudiante
    estudiante: {
      root: roleRoot(ESTUDIANTE),
      datosAlumno: route(ESTUDIANTE, 'datos-alumno'),
      horarios: route(ESTUDIANTE, 'horarios'),
      calificaciones: route(ESTUDIANTE, 'calificaciones'),
    },
  },
} as const;
