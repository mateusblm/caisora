import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { MatSnackBar } from "@angular/material/snack-bar";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter,
} from "@angular/router";
import { of } from "rxjs";
import { vi } from "vitest";
import { Ocupacao } from "../../../ocupacoes/models/ocupacao.model";
import { OcupacaoService } from "../../../ocupacoes/services/ocupacao.service";
import {
  Contrato,
  HistoricoContrato,
  VinculoContratoOcupacao,
} from "../../models/contrato.model";
import { ContratoService } from "../../services/contrato.service";
import { ContratoDetalheComponent } from "./contrato-detalhe.component";

describe("ContratoDetalheComponent", () => {
  let fixture: ComponentFixture<ContratoDetalheComponent>;

  const contratoServiceMock = {
    buscarPorId: vi.fn(),
    listarOcupacoes: vi.fn(),
    listarHistorico: vi.fn(),
    enviarParaAssinatura: vi.fn(),
    ativar: vi.fn(),
    suspender: vi.fn(),
    reativar: vi.fn(),
    solicitarEncerramento: vi.fn(),
    encerrar: vi.fn(),
    cancelar: vi.fn(),
    vincularOcupacao: vi.fn(),
    desvincularOcupacao: vi.fn(),
  };
  const ocupacaoServiceMock = {
    listarTodasAtivas: vi.fn(),
  };
  const dialogMock = {
    open: vi.fn(),
  };
  const snackBarMock = {
    open: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    contratoServiceMock.buscarPorId.mockReturnValue(of(criarContrato()));
    contratoServiceMock.listarOcupacoes.mockReturnValue(of([]));
    contratoServiceMock.listarHistorico.mockReturnValue(of(paginaHistorico()));
    contratoServiceMock.enviarParaAssinatura.mockReturnValue(
      of(criarContrato({ status: "PENDENTE_ASSINATURA" })),
    );
    contratoServiceMock.ativar.mockReturnValue(
      of(criarContrato({ status: "ATIVO" })),
    );
    contratoServiceMock.suspender.mockReturnValue(
      of(criarContrato({ status: "SUSPENSO" })),
    );
    contratoServiceMock.reativar.mockReturnValue(
      of(criarContrato({ status: "ATIVO" })),
    );
    contratoServiceMock.solicitarEncerramento.mockReturnValue(
      of(criarContrato({ status: "EM_ENCERRAMENTO" })),
    );
    contratoServiceMock.encerrar.mockReturnValue(
      of(criarContrato({ status: "ENCERRADO" })),
    );
    contratoServiceMock.cancelar.mockReturnValue(
      of(criarContrato({ status: "CANCELADO" })),
    );
    contratoServiceMock.vincularOcupacao.mockReturnValue(of(criarVinculo()));
    contratoServiceMock.desvincularOcupacao.mockReturnValue(
      of(
        criarVinculo({
          aberto: false,
          fimEm: "2026-08-01T12:00:00.000Z",
          motivoFim: "Mudança de vaga",
        }),
      ),
    );
    ocupacaoServiceMock.listarTodasAtivas.mockReturnValue(
      of([
        criarOcupacao(),
        criarOcupacao({
          id: "ocupacao-2",
          embarcacaoId: "embarcacao-2",
          embarcacaoNome: "Maré Alta",
          vagaCodigo: "B-02",
        }),
      ]),
    );
    dialogMock.open.mockReturnValue({
      afterClosed: () =>
        of({
          confirmado: true,
          data: "2026-07-05",
          descricao: "Ação confirmada",
        }),
    });

    TestBed.configureTestingModule({
      imports: [ContratoDetalheComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: "contrato-1" }),
            },
          },
        },
        {
          provide: ContratoService,
          useValue: contratoServiceMock,
        },
        {
          provide: OcupacaoService,
          useValue: ocupacaoServiceMock,
        },
        {
          provide: MatDialog,
          useValue: dialogMock,
        },
        {
          provide: MatSnackBar,
          useValue: snackBarMock,
        },
      ],
    });

    TestBed.overrideComponent(ContratoDetalheComponent, {
      add: {
        providers: [
          {
            provide: MatDialog,
            useValue: dialogMock,
          },
          {
            provide: MatSnackBar,
            useValue: snackBarMock,
          },
        ],
      },
    });

    await TestBed.compileComponents();
  });

  it("deve carregar contrato, ocupações e histórico", () => {
    criarComponente();
    const componente = fixture.componentInstance;

    expect(contratoServiceMock.buscarPorId).toHaveBeenCalledWith("contrato-1");
    expect(contratoServiceMock.listarOcupacoes).toHaveBeenCalledWith(
      "contrato-1",
    );
    expect(contratoServiceMock.listarHistorico).toHaveBeenCalledWith(
      "contrato-1",
      0,
      100,
    );
    expect(ocupacaoServiceMock.listarTodasAtivas).toHaveBeenCalledOnce();
    expect(componente["contrato"]()?.numero).toBe("CTR-2026-000001");
    expect(componente["historico"]()).toHaveLength(1);
  });

  it("deve habilitar ações conforme o status", () => {
    criarComponente();
    const componente = fixture.componentInstance;

    expect(componente["podeEditar"]()).toBe(true);
    expect(componente["podeEnviarParaAssinatura"]()).toBe(true);
    expect(componente["podeAtivar"]()).toBe(false);
    expect(componente["podeCancelar"]()).toBe(true);

    componente["contrato"].set(criarContrato({ status: "ATIVO" }));

    expect(componente["podeEditar"]()).toBe(false);
    expect(componente["podeSuspender"]()).toBe(true);
    expect(componente["podeGerenciarOcupacao"]()).toBe(true);
    expect(componente["podeSolicitarEncerramento"]()).toBe(true);
  });

  it("deve oferecer somente ocupação da embarcação contratada", () => {
    contratoServiceMock.buscarPorId.mockReturnValue(
      of(criarContrato({ status: "ATIVO" })),
    );
    criarComponente();

    expect(fixture.componentInstance["ocupacoesDisponiveis"]()).toEqual([
      expect.objectContaining({
        id: "ocupacao-1",
        embarcacaoId: "embarcacao-1",
      }),
    ]);
  });

  it("não deve oferecer nova ocupação quando já houver vínculo aberto", () => {
    contratoServiceMock.buscarPorId.mockReturnValue(
      of(criarContrato({ status: "ATIVO" })),
    );
    contratoServiceMock.listarOcupacoes.mockReturnValue(of([criarVinculo()]));
    criarComponente();

    expect(fixture.componentInstance["vinculoAberto"]()?.id).toBe("vinculo-1");
    expect(fixture.componentInstance["ocupacoesDisponiveis"]()).toEqual([]);
  });

  it("deve vincular ocupação selecionada", () => {
    contratoServiceMock.buscarPorId.mockReturnValue(
      of(criarContrato({ status: "ATIVO" })),
    );
    criarComponente();
    const componente = fixture.componentInstance;

    componente["ocupacaoSelecionadaId"].setValue("ocupacao-1");
    componente["vincularOcupacao"]();

    expect(contratoServiceMock.vincularOcupacao).toHaveBeenCalledWith(
      "contrato-1",
      "ocupacao-1",
    );
    expect(snackBarMock.open).toHaveBeenCalledWith(
      "Ocupação vinculada ao contrato.",
      "Fechar",
      expect.any(Object),
    );
  });

  it("deve enviar contrato para assinatura após confirmação", () => {
    criarComponente();

    fixture.componentInstance["solicitarEnvioParaAssinatura"]();

    expect(dialogMock.open).toHaveBeenCalled();
    expect(contratoServiceMock.enviarParaAssinatura).toHaveBeenCalledWith(
      "contrato-1",
      "Ação confirmada",
    );
    expect(snackBarMock.open).toHaveBeenCalledWith(
      "Contrato enviado para assinatura.",
      "Fechar",
      expect.any(Object),
    );
  });

  function criarComponente(): void {
    fixture = TestBed.createComponent(ContratoDetalheComponent);
    fixture.detectChanges();
  }

  function criarContrato(alteracoes: Partial<Contrato> = {}): Contrato {
    return {
      id: "contrato-1",
      numero: "CTR-2026-000001",
      status: "RASCUNHO",
      clienteId: "cliente-1",
      clienteNome: "João da Silva",
      embarcacaoId: "embarcacao-1",
      embarcacaoNome: "Aurora",
      tipoVagaContratada: "SECA",
      periodicidade: "MENSAL",
      dataInicio: "2026-07-05",
      dataFim: null,
      dataAssinatura: null,
      dataAtivacao: null,
      dataEncerramento: null,
      renovacaoAutomatica: true,
      diasAvisoPrevio: 30,
      valorBase: 1800,
      diaVencimento: 10,
      observacoes: null,
      criadoPorId: "usuario-1",
      criadoPorNome: "Administrador",
      organizacaoId: "organizacao-1",
      criadoEm: "2026-07-05T12:00:00.000Z",
      atualizadoEm: "2026-07-05T12:00:00.000Z",
      versao: 0,
      ...alteracoes,
    };
  }

  function criarVinculo(
    alteracoes: Partial<VinculoContratoOcupacao> = {},
  ): VinculoContratoOcupacao {
    return {
      id: "vinculo-1",
      contratoId: "contrato-1",
      ocupacaoId: "ocupacao-1",
      vagaId: "vaga-1",
      vagaCodigo: "A-01",
      inicioEm: "2026-07-05T12:00:00.000Z",
      fimEm: null,
      motivoFim: null,
      aberto: true,
      ...alteracoes,
    };
  }

  function criarHistorico(): HistoricoContrato {
    return {
      id: "historico-1",
      contratoId: "contrato-1",
      tipoEvento: "CRIADO",
      statusAnterior: null,
      statusNovo: "RASCUNHO",
      descricao: "Contrato criado",
      realizadoPorId: "usuario-1",
      realizadoPorNome: "Administrador",
      realizadoEm: "2026-07-05T12:00:00.000Z",
    };
  }

  function paginaHistorico() {
    return {
      content: [criarHistorico()],
      totalElements: 1,
      totalPages: 1,
      size: 100,
      number: 0,
      numberOfElements: 1,
      first: true,
      last: true,
      empty: false,
    };
  }

  function criarOcupacao(alteracoes: Partial<Ocupacao> = {}): Ocupacao {
    return {
      id: "ocupacao-1",
      embarcacaoId: "embarcacao-1",
      embarcacaoNome: "Aurora",
      embarcacaoModelo: "Focker 320",
      proprietarioNome: "João da Silva",
      vagaId: "vaga-1",
      vagaCodigo: "A-01",
      vagaTipo: "SECA",
      vagaSetor: "Pátio A",
      vagaLocalizacao: "Corredor principal",
      status: "ATIVA",
      inicioEm: "2026-07-05T12:00:00.000Z",
      fimPrevistoEm: null,
      encerradaEm: null,
      observacoes: null,
      organizacaoId: "organizacao-1",
      criadaEm: "2026-07-05T12:00:00.000Z",
      atualizadaEm: "2026-07-05T12:00:00.000Z",
      ...alteracoes,
    };
  }
});
