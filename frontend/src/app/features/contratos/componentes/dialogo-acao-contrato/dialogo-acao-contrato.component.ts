import { Component, inject } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";

export type TomAcaoContrato = "padrao" | "sucesso" | "perigo";

export interface DadosDialogoAcaoContrato {
  titulo: string;
  mensagem: string;
  detalhe?: string;
  textoConfirmacao: string;
  icone: string;
  tom: TomAcaoContrato;
  solicitarData?: boolean;
  rotuloData?: string;
  dataInicial?: string;
  solicitarDescricao?: boolean;
  rotuloDescricao?: string;
  descricaoObrigatoria?: boolean;
}

export interface ResultadoDialogoAcaoContrato {
  confirmado: boolean;
  data: string | null;
  descricao: string | null;
}

@Component({
  selector: "app-dialogo-acao-contrato",
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: "./dialogo-acao-contrato.component.html",
  styleUrl: "./dialogo-acao-contrato.component.scss",
})
export class DialogoAcaoContratoComponent {
  protected readonly dados = inject<DadosDialogoAcaoContrato>(MAT_DIALOG_DATA);

  private readonly dialogRef =
    inject<
      MatDialogRef<DialogoAcaoContratoComponent, ResultadoDialogoAcaoContrato>
    >(MatDialogRef);

  protected readonly formulario = new FormGroup({
    data: new FormControl(
      this.dados.dataInicial ?? "",
      this.dados.solicitarData ? [Validators.required] : [],
    ),
    descricao: new FormControl(
      "",
      this.dados.descricaoObrigatoria
        ? [Validators.required, Validators.maxLength(1000)]
        : [Validators.maxLength(1000)],
    ),
  });

  protected cancelar(): void {
    this.dialogRef.close({
      confirmado: false,
      data: null,
      descricao: null,
    });
  }

  protected confirmar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const valor = this.formulario.getRawValue();
    this.dialogRef.close({
      confirmado: true,
      data: valor.data || null,
      descricao: valor.descricao?.trim() || null,
    });
  }
}
