import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';

import {
  BreakpointObserver
} from '@angular/cdk/layout';

import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';

import {
  provideRouter
} from '@angular/router';

import {
  MatDialog
} from '@angular/material/dialog';

import {
  MatSnackBar
} from '@angular/material/snack-bar';

import {
  of,
  throwError
} from 'rxjs';

import {
  vi
} from 'vitest';

import {
  Embarcacao
} from '../../../embarcacoes/models/embarcacao.model';

import {
  EmbarcacaoService
} from '../../../embarcacoes/services/embarcacao.service';

import {
  Vaga
} from '../../../vagas/models/vaga.model';

import {
  VagaService
} from '../../../vagas/services/vaga.service';

import {
  Ocupacao
} from '../../models/ocupacao.model';

import {
  OcupacaoService
} from '../../services/ocupacao.service';

import {
  OcupacaoListagemComponent
} from './ocupacao-listagem.component';

describe(
  'OcupacaoListagemComponent',
  () => {

    let fixture:
      ComponentFixture<
        OcupacaoListagemComponent
      >;

    const ocupacaoServiceMock = {
      listar: vi.fn(),
      encerrar: vi.fn()
    };

    const embarcacaoServiceMock = {
      listarTodas: vi.fn()
    };

    const vagaServiceMock = {
      listarTodas: vi.fn()
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

      ocupacaoServiceMock.listar
        .mockReturnValue(
          of(paginaComOcupacao())
        );

      ocupacaoServiceMock.encerrar
        .mockReturnValue(
          of(
            criarOcupacao({
              status: 'ENCERRADA',
              encerradaEm:
                '2026-06-29T22:30:00.000Z'
            })
          )
        );

      embarcacaoServiceMock
        .listarTodas
        .mockReturnValue(
          of([criarEmbarcacao()])
        );

      vagaServiceMock
        .listarTodas
        .mockReturnValue(
          of([criarVaga()])
        );

      dialogMock.open.mockReturnValue({
        afterClosed: () => of(true)
      });

      TestBed.configureTestingModule({
        imports: [
          OcupacaoListagemComponent
        ],
        providers: [
          provideRouter([]),
          provideNoopAnimations(),
          {
            provide: OcupacaoService,
            useValue: ocupacaoServiceMock
          },
          {
            provide: EmbarcacaoService,
            useValue: embarcacaoServiceMock
          },
          {
            provide: VagaService,
            useValue: vagaServiceMock
          },
          {
            provide: BreakpointObserver,
            useValue: breakpointObserverMock
          }
        ]
      });

      TestBed.overrideComponent(
        OcupacaoListagemComponent,
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
      'deve carregar filtros e ocupações',
      () => {
        criarComponente();

        expect(
          embarcacaoServiceMock
            .listarTodas
        ).toHaveBeenCalledOnce();

        expect(
          vagaServiceMock.listarTodas
        ).toHaveBeenCalledOnce();

        expect(
          ocupacaoServiceMock.listar
        ).toHaveBeenCalledWith({
          pagina: 0,
          tamanho: 10,
          embarcacaoId: undefined,
          vagaId: undefined,
          status: undefined
        });

        expect(
          fixture.componentInstance[
            'ocupacoes'
          ]()
        ).toHaveLength(1);

        expect(
          fixture.componentInstance[
            'totalElementos'
          ]()
        ).toBe(1);
      }
    );

    it(
      'deve manter somente o filtro de embarcação',
      () => {
        criarComponente();

        const componente =
          fixture.componentInstance;

        componente[
          'filtroVaga'
        ].setValue(
          'vaga-1',
          {
            emitEvent: false
          }
        );

        componente[
          'filtroStatus'
        ].setValue(
          'ATIVA',
          {
            emitEvent: false
          }
        );

        componente[
          'filtroEmbarcacao'
        ].setValue(
          'embarcacao-1',
          {
            emitEvent: false
          }
        );

        ocupacaoServiceMock
          .listar
          .mockClear();

        componente[
          'alterarFiltroEmbarcacao'
        ]();

        expect(
          componente[
            'filtroVaga'
          ].value
        ).toBe('TODAS');

        expect(
          componente[
            'filtroStatus'
          ].value
        ).toBe('TODAS');

        expect(
          ocupacaoServiceMock.listar
        ).toHaveBeenCalledWith({
          pagina: 0,
          tamanho: 10,
          embarcacaoId:
            'embarcacao-1',
          vagaId: undefined,
          status: undefined
        });
      }
    );

    it(
      'deve limpar todos os filtros',
      () => {
        criarComponente();

        const componente =
          fixture.componentInstance;

        componente[
          'filtroEmbarcacao'
        ].setValue(
          'embarcacao-1',
          {
            emitEvent: false
          }
        );

        componente[
          'filtroVaga'
        ].setValue(
          'vaga-1',
          {
            emitEvent: false
          }
        );

        ocupacaoServiceMock
          .listar
          .mockClear();

        componente[
          'limparFiltros'
        ]();

        expect(
          componente[
            'filtroEmbarcacao'
          ].value
        ).toBe('TODAS');

        expect(
          componente[
            'filtroVaga'
          ].value
        ).toBe('TODAS');

        expect(
          componente[
            'filtroStatus'
          ].value
        ).toBe('TODAS');

        expect(
          ocupacaoServiceMock.listar
        ).toHaveBeenCalled();
      }
    );

    it(
      'deve encerrar ocupação confirmada',
      () => {
        criarComponente();

        ocupacaoServiceMock
          .listar
          .mockClear();

        fixture.componentInstance[
          'solicitarEncerramento'
        ](
          criarOcupacao()
        );

        expect(
          dialogMock.open
        ).toHaveBeenCalled();

        expect(
          ocupacaoServiceMock.encerrar
        ).toHaveBeenCalledWith(
          'ocupacao-1',
          expect.any(String)
        );

        expect(
          snackBarMock.open
        ).toHaveBeenCalledWith(
          'Ocupação encerrada com sucesso.',
          'Fechar',
          expect.any(Object)
        );

        expect(
          ocupacaoServiceMock.listar
        ).toHaveBeenCalled();
      }
    );

    it(
      'não deve encerrar quando confirmação for cancelada',
      () => {
        dialogMock.open
          .mockReturnValue({
            afterClosed: () =>
              of(false)
          });

        criarComponente();

        fixture.componentInstance[
          'solicitarEncerramento'
        ](
          criarOcupacao()
        );

        expect(
          ocupacaoServiceMock.encerrar
        ).not.toHaveBeenCalled();
      }
    );

    it(
      'não deve encerrar ocupação já encerrada',
      () => {
        criarComponente();

        fixture.componentInstance[
          'solicitarEncerramento'
        ](
          criarOcupacao({
            status: 'ENCERRADA',
            encerradaEm:
              '2026-06-29T22:30:00.000Z'
          })
        );

        expect(
          dialogMock.open
        ).not.toHaveBeenCalled();

        expect(
          ocupacaoServiceMock.encerrar
        ).not.toHaveBeenCalled();
      }
    );

    it(
      'deve exibir mensagem quando listagem falhar',
      () => {
        ocupacaoServiceMock
          .listar
          .mockReturnValue(
            throwError(
              () => new Error(
                'Falha'
              )
            )
          );

        criarComponente();

        expect(
          fixture.componentInstance[
            'ocupacoes'
          ]()
        ).toEqual([]);

        expect(
          fixture.componentInstance[
            'mensagemErro'
          ]()
        ).toBe(
          'Não foi possível carregar as ocupações.'
        );
      }
    );

    function criarComponente(): void {
      fixture =
        TestBed.createComponent(
          OcupacaoListagemComponent
        );

      fixture.detectChanges();
    }

    function paginaComOcupacao() {
      return {
        content: [
          criarOcupacao()
        ],
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

    function criarOcupacao(
      alteracoes:
        Partial<Ocupacao> = {}
    ): Ocupacao {
      return {
        id: 'ocupacao-1',
        embarcacaoId:
          'embarcacao-1',
        embarcacaoNome: 'Aurora',
        embarcacaoModelo: 'V33',
        proprietarioNome: 'João',
        vagaId: 'vaga-1',
        vagaCodigo: 'A-01',
        vagaTipo: 'MOLHADA',
        vagaSetor: 'Pier A',
        vagaLocalizacao:
          'Corredor principal',
        status: 'ATIVA',
        inicioEm:
          '2026-06-29T20:00:00.000Z',
        fimPrevistoEm: null,
        encerradaEm: null,
        observacoes: null,
        organizacaoId:
          'organizacao-1',
        criadaEm:
          '2026-06-29T20:00:00.000Z',
        atualizadaEm:
          '2026-06-29T20:00:00.000Z',
        ...alteracoes
      };
    }

    function criarEmbarcacao():
      Embarcacao {
      return {
        id: 'embarcacao-1',
        nome: 'Aurora',
        modelo: 'V33',
        numeroInscricao:
          'PR-123456'
      } as Embarcacao;
    }

    function criarVaga(): Vaga {
      return {
        id: 'vaga-1',
        codigo: 'A-01'
      } as Vaga;
    }
  }
);
