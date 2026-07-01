import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  HostListener,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MatProgressSpinnerModule
} from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import {
  EMPTY,
  Subject,
  catchError,
  finalize,
  map,
  merge,
  startWith,
  switchMap,
  tap,
  timer
} from 'rxjs';

import {
  ErroApi
} from '../../../../shared/modelos/erro-api.model';
import {
  CartaoPainelTvComponent
} from '../../componentes/cartao-painel-tv/cartao-painel-tv.component';
import {
  AcaoOperacionalPainelTv,
  MovimentacaoPainelTv,
  PainelTvOperacional
} from '../../models/painel-tv.model';
import {
  PainelTvService
} from '../../services/painel-tv.service';

@Component({
  selector: 'app-painel-tv',
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CartaoPainelTvComponent
  ],
  templateUrl: './painel-tv.component.html',
  styleUrl: './painel-tv.component.scss'
})
export class PainelTvComponent
implements OnInit {

  private static readonly INTERVALO_PADRAO =
    15_000;

  protected readonly limitePrincipal = 4;
  protected readonly limiteSecundario = 2;
  protected readonly limiteAlertas = 3;

  private readonly service =
    inject(PainelTvService);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly atualizacaoManual =
    new Subject<void>();

  protected readonly painel =
    signal<PainelTvOperacional | null>(null);

  protected readonly agora =
    signal(new Date());

  protected readonly carregando =
    signal(true);

  protected readonly atualizando =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly telaCheia =
    signal(false);

  protected readonly alertasVisiveis =
    computed(
      () =>
        this.painel()
          ?.alertas
          .slice(0, this.limiteAlertas)
        ?? []
    );

  protected readonly descidasVisiveis =
    computed(
      () =>
        this.painel()
          ?.descidasParaAgua
          .slice(0, this.limitePrincipal)
        ?? []
    );

  protected readonly retiradasVisiveis =
    computed(
      () =>
        this.painel()
          ?.retiradasDaAgua
          .slice(0, this.limitePrincipal)
        ?? []
    );

  protected readonly execucoesVisiveis =
    computed(
      () =>
        this.painel()
          ?.emExecucao
          .slice(0, this.limitePrincipal)
        ?? []
    );

  protected readonly transferenciasVisiveis =
    computed(
      () =>
        this.painel()
          ?.transferenciasDeVaga
          .slice(0, this.limiteSecundario)
        ?? []
    );

  protected readonly deslocamentosVisiveis =
    computed(
      () =>
        this.painel()
          ?.deslocamentosInternos
          .slice(0, this.limiteSecundario)
        ?? []
    );

  ngOnInit(): void {
    this.iniciarRelogio();
    this.iniciarAtualizacao();
  }

  @HostListener('document:fullscreenchange')
  protected aoAlterarTelaCheia(): void {
    this.telaCheia.set(
      document.fullscreenElement !== null
    );
  }

  protected atualizarAgora(): void {
    this.atualizacaoManual.next();
  }

  protected async alternarTelaCheia():
    Promise<void> {
    try {
      if (document.fullscreenElement) {
        await document.exitFullscreen();
        return;
      }

      await document.documentElement
        .requestFullscreen();
    } catch {
      this.mensagemErro.set(
        'O navegador não permitiu entrar '
        + 'em tela cheia.'
      );
    }
  }

  protected formatarRelogio(): string {
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
    ).format(this.agora());
  }

  protected formatarData(): string {
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
    ).format(this.agora());
  }

  protected formatarHora(
    valor: string
  ): string {
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

  protected formatarUltimaAtualizacao():
    string {
    const valor = this.painel()?.geradoEm;

    if (!valor) {
      return '--:--:--';
    }

    const data = new Date(valor);

    if (Number.isNaN(data.getTime())) {
      return '--:--:--';
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

  protected quantidadeRestante(
    quantidade: number,
    limite: number
  ): number {
    return Math.max(0, quantidade - limite);
  }

  protected rotuloAcao(
    acao: AcaoOperacionalPainelTv
  ): string {
    const rotulos: Record<
      AcaoOperacionalPainelTv,
      string
    > = {
      DESCER_PARA_AGUA: 'Descer',
      RETIRAR_DA_AGUA: 'Retirar da água',
      RETORNAR_PARA_VAGA: 'Retornar',
      TRANSFERIR_DE_VAGA: 'Transferir',
      DESLOCAR_INTERNAMENTE: 'Deslocar'
    };

    return rotulos[acao];
  }

  protected descricaoAlerta(
    item: MovimentacaoPainelTv
  ): string {
    const partes: string[] = [];

    if (item.alertas.includes('ATRASADA')) {
      partes.push(
        `${item.minutosAtraso} min atrasada`
      );
    }

    if (item.alertas.includes('URGENTE')) {
      partes.push('urgente');
    }

    if (
      item.alertas.includes('SEM_OPERADOR')
    ) {
      partes.push('sem operador');
    }

    if (
      item.alertas.includes('PROXIMA')
      && !item.alertas.includes('ATRASADA')
    ) {
      partes.push(
        item.minutosParaInicio <= 0
          ? 'agora'
          : `em ${item.minutosParaInicio} min`
      );
    }

    return partes.join(' · ');
  }

  private iniciarRelogio(): void {
    timer(0, 1_000)
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(
        () => this.agora.set(new Date())
      );
  }

  private iniciarAtualizacao(): void {
    const atualizacaoAutomatica =
      timer(
        PainelTvComponent.INTERVALO_PADRAO,
        PainelTvComponent.INTERVALO_PADRAO
      ).pipe(
        map(() => void 0)
      );

    merge(
      atualizacaoAutomatica,
      this.atualizacaoManual
    )
      .pipe(
        startWith(void 0),
        tap(() => {
          this.mensagemErro.set(null);

          if (this.painel()) {
            this.atualizando.set(true);
          } else {
            this.carregando.set(true);
          }
        }),
        switchMap(
          () =>
            this.service.buscar().pipe(
              tap((painel) => {
                this.painel.set(painel);
              }),
              catchError(
                (erro: HttpErrorResponse) => {
                  this.tratarErro(erro);
                  return EMPTY;
                }
              ),
              finalize(() => {
                this.carregando.set(false);
                this.atualizando.set(false);
              })
            )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private tratarErro(
    erro: HttpErrorResponse
  ): void {
    const resposta =
      erro.error as ErroApi | null;

    if (erro.status === 0) {
      this.mensagemErro.set(
        'Sem conexão com o servidor.'
      );
      return;
    }

    this.mensagemErro.set(
      resposta?.mensagem
      || 'Não foi possível atualizar o painel.'
    );
  }
}
