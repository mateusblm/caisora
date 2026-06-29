import {
  Component,
  inject
} from '@angular/core';

import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef
} from '@angular/material/dialog';

import {
  MatButtonModule
} from '@angular/material/button';

import {
  MatIconModule
} from '@angular/material/icon';

export type TomDialogoConfirmacao =
  | 'padrao'
  | 'perigo'
  | 'sucesso';

export interface DadosDialogoConfirmacao {
  titulo: string;
  mensagem: string;
  detalhe?: string;
  textoConfirmacao: string;
  icone: string;
  tom: TomDialogoConfirmacao;
}

@Component({
  selector: 'app-dialogo-confirmacao',
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl:
    './dialogo-confirmacao.component.html',
  styleUrl:
    './dialogo-confirmacao.component.scss'
})
export class DialogoConfirmacaoComponent {

  protected readonly dados =
    inject<DadosDialogoConfirmacao>(
      MAT_DIALOG_DATA
    );

  private readonly dialogRef =
    inject<
      MatDialogRef<
        DialogoConfirmacaoComponent,
        boolean
      >
    >(MatDialogRef);

  protected cancelar(): void {
    this.dialogRef.close(false);
  }

  protected confirmar(): void {
    this.dialogRef.close(true);
  }
}