import { Meta, StoryObj } from '@storybook/react';
import * as React from 'react';
import { z } from 'zod';

import { Button } from '../button';

import { Form } from './form';
import { FormDrawer } from './form-drawer';
import { Input } from './input';
import { Select } from './select';
import { Textarea } from './textarea';

const MyForm = ({ hideSubmit = false }: { hideSubmit?: boolean }) => {
  return (
    <Form
      onSubmit={async (values) => {
        alert(JSON.stringify(values, null, 2));
      }}
      schema={z.object({
        nombre: z.string().min(1, 'Requerido'),
        descripcion: z.string().min(1, 'Requerido'),
        grado: z.string().min(1, 'Requerido'),
      })}
      id="my-form"
    >
      {({ register, formState }) => (
        <>
          <Input
            label="Nombre"
            error={formState.errors['nombre']}
            registration={register('nombre')}
          />
          <Textarea
            label="Descripción"
            error={formState.errors['descripcion']}
            registration={register('descripcion')}
          />
          <Select
            label="Grado"
            error={formState.errors['grado']}
            registration={register('grado')}
            options={['1', '2', '3', '4', '5'].map((grado) => ({
              label: `${grado}.° de secundaria`,
              value: grado,
            }))}
          />

          {!hideSubmit && (
            <div>
              <Button type="submit" className="w-full">
                Guardar
              </Button>
            </div>
          )}
        </>
      )}
    </Form>
  );
};

const meta: Meta = {
  component: MyForm,
};

export default meta;

type Story = StoryObj<typeof MyForm>;

export const Default: Story = {
  render: () => <MyForm />,
};

export const AsFormDrawer: Story = {
  render: () => (
    <FormDrawer
      triggerButton={<Button>Abrir formulario</Button>}
      isDone={true}
      title="Mi formulario"
      submitButton={
        <Button form="my-form" type="submit">
          Guardar
        </Button>
      }
    >
      <MyForm hideSubmit />
    </FormDrawer>
  ),
};

const ConContador = () => {
  const [descripcion, setDescripcion] = React.useState(
    'Ficha de matrícula completada con los campos correspondientes y firma del apoderado',
  );
  return (
    <div className="w-96">
      <Textarea
        label="Descripción del documento (opcional)"
        maxLength={100}
        showCount
        value={descripcion}
        onChange={(e) => setDescripcion(e.target.value)}
      />
    </div>
  );
};

// Figma > Componentes > Text Area con contador de caracteres ("0/100").
export const TextareaConContador: Story = {
  render: () => <ConContador />,
};
