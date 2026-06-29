import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';

import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  finalize
} from 'rxjs';

import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';

import {
  MatButtonModule
} from '@angular/material/button';

import {
  MatCheckboxModule
} from '@angular/material/checkbox';

import {
  MatFormFieldModule
} from '@angular/material/form-field';

import {
  MatIconModule
} from '@angular/material/icon';

import {
  MatInputModule
} from '@angular/material/input';

import {
  MatProgressSpinnerModule
} from '@angular/material/progress-spinner';

import {
  MatSelectModule
} from '@angular/material/select';

import {
  MatSnackBar,
  MatSnackBarModule
} from '@angular/material/snack-bar';

import {
  ErroApi
} from '../../../../shared/modelos/erro-api.model';

import {
  DadosVaga,
  TipoVaga,
  Vaga
} from '../../models/vaga.model';

import {
  VagaService
} from '../../services/vaga.service';

interface OpcaoTipoVaga {
  valor: TipoVaga;
  rotulo: string;
}

type CampoFormulario =
  | 'codigo'
  | 'tipo'
  | 'setor'
  | 'localizacao'
  | 'comprimentoMaximoMetros'
  | 'bocaMaximaMetros'
  | 'caladoMaximoMetros'
  | 'alturaMaximaMetros'
  | 'pesoMaximoKg'
  | 'possuiAgua'
  | 'possuiEnergia'
  | 'observacoes';

@Component({
  selector: 'app-vaga-formulario',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl:
    './vaga-formulario.component.html',
  styleUrl:
    './vaga-formulario.component.scss'
})
export class VagaFormularioComponent
  implements OnInit {

  private readonly formBuilder =
    inject(FormBuilder);

  private readonly vagaService =
    inject(VagaService);

  private readonly activatedRoute =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly snackBar =
    inject(MatSnackBar);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly vagaId =
    this.activatedRoute.snapshot
      .paramMap
      .get('id');

  protected readonly modoEdicao =
    signal(this.vagaId !== null);

  protected readonly carregando =
    signal(this.vagaId !== null);

  protected readonly salvando =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly tipos: OpcaoTipoVaga[] = [
    {
      valor: 'MOLHADA',
      rotulo: 'Molhada'
    },
    {
      valor: 'SECA',
      rotulo: 'Seca'
    },
    {
      valor: 'POITA',
      rotulo: 'Poita'
    },
    {
      valor: 'OUTRA',
      rotulo: 'Outra'
    }
  ];

  protected readonly formulario =
    this.formBuilder.group({
      codigo:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.required,
              Validators.maxLength(50)
            ]
          }
        ),

      tipo:
        this.formBuilder.nonNullable.control<
          TipoVaga
        >(
          'MOLHADA',
          {
            validators: [
              Validators.required
            ]
          }
        ),

      setor:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(100)
            ]
          }
        ),

      localizacao:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(200)
            ]
          }
        ),

      comprimentoMaximoMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.required,
              Validators.min(0.01)
            ]
          }
        ),

      bocaMaximaMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.required,
              Validators.min(0.01)
            ]
          }
        ),

      caladoMaximoMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(0.01)
            ]
          }
        ),

      alturaMaximaMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(0.01)
            ]
          }
        ),

      pesoMaximoKg:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(0.01)
            ]
          }
        ),

      possuiAgua:
        this.formBuilder.nonNullable.control(
          false,
          {
            validators: [
              Validators.required
            ]
          }
        ),

      possuiEnergia:
        this.formBuilder.nonNullable.control(
          false,
          {
            validators: [
              Validators.required
            ]
          }
        ),

      observacoes:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(2000)
            ]
          }
        )
    });

  constructor() {
    this.limparErrosApiAoEditar();
  }

  ngOnInit(): void {
    if (this.vagaId) {
      this.carregarDadosEdicao(
        this.vagaId
      );
    }
  }

  protected salvar(): void {
    this.mensagemErro.set(null);
    this.limparErrosApi();

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();

      return;
    }

    const valores =
      this.formulario.getRawValue();

    const dados: DadosVaga = {
      codigo:
        valores.codigo
          .trim()
          .toUpperCase(),

      tipo:
        valores.tipo,

      setor:
        this.textoOuNull(
          valores.setor
        ),

      localizacao:
        this.textoOuNull(
          valores.localizacao
        ),

      comprimentoMaximoMetros:
        this.numeroObrigatorio(
          valores.comprimentoMaximoMetros
        ),

      bocaMaximaMetros:
        this.numeroObrigatorio(
          valores.bocaMaximaMetros
        ),

      caladoMaximoMetros:
        this.numeroOuNull(
          valores.caladoMaximoMetros
        ),

      alturaMaximaMetros:
        this.numeroOuNull(
          valores.alturaMaximaMetros
        ),

      pesoMaximoKg:
        this.numeroOuNull(
          valores.pesoMaximoKg
        ),

      possuiAgua:
        valores.possuiAgua,

      possuiEnergia:
        valores.possuiEnergia,

      observacoes:
        this.textoOuNull(
          valores.observacoes
        )
    };

    this.salvando.set(true);

    const requisicao =
      this.vagaId
        ? this.vagaService.atualizar(
            this.vagaId,
            dados
          )
        : this.vagaService.criar(
            dados
          );

    requisicao
      .pipe(
        finalize(() =>
          this.salvando.set(false)
        )
      )
      .subscribe({
        next: () => {
          this.snackBar.open(
            this.modoEdicao()
              ? 'Vaga atualizada com sucesso.'
              : 'Vaga cadastrada com sucesso.',
            'Fechar',
            {
              duration: 3500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );

          void this.router.navigateByUrl(
            '/vagas'
          );
        },

        error: (
          erro: HttpErrorResponse
        ) => {
          this.tratarErro(erro);
        }
      });
  }

  protected cancelar(): void {
    void this.router.navigateByUrl('/vagas');
  }

  protected normalizarCodigo(): void {
    const controle =
      this.formulario.controls.codigo;

    controle.setValue(
      controle.value
        .trim()
        .replace(/\s+/g, ' ')
        .toUpperCase(),
      {
        emitEvent: false
      }
    );
  }

  protected erroApi(
    campo: CampoFormulario
  ): string | null {
    return this.formulario.controls[
      campo
    ].getError('api') ?? null;
  }

  private carregarDadosEdicao(
    vagaId: string
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    this.vagaService
      .buscarPorId(vagaId)
      .pipe(
        finalize(() =>
          this.carregando.set(false)
        )
      )
      .subscribe({
        next: (vaga) => {
          this.preencherFormulario(vaga);
        },

        error: () => {
          this.mensagemErro.set(
            'Nao foi possivel carregar os dados da vaga.'
          );
        }
      });
  }

  private preencherFormulario(
    vaga: Vaga
  ): void {
    this.formulario.patchValue({
      codigo:
        vaga.codigo,

      tipo:
        vaga.tipo,

      setor:
        vaga.setor ?? '',

      localizacao:
        vaga.localizacao ?? '',

      comprimentoMaximoMetros:
        vaga.comprimentoMaximoMetros,

      bocaMaximaMetros:
        vaga.bocaMaximaMetros,

      caladoMaximoMetros:
        vaga.caladoMaximoMetros,

      alturaMaximaMetros:
        vaga.alturaMaximaMetros,

      pesoMaximoKg:
        vaga.pesoMaximoKg,

      possuiAgua:
        vaga.possuiAgua,

      possuiEnergia:
        vaga.possuiEnergia,

      observacoes:
        vaga.observacoes ?? ''
    });
  }

  private tratarErro(
    erro: HttpErrorResponse
  ): void {
    const resposta =
      erro.error as ErroApi | null;

    if (resposta?.errosCampos?.length) {
      this.aplicarErrosCampos(
        resposta.errosCampos
      );
    }

    if (
      resposta?.codigo
      === 'CONFLITO_DADOS'
    ) {
      this.adicionarErroApi(
        'codigo',
        resposta.mensagem
      );

      this.mensagemErro.set(
        resposta.mensagem
      );

      return;
    }

    const mapaCodigos:
      Record<string, CampoFormulario> = {
        CODIGO_OBRIGATORIO: 'codigo',
        TIPO_VAGA_OBRIGATORIO: 'tipo',
        COMPRIMENTO_INVALIDO:
          'comprimentoMaximoMetros',
        BOCA_INVALIDA:
          'bocaMaximaMetros',
        CALADO_INVALIDO:
          'caladoMaximoMetros',
        ALTURA_INVALIDA:
          'alturaMaximaMetros',
        PESO_INVALIDO:
          'pesoMaximoKg'
      };

    const campo =
      resposta?.codigo
        ? mapaCodigos[resposta.codigo]
        : undefined;

    if (campo && resposta?.mensagem) {
      this.adicionarErroApi(
        campo,
        resposta.mensagem
      );

      this.mensagemErro.set(
        'Revise os campos destacados.'
      );

      return;
    }

    if (erro.status === 0) {
      this.mensagemErro.set(
        'Nao foi possivel conectar ao servidor.'
      );

      return;
    }

    if (erro.status === 404) {
      this.mensagemErro.set(
        'A vaga nao foi encontrada.'
      );

      return;
    }

    if (resposta?.mensagem) {
      this.mensagemErro.set(
        resposta.mensagem
      );

      return;
    }

    this.mensagemErro.set(
      'Nao foi possivel salvar a vaga.'
    );
  }

  private aplicarErrosCampos(
    errosCampos: {
      campo: string;
      mensagem: string;
    }[]
  ): void {
    for (const erroCampo of errosCampos) {
      const controle =
        this.formulario.get(
          erroCampo.campo
        );

      if (!controle) {
        continue;
      }

      controle.setErrors({
        ...controle.errors,
        api: erroCampo.mensagem
      });
    }
  }

  private adicionarErroApi(
    campo: CampoFormulario,
    mensagem: string
  ): void {
    const controle =
      this.formulario.controls[campo];

    controle.setErrors({
      ...controle.errors,
      api: mensagem
    });
  }

  private limparErrosApiAoEditar(): void {
    const controles =
      Object.values(
        this.formulario.controls
      ) as AbstractControl[];

    controles.forEach((controle) => {
      controle.valueChanges
        .pipe(
          takeUntilDestroyed(
            this.destroyRef
          )
        )
        .subscribe(() => {
          this.removerErroApi(controle);
        });
    });
  }

  private limparErrosApi(): void {
    const controles =
      Object.values(
        this.formulario.controls
      ) as AbstractControl[];

    controles.forEach((controle) =>
      this.removerErroApi(controle)
    );
  }

  private removerErroApi(
    controle: AbstractControl
  ): void {
    const erros = controle.errors;

    if (!erros?.['api']) {
      return;
    }

    const {
      api: _api,
      ...outrosErros
    } = erros;

    controle.setErrors(
      Object.keys(outrosErros).length
        ? outrosErros
        : null
    );
  }

  private textoOuNull(
    texto: string
  ): string | null {
    const valor = texto.trim();

    return valor || null;
  }

  private numeroObrigatorio(
    valor: number | null
  ): number {
    return Number(valor);
  }

  private numeroOuNull(
    valor: number | null
  ): number | null {
    if (
      valor === null
      || valor === undefined
      || Number.isNaN(Number(valor))
    ) {
      return null;
    }

    return Number(valor);
  }
}
