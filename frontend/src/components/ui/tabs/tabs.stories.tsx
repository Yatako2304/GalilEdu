import { Meta, StoryObj } from '@storybook/react';
import * as React from 'react';

import { Tabs, type TabsProps } from './tabs';

const Demo = (props: Omit<TabsProps<string>, 'value' | 'onValueChange'>) => {
  const [valor, setValor] = React.useState(props.items[0].value);
  return <Tabs {...props} value={valor} onValueChange={setValor} />;
};

const meta: Meta<typeof Demo> = {
  title: 'Components/Tabs',
  component: Demo,
  args: {
    'aria-label': 'Secciones',
    items: [
      { value: 'familia', label: 'Familia' },
      { value: 'datos', label: 'Datos personales' },
      { value: 'salud', label: 'Ficha médica' },
      {
        value: 'pagos',
        label: 'Pagos',
        disabled: true,
        title: 'Disponible pronto',
      },
    ],
  },
};

export default meta;
type Story = StoryObj<typeof Demo>;

export const Underline: Story = { args: { variant: 'underline' } };
export const UnderlineAnchoCompleto: Story = {
  args: { variant: 'underline', fullWidth: true },
};
export const Pill: Story = { args: { variant: 'pill' } };
export const PillMediana: Story = { args: { variant: 'pill', size: 'md' } };
