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
  FiltroStatusVaga,
  FiltroTipoVaga,
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

type ModoBuscaVaga =
  | 'codigo'
  | 'setor';

@Component({
  selector: 'app-vaga-listagem',
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
    './vaga-listagem.component.html',
  styleUrl:
    './vaga-listagem.component.scss'
})
export class VagaListagemComponent
  implements OnInit {

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

  protected readonly modoBusca =
    new FormControl<ModoBuscaVaga>(
      'codigo',
      {
        nonNullable: true
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
    new FormControl<FiltroTipoVaga>(
      'TODAS',
      {
        nonNullable: true
      }
    );

  protected readonly filtroStatus =
    new FormControl<FiltroStatusVaga>(
      'TODAS',
      {
        nonNullable: true
      }
    );

  protected readonly vagas =
    signal<Vaga[]>([]);

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

  ngOnInit(): void {
    this.carregarVagas();
  }

  protected pesquisar(): void {
    if (this.busca.value.trim()) {
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

    this.carregarVagas(0);
  }

  protected alterarModoBusca(): void {
    this.busca.setValue(
      '',
      {
        emitEvent: false
      }
    );
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

    this.carregarVagas(0);
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

    this.carregarVagas(0);
  }

  protected limparFiltros(): void {
    this.busca.setValue('');
    this.modoBusca.setValue('codigo');
    this.filtroTipo.setValue('TODAS');
    this.filtroStatus.setValue('TODAS');

    this.carregarVagas(0);
  }

  protected paginaAnterior(): void {
    if (this.paginaAtual() > 0) {
      this.carregarVagas(
        this.paginaAtual() - 1
      );
    }
  }

  protected proximaPagina(): void {
    if (
      this.paginaAtual() + 1
      < this.totalPaginas()
    ) {
      this.carregarVagas(
        this.paginaAtual() + 1
      );
    }
  }

  protected solicitarAlteracaoStatus(
    vaga: Vaga
  ): void {
    if (this.alterandoStatusId() !== null) {
      return;
    }

    const novoStatus = !vaga.ativa;

    const dadosDialogo:
      DadosDialogoConfirmacao =
        novoStatus
          ? {
              titulo: 'Ativar vaga?',
              mensagem:
                `A vaga ${vaga.codigo} voltara a aparecer nas operacoes da marina.`,
              detalhe:
                'O cadastro permanecera com as mesmas dimensoes, infraestrutura e historico.',
              textoConfirmacao: 'Ativar',
              icone: 'anchor',
              tom: 'sucesso'
            }
          : {
              titulo: 'Inativar vaga?',
              mensagem:
                `A vaga ${vaga.codigo} deixara de ficar disponivel para novas operacoes.`,
              detalhe:
                'O cadastro nao sera excluido e podera ser reativado posteriormente.',
              textoConfirmacao: 'Inativar',
              icone: 'block',
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
              ? 'Confirmar ativacao da vaga'
              : 'Confirmar inativacao da vaga'
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
            vaga.id
          );

          return this.vagaService
            .alterarStatus(
              vaga.id,
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
              ? 'Vaga ativada com sucesso.'
              : 'Vaga inativada com sucesso.',
            'Fechar',
            {
              duration: 3500,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            }
          );

          this.carregarVagas(
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

  protected formatarTipo(
    tipo: TipoVaga
  ): string {
    return this.tipos.find(
      (opcao) => opcao.valor === tipo
    )?.rotulo ?? tipo;
  }

  protected formatarMetros(
    valor: number | null
  ): string {
    if (valor === null) {
      return 'Nao informado';
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
      return 'Nao informado';
    }

    return `${Number(valor).toLocaleString(
      'pt-BR',
      {
        maximumFractionDigits: 2
      }
    )} kg`;
  }

  protected descricaoLocalizacao(
    vaga: Vaga
  ): string {
    const partes = [
      vaga.setor,
      vaga.localizacao
    ].filter(Boolean);

    return partes.length
      ? partes.join(' - ')
      : 'Localizacao nao informada';
  }

  protected infraestrutura(
    vaga: Vaga
  ): string {
    if (
      vaga.possuiAgua
      && vaga.possuiEnergia
    ) {
      return 'Agua e energia';
    }

    if (vaga.possuiAgua) {
      return 'Agua';
    }

    if (vaga.possuiEnergia) {
      return 'Energia';
    }

    return 'Sem infraestrutura';
  }

  protected rotuloBusca(): string {
    return this.modoBusca.value === 'codigo'
      ? 'Buscar por codigo'
      : 'Buscar por setor';
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
      return 'Nao foi possivel conectar ao servidor.';
    }

    if (erro.status === 404) {
      return 'A vaga nao foi encontrada.';
    }

    return 'Nao foi possivel alterar o status da vaga.';
  }

  private carregarVagas(
    pagina = 0
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    const busca =
      this.busca.value.trim();

    const codigo =
      this.modoBusca.value === 'codigo'
        ? busca || undefined
        : undefined;

    const setor =
      this.modoBusca.value === 'setor'
        ? busca || undefined
        : undefined;

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

    this.vagaService
      .listar({
        pagina,
        tamanho: this.tamanhoPagina,
        codigo,
        setor,
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
            this.carregarVagas(
              Math.max(
                resposta.totalPages - 1,
                0
              )
            );

            return;
          }

          this.vagas.set(
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
          this.vagas.set([]);

          this.mensagemErro.set(
            'Nao foi possivel carregar as vagas.'
          );
        }
      });
  }
}
