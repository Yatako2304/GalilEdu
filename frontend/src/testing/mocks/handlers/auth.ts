import Cookies from 'js-cookie';
import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { DatosPersona, FamiliaApoderado } from '@/types/api';

import { db } from '../db';
import {
  authenticate,
  requireAuth,
  sanitizeUser,
  AUTH_COOKIE,
  networkDelay,
} from '../utils';

type LoginBody = {
  username: string;
  password: string;
};

// Contrato esperado del backend para autenticación:
//   POST /auth/login                 { username, password } -> { jwt, user }
//   POST /auth/logout                -> { message }
//   GET  /auth/me                    -> { data: User | null }
//   POST /auth/recuperar-contrasena             { email } -> { message }   (HU-4: envía el código)
//   POST /auth/recuperar-contrasena/verificar   { email, codigo } -> { message }
//   POST /auth/recuperar-contrasena/restablecer { email, codigo, contrasena } -> { message }
//   POST /auth/cambiar-contrasena/verificar { contrasenaActual } -> { message }   (HU-5)
//   POST /auth/cambiar-contrasena          { contrasenaActual, contrasenaNueva } -> { message }
//   GET  /auth/metodos                    -> { google, googleIdCliente }   (HU-2: botón de Google en el login)
//   POST /auth/google                     { codigo } -> { jwt, user }   (HU-3)
//   GET  /auth/mis-datos                  -> DatosPersona   (cualquier rol: «Mis datos», ampliación de HU-9)
//   PUT  /auth/mis-datos                  { ...DatosPersona } -> User (sesión actualizada)
//   GET  /auth/mi-familia                 -> FamiliaApoderado[]   (solo Apoderado; 403 a los demás)
//   GET  /seguridad/configuracion-acceso  -> { googleHabilitado, googleConfigurado }   (HU-2, administrador)
//   PUT  /seguridad/configuracion-acceso  { googleHabilitado } -> { googleHabilitado, googleConfigurado }
// En el modo simulado el código válido es 123456; la contraseña actual «incorrecta» da error. Google no se puede
// simular: el botón aparece al activarlo, pero POST /auth/google responde que no hay cuenta asociada.
const misDatosSimulados = new Map<string, DatosPersona>();

// Familia simulada: el apoderado es el padre de dos hermanos; sin madre registrada ni apoderado aparte.
const familiaSimulada = (yo: DatosPersona): FamiliaApoderado[] => [
  {
    nombre: `${yo.apellidoPaterno} Rojas`,
    estudiantes: [
      {
        id: 'est-1',
        nombreCompleto: `Mateo ${yo.apellidoPaterno} Rojas`,
        grado: '1.° de secundaria',
        anioAcademico: 2026,
        estado: 'MATRICULADO',
      },
      {
        id: 'est-2',
        nombreCompleto: `Sofía ${yo.apellidoPaterno} Rojas`,
        grado: '4.° de secundaria',
        anioAcademico: 2026,
        estado: 'MATRICULADO',
      },
    ],
    padre: {
      persona: yo,
      fallecido: false,
      motivoNoRegistra: null,
      relacion: null,
      esUsted: true,
    },
    madre: null,
    apoderado: null,
    contactosEmergencia: [
      {
        nombreCompleto: 'Rosa Medina Torres',
        parentesco: 'Abuela',
        celular: '987654321',
        telefonoFijo: null,
      },
    ],
  },
];

const misDatosPorDefecto = (user: {
  firstName: string;
  lastName: string;
  email: string;
}): DatosPersona => {
  const [apellidoPaterno, ...resto] = user.lastName.split(' ');
  return {
    tipoDocumento: 'DNI',
    numeroDocumento: '40000000',
    apellidoPaterno: apellidoPaterno || user.lastName,
    apellidoMaterno: resto.join(' ') || null,
    nombres: user.firstName,
    fechaNacimiento: '1990-01-01',
    nacionalidad: 'Peruana',
    lugarNacimiento: 'Perú',
    sexo: 'FEMENINO',
    estadoCivil: 'Soltero(a)',
    gradoInstruccion: 'Superior',
    celular: '999999999',
    correo: user.email,
    telefonoCasa: null,
    profesion: 'Docente',
    centroTrabajo: 'Colegio Galileo',
    cargo: 'Docente',
    distrito: 'Lince',
    direccion: 'Av. Arequipa 2500',
    esExalumno: false,
  };
};

const CODIGO_SIMULADO = '123456';
const ID_CLIENTE_SIMULADO = 'simulado.apps.googleusercontent.com';
const accesoSimulado = { googleHabilitado: false };
export const authHandlers = [
  http.get(`${env.API_URL}/auth/metodos`, async () => {
    await networkDelay();
    return HttpResponse.json({
      google: accesoSimulado.googleHabilitado,
      googleIdCliente: accesoSimulado.googleHabilitado
        ? ID_CLIENTE_SIMULADO
        : null,
    });
  }),

  http.post(`${env.API_URL}/auth/google`, async () => {
    await networkDelay();
    return HttpResponse.json(
      {
        message:
          'No se encontró una cuenta de GalilEdu asociada a este correo de Google.',
      },
      { status: 404 },
    );
  }),

  http.get(
    `${env.API_URL}/seguridad/configuracion-acceso`,
    async ({ cookies }) => {
      await networkDelay();
      requireAuth(cookies);
      return HttpResponse.json({ ...accesoSimulado, googleConfigurado: true });
    },
  ),

  http.put(
    `${env.API_URL}/seguridad/configuracion-acceso`,
    async ({ cookies, request }) => {
      await networkDelay();
      requireAuth(cookies);
      const { googleHabilitado } = (await request.json()) as {
        googleHabilitado: boolean;
      };
      accesoSimulado.googleHabilitado = googleHabilitado;
      return HttpResponse.json({ ...accesoSimulado, googleConfigurado: true });
    },
  ),

  http.post(`${env.API_URL}/auth/login`, async ({ request }) => {
    await networkDelay();

    try {
      const credentials = (await request.json()) as LoginBody;
      const result = authenticate(credentials);

      // todo: remove once tests in Github Actions are fixed
      Cookies.set(AUTH_COOKIE, result.jwt, { path: '/' });

      return HttpResponse.json(result, {
        headers: {
          // con un backend real la cookie debe ser Secure y HttpOnly
          'Set-Cookie': `${AUTH_COOKIE}=${result.jwt}; Path=/;`,
        },
      });
    } catch (error: any) {
      return HttpResponse.json(
        { message: error?.message || 'Error del servidor' },
        { status: 401 },
      );
    }
  }),

  http.post(`${env.API_URL}/auth/logout`, async () => {
    await networkDelay();

    // todo: remove once tests in Github Actions are fixed
    Cookies.remove(AUTH_COOKIE);

    return HttpResponse.json(
      { message: 'Sesión cerrada' },
      {
        headers: {
          'Set-Cookie': `${AUTH_COOKIE}=; Path=/;`,
        },
      },
    );
  }),

  http.get(`${env.API_URL}/auth/me`, async ({ cookies }) => {
    await networkDelay();

    const { user } = requireAuth(cookies);
    return HttpResponse.json({ data: user });
  }),

  http.get(`${env.API_URL}/auth/mis-datos`, async ({ cookies }) => {
    await networkDelay();
    const { user, error } = requireAuth(cookies);
    if (error || !user) {
      return HttpResponse.json({ message: error }, { status: 401 });
    }
    if (!misDatosSimulados.has(user.id)) {
      misDatosSimulados.set(user.id, misDatosPorDefecto(user));
    }
    return HttpResponse.json(misDatosSimulados.get(user.id));
  }),

  http.get(`${env.API_URL}/auth/mi-familia`, async ({ cookies }) => {
    await networkDelay();
    const { user, error } = requireAuth(cookies);
    if (error || !user) {
      return HttpResponse.json({ message: error }, { status: 401 });
    }
    if (!user.roles.includes(ROLES.APODERADO)) {
      return HttpResponse.json({ message: 'Acceso denegado' }, { status: 403 });
    }
    if (!misDatosSimulados.has(user.id)) {
      misDatosSimulados.set(user.id, misDatosPorDefecto(user));
    }
    return HttpResponse.json(familiaSimulada(misDatosSimulados.get(user.id)!));
  }),

  http.put(`${env.API_URL}/auth/mis-datos`, async ({ cookies, request }) => {
    await networkDelay();
    const { user, error } = requireAuth(cookies);
    if (error || !user) {
      return HttpResponse.json({ message: error }, { status: 401 });
    }
    const datos = (await request.json()) as DatosPersona;
    misDatosSimulados.set(user.id, datos);
    db.user.update({
      where: { id: { equals: user.id } },
      data: {
        firstName: datos.nombres,
        lastName:
          `${datos.apellidoPaterno} ${datos.apellidoMaterno ?? ''}`.trim(),
        email: datos.correo ?? user.email,
      },
    });
    const actualizado = db.user.findFirst({
      where: { id: { equals: user.id } },
    });
    return HttpResponse.json(sanitizeUser(actualizado!));
  }),

  http.post(`${env.API_URL}/auth/recuperar-contrasena`, async ({ request }) => {
    await networkDelay();
    const { email } = (await request.json()) as { email: string };
    if (email.includes('noexiste')) {
      return HttpResponse.json(
        { message: 'El correo no está asociado a ninguna cuenta.' },
        { status: 404 },
      );
    }
    return HttpResponse.json({
      message: 'Te enviamos un código de verificación a tu correo.',
    });
  }),

  http.post(
    `${env.API_URL}/auth/recuperar-contrasena/verificar`,
    async ({ request }) => {
      await networkDelay();
      const { codigo } = (await request.json()) as { codigo: string };
      if (codigo !== CODIGO_SIMULADO) {
        return HttpResponse.json(
          { message: 'Código de verificación incorrecto.' },
          { status: 400 },
        );
      }
      return HttpResponse.json({ message: 'Código verificado.' });
    },
  ),

  http.post(
    `${env.API_URL}/auth/cambiar-contrasena/verificar`,
    async ({ request }) => {
      await networkDelay();
      const { contrasenaActual } = (await request.json()) as {
        contrasenaActual: string;
      };
      if (!contrasenaActual || contrasenaActual === 'incorrecta') {
        return HttpResponse.json(
          { message: 'Contraseña incorrecta.' },
          { status: 400 },
        );
      }
      return HttpResponse.json({ message: 'Contraseña verificada.' });
    },
  ),

  http.post(`${env.API_URL}/auth/cambiar-contrasena`, async () => {
    await networkDelay();
    return HttpResponse.json({
      message: 'Su contraseña ha sido actualizada correctamente.',
    });
  }),

  http.post(
    `${env.API_URL}/auth/recuperar-contrasena/restablecer`,
    async () => {
      await networkDelay();
      return HttpResponse.json({
        message: 'Su contraseña ha sido actualizada correctamente.',
      });
    },
  ),
];
