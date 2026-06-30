import { BreakpointObserver } from '@angular/cdk/layout';
import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';
import {
  of,
  throwError
} from 'rxjs';
import { vi } from 'vitest';

import { Embarcacao } from '../../../embarcacoes/models/embarcacao.model';
import { EmbarcacaoService } from '../../../embarcacoes/services/embarcacao.service';
import { Movimentacao } from '../../models/movimentacao.model';
import { MovimentacaoService } from '../../services/movimentacao.service';
import { MovimentacaoListagemComponent } from './movimentacao-listagem.component';

describe(
  'MovimentacaoListagemComponent',
  () => {
    let fixture: ComponentFixture<
      MovimentacaoListagemComponent
    >;

    const movimentacaoServiceMock = {
      listar: vi.fn(),
      iniciar: vi.fn(),
      concluir: vi.fn(),
      cancelar: vi.fn()
    };

    const embarcacaoServiceMock = {
      listar: vi.fn()
    };

    const dialogMock = {
      open: vi.fn()
    };

    const snackBarMock = {
      open: vi.fn()
    };

    const breakpointObserverMock = {
      observe: vi.fn().mockReturnValue(
        of({
          matches: false,
          breakpoints: {}
        })
      )
    };

    beforeEach(async () => {
      vi.clearAllMocks();

      movimentacaoServiceMock.listar
        .mockReturnValue(
          of(paginaComMovimentacao())
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

      movimentacaoServiceMock.cancelar
        .mockReturnValue(
          of(
            criarMovimentacao({
              status: 'CANCELADA'
            })
          )
        );

      embarcacaoServiceMock.listar
        .mockReturnValue(
          of({
            content: [criarEmbarcacao()],
            totalElements: 1,
            totalPages: 1,
            size: 200,
            number: 0,
            numberOfElements: 1,
            first: true,
            last: true,
            empty: false
          })
        );

      dialogMock.open.mockReturnValue({
        afterClosed: () => of(true)
      });

      TestBed.configureTestingModule({
        imports: [
          MovimentacaoListagemComponent
        ],
        providers: [
          provideNoopAnimations(),
          provideRouter([]),
          {
            provide: MovimentacaoService,
            useValue: movimentacaoServiceMock
          },
          {
            provide: EmbarcacaoService,
            useValue: embarcacaoServiceMock
          },
          {
            provide: BreakpointObserver,
            useValue: breakpointObserverMock
          }
        ]
      });

      TestBed.overrideComponent(
        MovimentacaoListagemComponent,
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
    });

    it(
      'deve carregar embarcações e movimentações',
      () => {
        criarComponente();

        expect(
          embarcacaoServiceMock.listar
        ).toHaveBeenCalledWith({
          pagina: 0,
          tamanho: 200,
          ativa: true
        });

        expect(
          movimentacaoServiceMock.listar
        ).toHaveBeenCalledWith({
          pagina: 0,
          tamanho: 10,
          status: undefined,
          tipo: undefined,
          embarcacaoId: undefined,
          inicio: undefined,
          fim: undefined
        });

        expect(
          fixture.componentInstance[
            'movimentacoes'
          ]()
        ).toHaveLength(1);
      }
    );

    it(
      'deve manter somente o filtro por status',
      () => {
        criarComponente();

        const componente =
          fixture.componentInstance;

        componente['filtroTipo'].setValue(
          'TRANSFERENCIA',
          { emitEvent: false }
        );

        componente[
          'filtroEmbarcacao'
        ].setValue(
          'embarcacao-1',
          { emitEvent: false }
        );

        componente['filtroInicio'].setValue(
          '2026-07-01T08:00',
          { emitEvent: false }
        );

        componente['filtroFim'].setValue(
          '2026-07-01T18:00',
          { emitEvent: false }
        );

        componente['filtroStatus'].setValue(
          'AGENDADA',
          { emitEvent: false }
        );

        movimentacaoServiceMock.listar
          .mockClear();

        componente['alterarFiltroStatus']();

        expect(
          componente['filtroTipo'].value
        ).toBe('TODOS');

        expect(
          componente['filtroEmbarcacao'].value
        ).toBe('TODAS');

        expect(
          componente['filtroInicio'].value
        ).toBe('');

        expect(
          componente['filtroFim'].value
        ).toBe('');

        expect(
          movimentacaoServiceMock.listar
        ).toHaveBeenCalledWith(
          expect.objectContaining({
            status: 'AGENDADA',
            tipo: undefined,
            embarcacaoId: undefined
          })
        );
      }
    );

    it(
      'não deve aplicar período incompleto',
      () => {
        criarComponente();

        const componente =
          fixture.componentInstance;

        componente['filtroInicio'].setValue(
          '2026-07-01T08:00'
        );

        movimentacaoServiceMock.listar
          .mockClear();

        componente['aplicarFiltroPeriodo']();

        expect(
          movimentacaoServiceMock.listar
        ).not.toHaveBeenCalled();

        expect(
          snackBarMock.open
        ).toHaveBeenCalledWith(
          'Informe o início e o fim do período.',
          'Fechar',
          expect.any(Object)
        );
      }
    );

    it(
      'deve iniciar movimentação confirmada',
      () => {
        criarComponente();

        movimentacaoServiceMock.listar
          .mockClear();

        fixture.componentInstance[
          'solicitarInicio'
        ](criarMovimentacao());

        expect(dialogMock.open)
          .toHaveBeenCalled();

        expect(
          movimentacaoServiceMock.iniciar
        ).toHaveBeenCalledWith(
          'movimentacao-1',
          {
            iniciadaEm: expect.any(String),
            observacao: null
          }
        );

        expect(
          snackBarMock.open
        ).toHaveBeenCalledWith(
          'Movimentação iniciada com sucesso.',
          'Fechar',
          expect.any(Object)
        );
      }
    );

    it(
      'deve cancelar movimentação com motivo',
      () => {
        dialogMock.open.mockReturnValue({
          afterClosed: () => of(
            'Condições meteorológicas'
          )
        });

        criarComponente();

        fixture.componentInstance[
          'solicitarCancelamento'
        ](criarMovimentacao());

        expect(
          movimentacaoServiceMock.cancelar
        ).toHaveBeenCalledWith(
          'movimentacao-1',
          {
            canceladaEm: expect.any(String),
            motivo: 'Condições meteorológicas'
          }
        );
      }
    );

    it(
      'deve exibir mensagem quando a listagem falhar',
      () => {
        movimentacaoServiceMock.listar
          .mockReturnValue(
            throwError(
              () => new Error('Falha')
            )
          );

        criarComponente();

        expect(
          fixture.componentInstance[
            'movimentacoes'
          ]()
        ).toEqual([]);

        expect(
          fixture.componentInstance[
            'mensagemErro'
          ]()
        ).toBe(
          'Não foi possível carregar as movimentações.'
        );
      }
    );

    function criarComponente(): void {
      fixture = TestBed.createComponent(
        MovimentacaoListagemComponent
      );

      fixture.detectChanges();
    }

    function paginaComMovimentacao() {
      return {
        content: [criarMovimentacao()],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
        numberOfElements: 1,
        first: true,
        last: true,
        empty: false
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
        prioridade: 'ALTA',

        tipoPosicaoOrigem: 'VAGA',
        vagaOrigemId: 'vaga-1',
        vagaOrigemCodigo: 'A-01',
        descricaoOrigem: null,

        tipoPosicaoDestino: 'AGUA',
        vagaDestinoId: null,
        vagaDestinoCodigo: null,
        descricaoDestino: 'Rampa principal',

        agendadaPara:
          '2026-07-01T13:00:00.000Z',
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
          '2026-06-30T12:00:00.000Z',
        atualizadaEm:
          '2026-06-30T12:00:00.000Z',

        ...alteracoes
      };
    }

    function criarEmbarcacao(): Embarcacao {
      return {
        id: 'embarcacao-1',
        nome: 'Aurora',
        modelo: 'V33',
        numeroInscricao: 'PR-123456'
      } as Embarcacao;
    }
  }
);
