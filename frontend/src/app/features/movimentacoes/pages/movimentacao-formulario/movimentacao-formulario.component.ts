import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import {
  MatSnackBar,
  MatSnackBarModule
} from '@angular/material/snack-bar';
import {
  ActivatedRoute,
  Router
} from '@angular/router';
import {
  catchError,
  finalize,
  forkJoin,
  of
} from 'rxjs';

import { ErroApi } from '../../../../shared/modelos/erro-api.model';
import { Embarcacao } from '../../../embarcacoes/models/embarcacao.model';
import { EmbarcacaoService } from '../../../embarcacoes/services/embarcacao.service';
import { Ocupacao } from '../../../ocupacoes/models/ocupacao.model';
import { OcupacaoService } from '../../../ocupacoes/services/ocupacao.service';
import { Vaga } from '../../../vagas/models/vaga.model';
import { VagaService } from '../../../vagas/services/vaga.service';
import {
  AtualizarMovimentacao,
  CriarMovimentacao,
  Movimentacao,
  PosicaoEmbarcacao,
  PrioridadeMovimentacao,
  TipoMovimentacao,
  TipoPosicaoEmbarcacao,
  UsuarioOperador
} from '../../models/movimentacao.model';
import { MovimentacaoService } from '../../services/movimentacao.service';

type CampoFormulario =
  | 'embarcacaoId'
  | 'tipo'
  | 'prioridade'
  | 'tipoPosicaoDestino'
  | 'vagaDestinoId'
  | 'descricaoDestino'
  | 'agendadaPara'
  | 'operadorResponsavelId'
  | 'observacoes';

interface OpcaoTipo {
  valor: TipoMovimentacao;
  rotulo: string;
  descricao: string;
}

interface OpcaoDestino {
  valor: TipoPosicaoEmbarcacao;
  rotulo: string;
}

@Component({
  selector: 'app-movimentacao-formulario',
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
    './movimentacao-formulario.component.html',
  styleUrl:
    './movimentacao-formulario.component.scss'
})
export class MovimentacaoFormularioComponent
implements OnInit {

  private readonly formBuilder =
    inject(FormBuilder);

  private readonly movimentacaoService =
    inject(MovimentacaoService);

  private readonly embarcacaoService =
    inject(EmbarcacaoService);

  private readonly vagaService =
    inject(VagaService);

  private readonly ocupacaoService =
    inject(OcupacaoService);

  private readonly activatedRoute =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly snackBar =
    inject(MatSnackBar);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly movimentacaoId =
    this.activatedRoute.snapshot
      .paramMap
      .get('id');

  private preenchendoFormulario = false;

  protected readonly modoEdicao =
    signal(this.movimentacaoId !== null);

  protected readonly carregando =
    signal(true);

  protected readonly carregandoPosicao =
    signal(false);

  protected readonly salvando =
    signal(false);

  protected readonly somenteConsulta =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly movimentacaoAtual =
    signal<Movimentacao | null>(null);

  protected readonly posicaoAtual =
    signal<PosicaoEmbarcacao | null>(null);

  protected readonly embarcacoesDisponiveis =
    signal<Embarcacao[]>([]);

  protected readonly vagas =
    signal<Vaga[]>([]);

  protected readonly ocupacoesAtivas =
    signal<Ocupacao[]>([]);

  protected readonly movimentacoesAbertas =
    signal<Movimentacao[]>([]);

  protected readonly usuarios =
    signal<UsuarioOperador[]>([]);

  private readonly embarcacaoIdSelecionada =
    signal('');

  protected readonly tipoSelecionado =
    signal<TipoMovimentacao | ''>('');

  protected readonly tipoDestinoSelecionado =
    signal<TipoPosicaoEmbarcacao | ''>('');

  private readonly vagaDestinoIdSelecionada =
    signal('');

  protected readonly formulario =
    this.formBuilder.group({
      embarcacaoId:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.required
            ]
          }
        ),
      tipo:
        this.formBuilder.nonNullable.control<
          TipoMovimentacao | ''
        >(
          '',
          {
            validators: [
              Validators.required
            ]
          }
        ),
      prioridade:
        this.formBuilder.nonNullable.control<
          PrioridadeMovimentacao
        >(
          'NORMAL',
          {
            validators: [
              Validators.required
            ]
          }
        ),
      tipoPosicaoDestino:
        this.formBuilder.nonNullable.control<
          TipoPosicaoEmbarcacao | ''
        >(
          '',
          {
            validators: [
              Validators.required
            ]
          }
        ),
      vagaDestinoId:
        this.formBuilder.nonNullable.control(''),
      descricaoDestino:
        this.formBuilder.nonNullable.control(
          '',
          {
            validators: [
              Validators.maxLength(255)
            ]
          }
        ),
      agendadaPara:
        this.formBuilder.nonNullable.control(
          this.dataHoraInicial(),
          {
            validators: [
              Validators.required,
              this.dataHoraValida()
            ]
          }
        ),
      operadorResponsavelId:
        this.formBuilder.nonNullable.control(''),
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

  protected readonly ocupacaoAtivaSelecionada =
    computed(
      () =>
        this.ocupacoesAtivas()
          .find(
            (ocupacao) =>
              ocupacao.embarcacaoId
              === this.embarcacaoIdSelecionada()
          )
        ?? null
    );

  protected readonly vagaDestinoSelecionada =
    computed(
      () =>
        this.vagas()
          .find(
            (vaga) =>
              vaga.id
              === this.vagaDestinoIdSelecionada()
          )
        ?? null
    );

  protected readonly tiposDisponiveis =
    computed<OpcaoTipo[]>(() => {
      const posicao = this.posicaoAtual();
      const ocupacao =
        this.ocupacaoAtivaSelecionada();

      if (!posicao) {
        return [];
      }

      const opcoes: OpcaoTipo[] = [];

      if (
        posicao.tipo === 'VAGA'
        && ocupacao
        && ocupacao.vagaId === posicao.vagaId
      ) {
        opcoes.push(
          {
            valor: 'LANCAMENTO',
            rotulo: 'Lançamento',
            descricao:
              'Leva a embarcação da vaga para a água '
              + 'ou para o píer de espera.'
          },
          {
            valor: 'TRANSFERENCIA',
            rotulo: 'Transferência',
            descricao:
              'Muda definitivamente a embarcação '
              + 'para outra vaga.'
          }
        );
      }

      if (
        (
          posicao.tipo === 'AGUA'
          || posicao.tipo === 'PIER_ESPERA'
          || posicao.tipo === 'EXTERNA'
        )
        && ocupacao
      ) {
        opcoes.push({
          valor: 'RETIRADA',
          rotulo: 'Retirada',
          descricao:
            'Retorna a embarcação para a vaga '
            + 'da ocupação ativa.'
        });
      }

      opcoes.push({
        valor: 'DESLOCAMENTO_INTERNO',
        rotulo: 'Deslocamento interno',
        descricao:
          'Move temporariamente para água, píer, '
          + 'área de serviço ou área externa.'
      });

      const atual = this.movimentacaoAtual();

      if (
        atual
        && !opcoes.some(
          (opcao) => opcao.valor === atual.tipo
        )
      ) {
        opcoes.unshift(
          this.opcaoTipo(atual.tipo)
        );
      }

      return opcoes;
    });

  protected readonly destinosDisponiveis =
    computed<OpcaoDestino[]>(
      () =>
        this.destinosPermitidos(
          this.tipoSelecionado()
        )
    );

  protected readonly vagasDestinoDisponiveis =
    computed(() => {
      const tipo = this.tipoSelecionado();
      const ocupacao =
        this.ocupacaoAtivaSelecionada();

      if (
        this.tipoDestinoSelecionado() !== 'VAGA'
      ) {
        return [];
      }

      if (tipo === 'RETIRADA') {
        if (!ocupacao) {
          return [];
        }

        return this.vagas()
          .filter(
            (vaga) => vaga.id === ocupacao.vagaId
          );
      }

      if (tipo !== 'TRANSFERENCIA') {
        return [];
      }

      const vagasOcupadas = new Set(
        this.ocupacoesAtivas()
          .map((item) => item.vagaId)
      );

      const vagasReservadas = new Set(
        this.movimentacoesAbertas()
          .filter(
            (movimentacao) =>
              movimentacao.id
                !== this.movimentacaoId
              && movimentacao.tipoPosicaoDestino
                === 'VAGA'
              && movimentacao.vagaDestinoId
          )
          .map(
            (movimentacao) =>
              movimentacao.vagaDestinoId as string
          )
      );

      const vagaOrigemId =
        this.posicaoAtual()?.vagaId;

      const vagaAtualDestinoId =
        this.movimentacaoAtual()?.vagaDestinoId;

      return this.vagas()
        .filter(
          (vaga) =>
            (
              vaga.ativa
              || vaga.id === vagaAtualDestinoId
            )
            && vaga.id !== vagaOrigemId
            && (
              !vagasOcupadas.has(vaga.id)
              || vaga.id === vagaAtualDestinoId
            )
            && (
              !vagasReservadas.has(vaga.id)
              || vaga.id === vagaAtualDestinoId
            )
        )
        .sort(
          (primeira, segunda) =>
            primeira.codigo.localeCompare(
              segunda.codigo,
              'pt-BR',
              { numeric: true }
            )
        );
    });

  protected readonly incompatibilidades =
    computed(() => {
      const embarcacao =
        this.embarcacaoSelecionada();

      const vaga =
        this.vagaDestinoSelecionada();

      if (
        !embarcacao
        || !vaga
        || this.tipoDestinoSelecionado() !== 'VAGA'
      ) {
        return [];
      }

      const mensagens: string[] = [];

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.comprimentoTotalMetros,
        vaga.comprimentoMaximoMetros,
        'O comprimento da embarcação excede '
        + 'o limite da vaga.'
      );

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.bocaMetros,
        vaga.bocaMaximaMetros,
        'A boca da embarcação excede '
        + 'o limite da vaga.'
      );

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.caladoMetros,
        vaga.caladoMaximoMetros,
        'O calado da embarcação excede '
        + 'o limite da vaga.'
      );

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.alturaTotalMetros,
        vaga.alturaMaximaMetros,
        'A altura da embarcação excede '
        + 'o limite da vaga.'
      );

      this.adicionarIncompatibilidade(
        mensagens,
        embarcacao.pesoKg,
        vaga.pesoMaximoKg,
        'O peso da embarcação excede '
        + 'o limite da vaga.'
      );

      return mensagens;
    });

  protected readonly semEmbarcacoesDisponiveis =
    computed(
      () =>
        !this.modoEdicao()
        && this.embarcacoesDisponiveis().length === 0
    );

  constructor() {
    this.configurarReacoesFormulario();
    this.limparErrosApiAoEditar();
  }

  ngOnInit(): void {
    if (this.movimentacaoId) {
      this.carregarEdicao(
        this.movimentacaoId
      );
      return;
    }

    this.carregarCriacao();
  }

  protected salvar(): void {
    this.mensagemErro.set(null);
    this.limparErrosApi();

    this.formulario.updateValueAndValidity();

    if (
      this.formulario.invalid
      || this.incompatibilidades().length > 0
      || this.semEmbarcacoesDisponiveis()
      || this.somenteConsulta()
      || !this.posicaoAtual()
    ) {
      this.formulario.markAllAsTouched();

      if (!this.posicaoAtual()) {
        this.mensagemErro.set(
          'Selecione uma embarcação com '
          + 'posição atual disponível.'
        );
      } else if (
        this.incompatibilidades().length > 0
      ) {
        this.mensagemErro.set(
          'A embarcação não é compatível '
          + 'com os limites da vaga.'
        );
      }

      return;
    }

    const valores =
      this.formulario.getRawValue();

    const tipo = valores.tipo;
    const destino =
      valores.tipoPosicaoDestino;

    if (!tipo || !destino) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.salvando.set(true);

    const requisicao =
      this.movimentacaoId
        ? this.movimentacaoService.atualizar(
          this.movimentacaoId,
          this.montarAtualizacao(
            valores,
            destino
          )
        )
        : this.movimentacaoService.criar(
          this.montarCriacao(
            valores,
            tipo,
            destino
          )
        );

    requisicao
      .pipe(
        finalize(
          () => this.salvando.set(false)
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.snackBar.open(
            this.modoEdicao()
              ? 'Movimentação atualizada com sucesso.'
              : 'Movimentação agendada com sucesso.',
            'Fechar',
            {
              duration: 3500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );

          void this.router.navigateByUrl(
            '/movimentacoes'
          );
        },
        error: (erro: HttpErrorResponse) => {
          this.tratarErro(erro);
        }
      });
  }

  protected cancelar(): void {
    void this.router.navigateByUrl(
      '/movimentacoes'
    );
  }

  protected erroApi(
    campo: CampoFormulario
  ): string | null {
    return this.formulario.controls[campo]
      .getError('api') ?? null;
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
      embarcacao.anoFabricacao?.toString()
    ].filter(Boolean);

    return partes.length
      ? partes.join(' · ')
      : 'Fabricante e modelo não informados';
  }

  protected descricaoPosicaoAtual(): string {
    const posicao = this.posicaoAtual();

    if (!posicao) {
      return 'Posição não carregada';
    }

    if (posicao.tipo === 'VAGA') {
      const local = [
        posicao.vagaSetor,
        posicao.vagaLocalizacao
      ]
        .filter(Boolean)
        .join(' · ');

      return (
        `Vaga ${posicao.vagaCodigo ?? 'não informada'}`
        + (local ? ` — ${local}` : '')
      );
    }

    const rotulos: Record<
      TipoPosicaoEmbarcacao,
      string
    > = {
      VAGA: 'Vaga',
      AGUA: 'Água',
      PIER_ESPERA: 'Píer de espera',
      AREA_SERVICO: 'Área de serviço',
      EXTERNA: 'Área externa',
      DESCONHECIDA: 'Posição desconhecida'
    };

    return posicao.descricaoLocal?.trim()
      ? `${rotulos[posicao.tipo]} — `
        + posicao.descricaoLocal.trim()
      : rotulos[posicao.tipo];
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

  protected rotuloDestino(
    tipo: TipoPosicaoEmbarcacao
  ): string {
    const opcao = this.destinosPermitidos(
      this.tipoSelecionado()
    ).find(
      (item) => item.valor === tipo
    );

    return opcao?.rotulo ?? tipo;
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

  private configurarReacoesFormulario(): void {
    this.formulario.controls
      .embarcacaoId
      .valueChanges
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((embarcacaoId) => {
        this.embarcacaoIdSelecionada.set(
          embarcacaoId
        );

        if (!this.preenchendoFormulario) {
          this.aoAlterarEmbarcacao(
            embarcacaoId
          );
        }
      });

    this.formulario.controls
      .tipo
      .valueChanges
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((tipo) => {
        this.tipoSelecionado.set(tipo);

        if (!this.preenchendoFormulario) {
          this.aplicarRegrasTipo(
            tipo,
            false
          );
        }
      });

    this.formulario.controls
      .tipoPosicaoDestino
      .valueChanges
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((destino) => {
        this.tipoDestinoSelecionado.set(
          destino
        );

        if (!this.preenchendoFormulario) {
          this.configurarVagaDestino(
            this.tipoSelecionado(),
            destino,
            false
          );
        }
      });

    this.formulario.controls
      .vagaDestinoId
      .valueChanges
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((vagaId) => {
        this.vagaDestinoIdSelecionada.set(
          vagaId
        );
      });
  }

  private aoAlterarEmbarcacao(
    embarcacaoId: string
  ): void {
    this.posicaoAtual.set(null);

    this.formulario.controls.tipo.setValue(
      '',
      { emitEvent: false }
    );

    this.formulario.controls
      .tipoPosicaoDestino
      .setValue(
        '',
        { emitEvent: false }
      );

    this.formulario.controls
      .vagaDestinoId
      .setValue(
        '',
        { emitEvent: false }
      );

    this.tipoSelecionado.set('');
    this.tipoDestinoSelecionado.set('');
    this.vagaDestinoIdSelecionada.set('');

    if (!embarcacaoId) {
      return;
    }

    this.carregarPosicaoEmbarcacao(
      embarcacaoId
    );
  }

  private carregarPosicaoEmbarcacao(
    embarcacaoId: string
  ): void {
    this.carregandoPosicao.set(true);

    this.movimentacaoService
      .buscarPosicaoEmbarcacao(
        embarcacaoId
      )
      .pipe(
        finalize(
          () => this.carregandoPosicao.set(false)
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (posicao) => {
          this.posicaoAtual.set(posicao);
        },
        error: (erro: HttpErrorResponse) => {
          const resposta =
            erro.error as ErroApi | null;

          this.mensagemErro.set(
            resposta?.mensagem
            || 'Não foi possível carregar '
            + 'a posição da embarcação.'
          );
        }
      });
  }

  private carregarCriacao(): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    forkJoin({
      embarcacoes:
        this.embarcacaoService.listarTodas(),
      vagas:
        this.vagaService.listarTodas(),
      ocupacoesAtivas:
        this.ocupacaoService.listarTodasAtivas(),
      movimentacoesAbertas:
        this.movimentacaoService
          .listarTodasAbertas(),
      usuarios:
        this.movimentacaoService
          .listarUsuariosAtivos()
          .pipe(
            catchError(() => of([]))
          )
    })
      .pipe(
        finalize(
          () => this.carregando.set(false)
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({
          embarcacoes,
          vagas,
          ocupacoesAtivas,
          movimentacoesAbertas,
          usuarios
        }) => {
          this.vagas.set(vagas);
          this.ocupacoesAtivas.set(
            ocupacoesAtivas
          );
          this.movimentacoesAbertas.set(
            movimentacoesAbertas
          );
          this.usuarios.set(usuarios);

          const embarcacoesComMovimentacao =
            new Set(
              movimentacoesAbertas.map(
                (movimentacao) =>
                  movimentacao.embarcacaoId
              )
            );

          this.embarcacoesDisponiveis.set(
            embarcacoes
              .filter(
                (embarcacao) =>
                  embarcacao.ativa
                  && !embarcacoesComMovimentacao
                    .has(embarcacao.id)
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
        },
        error: () => {
          this.mensagemErro.set(
            'Não foi possível carregar os dados '
            + 'necessários para a movimentação.'
          );
        }
      });
  }

  private carregarEdicao(
    movimentacaoId: string
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    forkJoin({
      movimentacao:
        this.movimentacaoService
          .buscarPorId(movimentacaoId),
      embarcacoes:
        this.embarcacaoService.listarTodas(),
      vagas:
        this.vagaService.listarTodas(),
      ocupacoesAtivas:
        this.ocupacaoService.listarTodasAtivas(),
      movimentacoesAbertas:
        this.movimentacaoService
          .listarTodasAbertas(),
      usuarios:
        this.movimentacaoService
          .listarUsuariosAtivos()
          .pipe(
            catchError(() => of([]))
          )
    })
      .pipe(
        finalize(
          () => this.carregando.set(false)
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (dados) => {
          this.embarcacoesDisponiveis.set(
            dados.embarcacoes
          );
          this.vagas.set(dados.vagas);
          this.ocupacoesAtivas.set(
            dados.ocupacoesAtivas
          );
          this.movimentacoesAbertas.set(
            dados.movimentacoesAbertas
          );
          this.usuarios.set(dados.usuarios);
          this.movimentacaoAtual.set(
            dados.movimentacao
          );

          this.preencherFormulario(
            dados.movimentacao
          );

          this.movimentacaoService
            .buscarPosicaoEmbarcacao(
              dados.movimentacao.embarcacaoId
            )
            .pipe(
              takeUntilDestroyed(
                this.destroyRef
              )
            )
            .subscribe({
              next: (posicao) => {
                this.posicaoAtual.set(posicao);
              },
              error: () => {
                this.mensagemErro.set(
                  'Não foi possível carregar '
                  + 'a posição da embarcação.'
                );
              }
            });
        },
        error: () => {
          this.mensagemErro.set(
            'Não foi possível carregar '
            + 'a movimentação.'
          );
        }
      });
  }

  private preencherFormulario(
    movimentacao: Movimentacao
  ): void {
    this.preenchendoFormulario = true;

    this.formulario.patchValue({
      embarcacaoId:
        movimentacao.embarcacaoId,
      tipo:
        movimentacao.tipo,
      prioridade:
        movimentacao.prioridade,
      tipoPosicaoDestino:
        movimentacao.tipoPosicaoDestino,
      vagaDestinoId:
        movimentacao.vagaDestinoId ?? '',
      descricaoDestino:
        movimentacao.descricaoDestino ?? '',
      agendadaPara:
        this.paraDataHoraLocal(
          movimentacao.agendadaPara
        ),
      operadorResponsavelId:
        movimentacao.operadorResponsavelId
        ?? '',
      observacoes:
        movimentacao.observacoes ?? ''
    });

    this.embarcacaoIdSelecionada.set(
      movimentacao.embarcacaoId
    );
    this.tipoSelecionado.set(
      movimentacao.tipo
    );
    this.tipoDestinoSelecionado.set(
      movimentacao.tipoPosicaoDestino
    );
    this.vagaDestinoIdSelecionada.set(
      movimentacao.vagaDestinoId ?? ''
    );

    this.aplicarRegrasTipo(
      movimentacao.tipo,
      true
    );

    this.formulario.controls
      .embarcacaoId
      .disable({ emitEvent: false });

    this.formulario.controls
      .tipo
      .disable({ emitEvent: false });

    if (movimentacao.status !== 'AGENDADA') {
      this.somenteConsulta.set(true);
      this.formulario.disable({
        emitEvent: false
      });

      this.mensagemErro.set(
        'Somente movimentações agendadas '
        + 'podem ser editadas.'
      );
    }

    this.preenchendoFormulario = false;
  }

  private aplicarRegrasTipo(
    tipo: TipoMovimentacao | '',
    preservarDestino: boolean
  ): void {
    const destinos =
      this.destinosPermitidos(tipo);

    const destinoAtual =
      this.formulario.controls
        .tipoPosicaoDestino
        .value;

    const destino =
      preservarDestino
      && destinos.some(
        (opcao) =>
          opcao.valor === destinoAtual
      )
        ? destinoAtual
        : destinos[0]?.valor ?? '';

    this.formulario.controls
      .tipoPosicaoDestino
      .setValue(
        destino,
        { emitEvent: false }
      );

    this.tipoDestinoSelecionado.set(
      destino
    );

    this.configurarVagaDestino(
      tipo,
      destino,
      preservarDestino
    );
  }

  private configurarVagaDestino(
    tipo: TipoMovimentacao | '',
    destino: TipoPosicaoEmbarcacao | '',
    preservarValor: boolean
  ): void {
    const controle =
      this.formulario.controls.vagaDestinoId;

    if (destino !== 'VAGA') {
      controle.clearValidators();

      if (!preservarValor) {
        controle.setValue(
          '',
          { emitEvent: false }
        );
        this.vagaDestinoIdSelecionada.set('');
      }

      controle.disable({
        emitEvent: false
      });

      controle.updateValueAndValidity({
        emitEvent: false
      });
      return;
    }

    controle.setValidators([
      Validators.required
    ]);

    if (tipo === 'RETIRADA') {
      const vagaId =
        this.ocupacaoAtivaSelecionada()
          ?.vagaId
        ?? '';

      if (!preservarValor || !controle.value) {
        controle.setValue(
          vagaId,
          { emitEvent: false }
        );
        this.vagaDestinoIdSelecionada.set(
          vagaId
        );
      }

      controle.disable({
        emitEvent: false
      });
    } else {
      controle.enable({
        emitEvent: false
      });

      if (!preservarValor) {
        controle.setValue(
          '',
          { emitEvent: false }
        );
        this.vagaDestinoIdSelecionada.set('');
      }
    }

    controle.updateValueAndValidity({
      emitEvent: false
    });
  }

  private destinosPermitidos(
    tipo: TipoMovimentacao | ''
  ): OpcaoDestino[] {
    switch (tipo) {
      case 'LANCAMENTO':
        return [
          {
            valor: 'AGUA',
            rotulo: 'Água'
          },
          {
            valor: 'PIER_ESPERA',
            rotulo: 'Píer de espera'
          }
        ];

      case 'RETIRADA':
      case 'TRANSFERENCIA':
        return [
          {
            valor: 'VAGA',
            rotulo: 'Vaga'
          }
        ];

      case 'DESLOCAMENTO_INTERNO':
        return [
          {
            valor: 'AREA_SERVICO',
            rotulo: 'Área de serviço'
          },
          {
            valor: 'PIER_ESPERA',
            rotulo: 'Píer de espera'
          },
          {
            valor: 'AGUA',
            rotulo: 'Água'
          },
          {
            valor: 'EXTERNA',
            rotulo: 'Área externa'
          }
        ];

      default:
        return [];
    }
  }

  private opcaoTipo(
    tipo: TipoMovimentacao
  ): OpcaoTipo {
    const opcoes: Record<
      TipoMovimentacao,
      OpcaoTipo
    > = {
      LANCAMENTO: {
        valor: 'LANCAMENTO',
        rotulo: 'Lançamento',
        descricao:
          'Leva a embarcação para a água '
          + 'ou para o píer de espera.'
      },
      RETIRADA: {
        valor: 'RETIRADA',
        rotulo: 'Retirada',
        descricao:
          'Retorna a embarcação para '
          + 'a vaga da ocupação ativa.'
      },
      TRANSFERENCIA: {
        valor: 'TRANSFERENCIA',
        rotulo: 'Transferência',
        descricao:
          'Muda definitivamente '
          + 'a embarcação de vaga.'
      },
      DESLOCAMENTO_INTERNO: {
        valor: 'DESLOCAMENTO_INTERNO',
        rotulo: 'Deslocamento interno',
        descricao:
          'Realiza um deslocamento temporário.'
      }
    };

    return opcoes[tipo];
  }

  private montarCriacao(
    valores: ReturnType<
      typeof this.formulario.getRawValue
    >,
    tipo: TipoMovimentacao,
    destino: TipoPosicaoEmbarcacao
  ): CriarMovimentacao {
    return {
      embarcacaoId:
        valores.embarcacaoId,
      tipo,
      prioridade:
        valores.prioridade,
      tipoPosicaoDestino:
        destino,
      vagaDestinoId:
        destino === 'VAGA'
          ? valores.vagaDestinoId || null
          : null,
      descricaoDestino:
        this.textoOuNull(
          valores.descricaoDestino
        ),
      agendadaPara:
        this.paraIso(
          valores.agendadaPara
        ) as string,
      operadorResponsavelId:
        valores.operadorResponsavelId
        || null,
      observacoes:
        this.textoOuNull(
          valores.observacoes
        )
    };
  }

  private montarAtualizacao(
    valores: ReturnType<
      typeof this.formulario.getRawValue
    >,
    destino: TipoPosicaoEmbarcacao
  ): AtualizarMovimentacao {
    return {
      prioridade:
        valores.prioridade,
      tipoPosicaoDestino:
        destino,
      vagaDestinoId:
        destino === 'VAGA'
          ? valores.vagaDestinoId || null
          : null,
      descricaoDestino:
        this.textoOuNull(
          valores.descricaoDestino
        ),
      agendadaPara:
        this.paraIso(
          valores.agendadaPara
        ) as string,
      operadorResponsavelId:
        valores.operadorResponsavelId
        || null,
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

    if (resposta?.errosCampos?.length) {
      this.aplicarErrosCampos(
        resposta.errosCampos
      );
    }

    const mapaCodigos: Partial<
      Record<string, CampoFormulario>
    > = {
      EMBARCACAO_OBRIGATORIA:
        'embarcacaoId',
      TIPO_MOVIMENTACAO_OBRIGATORIO:
        'tipo',
      PRIORIDADE_OBRIGATORIA:
        'prioridade',
      TIPO_POSICAO_DESTINO_OBRIGATORIO:
        'tipoPosicaoDestino',
      VAGA_DESTINO_OBRIGATORIA:
        'vagaDestinoId',
      VAGA_DESTINO_INVALIDA:
        'vagaDestinoId',
      AGENDAMENTO_OBRIGATORIO:
        'agendadaPara',
      OPERADOR_RESPONSAVEL_INVALIDO:
        'operadorResponsavelId'
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

    if (erro.status === 0) {
      this.mensagemErro.set(
        'Não foi possível conectar ao servidor.'
      );
      return;
    }

    if (erro.status === 404) {
      this.mensagemErro.set(
        'A movimentação, embarcação ou vaga '
        + 'não foi encontrada.'
      );
      return;
    }

    this.mensagemErro.set(
      resposta?.mensagem
      || 'Não foi possível salvar '
      + 'a movimentação.'
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
    const controles = Object.values(
      this.formulario.controls
    ) as AbstractControl[];

    controles.forEach((controle) => {
      controle.valueChanges
        .pipe(
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe(() => {
          this.removerErroApi(controle);
        });
    });
  }

  private limparErrosApi(): void {
    const controles = Object.values(
      this.formulario.controls
    ) as AbstractControl[];

    controles.forEach(
      (controle) =>
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

  private dataHoraValida(): ValidatorFn {
    return (
      controle: AbstractControl
    ): ValidationErrors | null => {
      const valor =
        controle.value as string;

      if (!valor) {
        return null;
      }

      const data = new Date(valor);

      return Number.isNaN(data.getTime())
        ? { dataInvalida: true }
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

  private dataHoraInicial(): string {
    const data = new Date(
      Date.now() + 30 * 60_000
    );

    return this.paraDataHoraLocal(
      data.toISOString()
    );
  }

  private paraDataHoraLocal(
    valorIso: string
  ): string {
    const data = new Date(valorIso);
    const deslocamento =
      data.getTimezoneOffset() * 60_000;

    return new Date(
      data.getTime() - deslocamento
    )
      .toISOString()
      .slice(0, 16);
  }

  private paraIso(
    valorLocal: string
  ): string | null {
    const data = new Date(valorLocal);

    if (Number.isNaN(data.getTime())) {
      return null;
    }

    return data.toISOString();
  }

  private textoOuNull(
    texto: string
  ): string | null {
    const valor = texto.trim();

    return valor || null;
  }
}
