import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  FormControl,
  ReactiveFormsModule
} from '@angular/forms';

import {
  BreakpointObserver
} from '@angular/cdk/layout';

import {
  takeUntilDestroyed,
  toSignal
} from '@angular/core/rxjs-interop';

import {
  RouterLink
} from '@angular/router';

import {
  filter,
  finalize,
  forkJoin,
  map,
  switchMap
} from 'rxjs';

import {
  MatButtonModule
} from '@angular/material/button';

import {
  MatDialog,
  MatDialogModule
} from '@angular/material/dialog';

import {
  MatFormFieldModule
} from '@angular/material/form-field';

import {
  MatIconModule
} from '@angular/material/icon';

import {
  MatMenuModule
} from '@angular/material/menu';

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
  DadosDialogoConfirmacao,
  DialogoConfirmacaoComponent
} from '../../../../shared/componentes/dialogo-confirmacao/dialogo-confirmacao.component';

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
  FiltroStatusOcupacao,
  Ocupacao,
  StatusOcupacao
} from '../../models/ocupacao.model';

import {
  OcupacaoService
} from '../../services/ocupacao.service';

@Component({
  selector: 'app-ocupacao-listagem',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl:
    './ocupacao-listagem.component.html',
  styleUrl:
    './ocupacao-listagem.component.scss'
})
export class OcupacaoListagemComponent
  implements OnInit {

  private readonly ocupacaoService =
    inject(OcupacaoService);

  private readonly embarcacaoService =
    inject(EmbarcacaoService);

  private readonly vagaService =
    inject(VagaService);

  private readonly dialog =
    inject(MatDialog);

  private readonly snackBar =
    inject(MatSnackBar);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly breakpointObserver =
    inject(BreakpointObserver);

  private readonly formatadorDataHora =
    new Intl.DateTimeFormat(
      'pt-BR',
      {
        dateStyle: 'short',
        timeStyle: 'short'
      }
    );

  protected readonly telaDesktop = toSignal(
    this.breakpointObserver
      .observe('(min-width: 900px)')
      .pipe(
        map((resultado) => resultado.matches)
      ),
    {
      initialValue: false
    }
  );

  protected readonly filtroEmbarcacao =
    new FormControl(
      'TODAS',
      {
        nonNullable: true
      }
    );

  protected readonly filtroVaga =
    new FormControl(
      'TODAS',
      {
        nonNullable: true
      }
    );

  protected readonly filtroStatus =
    new FormControl<FiltroStatusOcupacao>(
      'TODAS',
      {
        nonNullable: true
      }
    );

  protected readonly ocupacoes =
    signal<Ocupacao[]>([]);

  protected readonly embarcacoes =
    signal<Embarcacao[]>([]);

  protected readonly vagas =
    signal<Vaga[]>([]);

  protected readonly carregando =
    signal(false);

  protected readonly carregandoFiltros =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly paginaAtual =
    signal(0);

  protected readonly totalPaginas =
    signal(0);

  protected readonly totalElementos =
    signal(0);

  protected readonly encerrandoId =
    signal<string | null>(null);

  protected readonly tamanhoPagina = 10;

  ngOnInit(): void {
    this.carregarOpcoesFiltros();
    this.carregarOcupacoes();
  }

  protected alterarFiltroEmbarcacao(): void {
    if (
      this.filtroEmbarcacao.value
        !== 'TODAS'
    ) {
      this.filtroVaga.setValue(
        'TODAS',
        {
          emitEvent: false
        }
      );

      this.filtroStatus.setValue(
        'TODAS',
        {
          emitEvent: false
        }
      );
    }

    this.carregarOcupacoes(0);
  }

  protected alterarFiltroVaga(): void {
    if (this.filtroVaga.value !== 'TODAS') {
      this.filtroEmbarcacao.setValue(
        'TODAS',
        {
          emitEvent: false
        }
      );

      this.filtroStatus.setValue(
        'TODAS',
        {
          emitEvent: false
        }
      );
    }

    this.carregarOcupacoes(0);
  }

  protected alterarFiltroStatus(): void {
    if (
      this.filtroStatus.value
        !== 'TODAS'
    ) {
      this.filtroEmbarcacao.setValue(
        'TODAS',
        {
          emitEvent: false
        }
      );

      this.filtroVaga.setValue(
        'TODAS',
        {
          emitEvent: false
        }
      );
    }

    this.carregarOcupacoes(0);
  }

  protected limparFiltros(): void {
    this.filtroEmbarcacao.setValue(
      'TODAS',
      {
        emitEvent: false
      }
    );

    this.filtroVaga.setValue(
      'TODAS',
      {
        emitEvent: false
      }
    );

    this.filtroStatus.setValue(
      'TODAS',
      {
        emitEvent: false
      }
    );

    this.carregarOcupacoes(0);
  }

  protected paginaAnterior(): void {
    if (this.paginaAtual() > 0) {
      this.carregarOcupacoes(
        this.paginaAtual() - 1
      );
    }
  }

  protected proximaPagina(): void {
    if (
      this.paginaAtual() + 1
        < this.totalPaginas()
    ) {
      this.carregarOcupacoes(
        this.paginaAtual() + 1
      );
    }
  }

  protected solicitarEncerramento(
    ocupacao: Ocupacao
  ): void {
    if (
      ocupacao.status !== 'ATIVA'
      || this.encerrandoId() !== null
    ) {
      return;
    }

    const dadosDialogo:
      DadosDialogoConfirmacao = {
        titulo: 'Encerrar ocupação?',
        mensagem:
          `A ocupação da embarcação ${ocupacao.embarcacaoNome} na vaga ${ocupacao.vagaCodigo} será encerrada agora.`,
        detalhe:
          'A vaga e a embarcação ficarão disponíveis para uma nova ocupação. Esta ação preserva o histórico e não poderá ser desfeita.',
        textoConfirmacao: 'Encerrar',
        icone: 'logout',
        tom: 'perigo'
      };

    const referenciaDialogo =
      this.dialog.open<
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
          ariaLabel:
            'Confirmar encerramento da ocupação'
        }
      );

    referenciaDialogo
      .afterClosed()
      .pipe(
        filter(
          (confirmado):
            confirmado is true =>
              confirmado === true
        ),

        switchMap(() => {
          this.encerrandoId.set(
            ocupacao.id
          );

          return this.ocupacaoService
            .encerrar(
              ocupacao.id,
              new Date().toISOString()
            )
            .pipe(
              finalize(() =>
                this.encerrandoId.set(
                  null
                )
              )
            );
        }),

        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: () => {
          this.snackBar.open(
            'Ocupação encerrada com sucesso.',
            'Fechar',
            {
              duration: 3500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );

          this.carregarOcupacoes(
            this.paginaAtual()
          );
        },

        error: (
          erro: HttpErrorResponse
        ) => {
          this.snackBar.open(
            this.obterMensagemErroEncerramento(
              erro
            ),
            'Fechar',
            {
              duration: 5000,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );
        }
      });
  }

  protected carregarOcupacoes(
    pagina = 0
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    const embarcacaoId =
      this.filtroEmbarcacao.value
        === 'TODAS'
        ? undefined
        : this.filtroEmbarcacao.value;

    const vagaId =
      this.filtroVaga.value === 'TODAS'
        ? undefined
        : this.filtroVaga.value;

    const status:
      StatusOcupacao | undefined =
        this.filtroStatus.value === 'TODAS'
          ? undefined
          : this.filtroStatus.value;

    this.ocupacaoService
      .listar({
        pagina,
        tamanho: this.tamanhoPagina,
        embarcacaoId,
        vagaId,
        status
      })
      .pipe(
        finalize(() =>
          this.carregando.set(false)
        )
      )
      .subscribe({
        next: (resposta) => {
          if (
            pagina > 0
            && resposta.content.length === 0
            && pagina >= resposta.totalPages
          ) {
            this.carregarOcupacoes(
              Math.max(
                resposta.totalPages - 1,
                0
              )
            );

            return;
          }

          this.ocupacoes.set(
            resposta.content
          );

          this.paginaAtual.set(
            resposta.number
          );

          this.totalPaginas.set(
            resposta.totalPages
          );

          this.totalElementos.set(
            resposta.totalElements
          );
        },

        error: () => {
          this.ocupacoes.set([]);

          this.mensagemErro.set(
            'Não foi possível carregar as ocupações.'
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
    ocupacao: Ocupacao
  ): string {
    const partes = [
      ocupacao.embarcacaoModelo,
      ocupacao.proprietarioNome
    ].filter(Boolean);

    return partes.join(' · ');
  }

  protected descricaoVaga(
    ocupacao: Ocupacao
  ): string {
    const partes = [
      ocupacao.vagaSetor,
      ocupacao.vagaLocalizacao
    ].filter(Boolean);

    return partes.length
      ? partes.join(' · ')
      : 'Localização não informada';
  }

  protected formatarDataHora(
    valor: string | null
  ): string {
    if (!valor) {
      return 'Sem previsão';
    }

    return this.formatadorDataHora.format(
      new Date(valor)
    );
  }

  protected rotuloStatus(
    status: StatusOcupacao
  ): string {
    return status === 'ATIVA'
      ? 'Ativa'
      : 'Encerrada';
  }

  protected formatarTipoVaga(
    tipo: Ocupacao['vagaTipo']
  ): string {
    const rotulos:
      Record<Ocupacao['vagaTipo'], string> = {
        MOLHADA: 'Molhada',
        SECA: 'Seca',
        POITA: 'Poita',
        OUTRA: 'Outra'
      };

    return rotulos[tipo];
  }

  private carregarOpcoesFiltros(): void {
    this.carregandoFiltros.set(true);

    forkJoin({
      embarcacoes:
        this.embarcacaoService
          .listarTodas(),

      vagas:
        this.vagaService
          .listarTodas()
    })
      .pipe(
        finalize(() =>
          this.carregandoFiltros.set(false)
        ),

        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: ({
          embarcacoes,
          vagas
        }) => {
          this.embarcacoes.set(
            [...embarcacoes].sort(
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

          this.vagas.set(
            [...vagas].sort(
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
          this.snackBar.open(
            'Não foi possível carregar as opções dos filtros.',
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

  private obterMensagemErroEncerramento(
    erro: HttpErrorResponse
  ): string {
    const resposta =
      erro.error as ErroApi | null;

    if (resposta?.mensagem) {
      return resposta.mensagem;
    }

    if (erro.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    if (erro.status === 404) {
      return 'A ocupação não foi encontrada.';
    }

    if (erro.status === 409) {
      return 'A ocupação já foi encerrada.';
    }

    return 'Não foi possível encerrar a ocupação.';
  }
}
