import { CircleCheck } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { IconBadge } from '@/components/ui/icon-badge';

export type TipoAvisoContrasena = 'restablecida' | 'actualizada';

const TEXTOS: Record<TipoAvisoContrasena, { titulo: string; texto: string }> = {
  // HU-4 · Figma > Iniciar sesión con el modal «Contraseña restablecida correctamente».
  restablecida: {
    titulo: 'Contraseña restablecida correctamente',
    texto:
      'Su contraseña ha sido actualizada correctamente. Debe volver a ingresar sus credenciales para acceder al sistema.',
  },
  // HU-5 · Figma > Iniciar sesión con el modal «Contraseña actualizada correctamente».
  actualizada: {
    titulo: 'Contraseña actualizada correctamente',
    texto:
      'Su contraseña ha sido actualizada correctamente. Se cerró la sesión en todos tus dispositivos. Inicia sesión nuevamente con tu nueva contraseña.',
  },
};

/** Aviso sobre el login después de restablecer (HU-4) o cambiar (HU-5) la contraseña. */
export const AvisoContrasena = ({
  tipo,
  onClose,
}: {
  tipo: TipoAvisoContrasena | null;
  onClose: () => void;
}) => {
  const textos = tipo ? TEXTOS[tipo] : null;
  return (
    <Dialog open={!!textos} onOpenChange={(abierto) => !abierto && onClose()}>
      {textos && (
        <DialogContent className="max-w-[448px] gap-0 rounded-xl border-stroke-tertiary bg-white p-0">
          <div className="flex flex-col gap-4 px-6 pb-6 pt-8">
            <IconBadge color="success" size="md">
              <CircleCheck aria-hidden="true" />
            </IconBadge>
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              {textos.titulo}
            </DialogTitle>
            <DialogDescription className="text-body-4 text-content-secondary">
              {textos.texto}
            </DialogDescription>
          </div>
          <div className="border-t border-stroke-tertiary px-6 py-4">
            <Button variant="secondary" className="w-full" onClick={onClose}>
              Continuar
            </Button>
          </div>
        </DialogContent>
      )}
    </Dialog>
  );
};
