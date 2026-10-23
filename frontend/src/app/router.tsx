import { QueryClient, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { Outlet, RouteObject, createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';

import { paths } from '@/config/paths';
import { ROLES } from '@/config/roles';
import { ProtectedRoute } from '@/lib/auth';
import { RoleGuard } from '@/lib/authorization';

import {
  default as AppRoot,
  ErrorBoundary as AppRootErrorBoundary,
} from './routes/app/root';

const convert = (queryClient: QueryClient) => (m: any) => {
  const { clientLoader, clientAction, default: Component, ...rest } = m;
  return {
    ...rest,
    loader: clientLoader?.(queryClient),
    action: clientAction?.(queryClient),
    Component,
  };
};

type Lazy = () => Promise<any>;

/** Árbol de rutas de un rol: /app/<segmento>/... protegido por RoleGuard. */
const roleRoutes = (
  queryClient: QueryClient,
  role: ROLES,
  path: string,
  inicio: Lazy,
  children: [string, Lazy][],
): RouteObject => ({
  path,
  element: (
    <RoleGuard role={role}>
      <Outlet />
    </RoleGuard>
  ),
  children: [
    { index: true, lazy: () => inicio().then(convert(queryClient)) },
    ...children.map(([childPath, lazy]) => ({
      path: childPath,
      lazy: () => lazy().then(convert(queryClient)),
    })),
  ],
});

/** `crear` permite probar las rutas con un router en memoria; la app usa el del navegador. */
export const createAppRouter = (
  queryClient: QueryClient,
  crear: (
    rutas: RouteObject[],
  ) => ReturnType<typeof createBrowserRouter> = createBrowserRouter,
) =>
  crear([
    {
      path: paths.home.path,
      lazy: () => import('./routes/landing').then(convert(queryClient)),
    },
    {
      path: paths.auth.login.path,
      lazy: () => import('./routes/auth/login').then(convert(queryClient)),
    },
    {
      path: paths.auth.recuperarContrasena.path,
      lazy: () =>
        import('./routes/auth/recuperar-contrasena').then(convert(queryClient)),
    },
    {
      path: paths.app.root.path,
      element: (
        <ProtectedRoute>
          <AppRoot />
        </ProtectedRoute>
      ),
      ErrorBoundary: AppRootErrorBoundary,
      children: [
        {
          index: true,
          lazy: () =>
            import('./routes/app/redirect-to-home').then(convert(queryClient)),
        },

        roleRoutes(
          queryClient,
          ROLES.ADMINISTRADOR,
          paths.app.admin.root.path,
          () => import('./routes/app/admin/inicio'),
          [
            [
              paths.app.admin.usuarios.path,
              () => import('./routes/app/admin/usuarios'),
            ],
          ],
        ),

        roleRoutes(
          queryClient,
          ROLES.PERSONAL_ADMINISTRATIVO,
          paths.app.administrativo.root.path,
          () => import('./routes/app/administrativo/inicio'),
          [
            [
              paths.app.administrativo.estudiantes.path,
              () => import('./routes/app/administrativo/estudiantes'),
            ],
            [
              paths.app.administrativo.nuevaInscripcion.path,
              () => import('./routes/app/administrativo/nueva-inscripcion'),
            ],
            [
              paths.app.administrativo.fichaEstudiante.path,
              () => import('./routes/app/administrativo/ficha-estudiante'),
            ],
            [
              paths.app.administrativo.matricula.path,
              () => import('./routes/app/administrativo/matricula'),
            ],
            [
              paths.app.administrativo.periodoMatriculaNuevo.path,
              () =>
                import('./routes/app/administrativo/periodo-matricula-nuevo'),
            ],
            [
              paths.app.administrativo.periodoMatricula.path,
              () => import('./routes/app/administrativo/periodo-matricula'),
            ],
            [
              paths.app.administrativo.periodoMatriculaEditar.path,
              () =>
                import('./routes/app/administrativo/periodo-matricula-editar'),
            ],
            [
              paths.app.administrativo.matriculasDelPeriodo.path,
              () => import('./routes/app/administrativo/matriculas-periodo'),
            ],
            [
              paths.app.administrativo.matriculaDetalle.path,
              () => import('./routes/app/administrativo/matricula-detalle'),
            ],
            [
              paths.app.administrativo.pensiones.path,
              () => import('./routes/app/administrativo/pensiones'),
            ],
            [
              paths.app.administrativo.tarifario.path,
              () =>
                import('./routes/app/administrativo/pensiones-configuracion'),
            ],
            [
              paths.app.administrativo.compromisoPago.path,
              () => import('./routes/app/administrativo/compromiso-pago'),
            ],
            [
              paths.app.administrativo.anioEscolar.path,
              () => import('./routes/app/administrativo/anio-escolar'),
            ],
            [
              paths.app.administrativo.anioEscolarDetalle.path,
              () => import('./routes/app/administrativo/anio-escolar-detalle'),
            ],
            [
              paths.app.administrativo.competencias.path,
              () => import('./routes/app/administrativo/competencias'),
            ],
            [
              paths.app.administrativo.ambientes.path,
              () => import('./routes/app/administrativo/ambientes'),
            ],
          ],
        ),

        roleRoutes(
          queryClient,
          ROLES.COORDINADOR,
          paths.app.coordinador.root.path,
          () => import('./routes/app/coordinador/inicio'),
          [
            [
              paths.app.coordinador.horarios.path,
              () => import('./routes/app/coordinador/horarios'),
            ],
            [
              paths.app.coordinador.calificaciones.path,
              () => import('./routes/app/coordinador/calificaciones'),
            ],
            [
              paths.app.coordinador.planEvaluacion.path,
              () => import('./routes/app/coordinador/plan-evaluacion'),
            ],
            [
              paths.app.coordinador.ambientes.path,
              () => import('./routes/app/coordinador/ambientes'),
            ],
          ],
        ),

        roleRoutes(
          queryClient,
          ROLES.DOCENTE,
          paths.app.docente.root.path,
          () => import('./routes/app/docente/inicio'),
          [
            [
              paths.app.docente.horario.path,
              () => import('./routes/app/docente/horario'),
            ],
            [
              paths.app.docente.calificaciones.path,
              () => import('./routes/app/docente/calificaciones'),
            ],
            [
              paths.app.docente.asistencia.path,
              () => import('./routes/app/docente/asistencia'),
            ],
          ],
        ),

        roleRoutes(
          queryClient,
          ROLES.TUTOR,
          paths.app.tutor.root.path,
          () => import('./routes/app/tutor/inicio'),
          [
            [
              paths.app.tutor.horario.path,
              () => import('./routes/app/tutor/horario'),
            ],
            [
              paths.app.tutor.calificaciones.path,
              () => import('./routes/app/tutor/calificaciones'),
            ],
            [
              paths.app.tutor.asistencia.path,
              () => import('./routes/app/tutor/asistencia'),
            ],
          ],
        ),

        roleRoutes(
          queryClient,
          ROLES.APODERADO,
          paths.app.apoderado.root.path,
          () => import('./routes/app/apoderado/inicio'),
          [
            [
              paths.app.apoderado.datosAlumno.path,
              () => import('./routes/app/apoderado/datos-alumno'),
            ],
            [
              paths.app.apoderado.horarios.path,
              () => import('./routes/app/apoderado/horarios'),
            ],
            [
              paths.app.apoderado.calificaciones.path,
              () => import('./routes/app/apoderado/calificaciones'),
            ],
            [
              paths.app.apoderado.matricula.path,
              () => import('./routes/app/apoderado/matricula'),
            ],
            [
              paths.app.apoderado.pensiones.path,
              () => import('./routes/app/apoderado/pensiones'),
            ],
          ],
        ),

        roleRoutes(
          queryClient,
          ROLES.ESTUDIANTE,
          paths.app.estudiante.root.path,
          () => import('./routes/app/estudiante/inicio'),
          [
            [
              paths.app.estudiante.datosAlumno.path,
              () => import('./routes/app/estudiante/datos-alumno'),
            ],
            [
              paths.app.estudiante.horarios.path,
              () => import('./routes/app/estudiante/horarios'),
            ],
            [
              paths.app.estudiante.calificaciones.path,
              () => import('./routes/app/estudiante/calificaciones'),
            ],
          ],
        ),
      ],
    },
    {
      path: '*',
      lazy: () => import('./routes/not-found').then(convert(queryClient)),
    },
  ]);

export const AppRouter = () => {
  const queryClient = useQueryClient();

  const router = useMemo(() => createAppRouter(queryClient), [queryClient]);

  return <RouterProvider router={router} />;
};
