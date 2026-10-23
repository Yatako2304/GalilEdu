import { Meta, StoryObj } from '@storybook/react';
import * as React from 'react';

import { Calendar } from './calendar';
import { DatePicker } from './date-picker';

const meta: Meta<typeof DatePicker> = {
  title: 'Components/DatePicker',
  component: DatePicker,
};

export default meta;
type Story = StoryObj<typeof DatePicker>;

const Demo = ({
  min,
  max,
  conError,
  size,
}: {
  min?: string;
  max?: string;
  conError?: boolean;
  size?: 'md' | 'sm';
}) => {
  const [fecha, setFecha] = React.useState<string | null>('2026-03-15');
  return (
    <div className="max-w-xs">
      <DatePicker
        label="Fecha de nacimiento"
        value={fecha}
        onChange={setFecha}
        min={min}
        max={max}
        size={size}
        error={
          conError
            ? { type: 'required', message: 'Elige una fecha' }
            : undefined
        }
      />
    </div>
  );
};

const DemoCalendario = () => {
  const [fecha, setFecha] = React.useState('2026-03-15');
  return <Calendar value={fecha} onChange={setFecha} />;
};

export const Campo: Story = { render: () => <Demo /> };
export const ConLimites: Story = {
  render: () => <Demo min="2026-03-05" max="2026-03-25" />,
};
export const ConError: Story = { render: () => <Demo conError /> };
// Compacto (no está en Figma): para modales con varios campos, donde el calendario de Figma queda muy grande.
export const Compacto: Story = { render: () => <Demo size="sm" /> };
export const SoloCalendario: Story = { render: () => <DemoCalendario /> };
