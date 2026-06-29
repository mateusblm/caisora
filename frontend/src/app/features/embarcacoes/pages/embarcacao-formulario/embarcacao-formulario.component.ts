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
  finalize,
  forkJoin,
  of,
  switchMap
} from 'rxjs';

import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';

import {
  MatAutocompleteModule
} from '@angular/material/autocomplete';

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
  Cliente
} from '../../../clientes/models/cliente.model';

import {
  ClienteService
} from '../../../clientes/services/cliente.service';

import {
  DadosEmbarcacao,
  Embarcacao,
  TipoEmbarcacao,
  TipoPropulsao
} from '../../models/embarcacao.model';

import {
  EmbarcacaoService
} from '../../services/embarcacao.service';

import {
  ErroApi
} from '../../../../shared/modelos/erro-api.model';

interface OpcaoEnum<T> {
  valor: T;
  rotulo: string;
}

type CampoFormulario =
  | 'proprietario'
  | 'nome'
  | 'tipo'
  | 'fabricante'
  | 'modelo'
  | 'anoFabricacao'
  | 'numeroInscricao'
  | 'numeroCasco'
  | 'portoInscricao'
  | 'codigoPaisBandeira'
  | 'comprimentoTotalMetros'
  | 'bocaMetros'
  | 'caladoMetros'
  | 'pontalMetros'
  | 'alturaTotalMetros'
  | 'pesoKg'
  | 'capacidadePessoas'
  | 'tipoPropulsao'
  | 'corPredominante'
  | 'observacoes';

@Component({
  selector: 'app-embarcacao-formulario',
  imports: [
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl:
    './embarcacao-formulario.component.html',
  styleUrl:
    './embarcacao-formulario.component.scss'
})
export class EmbarcacaoFormularioComponent
  implements OnInit {

  private readonly formBuilder =
    inject(FormBuilder);

  private readonly embarcacaoService =
    inject(EmbarcacaoService);

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

  private readonly embarcacaoId =
    this.activatedRoute.snapshot
      .paramMap
      .get('id');

  protected readonly anoMaximo =
    new Date().getFullYear() + 1;

  protected readonly modoEdicao =
    signal(this.embarcacaoId !== null);

  protected readonly carregando =
    signal(false);

  protected readonly carregandoProprietarios =
    signal(false);

  protected readonly salvando =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly proprietarios =
    signal<Cliente[]>([]);

  protected readonly proprietariosFiltrados =
    signal<Cliente[]>([]);

  protected readonly tipos:
    OpcaoEnum<TipoEmbarcacao>[] = [
      {
        valor: 'LANCHA',
        rotulo: 'Lancha'
      },
      {
        valor: 'VELEIRO',
        rotulo: 'Veleiro'
      },
      {
        valor: 'CATAMARA',
        rotulo: 'Catamarã'
      },
      {
        valor: 'IATE',
        rotulo: 'Iate'
      },
      {
        valor: 'MOTO_AQUATICA',
        rotulo: 'Moto aquática'
      },
      {
        valor: 'BOTE',
        rotulo: 'Bote'
      },
      {
        valor: 'CANOA',
        rotulo: 'Canoa'
      },
      {
        valor: 'ESCUNA',
        rotulo: 'Escuna'
      },
      {
        valor: 'TRAINEIRA',
        rotulo: 'Traineira'
      },
      {
        valor: 'PESQUEIRO',
        rotulo: 'Pesqueiro'
      },
      {
        valor: 'FLUTUANTE',
        rotulo: 'Flutuante'
      },
      {
        valor: 'OUTRA',
        rotulo: 'Outra'
      }
    ];

  protected readonly propulsoes:
    OpcaoEnum<TipoPropulsao>[] = [
      {
        valor: 'MOTOR',
        rotulo: 'Motor'
      },
      {
        valor: 'VELA',
        rotulo: 'Vela'
      },
      {
        valor: 'VELA_E_MOTOR',
        rotulo: 'Vela e motor'
      },
      {
        valor: 'REMO',
        rotulo: 'Remo'
      },
      {
        valor: 'SEM_PROPULSAO',
        rotulo: 'Sem propulsão'
      },
      {
        valor: 'OUTRA',
        rotulo: 'Outra'
      }
    ];

  protected readonly formulario =
    this.formBuilder.group({
      proprietario:
        this.formBuilder.control<
          Cliente | string | null
        >(
          null,
          {
            validators: [
              Validators.required,
              this.validarProprietarioSelecionado()
            ]
          }
        ),

      nome:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(150)
            ]
          }
        ),

      tipo:
        this.formBuilder.nonNullable.control<
          TipoEmbarcacao
        >(
          'LANCHA',
          {
            validators: [
              Validators.required
            ]
          }
        ),

      fabricante:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(100)
            ]
          }
        ),

      modelo:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(100)
            ]
          }
        ),

      anoFabricacao:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(1800),
              Validators.max(this.anoMaximo),
              this.validarNumeroInteiro()
            ]
          }
        ),

      numeroInscricao:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(50)
            ]
          }
        ),

      numeroCasco:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(100)
            ]
          }
        ),

      portoInscricao:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(150)
            ]
          }
        ),

      codigoPaisBandeira:
        this.formBuilder.nonNullable.control(
          'BR',
          {
            validators: [
              Validators.required,
              Validators.pattern(
                /^[A-Za-z]{2}$/
              )
            ]
          }
        ),

      comprimentoTotalMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.required,
              Validators.min(0.01)
            ]
          }
        ),

      bocaMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.required,
              Validators.min(0.01)
            ]
          }
        ),

      caladoMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(0.01)
            ]
          }
        ),

      pontalMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(0.01)
            ]
          }
        ),

      alturaTotalMetros:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(0.01)
            ]
          }
        ),

      pesoKg:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(0.01)
            ]
          }
        ),

      capacidadePessoas:
        this.formBuilder.control<number | null>(
          null,
          {
            validators: [
              Validators.min(1),
              this.validarNumeroInteiro()
            ]
          }
        ),

      tipoPropulsao:
        this.formBuilder.nonNullable.control<
          TipoPropulsao
        >(
          'MOTOR',
          {
            validators: [
              Validators.required
            ]
          }
        ),

      corPredominante:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(50)
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

  protected readonly exibirProprietario = (
    valor: Cliente | string | null
  ): string => {
    if (
      typeof valor === 'object'
      && valor !== null
    ) {
      return valor.nome;
    }

    return typeof valor === 'string'
      ? valor
      : '';
  };

  constructor() {
    this.formulario.controls
      .proprietario
      .valueChanges
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe((valor) => {
        const termo =
          typeof valor === 'string'
            ? valor
            : valor?.nome ?? '';

        this.filtrarProprietarios(termo);
      });

    this.limparErrosApiAoEditar();
  }

  ngOnInit(): void {
    if (this.embarcacaoId) {
      this.carregarDadosEdicao(
        this.embarcacaoId
      );
    } else {
      this.carregarProprietariosAtivos();
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

    if (
      !this.ehProprietarioSelecionado(
        valores.proprietario
      )
    ) {
      this.formulario.controls
        .proprietario
        .setErrors({
          proprietarioInvalido: true
        });

      return;
    }

    const dados: DadosEmbarcacao = {
      proprietarioId:
        valores.proprietario.id,

      nome:
        this.textoOuNull(
          valores.nome
        ),

      tipo:
        valores.tipo,

      fabricante:
        this.textoOuNull(
          valores.fabricante
        ),

      modelo:
        this.textoOuNull(
          valores.modelo
        ),

      anoFabricacao:
        valores.anoFabricacao,

      numeroInscricao:
        this.textoOuNull(
          valores.numeroInscricao
        ),

      numeroCasco:
        this.textoOuNull(
          valores.numeroCasco
        ),

      portoInscricao:
        this.textoOuNull(
          valores.portoInscricao
        ),

      codigoPaisBandeira:
        valores.codigoPaisBandeira
          .trim()
          .toUpperCase(),

      comprimentoTotalMetros:
        valores.comprimentoTotalMetros!,

      bocaMetros:
        valores.bocaMetros!,

      caladoMetros:
        valores.caladoMetros,

      pontalMetros:
        valores.pontalMetros,

      alturaTotalMetros:
        valores.alturaTotalMetros,

      pesoKg:
        valores.pesoKg,

      capacidadePessoas:
        valores.capacidadePessoas,

      tipoPropulsao:
        valores.tipoPropulsao,

      corPredominante:
        this.textoOuNull(
          valores.corPredominante
        ),

      observacoes:
        this.textoOuNull(
          valores.observacoes
        )
    };

    this.salvando.set(true);

    const requisicao =
      this.embarcacaoId
        ? this.embarcacaoService.atualizar(
            this.embarcacaoId,
            dados
          )
        : this.embarcacaoService.criar(
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
              ? 'Embarcação atualizada com sucesso.'
              : 'Embarcação cadastrada com sucesso.',
            'Fechar',
            {
              duration: 3500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );

          void this.router.navigateByUrl(
            '/embarcacoes'
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
      '/embarcacoes'
    );
  }

  protected normalizarCodigoPais(): void {
    const controle =
      this.formulario.controls
        .codigoPaisBandeira;

    controle.setValue(
      controle.value
        .replace(/[^A-Za-z]/g, '')
        .slice(0, 2)
        .toUpperCase(),
      {
        emitEvent: false
      }
    );

    controle.updateValueAndValidity({
      emitEvent: false
    });
  }

  protected normalizarIdentificador(
    campo:
      | 'numeroInscricao'
      | 'numeroCasco'
  ): void {
    const controle =
      this.formulario.controls[campo];

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

  protected formatarDocumentoCliente(
    documento: string
  ): string {
    const numeros =
      documento.replace(/\D/g, '');

    if (numeros.length === 11) {
      return numeros.replace(
        /(\d{3})(\d{3})(\d{3})(\d{2})/,
        '$1.$2.$3-$4'
      );
    }

    if (numeros.length === 14) {
      return numeros.replace(
        /(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/,
        '$1.$2.$3/$4-$5'
      );
    }

    return documento;
  }

  protected erroApi(
    campo: CampoFormulario
  ): string | null {
    return this.formulario.controls[
      campo
    ].getError('api') ?? null;
  }

  private carregarProprietariosAtivos(): void {
    this.carregandoProprietarios.set(true);

    this.clienteService
      .listarTodosAtivos()
      .pipe(
        finalize(() =>
          this.carregandoProprietarios.set(
            false
          )
        )
      )
      .subscribe({
        next: (proprietarios) => {
          this.definirProprietarios(
            proprietarios
          );
        },

        error: () => {
          this.mensagemErro.set(
            'Não foi possível carregar os proprietários disponíveis.'
          );
        }
      });
  }

  private carregarDadosEdicao(
    embarcacaoId: string
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    this.embarcacaoService
      .buscarPorId(embarcacaoId)
      .pipe(
        switchMap((embarcacao) =>
          forkJoin({
            embarcacao:
              of(embarcacao),

            proprietariosAtivos:
              this.clienteService
                .listarTodosAtivos(),

            proprietarioAtual:
              this.clienteService
                .buscarPorId(
                  embarcacao.proprietarioId
                )
          })
        ),

        finalize(() =>
          this.carregando.set(false)
        )
      )
      .subscribe({
        next: ({
          embarcacao,
          proprietariosAtivos,
          proprietarioAtual
        }) => {
          this.definirProprietarios([
            proprietarioAtual,
            ...proprietariosAtivos
          ]);

          this.preencherFormulario(
            embarcacao,
            proprietarioAtual
          );
        },

        error: () => {
          this.mensagemErro.set(
            'Não foi possível carregar os dados da embarcação.'
          );
        }
      });
  }

  private preencherFormulario(
    embarcacao: Embarcacao,
    proprietario: Cliente
  ): void {
    this.formulario.patchValue({
      proprietario,

      nome:
        embarcacao.nome ?? '',

      tipo:
        embarcacao.tipo,

      fabricante:
        embarcacao.fabricante ?? '',

      modelo:
        embarcacao.modelo ?? '',

      anoFabricacao:
        embarcacao.anoFabricacao,

      numeroInscricao:
        embarcacao.numeroInscricao ?? '',

      numeroCasco:
        embarcacao.numeroCasco ?? '',

      portoInscricao:
        embarcacao.portoInscricao ?? '',

      codigoPaisBandeira:
        embarcacao.codigoPaisBandeira
        ?? 'BR',

      comprimentoTotalMetros:
        embarcacao.comprimentoTotalMetros,

      bocaMetros:
        embarcacao.bocaMetros,

      caladoMetros:
        embarcacao.caladoMetros,

      pontalMetros:
        embarcacao.pontalMetros,

      alturaTotalMetros:
        embarcacao.alturaTotalMetros,

      pesoKg:
        embarcacao.pesoKg,

      capacidadePessoas:
        embarcacao.capacidadePessoas,

      tipoPropulsao:
        embarcacao.tipoPropulsao,

      corPredominante:
        embarcacao.corPredominante ?? '',

      observacoes:
        embarcacao.observacoes ?? ''
    });
  }

  private definirProprietarios(
    proprietarios: Cliente[]
  ): void {
    const proprietariosUnicos =
      new Map<string, Cliente>();

    for (const proprietario of proprietarios) {
      proprietariosUnicos.set(
        proprietario.id,
        proprietario
      );
    }

    const ordenados = [
      ...proprietariosUnicos.values()
    ].sort((primeiro, segundo) =>
      primeiro.nome.localeCompare(
        segundo.nome,
        'pt-BR'
      )
    );

    this.proprietarios.set(ordenados);
    this.proprietariosFiltrados.set(
      ordenados
    );
  }

  private filtrarProprietarios(
    termo: string
  ): void {
    const termoNormalizado =
      this.normalizarTextoBusca(termo);

    const documento =
      termo.replace(/\D/g, '');

    if (!termoNormalizado && !documento) {
      this.proprietariosFiltrados.set(
        this.proprietarios()
      );

      return;
    }

    this.proprietariosFiltrados.set(
      this.proprietarios().filter(
        (proprietario) => {
          const nome =
            this.normalizarTextoBusca(
              proprietario.nome
            );

          const cpfCnpj =
            proprietario.cpfCnpj
              .replace(/\D/g, '');

          return nome.includes(
            termoNormalizado
          ) || (
            documento.length > 0
            && cpfCnpj.includes(documento)
          );
        }
      )
    );
  }

  private validarProprietarioSelecionado():
    ValidatorFn {
    return (
      controle: AbstractControl
    ): ValidationErrors | null => {
      return this.ehProprietarioSelecionado(
        controle.value
      )
        ? null
        : {
            proprietarioInvalido: true
          };
    };
  }

  private ehProprietarioSelecionado(
    valor: unknown
  ): valor is Cliente {
    return (
      typeof valor === 'object'
      && valor !== null
      && 'id' in valor
      && 'nome' in valor
    );
  }

  private validarNumeroInteiro():
    ValidatorFn {
    return (
      controle: AbstractControl
    ): ValidationErrors | null => {
      const valor = controle.value;

      if (
        valor === null
        || valor === undefined
        || valor === ''
      ) {
        return null;
      }

      return Number.isInteger(
        Number(valor)
      )
        ? null
        : {
            numeroInteiro: true
          };
    };
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
      this.tratarConflitoIdentificador(
        resposta.mensagem
      );

      return;
    }

    const mapaCodigos:
      Record<string, CampoFormulario> = {
        PROPRIETARIO_OBRIGATORIO:
          'proprietario',

        PROPRIETARIO_INATIVO:
          'proprietario',

        TIPO_EMBARCACAO_OBRIGATORIO:
          'tipo',

        TIPO_PROPULSAO_OBRIGATORIO:
          'tipoPropulsao',

        COMPRIMENTO_INVALIDO:
          'comprimentoTotalMetros',

        BOCA_INVALIDA:
          'bocaMetros',

        CALADO_INVALIDO:
          'caladoMetros',

        PONTAL_INVALIDO:
          'pontalMetros',

        ALTURA_TOTAL_INVALIDA:
          'alturaTotalMetros',

        PESO_INVALIDO:
          'pesoKg',

        CAPACIDADE_PESSOAS_INVALIDA:
          'capacidadePessoas',

        ANO_FABRICACAO_INVALIDO:
          'anoFabricacao',

        PAIS_BANDEIRA_INVALIDO:
          'codigoPaisBandeira'
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
        'Não foi possível conectar ao servidor.'
      );

      return;
    }

    if (erro.status === 404) {
      this.mensagemErro.set(
        'A embarcação ou o proprietário não foi encontrado.'
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
      'Não foi possível salvar a embarcação.'
    );
  }

  private tratarConflitoIdentificador(
    mensagem: string
  ): void {
    const mensagemNormalizada =
      this.normalizarTextoBusca(
        mensagem
      );

    if (
      mensagemNormalizada.includes(
        'inscricao'
      )
    ) {
      this.adicionarErroApi(
        'numeroInscricao',
        'Já existe uma embarcação com este número de inscrição.'
      );
    } else if (
      mensagemNormalizada.includes(
        'casco'
      )
    ) {
      this.adicionarErroApi(
        'numeroCasco',
        'Já existe uma embarcação com este número de casco.'
      );
    }

    this.mensagemErro.set(mensagem);
  }

  private aplicarErrosCampos(
    errosCampos: {
      campo: string;
      mensagem: string;
    }[]
  ): void {
    for (const erroCampo of errosCampos) {
      const nomeCampo =
        erroCampo.campo === 'proprietarioId'
          ? 'proprietario'
          : erroCampo.campo;

      const controle =
        this.formulario.get(nomeCampo);

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

  private normalizarTextoBusca(
    texto: string
  ): string {
    return texto
      .normalize('NFD')
      .replace(
        /[\u0300-\u036f]/g,
        ''
      )
      .toLowerCase()
      .trim();
  }
}