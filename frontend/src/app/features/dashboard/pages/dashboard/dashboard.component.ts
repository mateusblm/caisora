import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import {
  MatDialog,
  MatDialogModule
} from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import {
  MatProgressSpinnerModule
} from '@angular/material/progress-spinner';
import {
  MatSnackBar,
  MatSnackBarModule
} from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import {
  EMPTY,
  Subject,
  catchError,
  filter,
  finalize,
  map,
  merge,
  startWith,
  switchMap,
  tap,
  timer
} from 'rxjs';

import {
  DadosDialogoConfirmacao,
  DialogoConfirmacaoComponent
} from '../../../../shared/componentes/dialogo-confirmacao/dialogo-confirmacao.component';
import {
  ErroApi
} from '../../../../shared/modelos/erro-api.model';
import {
  CartaoMovimentacaoOperacionalComponent
} from '../../componentes/cartao-movimentacao-operacional/cartao-movimentacao-operacional.component';
import {
  PainelOperacional
} from '../../models/painel-operacional.model';
import {
  PainelOperacionalService
} from '../../services/painel-operacional.service';
import {
  Movimentacao
} from '../../../movimentacoes/models/movimentacao.model';
import {
  MovimentacaoService
} from '../../../movimentacoes/services/movimentacao.service';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    CartaoMovimentacaoOperacionalComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent
implements OnInit {

  private static readonly INTERVALO_ATUALIZACAO =
    30_000;

  private readonly painelOperacionalService =
    inject(PainelOperacionalService);

  private readonly movimentacaoService =
    inject(MovimentacaoService);

  private readonly dialog =
    inject(MatDialog);

  private readonly snackBar =
    inject(MatSnackBar);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly atualizacaoManual =
    new Subject<void>();

  protected readonly painel =
    signal<PainelOperacional | null>(null);

  protected readonly agora =
    signal(new Date());

  protected readonly carregandoInicial =
    signal(true);

  protected readonly atualizando =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly operacaoId =
    signal<string | null>(null);

  protected readonly semOperacoesHoje =
    computed(() => {
      const painel = this.painel();

      if (!painel) {
        return true;
      }

      return (
        painel.atrasadas.length === 0
        && painel.emExecucao.length === 0
        && painel.proximosTrintaMinutos
          .length === 0
        && painel.proximasDuasHoras
          .length === 0
        && painel.restanteDia.length === 0
      );
    });

  ngOnInit(): void {
    this.iniciarRelogio();
    this.iniciarAtualizacaoAutomatica();
  }

  protected atualizarAgora(): void {
    if (this.atualizando()) {
      return;
    }

    this.atualizacaoManual.next();
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

    const dados: DadosDialogoConfirmacao = {
      titulo: 'Iniciar movimentação?',
      mensagem:
        `${this.rotuloOperacao(movimentacao)} `
        + `da embarcação `
        + `${movimentacao.embarcacaoNome}.`,
      detalhe:
        'O início será registrado agora e '
        + 'a operação não poderá mais ser editada.',
      textoConfirmacao: 'Iniciar agora',
      icone: 'play_arrow',
      tom: 'sucesso'
    };

    this.dialog
      .open<
        DialogoConfirmacaoComponent,
        DadosDialogoConfirmacao,
        boolean
      >(
        DialogoConfirmacaoComponent,
        {
          data: dados,
          width: 'calc(100vw - 32px)',
          maxWidth: '460px',
          autoFocus: false,
          restoreFocus: true,
          ariaLabel:
            'Confirmar início da movimentação'
        }
      )
      .afterClosed()
      .pipe(
        filter(
          (confirmado): confirmado is true =>
            confirmado === true
        ),
        switchMap(() => {
          this.operacaoId.set(
            movimentacao.id
          );

          return this.movimentacaoService
            .iniciar(
              movimentacao.id,
              {
                iniciadaEm:
                  new Date().toISOString(),
                observacao: null
              }
            )
            .pipe(
              finalize(
                () => this.operacaoId.set(null)
              )
            );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.exibirSucesso(
            'Movimentação iniciada com sucesso.'
          );
          this.atualizacaoManual.next();
        },
        error: (erro: HttpErrorResponse) => {
          this.exibirErroOperacao(
            erro,
            'Não foi possível iniciar '
            + 'a movimentação.'
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

    const dados: DadosDialogoConfirmacao = {
      titulo: 'Concluir movimentação?',
      mensagem:
        `Confirme que a embarcação `
        + `${movimentacao.embarcacaoNome} `
        + 'chegou ao destino.',
      detalhe:
        'A posição física e, quando aplicável, '
        + 'a ocupação serão atualizadas.',
      textoConfirmacao: 'Concluir',
      icone: 'check_circle',
      tom: 'sucesso'
    };

    this.dialog
      .open<
        DialogoConfirmacaoComponent,
        DadosDialogoConfirmacao,
        boolean
      >(
        DialogoConfirmacaoComponent,
        {
          data: dados,
          width: 'calc(100vw - 32px)',
          maxWidth: '460px',
          autoFocus: false,
          restoreFocus: true,
          ariaLabel:
            'Confirmar conclusão da movimentação'
        }
      )
      .afterClosed()
      .pipe(
        filter(
          (confirmado): confirmado is true =>
            confirmado === true
        ),
        switchMap(() => {
          this.operacaoId.set(
            movimentacao.id
          );

          return this.movimentacaoService
            .concluir(
              movimentacao.id,
              {
                concluidaEm:
                  new Date().toISOString(),
                observacao: null
              }
            )
            .pipe(
              finalize(
                () => this.operacaoId.set(null)
              )
            );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.exibirSucesso(
            'Movimentação concluída com sucesso.'
          );
          this.atualizacaoManual.next();
        },
        error: (erro: HttpErrorResponse) => {
          this.exibirErroOperacao(
            erro,
            'Não foi possível concluir '
            + 'a movimentação.'
          );
        }
      });
  }

  protected rolarPara(
    identificador: string
  ): void {
    document
      .getElementById(identificador)
      ?.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
  }

  protected formatarAtualizacao(): string {
    const geradoEm =
      this.painel()?.geradoEm;

    if (!geradoEm) {
      return 'Aguardando atualização';
    }

    const data = new Date(geradoEm);

    if (Number.isNaN(data.getTime())) {
      return 'Atualização concluída';
    }

    return new Intl.DateTimeFormat(
      'pt-BR',
      {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        timeZone:
          this.painel()?.fusoHorario
          ?? 'America/Sao_Paulo'
      }
    ).format(data);
  }

  protected formatarDataPainel(): string {
    const valor =
      this.painel()?.geradoEm
      ?? new Date().toISOString();

    const data = new Date(valor);

    return new Intl.DateTimeFormat(
      'pt-BR',
      {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
        timeZone:
          this.painel()?.fusoHorario
          ?? 'America/Sao_Paulo'
      }
    ).format(data);
  }

  protected formatarHora(
    valor: string | null
  ): string {
    if (!valor) {
      return '--:--';
    }

    const data = new Date(valor);

    if (Number.isNaN(data.getTime())) {
      return '--:--';
    }

    return new Intl.DateTimeFormat(
      'pt-BR',
      {
        hour: '2-digit',
        minute: '2-digit',
        timeZone:
          this.painel()?.fusoHorario
          ?? 'America/Sao_Paulo'
      }
    ).format(data);
  }

  protected rotuloOperacao(
    movimentacao: Movimentacao
  ): string {
    switch (movimentacao.tipo) {
      case 'LANCAMENTO':
        return 'Descida para a água';

      case 'RETIRADA':
        return 'Subida para a vaga';

      case 'RETORNO_PARA_VAGA':
        return 'Retorno para a vaga';

      case 'TRANSFERENCIA':
        return 'Transferência de vaga';

      case 'DESLOCAMENTO_INTERNO':
        return 'Deslocamento interno';
    }
  }

  private iniciarRelogio(): void {
    timer(60_000, 60_000)
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(
        () => this.agora.set(new Date())
      );
  }

  private iniciarAtualizacaoAutomatica(): void {
    const atualizacaoAutomatica =
      timer(
        DashboardComponent
          .INTERVALO_ATUALIZACAO,
        DashboardComponent
          .INTERVALO_ATUALIZACAO
      ).pipe(
        map(() => void 0)
      );

    merge(
      this.atualizacaoManual,
      atualizacaoAutomatica
    )
      .pipe(
        startWith(void 0),
        tap(() => {
          this.agora.set(new Date());
          this.mensagemErro.set(null);

          if (this.painel()) {
            this.atualizando.set(true);
          } else {
            this.carregandoInicial.set(true);
          }
        }),
        switchMap(
          () =>
            this.painelOperacionalService
              .buscar()
              .pipe(
                tap((painel) => {
                  this.painel.set(painel);
                  this.mensagemErro.set(null);
                }),
                catchError(
                  (erro: HttpErrorResponse) => {
                    this.tratarErroCarregamento(
                      erro
                    );

                    return EMPTY;
                  }
                ),
                finalize(() => {
                  this.carregandoInicial
                    .set(false);
                  this.atualizando.set(false);
                })
              )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private tratarErroCarregamento(
    erro: HttpErrorResponse
  ): void {
    const resposta =
      erro.error as ErroApi | null;

    if (erro.status === 0) {
      this.mensagemErro.set(
        'Não foi possível conectar ao servidor.'
      );
      return;
    }

    this.mensagemErro.set(
      resposta?.mensagem
      || (
        this.painel()
          ? 'Não foi possível atualizar o painel.'
          : 'Não foi possível carregar '
            + 'o painel operacional.'
      )
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
    const resposta =
      erro.error as ErroApi | null;

    const mensagem =
      resposta?.mensagem
      || (
        erro.status === 0
          ? 'Não foi possível conectar '
            + 'ao servidor.'
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
