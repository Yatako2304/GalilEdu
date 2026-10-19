import { ROLES } from '@/config/roles';

import { db, persistDb, serializeRoles } from './db';
import { hash } from './utils';

// Contraseña de los usuarios de prueba del backend simulado (solo desarrollo).
export const SEED_PASSWORD = 'galiledu123';

const SEED_USERS: {
  username: string;
  firstName: string;
  lastName: string;
  roles: ROLES[];
}[] = [
  {
    username: 'admin',
    firstName: 'Selene',
    lastName: 'Curo',
    roles: [ROLES.ADMINISTRADOR],
  },
  {
    username: 'administrativo',
    firstName: 'Rosa',
    lastName: 'Quispe',
    roles: [ROLES.PERSONAL_ADMINISTRATIVO],
  },
  {
    username: 'coordinador',
    firstName: 'Jorge',
    lastName: 'Paredes',
    roles: [ROLES.COORDINADOR],
  },
  {
    username: 'docente',
    firstName: 'Valeria',
    lastName: 'Montes Ríos',
    roles: [ROLES.DOCENTE],
  },
  {
    // Un docente que además es tutor: prueba el selector de rol del sidebar
    username: 'tutor',
    firstName: 'Lucía',
    lastName: 'Ayala Prado',
    roles: [ROLES.TUTOR, ROLES.DOCENTE],
  },
  {
    username: 'apoderado',
    firstName: 'Carmen',
    lastName: 'Ferrara',
    roles: [ROLES.APODERADO],
  },
  {
    username: 'estudiante',
    firstName: 'Valeria',
    lastName: 'Sánchez Ferrara',
    roles: [ROLES.ESTUDIANTE],
  },
];

/** La primera vez crea un usuario de prueba por rol (ver README > Usuarios de prueba). */
export const seedDb = async () => {
  if (db.user.count() > 0) return;

  SEED_USERS.forEach((user) => {
    db.user.create({
      ...user,
      email: `${user.username}@galiledu.edu.pe`,
      roles: serializeRoles(user.roles),
      password: hash(SEED_PASSWORD),
    });
  });

  await persistDb('user');
};
