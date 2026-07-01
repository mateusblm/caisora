import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import {
  Movimentacao
} from '../../../movimentacoes/models/movimentacao.model';
import {
  MovimentacaoService
} from '../../../movimentacoes/services/movimentacao.service';
import {
  PainelOperacional
} from '../../models/painel-operacional.model';
import {
  PainelOperacionalService
} from '../../services/painel-operacional.service';
import {
  DashboardComponent
} from './dashboard.component';

describe('DashboardComponent', () => {

  let fixture: ComponentFixture<
    DashboardComponent
  >;

  const painelServiceMock = {
    buscar: vi.fn()
  };

  const movimentacaoServiceMock = {
    iniciar: vi.fn(),
    concluir: vi.fn()
  };

  const dialogMock = {
    open: vi.fn()
  };

  const snackBarMock = {
    open: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    painelServiceMock.buscar
      .mockReturnValue(
        of(criarPainel())
      );

    movimentacaoServiceMock.iniciar
      .mockReturnValue(
        of(
          criarMovimentacao({
            status: 'EM_EXECUCAO'
          })
        )
      );

    movimentacaoServiceMock.concluir
      .mockReturnValue(
        of(
          criarMovimentacao({
            status: 'CONCLUIDA'
          })
        )
      );

    dialogMock.open.mockReturnValue({
      afterClosed: () => of(true)
    });

    TestBed.configureTestingModule({
      imports: [
        DashboardComponent
      ],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: PainelOperacionalService,
          useValue: painelServiceMock
        },
        {
          provide: MovimentacaoService,
          useValue: movimentacaoServiceMock
        }
      ]
    });

    TestBed.overrideComponent(
      DashboardComponent,
      {
        add: {
          providers: [
            {
              provide: MatDialog,
              useValue: dialogMock
            },
            {
              provide: MatSnackBar,
              useValue: snackBarMock
            }
          ]
        }
      }
    );

    await TestBed.compileComponents();

    fixture = TestBed.createComponent(
      DashboardComponent
    );

    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it(
    'deve carregar o painel operacional',
    () => {
      expect(
        painelServiceMock.buscar
      ).toHaveBeenCalled();

      expect(
        fixture.componentInstance[
          'painel'
        ]()?.indicadores.atrasadas
      ).toBe(1);

      expect(
        fixture.nativeElement.textContent
      ).toContain('Painel operacional');
    }
  );

  it(
    'deve iniciar movimentação confirmada',
    () => {
      const movimentacao =
        criarMovimentacao();

      painelServiceMock.buscar.mockClear();

      fixture.componentInstance[
        'solicitarInicio'
      ](movimentacao);

      expect(dialogMock.open)
        .toHaveBeenCalled();

      expect(
        movimentacaoServiceMock.iniciar
      ).toHaveBeenCalledWith(
        movimentacao.id,
        {
          iniciadaEm: expect.any(String),
          observacao: null
        }
      );

      expect(
        painelServiceMock.buscar
      ).toHaveBeenCalled();
    }
  );

  it(
    'deve concluir movimentação confirmada',
    () => {
      const movimentacao =
        criarMovimentacao({
          status: 'EM_EXECUCAO',
          iniciadaEm:
            '2026-06-30T14:30:00Z'
        });

      fixture.componentInstance[
        'solicitarConclusao'
      ](movimentacao);

      expect(
        movimentacaoServiceMock.concluir
      ).toHaveBeenCalledWith(
        movimentacao.id,
        {
          concluidaEm: expect.any(String),
          observacao: null
        }
      );
    }
  );

  it(
    'deve tratar retorno para vaga sem erro',
    () => {
      const movimentacao =
        criarMovimentacao({
          tipo: 'RETORNO_PARA_VAGA'
        });

      expect(
        fixture.componentInstance[
          'rotuloOperacao'
        ](movimentacao)
      ).toBe('Retorno para a vaga');
    }
  );

  function criarPainel():
    PainelOperacional {
    const movimentacao =
      criarMovimentacao();

    return {
      geradoEm:
        '2026-06-30T15:00:00Z',
      fusoHorario:
        'America/Sao_Paulo',
      inicioDia:
        '2026-06-30T03:00:00Z',
      fimDia:
        '2026-07-01T02:59:59.999999999Z',
      indicadores: {
        emExecucao: 0,
        atrasadas: 1,
        proximaHora: 0,
        urgentes: 1,
        semOperador: 1,
        concluidasHoje: 0
      },
      atrasadas: [movimentacao],
      emExecucao: [],
      proximosTrintaMinutos: [],
      proximasDuasHoras: [],
      restanteDia: [],
      concluidasRecentemente: []
    };
  }

  function criarMovimentacao(
    alteracoes: Partial<Movimentacao> = {}
  ): Movimentacao {
    return {
      id: 'movimentacao-1',
      embarcacaoId: 'embarcacao-1',
      embarcacaoNome: 'Aurora',
      embarcacaoModelo: 'V33',
      proprietarioNome: 'João',
      tipo: 'LANCAMENTO',
      status: 'AGENDADA',
      prioridade: 'URGENTE',
      tipoPosicaoOrigem: 'VAGA',
      vagaOrigemId: 'vaga-1',
      vagaOrigemCodigo: 'A-01',
      descricaoOrigem: null,
      tipoPosicaoDestino: 'AGUA',
      vagaDestinoId: null,
      vagaDestinoCodigo: null,
      descricaoDestino: 'Rampa principal',
      agendadaPara:
        '2026-06-30T14:45:00Z',
      iniciadaEm: null,
      concluidaEm: null,
      canceladaEm: null,
      solicitadaPorId: 'usuario-1',
      solicitadaPorNome: 'Administrador',
      operadorResponsavelId: null,
      operadorResponsavelNome: null,
      observacoes: null,
      motivoCancelamento: null,
      versao: 0,
      organizacaoId: 'organizacao-1',
      criadaEm:
        '2026-06-30T14:00:00Z',
      atualizadaEm:
        '2026-06-30T14:00:00Z',
      ...alteracoes
    };
  }
});
