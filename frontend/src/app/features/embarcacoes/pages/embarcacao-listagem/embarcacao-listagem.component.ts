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

import {
  RouterLink
} from '@angular/router';

@Component({
  selector: 'app-embarcacao-listagem',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    RouterLink
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

  protected nomeExibicao(
    embarcacao: Embarcacao
  ): string {
    return embarcacao.nome?.trim()
      || embarcacao.modelo?.trim()
      || 'Embarcação sem nome';
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

    return `${Number(valor)
      .toLocaleString(
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