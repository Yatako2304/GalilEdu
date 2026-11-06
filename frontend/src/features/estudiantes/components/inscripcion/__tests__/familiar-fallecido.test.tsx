/* eslint-disable testing-library/no-container --
   La fecha se localiza por su "name" de react-hook-form (hay una por familiar con la misma etiqueta). */
import { ROLES } from '@/config/roles';
import {
  createUser,
  fireEvent,
  renderApp,
  screen,
  userEvent,
} from '@/testing/test-utils';
import { EdicionEstudiante } from '@/types/api';

import { InscripcionWizard } from '../inscripcion-wizard';

// Validación de la iteración 1 con el PO: el padre o la madre fallecidos se registran con sus datos, pero no
// pueden ser responsables de matrícula, no recogen al estudiante y no tienen usuario.

// jsdom no implementa scrollTo (el wizard sube al inicio al cambiar de paso).
beforeAll(() => {
  window.scrollTo = vi.fn() as unknown as typeof window.scrollTo;
});

const anio = new Date().getFullYear() + 1;

/** Inscripción guardada: padre ya registrado (responsable) y madre fallecida, solo con sus datos de identidad. */
const edicion = (): EdicionEstudiante => ({
  id: 'e1',
  matriculado: false,
  conCuenta: ['PADRE'],
  acreditacion: null,
  datos: {
    estudiante: {
      persona: {
        tipoDocumento: 'DNI',
        numeroDocumento: '78945612',
        apellidoPaterno: 'Quispe',
        apellidoMaterno: 'Rojas',
        nombres: 'Mateo',
        sexo: 'MASCULINO',
        fechaNacimiento: '2015-05-10',
        nacionalidad: 'Peruana',
        direccion: 'Av. La Marina 1234',
        distrito: 'San Miguel',
      },
      paisNacimiento: 'Chile',
      tieneInternet: true,
      lenguaMaterna: 'Castellano',
      tieneDiscapacidad: false,
      colegioProcedencia: 'I.E.I. Mis Pequeños Pasos',
      gestionProcedencia: 'Privada',
      anioAcademico: anio,
      gradoCodigo: 'PRI-1',
      formaIngreso: 'Edad normativa',
    },
    fichaMedica: {
      tipoSangre: 'O+',
      pesoKg: 22,
      estaturaM: 1.18,
      tipoSeguro: 'EsSalud',
      tieneAlergias: false,
      fiebreMaxima: '39',
      haConvulsionado: false,
      tieneEnfermedad: false,
      tomaMedicamento: false,
    },
    padre: {
      noRegistra: false,
      personaId: 'id-luis',
      persona: {
        tipoDocumento: 'DNI',
        numeroDocumento: '41234567',
        apellidoPaterno: 'Quispe',
        nombres: 'Luis',
        fechaNacimiento: '1985-01-05',
        nacionalidad: 'Peruana',
        lugarNacimiento: 'Perú',
        sexo: 'MASCULINO',
        estadoCivil: 'Casado(a)',
        gradoInstruccion: 'Superior técnica',
        celular: '987000003',
        correo: 'luis@gmail.com',
        profesion: 'Contador',
        centroTrabajo: 'Independiente',
        cargo: 'Contador',
        distrito: 'San Miguel',
        direccion: 'Av. La Marina 1234',
      },
      viveConEstudiante: true,
      tienePatriaPotestad: true,
    },
    madre: {
      noRegistra: false,
      fallecido: true,
      personaId: 'id-carla',
      persona: {
        tipoDocumento: 'DNI',
        numeroDocumento: '42345678',
        apellidoPaterno: 'Rojas',
        nombres: 'Carla',
        fechaNacimiento: '1990-11-21',
        nacionalidad: 'Peruana',
        lugarNacimiento: 'Perú',
        sexo: 'FEMENINO',
        estadoCivil: 'Casado(a)',
        gradoInstruccion: 'Secundaria',
      },
      viveConEstudiante: false,
      tienePatriaPotestad: false,
    },
    apoderado: null,
    contactos: [
      { nombreCompleto: 'Rosa', parentesco: 'Abuela', celular: '987654321' },
      { nombreCompleto: 'Pedro', parentesco: 'Tío', celular: '976543210' },
    ],
    modoSalida: 'PERSONA_AUTORIZADA',
    autorizados: [],
    responsable: 'PADRE',
    correoResponsable: 'luis@gmail.com',
    celularResponsable: '987000003',
  },
});

const renderWizard = async () =>
  renderApp(
    <InscripcionWizard
      edicion={edicion()}
      onCancelar={vi.fn()}
      onRegistrada={vi.fn()}
    />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );

const avanzar = async (boton: string, titulo: string) => {
  await userEvent.click(screen.getByRole('button', { name: boton }));
  return screen.findByText(titulo);
};

test('la madre fallecida se registra con sus datos, pero no recoge al estudiante, no es responsable ni tiene usuario', async () => {
  await renderWizard();
  await screen.findByDisplayValue('Mateo');
  await avanzar('Siguiente: Ficha médica', 'Ficha médica del estudiante');
  await avanzar(
    'Siguiente: Padres y apoderado',
    'Registra al menos al padre o a la madre',
  );

  // Paso 3: "No registra" y "Fallecida" son casillas distintas; con "Fallecida" se ven sus datos.
  expect(screen.getAllByLabelText('No registra')).toHaveLength(2);
  expect(screen.getByLabelText('Fallecida')).toBeChecked();
  expect(screen.getByDisplayValue('Carla')).toBeInTheDocument();
  // De la fallecida solo se muestran sus datos de identidad: contacto, trabajo, domicilio y exalumno, solo del padre.
  for (const etiqueta of [
    'Celular',
    'Correo electrónico',
    'Teléfono de casa (opcional)',
    'Profesión u ocupación',
    'Centro de trabajo',
    'Cargo',
    'Distrito',
    'Dirección',
    'Es exalumno(a) del colegio',
  ]) {
    expect(screen.getAllByLabelText(etiqueta)).toHaveLength(1);
  }
  // País de nacimiento, estado civil y grado de instrucción se muestran (y son obligatorios) también para ella.
  for (const etiqueta of [
    'País de nacimiento',
    'Estado civil',
    'Grado de instrucción',
  ]) {
    expect(screen.getAllByLabelText(etiqueta)).toHaveLength(2);
  }
  // Sin "Vive con el estudiante" ni patria potestad para la madre (solo los del padre).
  expect(screen.getAllByLabelText('Vive con el estudiante')).toHaveLength(1);

  // Paso 4: solo el padre aparece entre quienes pueden recoger al estudiante.
  await avanzar('Siguiente: Contactos y salida', 'Autorización de salida');
  expect(screen.getByText('✓ Luis Quispe · Padre')).toBeInTheDocument();
  expect(screen.queryByText(/Carla Rojas · Madre/)).not.toBeInTheDocument();

  // Paso 5: la madre no se puede elegir como responsable y no tendrá usuario.
  await avanzar('Siguiente: Responsable', 'Usuarios en GalilEdu');
  expect(screen.getByRole('radio', { name: /Luis Quispe/ })).toBeChecked();
  expect(
    screen.queryByRole('radio', { name: /Carla Rojas/ }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByText('Carla Rojas (Madre): fallecido(a): no tendrá usuario.'),
  ).toBeInTheDocument();
  expect(
    screen.getByText('Luis Quispe (Padre): ya tiene usuario.'),
  ).toBeInTheDocument();
});

test('el padre y la madre (aunque esté fallecida) deben ser mayores de edad: el aviso sale al salir de la fecha', async () => {
  const { container } = await renderWizard();
  await screen.findByDisplayValue('Mateo');
  await avanzar('Siguiente: Ficha médica', 'Ficha médica del estudiante');
  await avanzar(
    'Siguiente: Padres y apoderado',
    'Registra al menos al padre o a la madre',
  );

  for (const nombre of [
    'padre.persona.fechaNacimiento',
    'madre.persona.fechaNacimiento',
  ]) {
    const fecha = container.querySelector(
      `[name="${nombre}"]`,
    ) as HTMLInputElement;
    fireEvent.change(fecha, { target: { value: '2025-05-07' } });
    fireEvent.blur(fecha);
  }
  expect(await screen.findAllByText('Debe ser mayor de edad')).toHaveLength(2);
});
