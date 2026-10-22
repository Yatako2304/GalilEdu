import { Meta, StoryObj } from '@storybook/react';
import { Users } from 'lucide-react';

import { ShortcutCard } from './shortcut-card';

const meta: Meta<typeof ShortcutCard> = {
  title: 'Components/ShortcutCard',
  component: ShortcutCard,
  args: {
    icon: <Users />,
    title: 'Usuarios',
    description: 'Registra y administra cuentas',
    to: '/',
    className: 'w-[272px]',
  },
};

export default meta;
type Story = StoryObj<typeof ShortcutCard>;

export const Default: Story = {};
