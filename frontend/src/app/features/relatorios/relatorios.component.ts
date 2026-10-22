import { Component, inject, signal } from '@angular/core';
import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import {
  RelatoriosService,
  OcupacaoRelatorio,
  MovimentacoesRelatorio,
  ContratosRelatorio,
  PermanenciaVagasRelatorio,
} from './relatorios.service';
import { AutenticacaoService } from '../../core/autenticacao/autenticacao.service';
@Component({
  selector: 'app-relatorios',
  imports: [
    DecimalPipe,
    KeyValuePipe,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
  ],
  templateUrl: './relatorios.component.html',
  styleUrl: './relatorios.component.scss',
})
export class RelatoriosComponent {
  private readonly service = inject(RelatoriosService);
  private readonly auth = inject(AutenticacaoService);
  protected tipo =
    this.pode('VAGAS') && this.pode('OCUPACOES')
      ? 'ocupacao'
      : this.pode('MOVIMENTACOES')
        ? 'movimentacoes'
        : this.pode('CONTRATOS')
          ? 'contratos'
          : '';
  protected setor = '';
  protected tipoVaga = '';
  protected movTipo = '';
  protected status = '';
  protected periodicidade = '';
  protected inicio = '';
  protected fim = '';
  protected readonly tiposVaga = ['MOLHADA', 'SECA', 'POITA', 'OUTRA'];
  protected readonly tiposMov = [
    'LANCAMENTO',
    'RETIRADA',
    'RETORNO_PARA_VAGA',
    'TRANSFERENCIA',
    'DESLOCAMENTO_INTERNO',
  ];
  protected readonly situacoes = [
    'RASCUNHO',
    'PENDENTE_ASSINATURA',
    'ATIVO',
    'SUSPENSO',
    'EM_ENCERRAMENTO',
    'ENCERRADO',
    'CANCELADO',
  ];
  protected readonly periodicidades = [
    'DIARIA',
    'MENSAL',
    'TRIMESTRAL',
    'SEMESTRAL',
    'ANUAL',
    'PERSONALIZADA',
  ];
  protected readonly permanencia = signal<PermanenciaVagasRelatorio | null>(null);
  protected readonly ocupacao = signal<OcupacaoRelatorio | null>(null);
  protected readonly movimentacoes = signal<MovimentacoesRelatorio | null>(null);
  protected readonly contratos = signal<ContratosRelatorio | null>(null);
  protected readonly erro = signal('');
  protected readonly carregando = signal(false);
  protected pode(modulo: 'VAGAS' | 'OCUPACOES' | 'MOVIMENTACOES' | 'CONTRATOS') {
    return this.auth.temPermissao(modulo, 'VISUALIZAR');
  }
  protected rotulo(v: string) {
    return v.toLowerCase().replaceAll('_', ' ');
  }
  protected limpar() {
    this.permanencia.set(null);
    this.ocupacao.set(null);
    this.movimentacoes.set(null);
    this.contratos.set(null);
    this.erro.set('');
  }
  protected consultar() {
    this.limpar();
    this.carregando.set(true);
    const params: Record<string, string> = {};
    const erro = (e: { error?: { mensagem?: string } }) => {
      this.erro.set(e.error?.mensagem || 'Não foi possível gerar o relatório.');
      this.carregando.set(false);
    };
    if (this.tipo === 'permanencia-vagas') {
      if (this.inicio) params['inicio'] = this.inicio;
      if (this.fim) params['fim'] = this.fim;
      if (this.setor.trim()) params['setor'] = this.setor.trim();
      if (this.tipoVaga) params['tipo'] = this.tipoVaga;
      this.service.permanenciaVagas(params).subscribe({
        next: (r) => {
          this.permanencia.set(r);
          this.carregando.set(false);
        },
        error: erro,
      });
    } else if (this.tipo === 'ocupacao') {
      if (this.setor.trim()) params['setor'] = this.setor.trim();
      if (this.tipoVaga) params['tipo'] = this.tipoVaga;
      this.service.ocupacao(params).subscribe({
        next: (r) => {
          this.ocupacao.set(r);
          this.carregando.set(false);
        },
        error: erro,
      });
    } else {
      if (this.inicio) params['inicio'] = this.inicio;
      if (this.fim) params['fim'] = this.fim;
      if (this.tipo === 'movimentacoes') {
        if (this.movTipo) params['tipo'] = this.movTipo;
        this.service.movimentacoes(params).subscribe({
          next: (r) => {
            this.movimentacoes.set(r);
            this.carregando.set(false);
          },
          error: erro,
        });
      } else {
        if (this.status) params['status'] = this.status;
        if (this.periodicidade) params['periodicidade'] = this.periodicidade;
        this.service.contratos(params).subscribe({
          next: (r) => {
            this.contratos.set(r);
            this.carregando.set(false);
          },
          error: erro,
        });
      }
    }
  }
}
