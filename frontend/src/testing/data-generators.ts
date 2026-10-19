import {
  randEmail,
  randFirstName,
  randLastName,
  randPassword,
  randUserName,
  randUuid,
} from '@ngneat/falso';

import { ROLES } from '@/config/roles';

const generateUser = () => ({
  id: randUuid() + Math.random(),
  username: randUserName({ withAccents: false }),
  firstName: randFirstName({ withAccents: false }),
  lastName: randLastName({ withAccents: false }),
  email: randEmail(),
  password: randPassword(),
  roles: [ROLES.ESTUDIANTE] as ROLES[],
  createdAt: Date.now(),
});

export const createUser = <T extends Partial<ReturnType<typeof generateUser>>>(
  overrides?: T,
) => {
  return { ...generateUser(), ...overrides };
};
