import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { Notifications } from '@/components/ui/notifications';
import { rtlRender } from '@/testing/test-utils';

// Los avisos de error (p. ej. "Ya existe la sección A") deben verse aunque haya un modal abierto: el modal usa z-50.
test('la capa de avisos queda por encima de la de los modales', () => {
  rtlRender(
    <>
      <Notifications />
      <Dialog open>
        <DialogContent>
          <DialogTitle>Modal</DialogTitle>
        </DialogContent>
      </Dialog>
    </>,
  );

  const avisos = document.querySelector('[aria-live="assertive"]');
  expect(avisos).toHaveClass('z-[60]');
  expect(document.querySelector('[role="dialog"]')).toHaveClass('z-50');
});
