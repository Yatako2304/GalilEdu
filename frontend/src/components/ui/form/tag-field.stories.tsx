import { Meta, StoryObj } from '@storybook/react';
import * as React from 'react';

import { TagField, type TagFieldProps } from './tag-field';

const Demo = (props: Omit<TagFieldProps, 'value' | 'onChange'>) => {
  const [valor, setValor] = React.useState(['Matemática', 'Comunicación']);
  return (
    <div className="max-w-md">
      <TagField {...props} value={valor} onChange={setValor} />
    </div>
  );
};

const meta: Meta<typeof Demo> = {
  title: 'Components/Form/TagField',
  component: Demo,
  args: {
    label: 'Cursos',
    placeholder: 'Escribe y presiona Enter',
    hint: 'Presiona Enter o coma para agregar',
  },
};

export default meta;
type Story = StoryObj<typeof Demo>;

export const Default: Story = {};
export const ConError: Story = {
  args: { error: { type: 'required', message: 'Agrega al menos un curso' } },
};
export const Deshabilitado: Story = { args: { disabled: true } };
