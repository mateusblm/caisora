import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import {
  ActivatedRoute,
  Router,
  convertToParamMap
} from '@angular/router';
import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Embarcacao } from '../../../embarcacoes/models/embarcacao.model';
import { EmbarcacaoService } from '../../../embarcacoes/services/embarcacao.service';
import { Ocupacao } from '../../../ocupacoes/models/ocupacao.model';
import { OcupacaoService } from '../../../ocupacoes/services/ocupacao.service';
import { Vaga } from '../../../vagas/models/vaga.model';
import { VagaService } from '../../../vagas/services/vaga.service';
import {
  Movimentacao,
  PosicaoEmbarcacao,
  UsuarioOperador
} from '../../models/movimentacao.model';
import { MovimentacaoService } from '../../services/movimentacao.service';
import { MovimentacaoFormularioComponent } from './movimentacao-formulario.component';

describe(
  'MovimentacaoFormularioComponent',
  () => {
    let fixture: ComponentFixture<
      MovimentacaoFormularioComponent
    >;

    const movimentacaoServiceMock = {
      buscarPorId: vi.fn(),
      buscarPosicaoEmbarcacao: vi.fn(),
      listarTodasAbertas: vi.fn(),
      listarUsuariosAtivos: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn()
    };

    const embarcacaoServiceMock = {
      listarTodas: vi.fn()
    };

    const vagaServiceMock = {
      listarTodas: vi.fn()
    };

    const ocupacaoServiceMock = {
      listarTodasAtivas: vi.fn()
    };

    const routerMock = {
      navigateByUrl: vi.fn()
    };

    const snackBarMock = {
      open: vi.fn()
    };

    beforeEach(async () => {
      vi.clearAllMocks();

      movimentacaoServiceMock
        .listarTodasAbertas
        .mockReturnValue(of([]));

      movimentacaoServiceMock
        .listarUsuariosAtivos
        .mockReturnValue(of([]));

      movimentacaoServiceMock
        .buscarPosicaoEmbarcacao
        .mockReturnValue(
          of(criarPosicao())
        );

      movimentacaoServiceMock
        .criar
        .mockReturnValue(
          of(criarMovimentacao())
        );

      movimentacaoServiceMock
        .atualizar
        .mockReturnValue(
          of(criarMovimentacao())
        );

      embarcacaoServiceMock
        .listarTodas
        .mockReturnValue(
          of([criarEmbarcacao()])
        );

      vagaServiceMock
        .listarTodas
        .mockReturnValue(
          of([
            criarVaga(),
            criarVagaDestino()
          ])
        );

      ocupacaoServiceMock
        .listarTodasAtivas
        .mockReturnValue(
          of([criarOcupacao()])
        );

      await TestBed
        .configureTestingModule({
          imports: [
            MovimentacaoFormularioComponent
          ],
          providers: [
            provideNoopAnimations(),
            {
              provide: MovimentacaoService,
              useValue: movimentacaoServiceMock
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
              provide: OcupacaoService,
              useValue: ocupacaoServiceMock
            },
            {
              provide: ActivatedRoute,
              useValue: {
                snapshot: {
                  paramMap:
                    convertToParamMap({})
                }
              }
            },
            {
              provide: Router,
              useValue: routerMock
            },
            {
              provide: MatSnackBar,
              useValue: snackBarMock
            }
          ]
        })
        .compileComponents();

      fixture = TestBed.createComponent(
        MovimentacaoFormularioComponent
      );

      fixture.detectChanges();
    });

    it(
      'deve carregar os dados necessários',
      () => {
        expect(
          embarcacaoServiceMock.listarTodas
        ).toHaveBeenCalled();

        expect(
          vagaServiceMock.listarTodas
        ).toHaveBeenCalled();

        expect(
          ocupacaoServiceMock.listarTodasAtivas
        ).toHaveBeenCalled();

        expect(
          movimentacaoServiceMock
            .listarTodasAbertas
        ).toHaveBeenCalled();

        expect(
          fixture.componentInstance[
            'embarcacoesDisponiveis'
          ]()
        ).toHaveLength(1);
      }
    );

    it(
      'deve carregar a posição ao selecionar embarcação',
      () => {
        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.controls
          .embarcacaoId
          .setValue('embarcacao-1');

        expect(
          movimentacaoServiceMock
            .buscarPosicaoEmbarcacao
        ).toHaveBeenCalledWith(
          'embarcacao-1'
        );

        expect(
          fixture.componentInstance[
            'posicaoAtual'
          ]()?.tipo
        ).toBe('VAGA');
      }
    );

    it(
      'deve configurar lançamento para água',
      () => {
        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.controls
          .embarcacaoId
          .setValue('embarcacao-1');

        formulario.controls
          .tipo
          .setValue('LANCAMENTO');

        expect(
          formulario.controls
            .tipoPosicaoDestino
            .value
        ).toBe('AGUA');

        expect(
          formulario.controls
            .vagaDestinoId
            .disabled
        ).toBe(true);
      }
    );

    it(
      'deve mostrar todos os tipos com indisponiveis desabilitados',
      () => {
        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.controls
          .embarcacaoId
          .setValue('embarcacao-1');

        const tipos =
          fixture.componentInstance[
            'tiposDisponiveis'
          ]();

        expect(
          tipos.map((opcao) => opcao.valor)
        ).toEqual([
          'LANCAMENTO',
          'RETIRADA',
          'RETORNO_PARA_VAGA',
          'TRANSFERENCIA',
          'DESLOCAMENTO_INTERNO'
        ]);

        expect(
          tipos.find(
            (opcao) =>
              opcao.valor === 'RETORNO_PARA_VAGA'
          )
        ).toEqual(
          expect.objectContaining({
            disponivel: false,
            motivoIndisponibilidade:
              expect.stringContaining(
                'area de servico'
              )
          })
        );
      }
    );

    it(
      'deve permitir retorno para vaga da area de servico com ocupacao ativa',
      () => {
        movimentacaoServiceMock
          .buscarPosicaoEmbarcacao
          .mockReturnValue(
            of(
              criarPosicao({
                tipo: 'AREA_SERVICO',
                vagaId: null,
                vagaCodigo: null
              })
            )
          );

        const formulario =
          fixture.componentInstance[
            'formulario'
          ];

        formulario.controls
          .embarcacaoId
          .setValue('embarcacao-1');

        formulario.controls.tipo
          .setValue('RETORNO_PARA_VAGA');

        expect(
          formulario.controls
            .tipoPosicaoDestino
            .value
        ).toBe('VAGA');

        expect(
          formulario.controls
            .vagaDestinoId
            .value
        ).toBe('vaga-1');

        expect(
          formulario.controls
            .vagaDestinoId
            .disabled
        ).toBe(true);
      }
    );

    it(
      'nao deve enviar tipo indisponivel mesmo se o HTML for manipulado',
      () => {
        const componente =
          fixture.componentInstance;

        const formulario =
          componente['formulario'];

        formulario.controls
          .embarcacaoId
          .setValue('embarcacao-1');

        formulario.controls.tipo
          .setValue('RETORNO_PARA_VAGA');

        componente['salvar']();

        expect(
          movimentacaoServiceMock.criar
        ).not.toHaveBeenCalled();
      }
    );

    it(
      'deve manter UsuarioOperador exportado',
      () => {
        const operador: UsuarioOperador = {
          id: 'usuario-1',
          nome: 'Operador',
          email: 'operador@caisora.com.br',
          perfil: 'OPERADOR',
          ativo: true,
          organizacaoId: 'organizacao-1',
          organizacaoNome: 'Marina',
          criadoEm: '2026-07-01T10:00:00.000Z',
          atualizadoEm: '2026-07-01T10:00:00.000Z'
        };

        expect(operador.nome).toBe('Operador');
      }
    );

    it(
      'deve criar movimentação válida',
      () => {
        const componente =
          fixture.componentInstance;

        const formulario =
          componente['formulario'];

        formulario.controls
          .embarcacaoId
          .setValue('embarcacao-1');

        formulario.controls
          .tipo
          .setValue('LANCAMENTO');

        formulario.controls
          .prioridade
          .setValue('ALTA');

        formulario.controls
          .descricaoDestino
          .setValue('Rampa principal');

        formulario.controls
          .observacoes
          .setValue('Preparar carreta');

        componente['salvar']();

        expect(
          movimentacaoServiceMock.criar
        ).toHaveBeenCalledWith(
          expect.objectContaining({
            embarcacaoId: 'embarcacao-1',
            tipo: 'LANCAMENTO',
            prioridade: 'ALTA',
            tipoPosicaoDestino: 'AGUA',
            vagaDestinoId: null,
            descricaoDestino: 'Rampa principal',
            observacoes: 'Preparar carreta'
          })
        );

        expect(
          routerMock.navigateByUrl
        ).toHaveBeenCalledWith(
          '/movimentacoes'
        );
      }
    );

    function criarEmbarcacao(): Embarcacao {
      return {
        id: 'embarcacao-1',
        proprietarioId: 'cliente-1',
        proprietarioNome: 'João',
        nome: 'Aurora',
        tipo: 'LANCHA',
        fabricante: 'Volvo',
        modelo: 'V33',
        anoFabricacao: 2024,
        numeroInscricao: 'PR-123456',
        numeroCasco: null,
        portoInscricao: null,
        codigoPaisBandeira: 'BR',
        comprimentoTotalMetros: 10,
        bocaMetros: 3,
        caladoMetros: 1,
        pontalMetros: null,
        alturaTotalMetros: 3,
        pesoKg: 5000,
        capacidadePessoas: 8,
        tipoPropulsao: 'MOTOR',
        corPredominante: 'Branca',
        observacoes: null,
        ativa: true,
        organizacaoId: 'organizacao-1',
        criadaEm:
          '2026-06-30T10:00:00.000Z',
        atualizadaEm:
          '2026-06-30T10:00:00.000Z'
      };
    }

    function criarVaga(): Vaga {
      return {
        id: 'vaga-1',
        codigo: 'A-01',
        tipo: 'SECA',
        setor: 'Galpão',
        localizacao: null,
        comprimentoMaximoMetros: 12,
        bocaMaximaMetros: 4,
        caladoMaximoMetros: 2,
        alturaMaximaMetros: 5,
        pesoMaximoKg: 9000,
        possuiAgua: false,
        possuiEnergia: true,
        observacoes: null,
        ativa: true,
        organizacaoId: 'organizacao-1',
        criadaEm:
          '2026-06-30T10:00:00.000Z',
        atualizadaEm:
          '2026-06-30T10:00:00.000Z'
      };
    }

    function criarVagaDestino(): Vaga {
      return {
        ...criarVaga(),
        id: 'vaga-2',
        codigo: 'A-02'
      };
    }

    function criarOcupacao(): Ocupacao {
      return {
        id: 'ocupacao-1',
        embarcacaoId: 'embarcacao-1',
        embarcacaoNome: 'Aurora',
        embarcacaoModelo: 'V33',
        proprietarioNome: 'João',
        vagaId: 'vaga-1',
        vagaCodigo: 'A-01',
        vagaTipo: 'SECA',
        vagaSetor: 'Galpão',
        vagaLocalizacao: null,
        status: 'ATIVA',
        inicioEm:
          '2026-06-20T10:00:00.000Z',
        fimPrevistoEm: null,
        encerradaEm: null,
        observacoes: null,
        organizacaoId: 'organizacao-1',
        criadaEm:
          '2026-06-20T10:00:00.000Z',
        atualizadaEm:
          '2026-06-20T10:00:00.000Z'
      };
    }

    function criarPosicao(
      alteracoes: Partial<PosicaoEmbarcacao> = {}
    ):
      PosicaoEmbarcacao {
      return {
        id: 'posicao-1',
        embarcacaoId: 'embarcacao-1',
        embarcacaoNome: 'Aurora',
        embarcacaoModelo: 'V33',
        proprietarioNome: 'João',
        tipo: 'VAGA',
        vagaId: 'vaga-1',
        vagaCodigo: 'A-01',
        vagaSetor: 'Galpão',
        vagaLocalizacao: null,
        descricaoLocal: null,
        movimentacaoOrigemId: null,
        versao: 0,
        organizacaoId: 'organizacao-1',
        criadaEm:
          '2026-06-30T10:00:00.000Z',
        atualizadaEm:
          '2026-06-30T10:00:00.000Z',
        ...alteracoes
      };
    }

    function criarMovimentacao():
      Movimentacao {
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
          '2026-06-30T10:00:00.000Z',
        atualizadaEm:
          '2026-06-30T10:00:00.000Z'
      };
    }
  }
);
