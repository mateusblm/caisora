import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';

import {
  MovimentacaoPainelTv
} from '../../models/painel-tv.model';
import {
  CartaoPainelTvComponent
} from './cartao-painel-tv.component';

describe('CartaoPainelTvComponent', () => {

  let fixture: ComponentFixture<
    CartaoPainelTvComponent
  >;

  beforeEach(async () => {
    await TestBed
      .configureTestingModule({
        imports: [
          CartaoPainelTvComponent
        ]
      })
      .compileComponents();

    fixture = TestBed.createComponent(
      CartaoPainelTvComponent
    );

    fixture.componentInstance.movimentacao =
      criarMovimentacao();

    fixture.detectChanges();
  });

  it('deve exibir a ação operacional', () => {
    expect(
      fixture.nativeElement.textContent
    ).toContain('Descer para a água');
  });

  it('deve exibir o atraso', () => {
    expect(
      fixture.nativeElement.textContent
    ).toContain('12 min atrasada');
  });

  function criarMovimentacao():
    MovimentacaoPainelTv {
    return {
      id: 'movimentacao-1',
      embarcacaoId: 'embarcacao-1',
      embarcacaoNome: 'Aurora',
      embarcacaoModelo: 'V33',
      proprietarioNome: 'João',
      tipo: 'LANCAMENTO',
      acaoOperacional: 'DESCER_PARA_AGUA',
      status: 'AGENDADA',
      prioridade: 'URGENTE',
      situacao: 'ATRASADA',
      origem: {
        tipo: 'VAGA',
        vagaCodigo: 'A-01',
        descricao: null,
        rotulo: 'Vaga A-01'
      },
      destino: {
        tipo: 'AGUA',
        vagaCodigo: null,
        descricao: 'Rampa principal',
        rotulo: 'Água · Rampa principal'
      },
      agendadaPara:
        '2026-07-01T12:00:00Z',
      iniciadaEm: null,
      operadorResponsavelNome: null,
      observacoes: null,
      minutosAtraso: 12,
      minutosParaInicio: 0,
      minutosEmExecucao: 0,
      alertas: [
        'ATRASADA',
        'URGENTE',
        'SEM_OPERADOR'
      ]
    };
  }
});
