import { Meta, StoryObj } from '@storybook/react';
import { Globe } from 'lucide-react';

import { Checkbox, Radio } from './checkbox';
import { CheckboxCard, RadioCard } from './option-card';

const meta: Meta<typeof Checkbox> = {
  title: 'Components/Form/Checkbox y Radio',
  component: Checkbox,
};

export default meta;
type Story = StoryObj<typeof Checkbox>;

export const Casillas: Story = {
  render: () => (
    <div className="flex flex-col gap-3">
      <Checkbox label="Es exalumno(a) del colegio" />
      <Checkbox label="Marcada" defaultChecked />
      <Checkbox label="Con ayuda" helperText="Texto de ayuda" />
      <Checkbox label="Con error" invalid />
      <Checkbox label="Deshabilitada" disabled defaultChecked />
    </div>
  ),
};

export const Radios: Story = {
  render: () => (
    <div className="flex flex-col gap-3">
      <Radio
        name="salida"
        label="Recogido por persona autorizada"
        defaultChecked
      />
      <Radio name="salida" label="Puede salir solo(a)" />
      <Radio name="salida" label="Deshabilitado" disabled />
    </div>
  ),
};

export const Tarjetas: Story = {
  render: () => (
    <div className="grid max-w-2xl grid-cols-2 gap-3">
      <RadioCard
        name="plan"
        label="Mensual"
        helperText="10 cuotas"
        defaultChecked
      />
      <RadioCard
        name="plan"
        label="Anual"
        helperText="Un solo pago"
        controlPosition="right"
      />
      <RadioCard
        name="plan"
        label="Con ícono"
        helperText="Texto de ayuda"
        icon={<Globe />}
      />
      <RadioCard
        name="plan"
        label="Deshabilitada"
        helperText="Texto de ayuda"
        disabled
      />
      <CheckboxCard label="Matemática" defaultChecked />
      <CheckboxCard label="Comunicación" icon={<Globe />} />
    </div>
  ),
};
