import { Meta, StoryObj } from '@storybook/react';
import * as React from 'react';

import { Rating } from './rating';

const meta: Meta<typeof Rating> = {
  title: 'Components/Rating',
  component: Rating,
  args: { value: 3 },
};

export default meta;
type Story = StoryObj<typeof Rating>;

const DemoEditable = () => {
  const [valor, setValor] = React.useState(0);
  return <Rating value={valor} onChange={setValor} size="lg" />;
};

export const Pequena: Story = {};
export const Grande: Story = { args: { size: 'lg' } };
export const Editable: Story = { render: () => <DemoEditable /> };
