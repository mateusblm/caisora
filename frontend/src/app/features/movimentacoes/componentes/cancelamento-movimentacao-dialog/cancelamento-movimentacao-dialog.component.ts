import { Component, inject } from '@angular/core';
import {
  FormControl,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface DadosCancelamentoMovimentacao {
  embarcacaoNome: string;
}

@Component({
  selector: 'app-cancelamento-movimentacao-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl:
    './cancelamento-movimentacao-dialog.component.html',
  styleUrl:
    './cancelamento-movimentacao-dialog.component.scss'
})
export class CancelamentoMovimentacaoDialogComponent {

  protected readonly dados =
    inject<DadosCancelamentoMovimentacao>(
      MAT_DIALOG_DATA
    );

  private readonly dialogRef = inject<
    MatDialogRef<
      CancelamentoMovimentacaoDialogComponent,
      string | null
    >
  >(MatDialogRef);

  protected readonly motivo =
    new FormControl(
      '',
      {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.maxLength(1000)
        ]
      }
    );

  protected cancelar(): void {
    this.dialogRef.close(null);
  }

  protected confirmar(): void {
    const motivo = this.motivo.value.trim();

    if (!motivo) {
      this.motivo.markAsTouched();
      return;
    }

    this.dialogRef.close(motivo);
  }
}
