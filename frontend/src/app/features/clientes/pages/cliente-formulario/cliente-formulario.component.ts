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
  ValidationErrors,
  ValidatorFn,
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
  DadosCliente,
  TipoPessoa
} from '../../models/cliente.model';

import {
  ClienteService
} from '../../services/cliente.service';

import {
  ErroApi
} from '../../../../shared/modelos/erro-api.model';

@Component({
  selector: 'app-cliente-formulario',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl: './cliente-formulario.component.html',
  styleUrl: './cliente-formulario.component.scss'
})
export class ClienteFormularioComponent
  implements OnInit {

  private readonly formBuilder =
    inject(FormBuilder);

  private readonly clienteService =
    inject(ClienteService);

  private readonly activatedRoute =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly snackBar =
    inject(MatSnackBar);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly clienteId =
    this.activatedRoute.snapshot
      .paramMap
      .get('id');

  protected readonly modoEdicao =
    signal(this.clienteId !== null);

  protected readonly carregando =
    signal(false);

  protected readonly salvando =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly formulario =
    this.formBuilder.nonNullable.group({
      tipoPessoa:
        this.formBuilder.nonNullable.control<TipoPessoa>(
          'FISICA',
          {
            validators: [
              Validators.required
            ]
          }
        ),

      nome: [
        '',
        [
          Validators.required,
          Validators.maxLength(150)
        ]
      ],

      razaoSocial: [
        '',
        [
          Validators.maxLength(200)
        ]
      ],

      cpfCnpj: [
        '',
        [
          Validators.required,
          Validators.maxLength(18),
          this.criarValidadorDocumento('FISICA')
        ]
      ],

      email: [
        '',
        [
          Validators.email,
          Validators.maxLength(150)
        ]
      ],

      telefone: [
        '',
        [
          Validators.maxLength(20)
        ]
      ],

      celular: [
        '',
        [
          Validators.maxLength(20)
        ]
      ],

      observacoes: [
        '',
        [
          Validators.maxLength(2000)
        ]
      ]
    });

  ngOnInit(): void {
    this.formulario.controls.tipoPessoa
      .valueChanges
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((tipoPessoa) => {
        this.atualizarValidacoesTipoPessoa(
          tipoPessoa
        );

        this.formatarDocumento();
      });

    this.atualizarValidacoesTipoPessoa(
      this.formulario.controls
        .tipoPessoa.value
    );

    if (this.clienteId) {
      this.carregarCliente(this.clienteId);
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

    const dados: DadosCliente = {
      tipoPessoa: valores.tipoPessoa,
      nome: valores.nome.trim(),

      razaoSocial:
        valores.tipoPessoa === 'JURIDICA'
          ? this.textoOuNull(
              valores.razaoSocial
            )
          : null,

      cpfCnpj: valores.cpfCnpj,

      email: this.textoOuNull(
        valores.email
      ),

      telefone: this.textoOuNull(
        valores.telefone
      ),

      celular: this.textoOuNull(
        valores.celular
      ),

      observacoes: this.textoOuNull(
        valores.observacoes
      )
    };

    this.salvando.set(true);

    const requisicao =
      this.clienteId
        ? this.clienteService.atualizar(
            this.clienteId,
            dados
          )
        : this.clienteService.criar(dados);

    requisicao
      .pipe(
        finalize(() =>
          this.salvando.set(false)
        )
      )
      .subscribe({
        next: () => {
          const mensagem =
            this.modoEdicao()
              ? 'Cliente atualizado com sucesso.'
              : 'Cliente cadastrado com sucesso.';

          this.snackBar.open(
            mensagem,
            'Fechar',
            {
              duration: 3500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );

          void this.router.navigateByUrl(
            '/clientes'
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
    void this.router.navigateByUrl(
      '/clientes'
    );
  }

  protected formatarDocumento(): void {
    const controle =
      this.formulario.controls.cpfCnpj;

    const tipoPessoa =
      this.formulario.controls
        .tipoPessoa.value;

    const documentoFormatado =
      this.aplicarMascaraDocumento(
        controle.value,
        tipoPessoa
      );

    controle.setValue(
      documentoFormatado,
      {
        emitEvent: false
      }
    );

    controle.updateValueAndValidity({
      emitEvent: false
    });
  }

  protected formatarTelefone(
    campo: 'telefone' | 'celular'
  ): void {
    const controle =
      this.formulario.controls[campo];

    controle.setValue(
      this.aplicarMascaraTelefone(
        controle.value
      ),
      {
        emitEvent: false
      }
    );
  }

  protected rotuloDocumento(): string {
    return this.formulario.controls
      .tipoPessoa.value === 'FISICA'
        ? 'CPF'
        : 'CNPJ';
  }

  protected placeholderDocumento(): string {
    return this.formulario.controls
      .tipoPessoa.value === 'FISICA'
        ? '000.000.000-00'
        : '00.000.000/0000-00';
  }

  protected erroApi(
    campo:
      | 'tipoPessoa'
      | 'nome'
      | 'razaoSocial'
      | 'cpfCnpj'
      | 'email'
      | 'telefone'
      | 'celular'
      | 'observacoes'
  ): string | null {
    return this.formulario.controls[campo]
      .getError('api') ?? null;
  }

  private carregarCliente(
    clienteId: string
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    this.clienteService
      .buscarPorId(clienteId)
      .pipe(
        finalize(() =>
          this.carregando.set(false)
        )
      )
      .subscribe({
        next: (cliente) => {
          this.formulario.patchValue({
            tipoPessoa:
              cliente.tipoPessoa,

            nome:
              cliente.nome,

            razaoSocial:
              cliente.razaoSocial ?? '',

            cpfCnpj:
              this.aplicarMascaraDocumento(
                cliente.cpfCnpj,
                cliente.tipoPessoa
              ),

            email:
              cliente.email ?? '',

            telefone:
              this.aplicarMascaraTelefone(
                cliente.telefone ?? ''
              ),

            celular:
              this.aplicarMascaraTelefone(
                cliente.celular ?? ''
              ),

            observacoes:
              cliente.observacoes ?? ''
          });

          this.atualizarValidacoesTipoPessoa(
            cliente.tipoPessoa
          );
        },

        error: () => {
          this.mensagemErro.set(
            'Não foi possível carregar o cliente.'
          );
        }
      });
  }

  private atualizarValidacoesTipoPessoa(
    tipoPessoa: TipoPessoa
  ): void {
    const razaoSocial =
      this.formulario.controls
        .razaoSocial;

    const cpfCnpj =
      this.formulario.controls
        .cpfCnpj;

    if (tipoPessoa === 'JURIDICA') {
      razaoSocial.setValidators([
        Validators.required,
        Validators.maxLength(200)
      ]);
    } else {
      razaoSocial.setValidators([
        Validators.maxLength(200)
      ]);

      razaoSocial.setValue(
        '',
        {
          emitEvent: false
        }
      );
    }

    cpfCnpj.setValidators([
      Validators.required,
      Validators.maxLength(18),
      this.criarValidadorDocumento(
        tipoPessoa
      )
    ]);

    razaoSocial.updateValueAndValidity({
      emitEvent: false
    });

    cpfCnpj.updateValueAndValidity({
      emitEvent: false
    });
  }

  private criarValidadorDocumento(
    tipoPessoa: TipoPessoa
  ): ValidatorFn {
    return (
      controle: AbstractControl
    ): ValidationErrors | null => {

      const documento =
        String(controle.value ?? '')
          .replace(/\D/g, '');

      if (!documento) {
        return null;
      }

      const quantidadeEsperada =
        tipoPessoa === 'FISICA'
          ? 11
          : 14;

      return documento.length
        === quantidadeEsperada
          ? null
          : {
              documentoInvalido: true
            };
    };
  }

  private aplicarMascaraDocumento(
    valor: string,
    tipoPessoa: TipoPessoa
  ): string {
    const limite =
      tipoPessoa === 'FISICA'
        ? 11
        : 14;

    const numeros =
      valor
        .replace(/\D/g, '')
        .slice(0, limite);

    if (tipoPessoa === 'FISICA') {
      return numeros
        .replace(
          /^(\d{3})(\d)/,
          '$1.$2'
        )
        .replace(
          /^(\d{3})\.(\d{3})(\d)/,
          '$1.$2.$3'
        )
        .replace(
          /(\d{3})(\d{1,2})$/,
          '$1-$2'
        );
    }

    return numeros
      .replace(
        /^(\d{2})(\d)/,
        '$1.$2'
      )
      .replace(
        /^(\d{2})\.(\d{3})(\d)/,
        '$1.$2.$3'
      )
      .replace(
        /\.(\d{3})(\d)/,
        '.$1/$2'
      )
      .replace(
        /(\d{4})(\d{1,2})$/,
        '$1-$2'
      );
  }

  private aplicarMascaraTelefone(
    valor: string
  ): string {
    const numeros =
      valor
        .replace(/\D/g, '')
        .slice(0, 11);

    if (numeros.length <= 10) {
      return numeros
        .replace(
          /^(\d{2})(\d)/,
          '($1) $2'
        )
        .replace(
          /(\d{4})(\d{1,4})$/,
          '$1-$2'
        );
    }

    return numeros
      .replace(
        /^(\d{2})(\d)/,
        '($1) $2'
      )
      .replace(
        /(\d{5})(\d{1,4})$/,
        '$1-$2'
      );
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
      this.formulario.controls
        .cpfCnpj
        .setErrors({
          ...this.formulario.controls
            .cpfCnpj.errors,

          api:
            'Já existe um cliente com este CPF ou CNPJ.'
        });

      this.mensagemErro.set(
        'Já existe um cliente com este documento.'
      );

      return;
    }

    if (
      resposta?.codigo
      === 'CPF_INVALIDO'
      || resposta?.codigo
      === 'CNPJ_INVALIDO'
      || resposta?.codigo
      === 'DOCUMENTO_OBRIGATORIO'
    ) {
      this.formulario.controls
        .cpfCnpj
        .setErrors({
          ...this.formulario.controls
            .cpfCnpj.errors,

          api: resposta.mensagem
        });

      return;
    }

    if (
      resposta?.codigo
      === 'RAZAO_SOCIAL_OBRIGATORIA'
    ) {
      this.formulario.controls
        .razaoSocial
        .setErrors({
          ...this.formulario.controls
            .razaoSocial.errors,

          api: resposta.mensagem
        });

      return;
    }

    if (resposta?.mensagem) {
      this.mensagemErro.set(
        resposta.mensagem
      );

      return;
    }

    if (erro.status === 0) {
      this.mensagemErro.set(
        'Não foi possível conectar ao servidor.'
      );

      return;
    }

    this.mensagemErro.set(
      'Não foi possível salvar o cliente.'
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

  private limparErrosApi(): void {
    Object.values(
      this.formulario.controls
    ).forEach((controle) => {
      const erros =
        controle.errors;

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
    });
  }

  private textoOuNull(
    texto: string
  ): string | null {
    const textoNormalizado =
      texto.trim();

    return textoNormalizado
      ? textoNormalizado
      : null;
  }
}