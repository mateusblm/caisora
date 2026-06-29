import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';

import {
  convertToParamMap,
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';

import {
  MatSnackBar
} from '@angular/material/snack-bar';

import {
  of
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
  OcupacaoFormularioComponent
} from './ocupacao-formulario.component';

describe(
  'OcupacaoFormularioComponent',
  () => {

    let fixture:
      ComponentFixture<
        OcupacaoFormularioComponent
      >;

    const ocupacaoServiceMock = {
      listarTodasAtivas: vi.fn(),
      buscarPorId: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn()
    };

    const embarcacaoServiceMock = {
      listarTodas: vi.fn()
    };

    const vagaServiceMock = {
      listarTodas: vi.fn()
    };

    const routerMock = {
      navigateByUrl: vi.fn()
    };

    const snackBarMock = {
      open: vi.fn()
    };

    beforeEach(() => {
      vi.clearAllMocks();

      ocupacaoServiceMock
        .listarTodasAtivas
        .mockReturnValue(of([]));

      ocupacaoServiceMock.criar
        .mockReturnValue(
          of(criarOcupacao())
        );

      ocupacaoServiceMock.atualizar
        .mockReturnValue(
          of(criarOcupacao())
        );

      ocupacaoServiceMock.buscarPorId
        .mockReturnValue(
          of(criarOcupacao())
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
    });

    it(
      'deve remover embarcações e vagas já ocupadas',
      async () => {
        const embarcacaoLivre =
          criarEmbarcacao({
            id: 'embarcacao-livre',
            nome: 'Livre'
          });

        const embarcacaoOcupada =
          criarEmbarcacao({
            id: 'embarcacao-ocupada',
            nome: 'Ocupada'
          });

        const embarcacaoInativa =
          criarEmbarcacao({
            id: 'embarcacao-inativa',
            nome: 'Inativa',
            ativa: false
          });

        const vagaLivre =
          criarVaga({
            id: 'vaga-livre',
            codigo: 'A-01'
          });

        const vagaOcupada =
          criarVaga({
            id: 'vaga-ocupada',
            codigo: 'A-02'
          });

        const vagaInativa =
          criarVaga({
            id: 'vaga-inativa',
            codigo: 'A-03',
            ativa: false
          });

        embarcacaoServiceMock
          .listarTodas
          .mockReturnValue(
            of([
              embarcacaoLivre,
              embarcacaoOcupada,
              embarcacaoInativa
            ])
          );

        vagaServiceMock
          .listarTodas
          .mockReturnValue(
            of([
              vagaLivre,
              vagaOcupada,
              vagaInativa
            ])
          );

        ocupacaoServiceMock
          .listarTodasAtivas
          .mockReturnValue(
            of([
              criarOcupacao({
                embarcacaoId:
                  'embarcacao-ocupada',
                vagaId:
                  'vaga-ocupada'
              })
            ])
          );

        await criarComponente(null);

        expect(
          fixture.componentInstance[
            'embarcacoesDisponiveis'
          ]().map(
            (embarcacao) =>
              embarcacao.id
          )
        ).toEqual([
          'embarcacao-livre'
        ]);

        expect(
          fixture.componentInstance[
            'vagasDisponiveis'
          ]().map(
            (vaga) => vaga.id
          )
        ).toEqual([
          'vaga-livre'
        ]);
      }
    );

    it(
      'deve detectar incompatibilidade dimensional',
      async () => {
        embarcacaoServiceMock
          .listarTodas
          .mockReturnValue(
            of([
              criarEmbarcacao({
                comprimentoTotalMetros:
                  15
              })
            ])
          );

        vagaServiceMock
          .listarTodas
          .mockReturnValue(
            of([
              criarVaga({
                comprimentoMaximoMetros:
                  12
              })
            ])
          );

        await criarComponente(null);

        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.patchValue({
          embarcacaoId:
            'embarcacao-1',
          vagaId: 'vaga-1'
        });

        fixture.detectChanges();

        expect(
          fixture.componentInstance[
            'incompatibilidades'
          ]()
        ).toContain(
          'O comprimento da embarcação excede o limite da vaga.'
        );
      }
    );

    it(
      'deve criar ocupação válida',
      async () => {
        await criarComponente(null);

        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.setValue({
          embarcacaoId:
            'embarcacao-1',
          vagaId: 'vaga-1',
          inicioEm:
            paraDataHoraLocal(
              new Date(
                Date.now() - 3_600_000
              )
            ),
          fimPrevistoEm:
            paraDataHoraLocal(
              new Date(
                Date.now() + 3_600_000
              )
            ),
          observacoes:
            ' Ocupação mensal '
        });

        fixture.componentInstance[
          'salvar'
        ]();

        expect(
          ocupacaoServiceMock.criar
        ).toHaveBeenCalledWith(
          expect.objectContaining({
            embarcacaoId:
              'embarcacao-1',
            vagaId: 'vaga-1',
            inicioEm:
              expect.stringMatching(
                /Z$/
              ),
            fimPrevistoEm:
              expect.stringMatching(
                /Z$/
              ),
            observacoes:
              'Ocupação mensal'
          })
        );

        expect(
          routerMock.navigateByUrl
        ).toHaveBeenCalledWith(
          '/ocupacoes'
        );

        expect(
          snackBarMock.open
        ).toHaveBeenCalled();
      }
    );

    it(
      'não deve salvar início futuro',
      async () => {
        await criarComponente(null);

        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.patchValue({
          embarcacaoId:
            'embarcacao-1',
          vagaId: 'vaga-1',
          inicioEm:
            paraDataHoraLocal(
              new Date(
                Date.now() + 7_200_000
              )
            )
        });

        fixture.componentInstance[
          'salvar'
        ]();

        expect(
          formulario.controls
            .inicioEm
            .hasError(
              'inicioFuturo'
            )
        ).toBe(true);

        expect(
          ocupacaoServiceMock.criar
        ).not.toHaveBeenCalled();
      }
    );

    it(
      'não deve salvar fim anterior ao início',
      async () => {
        await criarComponente(null);

        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.setValue({
          embarcacaoId:
            'embarcacao-1',
          vagaId: 'vaga-1',
          inicioEm:
            paraDataHoraLocal(
              new Date(
                Date.now() - 3_600_000
              )
            ),
          fimPrevistoEm:
            paraDataHoraLocal(
              new Date(
                Date.now() - 7_200_000
              )
            ),
          observacoes: ''
        });

        fixture.componentInstance[
          'salvar'
        ]();

        expect(
          formulario.hasError(
            'fimNaoPosterior'
          )
        ).toBe(true);

        expect(
          ocupacaoServiceMock.criar
        ).not.toHaveBeenCalled();
      }
    );

    it(
      'deve carregar edição com campos de vínculo bloqueados',
      async () => {
        const ocupacao =
          criarOcupacao({
            fimPrevistoEm:
              '2026-07-10T20:00:00.000Z',
            observacoes:
              'Previsão inicial'
          });

        ocupacaoServiceMock
          .buscarPorId
          .mockReturnValue(
            of(ocupacao)
          );

        await criarComponente(
          'ocupacao-1'
        );

        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        expect(
          formulario.controls
            .embarcacaoId.disabled
        ).toBe(true);

        expect(
          formulario.controls
            .vagaId.disabled
        ).toBe(true);

        expect(
          formulario.controls
            .inicioEm.disabled
        ).toBe(true);

        expect(
          formulario.controls
            .fimPrevistoEm.enabled
        ).toBe(true);

        expect(
          formulario.controls
            .observacoes.enabled
        ).toBe(true);
      }
    );

    it(
      'deve atualizar somente previsão e observações',
      async () => {
        await criarComponente(
          'ocupacao-1'
        );

        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.patchValue({
          fimPrevistoEm:
            paraDataHoraLocal(
              new Date(
                Date.now() + 7_200_000
              )
            ),
          observacoes:
            ' Nova previsão '
        });

        fixture.componentInstance[
          'salvar'
        ]();

        expect(
          ocupacaoServiceMock.atualizar
        ).toHaveBeenCalledWith(
          'ocupacao-1',
          expect.objectContaining({
            fimPrevistoEm:
              expect.stringMatching(
                /Z$/
              ),
            observacoes:
              'Nova previsão'
          })
        );

        const dados =
          ocupacaoServiceMock
            .atualizar
            .mock.calls[0][1];

        expect(
          dados
        ).not.toHaveProperty(
          'embarcacaoId'
        );

        expect(
          dados
        ).not.toHaveProperty(
          'vagaId'
        );

        expect(
          dados
        ).not.toHaveProperty(
          'inicioEm'
        );
      }
    );

    it(
      'deve bloquear edição de ocupação encerrada',
      async () => {
        ocupacaoServiceMock
          .buscarPorId
          .mockReturnValue(
            of(
              criarOcupacao({
                status: 'ENCERRADA',
                encerradaEm:
                  '2026-06-29T22:30:00.000Z'
              })
            )
          );

        await criarComponente(
          'ocupacao-1'
        );

        expect(
          fixture.componentInstance[
            'ocupacaoEncerrada'
          ]()
        ).toBe(true);

        expect(
          fixture.componentInstance[
            'formulario'
          ].disabled
        ).toBe(true);

        fixture.componentInstance[
          'salvar'
        ]();

        expect(
          ocupacaoServiceMock.atualizar
        ).not.toHaveBeenCalled();
      }
    );

    async function criarComponente(
      ocupacaoId: string | null
    ): Promise<void> {
      TestBed.configureTestingModule({
        imports: [
          OcupacaoFormularioComponent
        ],
        providers: [
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
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                paramMap:
                  convertToParamMap(
                    ocupacaoId
                      ? {
                          id: ocupacaoId
                        }
                      : {}
                  )
              }
            }
          },
          {
            provide: Router,
            useValue: routerMock
          }
        ]
      });

      TestBed.overrideComponent(
        OcupacaoFormularioComponent,
        {
          add: {
            providers: [
              {
                provide: MatSnackBar,
                useValue: snackBarMock
              }
            ]
          }
        }
      );

      await TestBed.compileComponents();

      fixture =
        TestBed.createComponent(
          OcupacaoFormularioComponent
        );

      fixture.detectChanges();
      await fixture.whenStable();
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

    function criarEmbarcacao(
      alteracoes:
        Partial<Embarcacao> = {}
    ): Embarcacao {
      return {
        id: 'embarcacao-1',
        nome: 'Aurora',
        fabricante: 'Schaefer',
        modelo: 'V33',
        anoFabricacao: 2023,
        numeroInscricao:
          'PR-123456',
        proprietarioNome: 'João',
        comprimentoTotalMetros: 10,
        bocaMetros: 3,
        caladoMetros: 1,
        alturaTotalMetros: 3,
        pesoKg: 5000,
        ativa: true,
        ...alteracoes
      } as Embarcacao;
    }

    function criarVaga(
      alteracoes:
        Partial<Vaga> = {}
    ): Vaga {
      return {
        id: 'vaga-1',
        codigo: 'A-01',
        setor: 'Pier A',
        localizacao:
          'Corredor principal',
        comprimentoMaximoMetros: 12,
        bocaMaximaMetros: 4,
        caladoMaximoMetros: 1.5,
        alturaMaximaMetros: 4,
        pesoMaximoKg: 7000,
        ativa: true,
        ...alteracoes
      } as Vaga;
    }

    function paraDataHoraLocal(
      data: Date
    ): string {
      const deslocamento =
        data.getTimezoneOffset()
          * 60_000;

      return new Date(
        data.getTime() - deslocamento
      )
        .toISOString()
        .slice(0, 16);
    }
  }
);
