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
  MatInputModule
} from '@angular/material/input';

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
  Embarcacao,
  FiltroStatusEmbarcacao,
  FiltroTipoEmbarcacao,
  TipoEmbarcacao,
  TipoPropulsao
} from '../../models/embarcacao.model';

import {
  EmbarcacaoService
} from '../../services/embarcacao.service';

interface OpcaoTipoEmbarcacao {
  valor: TipoEmbarcacao;
  rotulo: string;
}

@Component({
  selector: 'app-embarcacao-listagem',
  imports: [
    ReactiveFormsModule,
    RouterLink,
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
  templateUrl:
    './embarcacao-listagem.component.html',
  styleUrl:
    './embarcacao-listagem.component.scss'
})
export class EmbarcacaoListagemComponent
  implements OnInit {

  private readonly embarcacaoService =
    inject(EmbarcacaoService);

  private readonly dialog =
    inject(MatDialog);

  private readonly snackBar =
    inject(MatSnackBar);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly breakpointObserver =
    inject(BreakpointObserver);

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

  protected readonly busca =
    new FormControl(
      '',
      {
        nonNullable: true
      }
    );

  protected readonly filtroTipo =
    new FormControl<FiltroTipoEmbarcacao>(
      'TODAS',
      {
        nonNullable: true
      }
    );

  protected readonly filtroStatus =
    new FormControl<FiltroStatusEmbarcacao>(
      'TODAS',
      {
        nonNullable: true
      }
    );

  protected readonly embarcacoes =
    signal<Embarcacao[]>([]);

  protected readonly carregando =
    signal(false);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly paginaAtual =
    signal(0);

  protected readonly totalPaginas =
    signal(0);

  protected readonly totalElementos =
    signal(0);

  protected readonly alterandoStatusId =
    signal<string | null>(null);

  protected readonly tamanhoPagina = 10;

  protected readonly tipos:
    OpcaoTipoEmbarcacao[] = [
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

  ngOnInit(): void {
    this.carregarEmbarcacoes();
  }

  protected pesquisar(): void {
    const nome = this.busca.value.trim();

    if (nome) {
      this.filtroTipo.setValue(
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

    this.carregarEmbarcacoes(0);
  }

  protected alterarFiltroTipo(): void {
    if (this.filtroTipo.value !== 'TODAS') {
      this.busca.setValue(
        '',
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

    this.carregarEmbarcacoes(0);
  }

  protected alterarFiltroStatus(): void {
    if (
      this.filtroStatus.value !== 'TODAS'
    ) {
      this.busca.setValue(
        '',
        {
          emitEvent: false
        }
      );

      this.filtroTipo.setValue(
        'TODAS',
        {
          emitEvent: false
        }
      );
    }

    this.carregarEmbarcacoes(0);
  }

  protected limparFiltros(): void {
    this.busca.setValue('');
    this.filtroTipo.setValue('TODAS');
    this.filtroStatus.setValue('TODAS');

    this.carregarEmbarcacoes(0);
  }

  protected paginaAnterior(): void {
    if (this.paginaAtual() > 0) {
      this.carregarEmbarcacoes(
        this.paginaAtual() - 1
      );
    }
  }

  protected proximaPagina(): void {
    if (
      this.paginaAtual() + 1
      < this.totalPaginas()
    ) {
      this.carregarEmbarcacoes(
        this.paginaAtual() + 1
      );
    }
  }

  protected solicitarAlteracaoStatus(
    embarcacao: Embarcacao
  ): void {
    if (this.alterandoStatusId() !== null) {
      return;
    }

    const novoStatus =
      !embarcacao.ativa;

    const nome =
      this.nomeExibicao(embarcacao);

    const dadosDialogo:
      DadosDialogoConfirmacao =
        novoStatus
          ? {
              titulo: 'Ativar embarcação?',
              mensagem:
                `A embarcação ${nome} voltará a ficar disponível para as operações da marina.`,
              detalhe:
                'O cadastro permanecerá com os mesmos dados, proprietário e histórico.',
              textoConfirmacao: 'Ativar',
              icone: 'sailing',
              tom: 'sucesso'
            }
          : {
              titulo: 'Inativar embarcação?',
              mensagem:
                `A embarcação ${nome} deixará de ficar disponível para novas operações da marina.`,
              detalhe:
                'O cadastro não será excluído e poderá ser reativado posteriormente.',
              textoConfirmacao: 'Inativar',
              icone: 'directions_boat_filled',
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
          maxWidth: '440px',
          autoFocus: false,
          restoreFocus: true,
          ariaLabel:
            novoStatus
              ? 'Confirmar ativação da embarcação'
              : 'Confirmar inativação da embarcação'
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
          this.alterandoStatusId.set(
            embarcacao.id
          );

          return this.embarcacaoService
            .alterarStatus(
              embarcacao.id,
              novoStatus
            )
            .pipe(
              finalize(() =>
                this.alterandoStatusId.set(
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
            novoStatus
              ? 'Embarcação ativada com sucesso.'
              : 'Embarcação inativada com sucesso.',
            'Fechar',
            {
              duration: 3500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );

          this.carregarEmbarcacoes(
            this.paginaAtual()
          );
        },

        error: (
          erro: HttpErrorResponse
        ) => {
          this.snackBar.open(
            this.obterMensagemErroStatus(
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

  protected nomeExibicao(
    embarcacao: Embarcacao
  ): string {
    return (
      embarcacao.nome?.trim()
      || embarcacao.modelo?.trim()
      || 'Embarcação sem nome'
    );
  }

  protected formatarTipo(
    tipo: TipoEmbarcacao
  ): string {
    return this.tipos.find(
      (opcao) => opcao.valor === tipo
    )?.rotulo ?? tipo;
  }

  protected formatarPropulsao(
    tipo: TipoPropulsao
  ): string {
    const rotulos:
      Record<TipoPropulsao, string> = {
        MOTOR: 'Motor',
        VELA: 'Vela',
        VELA_E_MOTOR: 'Vela e motor',
        REMO: 'Remo',
        SEM_PROPULSAO: 'Sem propulsão',
        OUTRA: 'Outra'
      };

    return rotulos[tipo];
  }

  protected formatarMetros(
    valor: number | null
  ): string {
    if (valor === null) {
      return 'Não informado';
    }

    return `${Number(valor).toLocaleString(
      'pt-BR',
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }
    )} m`;
  }

  protected descricaoModelo(
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

  private obterMensagemErroStatus(
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
      return 'A embarcação não foi encontrada.';
    }

    return 'Não foi possível alterar o status da embarcação.';
  }

  private carregarEmbarcacoes(
    pagina = 0
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    const nome =
      this.busca.value.trim();

    const tipo =
      this.filtroTipo.value === 'TODAS'
        ? undefined
        : this.filtroTipo.value;

    const status =
      this.filtroStatus.value;

    const ativa =
      status === 'ATIVAS'
        ? true
        : status === 'INATIVAS'
          ? false
          : undefined;

    this.embarcacaoService
      .listar({
        pagina,
        tamanho: this.tamanhoPagina,
        nome: nome || undefined,
        tipo,
        ativa
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
            this.carregarEmbarcacoes(
              Math.max(
                resposta.totalPages - 1,
                0
              )
            );

            return;
          }

          this.embarcacoes.set(
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
          this.embarcacoes.set([]);

          this.mensagemErro.set(
            'Não foi possível carregar as embarcações.'
          );
        }
      });
  }
}