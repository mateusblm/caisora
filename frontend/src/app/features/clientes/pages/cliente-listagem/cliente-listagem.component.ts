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
  toSignal
} from '@angular/core/rxjs-interop';

import {
  RouterLink
} from '@angular/router';

import {
  MatDialog,
  MatDialogModule
} from '@angular/material/dialog';

import {
  MatMenuModule
} from '@angular/material/menu';

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
  filter,
  finalize,
  map,
  switchMap
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
  Cliente,
  FiltroStatusCliente
} from '../../models/cliente.model';

import {
  ClienteService
} from '../../services/cliente.service';

@Component({
  selector: 'app-cliente-listagem',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatDialogModule,
    MatMenuModule,
    MatSnackBarModule
  ],
  templateUrl: './cliente-listagem.component.html',
  styleUrl: './cliente-listagem.component.scss'
})
export class ClienteListagemComponent
  implements OnInit {

  private readonly clienteService =
    inject(ClienteService);

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

  protected readonly filtroStatus =
    new FormControl<FiltroStatusCliente>(
      'TODOS',
      {
        nonNullable: true
      }
    );

  protected readonly clientes =
    signal<Cliente[]>([]);

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

  ngOnInit(): void {
    this.carregarClientes();
  }

  protected pesquisar(): void {
    const nome = this.busca.value.trim();

    if (nome) {
      this.filtroStatus.setValue(
        'TODOS',
        {
          emitEvent: false
        }
      );
    }

    this.carregarClientes(0);
  }

  protected alterarFiltroStatus(): void {
    if (
      this.filtroStatus.value !== 'TODOS'
    ) {
      this.busca.setValue(
        '',
        {
          emitEvent: false
        }
      );
    }

    this.carregarClientes(0);
  }

  protected limparFiltros(): void {
    this.busca.setValue('');
    this.filtroStatus.setValue('TODOS');

    this.carregarClientes(0);
  }

  protected paginaAnterior(): void {
    if (this.paginaAtual() > 0) {
      this.carregarClientes(
        this.paginaAtual() - 1
      );
    }
  }

  protected proximaPagina(): void {
    if (
      this.paginaAtual() + 1
      < this.totalPaginas()
    ) {
      this.carregarClientes(
        this.paginaAtual() + 1
      );
    }
  }

  protected solicitarAlteracaoStatus(
      cliente: Cliente
    ): void {
      if (this.alterandoStatusId()) {
        return;
      }

      const novoStatus = !cliente.ativo;

      const dadosDialogo:
        DadosDialogoConfirmacao =
          novoStatus
            ? {
                titulo: 'Ativar cliente?',
                mensagem:
                  `O cliente ${cliente.nome} voltará a ficar disponível para as operações da marina.`,
                detalhe:
                  'O cadastro permanecerá com os mesmos dados e histórico.',
                textoConfirmacao: 'Ativar',
                icone: 'person_add',
                tom: 'sucesso'
              }
            : {
                titulo: 'Inativar cliente?',
                mensagem:
                  `O cliente ${cliente.nome} deixará de aparecer como ativo nas operações da marina.`,
                detalhe:
                  'O cadastro não será excluído e poderá ser reativado posteriormente.',
                textoConfirmacao: 'Inativar',
                icone: 'person_off',
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
                ? 'Confirmar ativação do cliente'
                : 'Confirmar inativação do cliente'
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
              cliente.id
            );

            return this.clienteService
              .alterarStatus(
                cliente.id,
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
                ? 'Cliente ativado com sucesso.'
                : 'Cliente inativado com sucesso.',
              'Fechar',
              {
                duration: 3500,
                horizontalPosition: 'center',
                verticalPosition: 'bottom'
              }
            );

            this.carregarClientes(
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

  protected formatarTipoPessoa(
    cliente: Cliente
  ): string {
    return cliente.tipoPessoa === 'FISICA'
      ? 'Pessoa física'
      : 'Pessoa jurídica';
  }

  protected formatarDocumento(
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

  protected obterContato(
    cliente: Cliente
  ): string {
    return (
      cliente.celular
      ?? cliente.telefone
      ?? 'Não informado'
    );
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
      return 'O cliente não foi encontrado.';
    }

    return 'Não foi possível alterar o status do cliente.';
  }

  private carregarClientes(
    pagina = 0
  ): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    const nome =
      this.busca.value.trim();

    const filtroStatus =
      this.filtroStatus.value;

    const ativo =
      filtroStatus === 'ATIVOS'
        ? true
        : filtroStatus === 'INATIVOS'
          ? false
          : undefined;

    this.clienteService
      .listar({
        pagina,
        tamanho: this.tamanhoPagina,
        nome: nome || undefined,
        ativo
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
            this.carregarClientes(
              Math.max(
                resposta.totalPages - 1,
                0
              )
            );

            return;
          }
          this.clientes.set(
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
          this.clientes.set([]);

          this.mensagemErro.set(
            'Não foi possível carregar os clientes.'
          );
        }
      });
  }
}