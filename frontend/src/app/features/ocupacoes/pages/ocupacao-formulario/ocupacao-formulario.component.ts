import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';

import {
  Component,
  DestroyRef,
  computed,
  inject,
  OnInit,
  signal
} from '@angular/core';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  finalize,
  forkJoin
} from 'rxjs';

import {
  takeUntilDestroyed,
  toSignal
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
  ErroApi
} from '../../../../shared/modelos/erro-api.model';

import {
  Embarcacao
} from '../../../embarcacoes/models/embarcacao.model';

import {
  EmbarcacaoService
} from '../../../embarcacoes/services/embarcacao.service';

import {
  Vaga
} from '../../../vagas/models/vaga.model';

import {
  VagaService
} from '../../../vagas/services/vaga.service';

import {
  AtualizarOcupacao,
  CriarOcupacao,
  Ocupacao
} from '../../models/ocupacao.model';

import {
  OcupacaoService
} from '../../services/ocupacao.service';

type CampoFormulario =
  | 'embarcacaoId'
  | 'vagaId'
  | 'inicioEm'
  | 'fimPrevistoEm'
  | 'observacoes';

@Component({
  selector: 'app-ocupacao-formulario',
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
  templateUrl:
    './ocupacao-formulario.component.html',
  styleUrl:
    './ocupacao-formulario.component.scss'
})
export class OcupacaoFormularioComponent
  implements OnInit {

  private readonly formBuilder =
    inject(FormBuilder);

  private readonly ocupacaoService =
    inject(OcupacaoService);

  private readonly embarcacaoService =
    inject(EmbarcacaoService);

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

  private readonly ocupacaoId =
    this.activatedRoute.snapshot
      .paramMap
      .get('id');

  protected readonly modoEdicao =
    signal(this.ocupacaoId !== null);

  protected readonly carregando =
    signal(true);

  protected readonly salvando =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly ocupacaoEncerrada =
    signal(false);

  protected readonly embarcacoesDisponiveis =
    signal<Embarcacao[]>([]);

  protected readonly vagasDisponiveis =
    signal<Vaga[]>([]);

  protected readonly formulario =
    this.formBuilder.group(
      {
        embarcacaoId:
          this.formBuilder.nonNullable.control(
            '',
            {
              validators: [
                Validators.required
              ]
            }
          ),

        vagaId:
          this.formBuilder.nonNullable.control(
            '',
            {
              validators: [
                Validators.required
              ]
            }
          ),

        inicioEm:
          this.formBuilder.nonNullable.control(
            this.agoraLocal(),
            {
              validators: [
                Validators.required,
                this.inicioNaoFuturo()
              ]
            }
          ),

        fimPrevistoEm:
          this.formBuilder.nonNullable.control(
            ''
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
      },
      {
        validators: [
          this.fimPosteriorAoInicio()
        ]
      }
    );

  private readonly embarcacaoIdSelecionada =
    toSignal(
      this.formulario.controls
        .embarcacaoId
        .valueChanges,
      {
        initialValue:
          this.formulario.controls
            .embarcacaoId
            .value
      }
    );

  private readonly vagaIdSelecionada =
    toSignal(
      this.formulario.controls
        .vagaId
        .valueChanges,
      {
        initialValue:
          this.formulario.controls
            .vagaId
            .value
      }
    );

  protected readonly embarcacaoSelecionada =
    computed(
      () =>
        this.embarcacoesDisponiveis()
          .find(
            (embarcacao) =>
              embarcacao.id
                === this.embarcacaoIdSelecionada()
          )
          ?? null
    );

  protected readonly vagaSelecionada =
    computed(
      () =>
        this.vagasDisponiveis()
          .find(
            (vaga) =>
              vaga.id
                === this.vagaIdSelecionada()
          )
          ?? null
    );

  protected readonly incompatibilidades =
    computed(() => {
      const embarcacao =
        this.embarcacaoSelecionada();

      const vaga =
        this.vagaSelecionada();

      if (!embarcacao || !vaga) {
        return [];
      }

      const mensagens: string[] = [];

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.comprimentoTotalMetros,
        vaga.comprimentoMaximoMetros,
        'O comprimento da embarcação excede o limite da vaga.'
      );

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.bocaMetros,
        vaga.bocaMaximaMetros,
        'A boca da embarcação excede o limite da vaga.'
      );

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.caladoMetros,
        vaga.caladoMaximoMetros,
        'O calado da embarcação excede o limite da vaga.'
      );

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.alturaTotalMetros,
        vaga.alturaMaximaMetros,
        'A altura da embarcação excede o limite da vaga.'
      );

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.pesoKg,
        vaga.pesoMaximoKg,
        'O peso da embarcação excede o limite da vaga.'
      );

      return mensagens;
    });

  protected readonly semOpcoesDisponiveis =
    computed(
      () =>
        !this.modoEdicao()
        && (
          this.embarcacoesDisponiveis()
            .length === 0
          || this.vagasDisponiveis()
            .length === 0
        )
    );

  constructor() {
    this.limparErrosApiAoEditar();
  }

  ngOnInit(): void {
    if (this.ocupacaoId) {
      this.carregarEdicao(
        this.ocupacaoId
      );

      return;
    }

    this.carregarCriacao();
  }

  protected salvar(): void {
    this.mensagemErro.set(null);
    this.limparErrosApi();

    this.formulario.controls.inicioEm
      .updateValueAndValidity();

    this.formulario
      .updateValueAndValidity();

    if (
      this.formulario.invalid
      || this.incompatibilidades().length > 0
      || this.semOpcoesDisponiveis()
      || this.ocupacaoEncerrada()
    ) {
      this.formulario.markAllAsTouched();

      if (
        this.incompatibilidades().length > 0
      ) {
        this.mensagemErro.set(
          'A embarcação selecionada não é compatível com os limites da vaga.'
        );
      }

      return;
    }

    const valores =
      this.formulario.getRawValue();

    this.salvando.set(true);

    const requisicao =
      this.ocupacaoId
        ? this.ocupacaoService.atualizar(
            this.ocupacaoId,
            this.montarAtualizacao(
              valores
            )
          )
        : this.ocupacaoService.criar(
            this.montarCriacao(
              valores
            )
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
              ? 'Ocupação atualizada com sucesso.'
              : 'Ocupação registrada com sucesso.',
            'Fechar',
            {
              duration: 3500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );

          void this.router.navigateByUrl(
            '/ocupacoes'
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
      '/ocupacoes'
    );
  }

  protected erroApi(
    campo: CampoFormulario
  ): string | null {
    return this.formulario.controls[
      campo
    ].getError('api') ?? null;
  }

  protected nomeExibicaoEmbarcacao(
    embarcacao: Embarcacao
  ): string {
    return (
      embarcacao.nome?.trim()
      || embarcacao.modelo?.trim()
      || embarcacao.numeroInscricao?.trim()
      || 'Embarcação sem nome'
    );
  }

  protected descricaoEmbarcacao(
    embarcacao: Embarcacao
  ): string {
    const partes = [
      embarcacao.fabricante,
      embarcacao.modelo,
      embarcacao.anoFabricacao
        ?.toString()
    ].filter(Boolean);

    return partes.length
      ? partes.join(' · ')
      : 'Fabricante e modelo não informados';
  }

  protected descricaoVaga(
    vaga: Vaga
  ): string {
    const partes = [
      vaga.setor,
      vaga.localizacao
    ].filter(Boolean);

    return partes.length
      ? partes.join(' · ')
      : 'Localização não informada';
  }

  protected formatarMetros(
    valor: number | null
  ): string {
    if (valor === null) {
      return 'Sem limite';
    }

    return `${Number(valor).toLocaleString(
      'pt-BR',
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }
    )} m`;
  }

  protected formatarPeso(
    valor: number | null
  ): string {
    if (valor === null) {
      return 'Sem limite';
    }

    return `${Number(valor).toLocaleString(
      'pt-BR',
      {
        maximumFractionDigits: 2
      }
    )} kg`;
  }

  private carregarCriacao(): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    forkJoin({
      embarcacoes:
        this.embarcacaoService
          .listarTodas(),

      vagas:
        this.vagaService
          .listarTodas(),

      ocupacoesAtivas:
        this.ocupacaoService
          .listarTodasAtivas()
    })
      .pipe(
        finalize(() =>
          this.carregando.set(false)
        ),

        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: ({
          embarcacoes,
          vagas,
          ocupacoesAtivas
        }) => {
          const embarcacoesOcupadas =
            new Set(
              ocupacoesAtivas.map(
                (ocupacao) =>
                  ocupacao.embarcacaoId
              )
            );

          const vagasOcupadas =
            new Set(
              ocupacoesAtivas.map(
                (ocupacao) =>
                  ocupacao.vagaId
              )
            );

          this.embarcacoesDisponiveis.set(
            embarcacoes
              .filter(
                (embarcacao) =>
                  embarcacao.ativa
                  && !embarcacoesOcupadas.has(
                    embarcacao.id
                  )
              )
              .sort(
                (primeira, segunda) =>
                  this.nomeExibicaoEmbarcacao(
                    primeira
                  ).localeCompare(
                    this.nomeExibicaoEmbarcacao(
                      segunda
                    ),
                    'pt-BR'
                  )
              )
          );

          this.vagasDisponiveis.set(
            vagas
              .filter(
                (vaga) =>
                  vaga.ativa
                  && !vagasOcupadas.has(
                    vaga.id
                  )
              )
              .sort(
                (primeira, segunda) =>
                  primeira.codigo.localeCompare(
                    segunda.codigo,
                    'pt-BR',
                    {
                      numeric: true
                    }
                  )
              )
          );
        },

        error: () => {
          this.mensagemErro.set(
            'Não foi possível carregar as embarcações e vagas disponíveis.'
          );
        }
      });
  }

  private carregarEdicao(
    ocupacaoId: string
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    forkJoin({
      ocupacao:
        this.ocupacaoService
          .buscarPorId(ocupacaoId),

      embarcacoes:
        this.embarcacaoService
          .listarTodas(),

      vagas:
        this.vagaService
          .listarTodas()
    })
      .pipe(
        finalize(() =>
          this.carregando.set(false)
        ),

        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: ({
          ocupacao,
          embarcacoes,
          vagas
        }) => {
          this.embarcacoesDisponiveis.set(
            embarcacoes
          );

          this.vagasDisponiveis.set(
            vagas
          );

          this.preencherFormulario(
            ocupacao
          );
        },

        error: () => {
          this.mensagemErro.set(
            'Não foi possível carregar os dados da ocupação.'
          );
        }
      });
  }

  private preencherFormulario(
    ocupacao: Ocupacao
  ): void {
    this.formulario.patchValue({
      embarcacaoId:
        ocupacao.embarcacaoId,

      vagaId:
        ocupacao.vagaId,

      inicioEm:
        this.paraDataHoraLocal(
          ocupacao.inicioEm
        ),

      fimPrevistoEm:
        ocupacao.fimPrevistoEm
          ? this.paraDataHoraLocal(
              ocupacao.fimPrevistoEm
            )
          : '',

      observacoes:
        ocupacao.observacoes ?? ''
    });

    this.formulario.controls
      .embarcacaoId
      .disable({
        emitEvent: false
      });

    this.formulario.controls
      .vagaId
      .disable({
        emitEvent: false
      });

    this.formulario.controls
      .inicioEm
      .disable({
        emitEvent: false
      });

    if (
      ocupacao.status === 'ENCERRADA'
    ) {
      this.ocupacaoEncerrada.set(true);
      this.formulario.disable({
        emitEvent: false
      });

      this.mensagemErro.set(
        'Esta ocupação já foi encerrada e permanece disponível somente para consulta.'
      );
    }
  }

  private montarCriacao(
    valores:
      ReturnType<
        typeof this.formulario.getRawValue
      >
  ): CriarOcupacao {
    return {
      embarcacaoId:
        valores.embarcacaoId,

      vagaId:
        valores.vagaId,

      inicioEm:
        this.paraIso(
          valores.inicioEm
        ) as string,

      fimPrevistoEm:
        valores.fimPrevistoEm
          ? this.paraIso(
              valores.fimPrevistoEm
            )
          : null,

      observacoes:
        this.textoOuNull(
          valores.observacoes
        )
    };
  }

  private montarAtualizacao(
    valores:
      ReturnType<
        typeof this.formulario.getRawValue
      >
  ): AtualizarOcupacao {
    return {
      fimPrevistoEm:
        valores.fimPrevistoEm
          ? this.paraIso(
              valores.fimPrevistoEm
            )
          : null,

      observacoes:
        this.textoOuNull(
          valores.observacoes
        )
    };
  }

  private tratarErro(
    erro: HttpErrorResponse
  ): void {
    const resposta =
      erro.error as ErroApi | null;

    if (
      resposta?.errosCampos?.length
    ) {
      this.aplicarErrosCampos(
        resposta.errosCampos
      );
    }

    const mapaCodigos:
      Record<string, CampoFormulario> = {
        EMBARCACAO_OBRIGATORIA:
          'embarcacaoId',

        VAGA_OBRIGATORIA:
          'vagaId',

        INICIO_OCUPACAO_OBRIGATORIO:
          'inicioEm',

        INICIO_OCUPACAO_FUTURO:
          'inicioEm',

        FIM_PREVISTO_INVALIDO:
          'fimPrevistoEm',

        COMPRIMENTO_EXCEDE_LIMITE_VAGA:
          'vagaId',

        BOCA_EXCEDE_LIMITE_VAGA:
          'vagaId',

        CALADO_EXCEDE_LIMITE_VAGA:
          'vagaId',

        ALTURA_EXCEDE_LIMITE_VAGA:
          'vagaId',

        PESO_EXCEDE_LIMITE_VAGA:
          'vagaId'
      };

    const campo =
      resposta?.codigo
        ? mapaCodigos[resposta.codigo]
        : undefined;

    if (
      campo
      && resposta?.mensagem
    ) {
      this.adicionarErroApi(
        campo,
        resposta.mensagem
      );

      this.mensagemErro.set(
        'Revise os campos destacados.'
      );

      return;
    }

    if (
      resposta?.codigo
        === 'CONFLITO_DADOS'
      && resposta.mensagem
    ) {
      const mensagem =
        resposta.mensagem.toLowerCase();

      if (
        mensagem.includes('embarcacao')
        || mensagem.includes('embarcação')
      ) {
        this.adicionarErroApi(
          'embarcacaoId',
          resposta.mensagem
        );
      } else if (
        mensagem.includes('vaga')
      ) {
        this.adicionarErroApi(
          'vagaId',
          resposta.mensagem
        );
      }

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

    if (erro.status === 404) {
      this.mensagemErro.set(
        'A ocupação, embarcação ou vaga não foi encontrada.'
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
      'Não foi possível salvar a ocupação.'
    );
  }

  private aplicarErrosCampos(
    errosCampos: {
      campo: string;
      mensagem: string;
    }[]
  ): void {
    for (
      const erroCampo
      of errosCampos
    ) {
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

    controles.forEach(
      (controle) => {
        controle.valueChanges
          .pipe(
            takeUntilDestroyed(
              this.destroyRef
            )
          )
          .subscribe(() => {
            this.removerErroApi(
              controle
            );

            this.mensagemErro.set(
              null
            );
          });
      }
    );
  }

  private limparErrosApi(): void {
    const controles =
      Object.values(
        this.formulario.controls
      ) as AbstractControl[];

    controles.forEach(
      (controle) =>
        this.removerErroApi(
          controle
        )
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

  private inicioNaoFuturo():
    ValidatorFn {
    return (
      controle: AbstractControl
    ): ValidationErrors | null => {
      const valor =
        controle.value as string;

      if (!valor) {
        return null;
      }

      const data =
        new Date(valor);

      if (
        Number.isNaN(
          data.getTime()
        )
      ) {
        return {
          dataInvalida: true
        };
      }

      const limite =
        Date.now() + 60_000;

      return data.getTime() > limite
        ? {
            inicioFuturo: true
          }
        : null;
    };
  }

  private fimPosteriorAoInicio():
    ValidatorFn {
    return (
      controle: AbstractControl
    ): ValidationErrors | null => {
      const inicio =
        controle.get('inicioEm')
          ?.value as string;

      const fim =
        controle.get('fimPrevistoEm')
          ?.value as string;

      if (!inicio || !fim) {
        return null;
      }

      const inicioData =
        new Date(inicio);

      const fimData =
        new Date(fim);

      if (
        Number.isNaN(
          inicioData.getTime()
        )
        || Number.isNaN(
          fimData.getTime()
        )
      ) {
        return {
          periodoInvalido: true
        };
      }

      return fimData.getTime()
        <= inicioData.getTime()
        ? {
            fimNaoPosterior: true
          }
        : null;
    };
  }

  private adicionarIncompatibilidade(
    mensagens: string[],
    medidaEmbarcacao: number | null,
    limiteVaga: number | null,
    mensagem: string
  ): void {
    if (
      medidaEmbarcacao !== null
      && limiteVaga !== null
      && Number(medidaEmbarcacao)
        > Number(limiteVaga)
    ) {
      mensagens.push(mensagem);
    }
  }

  private agoraLocal(): string {
    return this.paraDataHoraLocal(
      new Date().toISOString()
    );
  }

  private paraDataHoraLocal(
    valorIso: string
  ): string {
    const data =
      new Date(valorIso);

    const deslocamento =
      data.getTimezoneOffset()
        * 60_000;

    return new Date(
      data.getTime() - deslocamento
    )
      .toISOString()
      .slice(0, 16);
  }

  private paraIso(
    valorLocal: string
  ): string | null {
    const data =
      new Date(valorLocal);

    if (
      Number.isNaN(
        data.getTime()
      )
    ) {
      return null;
    }

    return data.toISOString();
  }

  private textoOuNull(
    texto: string
  ): string | null {
    const valor =
      texto.trim();

    return valor || null;
  }
}
