import { Meta, StoryObj } from '@storybook/react';
import * as React from 'react';

import { SegmentedControl } from './segmented-control';

type Estado = 'PRESENTE' | 'TARDANZA' | 'FALTA';

const opciones = [
  { value: 'PRESENTE' as const, label: 'Presente', color: 'success' as const },
  { value: 'TARDANZA' as const, label: 'Tardanza', color: 'warning' as const },
  { value: 'FALTA' as const, label: 'Falta', color: 'error' as const },
];

const Selector = ({
  inicial,
  invalid,
}: {
  inicial: Estado | null;
  invalid?: boolean;
}) => {
  const [valor, setValor] = React.useState<Estado | null>(inicial);
  return (
    <SegmentedControl
      label="Asistencia de Valeria Acevedo"
      options={opciones}
      value={valor}
      onChange={setValor}
      invalid={invalid}
    />
  );
};

const meta: Meta<typeof Selector> = {
  title: 'Components/SegmentedControl',
  component: Selector,
  args: { inicial: null },
};

export default meta;
type Story = StoryObj<typeof Selector>;

export const SinMarcar: Story = {};
export const Presente: Story = { args: { inicial: 'PRESENTE' } };
export const Tardanza: Story = { args: { inicial: 'TARDANZA' } };
export const Falta: Story = { args: { inicial: 'FALTA' } };
export const SinMarcarConError: Story = { args: { invalid: true } };
