import {
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';

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
  finalize,
  map
} from 'rxjs';

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
    MatSelectModule
  ],
  templateUrl: './cliente-listagem.component.html',
  styleUrl: './cliente-listagem.component.scss'
})
export class ClienteListagemComponent
  implements OnInit {

  private readonly clienteService =
    inject(ClienteService);

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