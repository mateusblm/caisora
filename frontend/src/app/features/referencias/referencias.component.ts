import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import {
  ReferenciasService,
  GrupoReferencia,
  ConsultaReferencia,
  DisponibilidadeMares,
} from './referencias.service';
const CONFIG = {
  paises: {
    titulo: 'Países e bandeiras',
    descricao: 'Referência de identificação e códigos de países mantida pelo IBGE.',
    fonte: 'https://servicodados.ibge.gov.br/api/docs/paises',
    colunas: [
      ['codigoM49', 'Código M49'],
      ['nome', 'País'],
      ['codigoIso2', 'ISO alpha-2'],
      ['codigoIso3', 'ISO alpha-3'],
    ],
  },
  municipios: {
    titulo: 'Municípios brasileiros',
    descricao: 'Consulte o código IBGE do município e sua unidade federativa.',
    fonte: 'https://servicodados.ibge.gov.br/api/docs/localidades',
    colunas: [
      ['codigoIbge', 'Código IBGE'],
      ['nome', 'Município'],
      ['codigoUf', 'Código UF'],
      ['siglaUf', 'UF'],
      ['nomeUf', 'Nome da UF'],
    ],
  },
  feriados: {
    titulo: 'Calendário de feriados',
    descricao: 'Datas e classificações fornecidas pela BrasilAPI para o ano e a UF selecionados.',
    fonte: 'https://brasilapi.com.br/docs#tag/Feriados-Nacionais',
    colunas: [
      ['data', 'Data'],
      ['nome', 'Nome'],
      ['tipo', 'Tipo do provedor'],
      ['uf', 'UF informada'],
      ['diaSemana', 'Dia da semana informado'],
    ],
  },
  mares: {
    titulo: 'Previsões de marés',
    descricao: 'Previsões de maré alta e baixa para as coordenadas configuradas da sua marina.',
    fonte: 'https://www.worldtides.info/apidocs',
    colunas: [
      ['dataHora', 'Data/hora (UTC)'],
      ['tipo', 'Maré'],
      ['altura', 'Altura prevista'],
      ['unidade', 'Unidade'],
    ],
  },
} as const;
@Component({
  selector: 'app-referencias',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    RouterLinkActive,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
  ],
  templateUrl: './referencias.component.html',
  styleUrl: './referencias.component.scss',
})
export class ReferenciasComponent {
  private readonly service = inject(ReferenciasService);
  protected readonly grupo = inject(ActivatedRoute).snapshot.data['grupo'] as GrupoReferencia;
  protected readonly config = CONFIG[this.grupo];
  protected readonly links = [
    ['paises', 'Países'],
    ['municipios', 'Municípios'],
    ['feriados', 'Feriados'],
    ['mares', 'Marés'],
  ];
  protected busca = '';
  protected uf = '';
  protected ano = new Date().getUTCFullYear();
  protected inicio = new Date().toISOString().slice(0, 10);
  protected dias = 1;
  protected readonly hoje = new Date().toISOString().slice(0, 10);
  protected readonly limite = new Date(Date.now() + 6 * 86400000).toISOString().slice(0, 10);
  protected readonly carregando = signal(false);
  protected readonly erro = signal('');
  protected readonly resultado = signal<ConsultaReferencia | null>(null);
  protected readonly disponibilidade = signal<DisponibilidadeMares | null>(null);
  constructor() {
    if (this.grupo === 'mares') this.verificarDisponibilidade();
  }
  protected verificarDisponibilidade() {
    this.erro.set('');
    this.service.disponibilidadeMares().subscribe({
      next: (d) => this.disponibilidade.set(d),
      error: () =>
        this.erro.set('Não foi possível verificar a disponibilidade das marés. Tente novamente.'),
    });
  }
  protected consultar() {
    if (this.grupo === 'mares' && !this.disponibilidade()?.habilitada) return;
    this.carregando.set(true);
    this.erro.set('');
    this.resultado.set(null);
    const params: Record<string, string> = {};
    if (this.grupo === 'paises' || this.grupo === 'municipios') {
      if (this.busca.trim()) params['busca'] = this.busca.trim();
    }
    if (this.grupo === 'municipios' || this.grupo === 'feriados') {
      if (this.uf.trim()) params['uf'] = this.uf.trim().toUpperCase();
    }
    if (this.grupo === 'feriados') params['ano'] = String(this.ano);
    if (this.grupo === 'mares') {
      params['inicio'] = this.inicio;
      params['dias'] = String(this.dias);
    }
    this.service.consultar(this.grupo, params).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.carregando.set(false);
      },
      error: (e) => {
        this.erro.set(
          e.error?.mensagem || 'Não foi possível consultar a referência externa. Tente novamente.',
        );
        this.carregando.set(false);
      },
    });
  }
  protected tipo(valor: string | number | null | undefined) {
    if (this.grupo === 'mares')
      return valor === 'High' ? 'Alta' : valor === 'Low' ? 'Baixa' : (valor ?? '—');
    return valor === 'national'
      ? 'Nacional (national)'
      : valor === 'state'
        ? 'Estadual (state)'
        : (valor ?? 'Não informado');
  }
}
