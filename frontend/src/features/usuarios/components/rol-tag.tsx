import { Tag, TagProps } from '@/components/ui/tag';
import { ROLES } from '@/config/roles';
import { cn } from '@/utils/cn';

// Figma > Usuarios > columna "Rol": píldoras con color por rol.
const COLOR: Record<ROLES, TagProps['color']> = {
  [ROLES.ADMINISTRADOR]: 'error',
  [ROLES.PERSONAL_ADMINISTRATIVO]: 'success',
  [ROLES.COORDINADOR]: 'primary',
  [ROLES.APODERADO]: 'warning',
  [ROLES.DOCENTE]: 'info',
  [ROLES.TUTOR]: 'primary',
  [ROLES.ESTUDIANTE]: 'neutral',
};

export const NOMBRE_ROL: Record<ROLES, string> = {
  [ROLES.ADMINISTRADOR]: 'Administrador',
  [ROLES.PERSONAL_ADMINISTRATIVO]: 'Personal administrativo',
  [ROLES.COORDINADOR]: 'Coordinador',
  [ROLES.APODERADO]: 'Apoderado',
  [ROLES.DOCENTE]: 'Docente',
  [ROLES.TUTOR]: 'Tutor',
  [ROLES.ESTUDIANTE]: 'Estudiante',
};

export const RolTag = ({ rol }: { rol: ROLES }) => (
  <Tag color={COLOR[rol]} size="md" className="rounded-full">
    {NOMBRE_ROL[rol]}
  </Tag>
);

/** Primer rol + contador "+N" cuando el usuario tiene varios (Figma muestra un "+"). */
export const RolesCelda = ({ roles }: { roles: ROLES[] }) => {
  const [primero, ...resto] = roles;
  if (!primero) return null;
  return (
    <div className="flex items-center gap-2">
      <RolTag rol={primero} />
      {resto.length > 0 && (
        <span
          title={resto.map((r) => NOMBRE_ROL[r]).join(', ')}
          className={cn(
            'inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-surface-tertiary px-2 text-caption-1 text-content-tertiary',
          )}
        >
          +{resto.length}
        </span>
      )}
    </div>
  );
};
