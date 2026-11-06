import * as React from 'react';

import {
  fireEvent,
  rtlRender,
  screen,
  userEvent,
  within,
} from '@/testing/test-utils';

import { Button } from '../button';
import { Dialog, DialogContent, DialogTitle } from '../dialog';
import { Select } from '../form';

// Figma > Componentes > Select + Dropdown Item List. La lista se dibuja en el <body> con `position: fixed`: flota sobre
// cualquier contenedor (un modal con `overflow`) sin empujarlo ni recortarse.

const OPCIONES = [
  { label: 'Aula A-401', value: 'a401' },
  { label: 'Aula A-502', value: 'a502' },
  { label: 'Laboratorio', value: 'lab' },
];

const Campo = ({ valorInicial = '' }: { valorInicial?: string }) => {
  const [valor, setValor] = React.useState(valorInicial);
  return (
    <Select
      label="Ambiente"
      value={valor}
      onChange={(e) => setValor(e.target.value)}
      options={OPCIONES}
    />
  );
};

/** Pone el campo en un lugar de la pantalla: arriba (cabe hacia abajo) o pegado al fondo (no cabe). */
const colocarCampo = (arriba: number, alto = 44) =>
  vi
    .spyOn(HTMLElement.prototype, 'getBoundingClientRect')
    .mockImplementation(function (this: HTMLElement) {
      const esElCampo = this.getAttribute('role') === 'combobox';
      return {
        x: 40,
        y: esElCampo ? arriba : 0,
        left: 40,
        top: esElCampo ? arriba : 0,
        right: 340,
        bottom: esElCampo ? arriba + alto : 0,
        width: 300,
        height: esElCampo ? alto : 0,
        toJSON: () => ({}),
      } as DOMRect;
    });

afterEach(() => vi.restoreAllMocks());

// Cada Select tiene un <select> nativo (oculto) y un botón con rol combobox que es el que se ve; las <option> del
// nativo también tienen rol option, así que las opciones se buscan dentro de la lista que se abre.
const boton = (raiz: HTMLElement = document.body) =>
  within(raiz)
    .getAllByRole('combobox')
    .find((e) => e.tagName === 'BUTTON') as HTMLElement;

const opcion = (nombre: string) =>
  within(screen.getByRole('listbox')).getByRole('option', { name: nombre });

test('sin valor muestra solo la etiqueta (el campo vacío), y elegir una opción llena el campo', async () => {
  rtlRender(<Campo />);

  const campo = boton();
  expect(campo).toHaveTextContent('');
  expect(screen.getByText('Ambiente')).toBeInTheDocument();

  await userEvent.click(campo);
  await userEvent.click(opcion('Aula A-502'));

  expect(campo).toHaveTextContent('Aula A-502');
  expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
});

test('la lista cuelga del body, con posición fija, pegada al campo y sin escalar su ancho', async () => {
  colocarCampo(100);
  rtlRender(<Campo />);

  await userEvent.click(boton());
  const lista = screen.getByRole('listbox');

  expect(lista.parentElement).toBe(document.body);
  expect(lista).toHaveClass('fixed');
  expect(lista).toHaveStyle({ left: '40px', width: '300px', top: '148px' });
  expect(getComputedStyle(lista).bottom).toBe('');
  // Por encima de los modales (z-50) y respondiendo aunque el <body> tenga los eventos de puntero apagados.
  expect(lista).toHaveClass('z-[60]');
  expect(lista).toHaveStyle({ pointerEvents: 'auto' });
});

test('si no cabe abajo y arriba hay más sitio, se abre hacia arriba', async () => {
  // Ventana de 768 px de alto: el campo está a 700 px, sin sitio para tres opciones debajo.
  colocarCampo(700);
  rtlRender(<Campo />);

  await userEvent.click(boton());
  const lista = screen.getByRole('listbox');

  expect(getComputedStyle(lista).top).toBe('');
  // bottom = alto de ventana - arriba del campo + separación
  expect(getComputedStyle(lista).bottom).toBe(
    `${window.innerHeight - 700 + 4}px`,
  );
});

test('el alto de la lista nunca pasa del espacio disponible ni de 16 rem', async () => {
  colocarCampo(700);
  const muchas = Array.from({ length: 30 }, (_, i) => ({
    label: `Opción ${i + 1}`,
    value: String(i),
  }));
  rtlRender(<Select label="Ambiente" options={muchas} />);

  await userEvent.click(boton());
  const maxHeight = Number(
    getComputedStyle(screen.getByRole('listbox')).maxHeight.replace('px', ''),
  );

  expect(maxHeight).toBeLessThanOrEqual(256);
  expect(maxHeight).toBeGreaterThan(0);
});

test('un clic fuera la cierra y un clic en una opción no cuenta como "fuera"', async () => {
  rtlRender(
    <>
      <Campo />
      <button>Otro</button>
    </>,
  );

  await userEvent.click(boton());
  expect(screen.getByRole('listbox')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Otro' }));
  expect(screen.queryByRole('listbox')).not.toBeInTheDocument();

  await userEvent.click(boton());
  await userEvent.click(opcion('Laboratorio'));
  expect(boton()).toHaveTextContent('Laboratorio');
});

test('el teclado sigue funcionando: flechas para moverse y Enter para elegir', async () => {
  rtlRender(<Campo />);

  boton().focus();
  await userEvent.keyboard('{ArrowDown}{ArrowDown}{Enter}');

  expect(boton()).toHaveTextContent('Aula A-502');
});

test('dentro de un modal: la lista queda fuera del diálogo, elegir no lo cierra y los clics responden', async () => {
  rtlRender(
    <Dialog open>
      <DialogContent className="overflow-hidden">
        <DialogTitle>Asignar bloque horario</DialogTitle>
        <Campo />
        <Button>Aceptar</Button>
      </DialogContent>
    </Dialog>,
  );

  const dialogo = screen.getByRole('dialog');
  await userEvent.click(boton(dialogo));
  const lista = screen.getByRole('listbox');

  // No cuelga del diálogo: su `overflow` no la recorta ni le crece el alto.
  expect(dialogo.contains(lista)).toBe(false);

  await userEvent.click(opcion('Aula A-401'));

  // El modal sigue abierto y el campo tiene el valor elegido.
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(boton(dialogo)).toHaveTextContent('Aula A-401');
  expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
});

test('un Select deshabilitado no abre la lista', async () => {
  rtlRender(<Select label="Docente" disabled options={OPCIONES} value="" />);

  await userEvent.click(boton());

  expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
});

test('la rueda y el gesto táctil sobre la lista no suben hasta el documento (un modal de Radix los bloquearía)', async () => {
  const alDocumento = vi.fn();
  document.addEventListener('wheel', alDocumento);
  document.addEventListener('touchmove', alDocumento);
  rtlRender(<Campo />);

  await userEvent.click(boton());
  fireEvent.wheel(screen.getByRole('listbox'), { deltaY: 120 });
  fireEvent.touchMove(screen.getByRole('listbox'));
  expect(alDocumento).not.toHaveBeenCalled();

  // Fuera de la lista el comportamiento no cambia.
  fireEvent.wheel(document.body, { deltaY: 120 });
  expect(alDocumento).toHaveBeenCalledTimes(1);

  document.removeEventListener('wheel', alDocumento);
  document.removeEventListener('touchmove', alDocumento);
});
