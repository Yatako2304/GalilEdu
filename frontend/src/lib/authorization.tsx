import * as React from 'react';
import { Navigate, useLocation } from 'react-router';

import { ROLES, getRoleBySegment, getRoleHomeHref } from '@/config/roles';
import { User } from '@/types/api';

import { useUser } from './auth';

export { ROLES };

// Políticas finas (por recurso). Agrega aquí reglas como "solo el docente del curso
// puede editar sus notas" y úsalas con <Authorization policyCheck={...}>.
export const POLICIES = {
  'usuarios:gestionar': (user: User) =>
    user.roles.includes(ROLES.ADMINISTRADOR),
};

/** Rol con el que el usuario navega ahora mismo: se toma del segmento /app/<rol>. */
export const useActiveRole = (): ROLES | undefined => {
  const location = useLocation();
  const segment = location.pathname.split('/')[2];
  return getRoleBySegment(segment);
};

export const getDefaultHomeHref = (user: User) =>
  user.roles.length > 0 ? getRoleHomeHref(user.roles[0]) : '/auth/login';

export const useAuthorization = () => {
  const user = useUser();

  if (!user.data) {
    throw Error('El usuario no existe');
  }

  const checkAccess = React.useCallback(
    ({ allowedRoles }: { allowedRoles: ROLES[] }) => {
      if (allowedRoles && allowedRoles.length > 0 && user.data) {
        return allowedRoles.some((role) => user.data?.roles.includes(role));
      }

      return true;
    },
    [user.data],
  );

  return { checkAccess, roles: user.data.roles };
};

type AuthorizationProps = {
  forbiddenFallback?: React.ReactNode;
  children: React.ReactNode;
} & (
  | {
      allowedRoles: ROLES[];
      policyCheck?: never;
    }
  | {
      allowedRoles?: never;
      policyCheck: boolean;
    }
);

export const Authorization = ({
  policyCheck,
  allowedRoles,
  forbiddenFallback = null,
  children,
}: AuthorizationProps) => {
  const { checkAccess } = useAuthorization();

  let canAccess = false;

  if (allowedRoles) {
    canAccess = checkAccess({ allowedRoles });
  }

  if (typeof policyCheck !== 'undefined') {
    canAccess = policyCheck;
  }

  return <>{canAccess ? children : forbiddenFallback}</>;
};

/** Protege todo el árbol de rutas de un rol: si el usuario no lo tiene, lo manda a su inicio. */
export const RoleGuard = ({
  role,
  children,
}: {
  role: ROLES;
  children: React.ReactNode;
}) => {
  const user = useUser();

  if (!user.data) return null;

  if (!user.data.roles.includes(role)) {
    return <Navigate to={getDefaultHomeHref(user.data)} replace />;
  }

  return children;
};
