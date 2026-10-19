import { cn } from '@/utils/cn';

// Figma > Componentes > User (círculo con iniciales, ej. "VS")
type AvatarProps = {
  name: string;
  size?: 'sm' | 'md';
  className?: string;
};

export const getInitials = (name: string) =>
  name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');

export const Avatar = ({ name, size = 'md', className }: AvatarProps) => (
  <span
    className={cn(
      'inline-flex shrink-0 items-center justify-center rounded-full bg-deep-blue-100 font-semibold text-content-accent-primary',
      size === 'md' ? 'size-10 text-body-3' : 'size-8 text-body-4',
      className,
    )}
    aria-hidden="true"
  >
    {getInitials(name)}
  </span>
);
