import Cookies from 'js-cookie';
import { delay } from 'msw';

import { ROLES } from '@/config/roles';

import { db } from './db';

export const encode = (obj: any) => {
  const btoa =
    typeof window === 'undefined'
      ? (str: string) => Buffer.from(str, 'binary').toString('base64')
      : window.btoa;
  return btoa(encodeURIComponent(JSON.stringify(obj)));
};

export const decode = (str: string) => {
  const atob =
    typeof window === 'undefined'
      ? (str: string) => Buffer.from(str, 'base64').toString('binary')
      : window.atob;
  return JSON.parse(decodeURIComponent(atob(str)));
};

export const hash = (str: string) => {
  let hash = 5381,
    i = str.length;

  while (i) {
    hash = (hash * 33) ^ str.charCodeAt(--i);
  }
  return String(hash >>> 0);
};

export const networkDelay = () => {
  const delayTime = import.meta.env.TEST
    ? 200
    : Math.floor(Math.random() * 400) + 200;
  return delay(delayTime);
};

type DbUser = ReturnType<typeof db.user.create>;

/** Quita el password y convierte los roles a arreglo, como lo devolvería el backend. */
export const sanitizeUser = (user: DbUser) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { password, roles, ...rest } = user;
  return {
    ...rest,
    roles: (roles ? roles.split(',') : []) as ROLES[],
  };
};

export function authenticate({
  username,
  password,
}: {
  username: string;
  password: string;
}) {
  const user = db.user.findFirst({
    where: {
      username: {
        equals: username,
      },
    },
  });

  if (user?.password === hash(password)) {
    const sanitizedUser = sanitizeUser(user);
    const encodedToken = encode({ id: sanitizedUser.id });
    return { user: sanitizedUser, jwt: encodedToken };
  }

  const error = new Error('Usuario o contraseña incorrectos');
  throw error;
}

export const AUTH_COOKIE = `galiledu_mock_session`;

export function requireAuth(cookies: Record<string, string>) {
  try {
    const encodedToken = cookies[AUTH_COOKIE] || Cookies.get(AUTH_COOKIE);
    if (!encodedToken) {
      return { error: 'No autorizado', user: null };
    }
    const decodedToken = decode(encodedToken) as { id: string };

    const user = db.user.findFirst({
      where: {
        id: {
          equals: decodedToken.id,
        },
      },
    });

    if (!user) {
      return { error: 'No autorizado', user: null };
    }

    return { user: sanitizeUser(user) };
  } catch (err: any) {
    return { error: 'No autorizado', user: null };
  }
}

export function requireRole(
  user: ReturnType<typeof sanitizeUser> | null | undefined,
  roles: ROLES[],
) {
  if (!user || !roles.some((role) => user.roles.includes(role))) {
    throw Error('No tiene permisos para esta acción');
  }
}
