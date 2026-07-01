import { BreakpointObserver } from '@angular/cdk/layout';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal
} from '@angular/core';
import {
  takeUntilDestroyed,
  toSignal
} from '@angular/core/rxjs-interop';
import {
  FormControl,
  ReactiveFormsModule
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MatDialog,
  MatDialogModule
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import {
  MatSnackBar,
  MatSnackBarModule
} from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import {
  filter,
  finalize,
  map,
  switchMap
} from 'rxjs';

import {
  DadosDialogoConfirmacao,
  DialogoConfirmacaoComponent
} from '../../../../shared/componentes/dialogo-confirmacao/dialogo-confirmacao.component';
import { ErroApi } from '../../../../shared/modelos/erro-api.model';
import { Embarcacao } from '../../../embarcacoes/models/embarcacao.model';
import { EmbarcacaoService } from '../../../embarcacoes/services/embarcacao.service';
import {
  DadosCancelamentoMovimentacao,
  CancelamentoMovimentacaoDialogComponent
} from '../../componentes/cancelamento-movimentacao-dialog/cancelamento-movimentacao-dialog.component';
import {
  FiltroStatusMovimentacao,
  FiltroTipoMovimentacao,
  Movimentacao,
  PrioridadeMovimentacao,
  StatusMovimentacao,
  TipoMovimentacao,
  TipoPosicaoEmbarcacao
} from '../../models/movimentacao.model';
import { MovimentacaoService } from '../../services/movimentacao.service';

@Component({
  selector: 'app-movimentacao-listagem',
  imports: [
    NgClass,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl: './movimentacao-listagem.component.html',
  styleUrl: './movimentacao-listagem.component.scss'
})
export class MovimentacaoListagemComponent implements OnInit {

  private readonly movimentacaoService = inject(MovimentacaoService);
  private readonly embarcacaoService = inject(EmbarcacaoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly breakpointObserver = inject(BreakpointObserver);

  private readonly formatadorDataHora = new Intl.DateTimeFormat(
    'pt-BR',
    {
      dateStyle: 'short',
      timeStyle: 'short'
    }
  );

  protected readonly telaDesktop = toSignal(
    this.breakpointObserver
      .observe('(min-width: 1050px)')
      .pipe(
        map((resultado) => resultado.matches)
      ),
    {
      initialValue: false
    }
  );

  protected readonly filtroStatus =
    new FormControl<FiltroStatusMovimentacao>(
      'TODAS',
      { nonNullable: true }
    );

  protected readonly filtroTipo =
    new FormControl<FiltroTipoMovimentacao>(
      'TODOS',
      { nonNullable: true }
    );

  protected readonly filtroEmbarcacao =
    new FormControl<string>(
      'TODAS',
      { nonNullable: true }
    );

  protected readonly filtroInicio =
    new FormControl<string>(
      '',
      { nonNullable: true }
    );

  protected readonly filtroFim =
    new FormControl<string>(
      '',
      { nonNullable: true }
    );

  protected readonly movimentacoes =
    signal<Movimentacao[]>([]);

  protected readonly embarcacoes =
    signal<Embarcacao[]>([]);

  protected readonly carregando =
    signal(false);

  protected readonly carregandoEmbarcacoes =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly paginaAtual =
    signal(0);

  protected readonly totalPaginas =
    signal(0);

  protected readonly totalElementos =
    signal(0);

  protected readonly operacaoId =
    signal<string | null>(null);

  protected readonly tamanhoPagina = 10;

  ngOnInit(): void {
    this.carregarEmbarcacoes();
    this.carregarMovimentacoes();
  }

  protected alterarFiltroStatus(): void {
    if (this.filtroStatus.value !== 'TODAS') {
      this.filtroTipo.setValue(
        'TODOS',
        { emitEvent: false }
      );
      this.filtroEmbarcacao.setValue(
        'TODAS',
        { emitEvent: false }
      );
      this.limparPeriodo();
    }

    this.carregarMovimentacoes(0);
  }

  protected alterarFiltroTipo(): void {
    if (this.filtroTipo.value !== 'TODOS') {
      this.filtroStatus.setValue(
        'TODAS',
        { emitEvent: false }
      );
      this.filtroEmbarcacao.setValue(
        'TODAS',
        { emitEvent: false }
      );
      this.limparPeriodo();
    }

    this.carregarMovimentacoes(0);
  }

  protected alterarFiltroEmbarcacao(): void {
    if (this.filtroEmbarcacao.value !== 'TODAS') {
      this.filtroStatus.setValue(
        'TODAS',
        { emitEvent: false }
      );
      this.filtroTipo.setValue(
        'TODOS',
        { emitEvent: false }
      );
      this.limparPeriodo();
    }

    this.carregarMovimentacoes(0);
  }

  protected aplicarFiltroPeriodo(): void {
    const inicioTexto = this.filtroInicio.value;
    const fimTexto = this.filtroFim.value;

    if (!inicioTexto || !fimTexto) {
      this.snackBar.open(
        'Informe o início e o fim do período.',
        'Fechar',
        {
          duration: 4000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        }
      );
      return;
    }

    const inicio = new Date(inicioTexto);
    const fim = new Date(fimTexto);

    if (
      Number.isNaN(inicio.getTime())
      || Number.isNaN(fim.getTime())
      || fim <= inicio
    ) {
      this.snackBar.open(
        'O fim do período deve ser posterior ao início.',
        'Fechar',
        {
          duration: 4000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        }
      );
      return;
    }

    this.filtroStatus.setValue(
      'TODAS',
      { emitEvent: false }
    );
    this.filtroTipo.setValue(
      'TODOS',
      { emitEvent: false }
    );
    this.filtroEmbarcacao.setValue(
      'TODAS',
      { emitEvent: false }
    );

    this.carregarMovimentacoes(0);
  }

  protected limparFiltros(): void {
    this.filtroStatus.setValue(
      'TODAS',
      { emitEvent: false }
    );
    this.filtroTipo.setValue(
      'TODOS',
      { emitEvent: false }
    );
    this.filtroEmbarcacao.setValue(
      'TODAS',
      { emitEvent: false }
    );
    this.limparPeriodo();

    this.carregarMovimentacoes(0);
  }

  protected paginaAnterior(): void {
    if (this.paginaAtual() > 0) {
      this.carregarMovimentacoes(
        this.paginaAtual() - 1
      );
    }
  }

  protected proximaPagina(): void {
    if (
      this.paginaAtual() + 1
      < this.totalPaginas()
    ) {
      this.carregarMovimentacoes(
        this.paginaAtual() + 1
      );
    }
  }

  protected solicitarInicio(
    movimentacao: Movimentacao
  ): void {
    if (
      movimentacao.status !== 'AGENDADA'
      || this.operacaoId() !== null
    ) {
      return;
    }

    const dadosDialogo: DadosDialogoConfirmacao = {
      titulo: 'Iniciar movimentação?',
      mensagem:
        `A movimentação da embarcação `
        + `${movimentacao.embarcacaoNome} será iniciada agora.`,
      detalhe:
        'Após o início, ela não poderá ser editada nem cancelada.',
      textoConfirmacao: 'Iniciar',
      icone: 'play_arrow',
      tom: 'sucesso'
    };

    const referencia = this.dialog.open<
      DialogoConfirmacaoComponent,
      DadosDialogoConfirmacao,
      boolean
    >(
      DialogoConfirmacaoComponent,
      {
        data: dadosDialogo,
        width: 'calc(100vw - 32px)',
        maxWidth: '460px',
        autoFocus: false,
        restoreFocus: true,
        ariaLabel: 'Confirmar início da movimentação'
      }
    );

    referencia
      .afterClosed()
      .pipe(
        filter(
          (confirmado): confirmado is true =>
            confirmado === true
        ),
        switchMap(() => {
          this.operacaoId.set(movimentacao.id);

          return this.movimentacaoService
            .iniciar(
              movimentacao.id,
              {
                iniciadaEm: new Date().toISOString(),
                observacao: null
              }
            )
            .pipe(
              finalize(() => this.operacaoId.set(null))
            );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.exibirSucesso(
            'Movimentação iniciada com sucesso.'
          );
          this.carregarMovimentacoes(
            this.paginaAtual()
          );
        },
        error: (erro: HttpErrorResponse) => {
          this.exibirErroOperacao(
            erro,
            'Não foi possível iniciar a movimentação.'
          );
        }
      });
  }

  protected solicitarConclusao(
    movimentacao: Movimentacao
  ): void {
    if (
      movimentacao.status !== 'EM_EXECUCAO'
      || this.operacaoId() !== null
    ) {
      return;
    }

    const dadosDialogo: DadosDialogoConfirmacao = {
      titulo: 'Concluir movimentação?',
      mensagem:
        `Confirme que a embarcação `
        + `${movimentacao.embarcacaoNome} chegou ao destino.`,
      detalhe:
        'A posição atual da embarcação e, quando aplicável, '
        + 'a ocupação serão atualizadas.',
      textoConfirmacao: 'Concluir',
      icone: 'check_circle',
      tom: 'sucesso'
    };

    const referencia = this.dialog.open<
      DialogoConfirmacaoComponent,
      DadosDialogoConfirmacao,
      boolean
    >(
      DialogoConfirmacaoComponent,
      {
        data: dadosDialogo,
        width: 'calc(100vw - 32px)',
        maxWidth: '460px',
        autoFocus: false,
        restoreFocus: true,
        ariaLabel: 'Confirmar conclusão da movimentação'
      }
    );

    referencia
      .afterClosed()
      .pipe(
        filter(
          (confirmado): confirmado is true =>
            confirmado === true
        ),
        switchMap(() => {
          this.operacaoId.set(movimentacao.id);

          return this.movimentacaoService
            .concluir(
              movimentacao.id,
              {
                concluidaEm: new Date().toISOString(),
                observacao: null
              }
            )
            .pipe(
              finalize(() => this.operacaoId.set(null))
            );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.exibirSucesso(
            'Movimentação concluída com sucesso.'
          );
          this.carregarMovimentacoes(
            this.paginaAtual()
          );
        },
        error: (erro: HttpErrorResponse) => {
          this.exibirErroOperacao(
            erro,
            'Não foi possível concluir a movimentação.'
          );
        }
      });
  }

  protected solicitarCancelamento(
    movimentacao: Movimentacao
  ): void {
    if (
      movimentacao.status !== 'AGENDADA'
      || this.operacaoId() !== null
    ) {
      return;
    }

    const dados: DadosCancelamentoMovimentacao = {
      embarcacaoNome: movimentacao.embarcacaoNome
    };

    const referencia = this.dialog.open<
      CancelamentoMovimentacaoDialogComponent,
      DadosCancelamentoMovimentacao,
      string | null
    >(
      CancelamentoMovimentacaoDialogComponent,
      {
        data: dados,
        width: 'calc(100vw - 32px)',
        maxWidth: '520px',
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        ariaLabel: 'Cancelar movimentação'
      }
    );

    referencia
      .afterClosed()
      .pipe(
        filter(
          (motivo): motivo is string =>
            Boolean(motivo?.trim())
        ),
        switchMap((motivo) => {
          this.operacaoId.set(movimentacao.id);

          return this.movimentacaoService
            .cancelar(
              movimentacao.id,
              {
                canceladaEm: new Date().toISOString(),
                motivo: motivo.trim()
              }
            )
            .pipe(
              finalize(() => this.operacaoId.set(null))
            );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.exibirSucesso(
            'Movimentação cancelada com sucesso.'
          );
          this.carregarMovimentacoes(
            this.paginaAtual()
          );
        },
        error: (erro: HttpErrorResponse) => {
          this.exibirErroOperacao(
            erro,
            'Não foi possível cancelar a movimentação.'
          );
        }
      });
  }

  protected carregarMovimentacoes(
    pagina = 0
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    const status: StatusMovimentacao | undefined =
      this.filtroStatus.value === 'TODAS'
        ? undefined
        : this.filtroStatus.value;

    const tipo: TipoMovimentacao | undefined =
      this.filtroTipo.value === 'TODOS'
        ? undefined
        : this.filtroTipo.value;

    const embarcacaoId =
      this.filtroEmbarcacao.value === 'TODAS'
        ? undefined
        : this.filtroEmbarcacao.value;

    const periodo = this.obterPeriodo();

    this.movimentacaoService
      .listar({
        pagina,
        tamanho: this.tamanhoPagina,
        status,
        tipo,
        embarcacaoId,
        inicio: periodo?.inicio,
        fim: periodo?.fim
      })
      .pipe(
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (resposta) => {
          if (
            pagina > 0
            && resposta.content.length === 0
            && pagina >= resposta.totalPages
          ) {
            this.carregarMovimentacoes(
              Math.max(resposta.totalPages - 1, 0)
            );
            return;
          }

          this.movimentacoes.set(resposta.content);
          this.paginaAtual.set(resposta.number);
          this.totalPaginas.set(resposta.totalPages);
          this.totalElementos.set(resposta.totalElements);
        },
        error: () => {
          this.movimentacoes.set([]);
          this.mensagemErro.set(
            'Não foi possível carregar as movimentações.'
          );
        }
      });
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
    movimentacao: Movimentacao
  ): string {
    return [
      movimentacao.embarcacaoModelo,
      movimentacao.proprietarioNome
    ]
      .filter(Boolean)
      .join(' · ');
  }

  protected descricaoOrigem(
    movimentacao: Movimentacao
  ): string {
    return this.descricaoPosicao(
      movimentacao.tipoPosicaoOrigem,
      movimentacao.vagaOrigemCodigo,
      movimentacao.descricaoOrigem
    );
  }

  protected descricaoDestino(
    movimentacao: Movimentacao
  ): string {
    return this.descricaoPosicao(
      movimentacao.tipoPosicaoDestino,
      movimentacao.vagaDestinoCodigo,
      movimentacao.descricaoDestino
    );
  }

  protected formatarDataHora(
    valor: string | null
  ): string {
    if (!valor) {
      return 'Não informado';
    }

    const data = new Date(valor);

    if (Number.isNaN(data.getTime())) {
      return 'Data inválida';
    }

    return this.formatadorDataHora.format(data);
  }

  protected rotuloStatus(
    status: StatusMovimentacao
  ): string {
    const rotulos: Record<
      StatusMovimentacao,
      string
    > = {
      AGENDADA: 'Agendada',
      EM_EXECUCAO: 'Em execução',
      CONCLUIDA: 'Concluída',
      CANCELADA: 'Cancelada'
    };

    return rotulos[status];
  }

  protected rotuloTipo(
    tipo: TipoMovimentacao
  ): string {
    const rotulos: Record<
      TipoMovimentacao,
      string
    > = {
      LANCAMENTO: 'Lançamento',
      RETIRADA: 'Retirada',
      RETORNO_PARA_VAGA: 'Retorno para a vaga',
      TRANSFERENCIA: 'Transferência',
      DESLOCAMENTO_INTERNO: 'Deslocamento interno'
    };

    return rotulos[tipo];
  }

  protected rotuloPrioridade(
    prioridade: PrioridadeMovimentacao
  ): string {
    const rotulos: Record<
      PrioridadeMovimentacao,
      string
    > = {
      NORMAL: 'Normal',
      ALTA: 'Alta',
      URGENTE: 'Urgente'
    };

    return rotulos[prioridade];
  }

  protected classeStatus(
    status: StatusMovimentacao
  ): string {
    return `status--${status.toLowerCase()}`;
  }

  protected classePrioridade(
    prioridade: PrioridadeMovimentacao
  ): string {
    return `prioridade--${prioridade.toLowerCase()}`;
  }

  protected possuiAcao(
    movimentacao: Movimentacao
  ): boolean {
    return (
      movimentacao.status === 'AGENDADA'
      || movimentacao.status === 'EM_EXECUCAO'
    );
  }

  private carregarEmbarcacoes(): void {
    this.carregandoEmbarcacoes.set(true);

    this.embarcacaoService
      .listar({
        pagina: 0,
        tamanho: 200,
        ativa: true
      })
      .pipe(
        finalize(
          () => this.carregandoEmbarcacoes.set(false)
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (resposta) => {
          const embarcacoes = [...resposta.content]
            .sort(
              (primeira, segunda) =>
                this.nomeExibicaoEmbarcacao(primeira)
                  .localeCompare(
                    this.nomeExibicaoEmbarcacao(segunda),
                    'pt-BR'
                  )
            );

          this.embarcacoes.set(embarcacoes);
        },
        error: () => {
          this.snackBar.open(
            'Não foi possível carregar as embarcações do filtro.',
            'Fechar',
            {
              duration: 4500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );
        }
      });
  }

  private descricaoPosicao(
    tipo: TipoPosicaoEmbarcacao,
    vagaCodigo: string | null,
    descricao: string | null
  ): string {
    if (tipo === 'VAGA') {
      return vagaCodigo
        ? `Vaga ${vagaCodigo}`
        : 'Vaga não informada';
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

    const local = rotulos[tipo];

    return descricao?.trim()
      ? `${local} · ${descricao.trim()}`
      : local;
  }

  private obterPeriodo():
    | {
      inicio: string;
      fim: string;
    }
    | undefined {
    if (
      !this.filtroInicio.value
      || !this.filtroFim.value
    ) {
      return undefined;
    }

    const inicio = new Date(this.filtroInicio.value);
    const fim = new Date(this.filtroFim.value);

    if (
      Number.isNaN(inicio.getTime())
      || Number.isNaN(fim.getTime())
      || fim <= inicio
    ) {
      return undefined;
    }

    return {
      inicio: inicio.toISOString(),
      fim: fim.toISOString()
    };
  }

  private limparPeriodo(): void {
    this.filtroInicio.setValue(
      '',
      { emitEvent: false }
    );
    this.filtroFim.setValue(
      '',
      { emitEvent: false }
    );
  }

  private exibirSucesso(
    mensagem: string
  ): void {
    this.snackBar.open(
      mensagem,
      'Fechar',
      {
        duration: 3500,
        horizontalPosition: 'center',
        verticalPosition: 'bottom'
      }
    );
  }

  private exibirErroOperacao(
    erro: HttpErrorResponse,
    mensagemPadrao: string
  ): void {
    const resposta = erro.error as ErroApi | null;

    const mensagem =
      resposta?.mensagem
      || (
        erro.status === 0
          ? 'Não foi possível conectar ao servidor.'
          : mensagemPadrao
      );

    this.snackBar.open(
      mensagem,
      'Fechar',
      {
        duration: 5000,
        horizontalPosition: 'center',
        verticalPosition: 'bottom'
      }
    );
  }
}
