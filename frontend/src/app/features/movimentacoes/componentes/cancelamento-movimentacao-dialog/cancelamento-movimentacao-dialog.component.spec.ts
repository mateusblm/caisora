import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import {
  MAT_DIALOG_DATA,
  MatDialogRef
} from '@angular/material/dialog';
import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';
import { vi } from 'vitest';

import { CancelamentoMovimentacaoDialogComponent } from './cancelamento-movimentacao-dialog.component';

describe(
  'CancelamentoMovimentacaoDialogComponent',
  () => {
    let fixture: ComponentFixture<
      CancelamentoMovimentacaoDialogComponent
    >;

    const dialogRefMock = {
      close: vi.fn()
    };

    beforeEach(async () => {
      vi.clearAllMocks();

      await TestBed
        .configureTestingModule({
          imports: [
            CancelamentoMovimentacaoDialogComponent
          ],
          providers: [
            provideNoopAnimations(),
            {
              provide: MAT_DIALOG_DATA,
              useValue: {
                embarcacaoNome: 'Aurora'
              }
            },
            {
              provide: MatDialogRef,
              useValue: dialogRefMock
            }
          ]
        })
        .compileComponents();

      fixture = TestBed.createComponent(
        CancelamentoMovimentacaoDialogComponent
      );

      fixture.detectChanges();
    });

    it(
      'deve fechar com o motivo informado',
      () => {
        const componente =
          fixture.componentInstance;

        componente['motivo'].setValue(
          'Solicitação do cliente'
        );

        componente['confirmar']();

        expect(dialogRefMock.close)
          .toHaveBeenCalledWith(
            'Solicitação do cliente'
          );
      }
    );

    it(
      'não deve confirmar motivo vazio',
      () => {
        fixture.componentInstance[
          'confirmar'
        ]();

        expect(dialogRefMock.close)
          .not.toHaveBeenCalled();
      }
    );

    it(
      'deve fechar sem motivo ao voltar',
      () => {
        fixture.componentInstance[
          'cancelar'
        ]();

        expect(dialogRefMock.close)
          .toHaveBeenCalledWith(null);
      }
    );
  }
);
