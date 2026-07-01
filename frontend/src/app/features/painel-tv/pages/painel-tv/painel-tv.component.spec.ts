import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import {
  MovimentacaoPainelTv,
  PainelTvOperacional
} from '../../models/painel-tv.model';
import { PainelTvService } from '../../services/painel-tv.service';
import { PainelTvComponent } from './painel-tv.component';

describe('PainelTvComponent', () => {

  let fixture: ComponentFixture<
    PainelTvComponent
  >;

  const serviceMock = {
    buscar: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    serviceMock.buscar.mockReturnValue(
      of(criarPainel())
    );

    await TestBed
      .configureTestingModule({
        imports: [
          PainelTvComponent
        ],
        providers: [
          provideNoopAnimations(),
          provideRouter([]),
          {
            provide: PainelTvService,
            useValue: serviceMock
          }
        ]
      })
      .compileComponents();

    fixture = TestBed.createComponent(
      PainelTvComponent
    );

    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('deve carregar o painel', () => {
    expect(serviceMock.buscar)
      .toHaveBeenCalled();

    expect(
      fixture.nativeElement.textContent
    ).toContain('Descer para a água');

    expect(
      fixture.nativeElement.textContent
    ).toContain('Aurora');
  });

  it('deve atualizar manualmente', () => {
    serviceMock.buscar.mockClear();

    fixture.componentInstance[
      'atualizarAgora'
    ]();

    expect(serviceMock.buscar)
      .toHaveBeenCalledTimes(1);
  });

  it('deve tratar retorno para vaga sem erro', () => {
    const painel = criarPainel();

    serviceMock.buscar.mockReturnValue(
      of({
        ...painel,
        deslocamentosInternos: [
          criarMovimentacao({
            tipo: 'RETORNO_PARA_VAGA',
            acaoOperacional:
              'RETORNAR_PARA_VAGA'
          })
        ],
        resumo: {
          ...painel.resumo,
          deslocamentosInternos: 1
        }
      })
    );

    fixture.componentInstance[
      'atualizarAgora'
    ]();

    fixture.detectChanges();

    expect(
      fixture.nativeElement.textContent
    ).toContain('Retornar para a vaga');
  });

  function criarPainel():
    PainelTvOperacional {
    const movimentacao =
      criarMovimentacao();

    return {
      geradoEm: '2026-07-01T12:00:00Z',
      fusoHorario: 'America/Sao_Paulo',
      inicioDia: '2026-07-01T03:00:00Z',
      fimDia:
        '2026-07-02T02:59:59.999999999Z',
      atualizarAposSegundos: 15,
      resumo: {
        descidasParaAgua: 1,
        retiradasDaAgua: 0,
        transferenciasDeVaga: 0,
        deslocamentosInternos: 0,
        emExecucao: 0,
        alertas: 1
      },
      alertas: [movimentacao],
      descidasParaAgua: [movimentacao],
      retiradasDaAgua: [],
      transferenciasDeVaga: [],
      deslocamentosInternos: [],
      emExecucao: []
    };
  }

  function criarMovimentacao(
    alteracoes: Partial<MovimentacaoPainelTv> = {}
  ):
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
      ],
      ...alteracoes
    };
  }
});
