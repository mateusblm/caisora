import { BreakpointObserver } from "@angular/cdk/layout";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { of, throwError } from "rxjs";
import { vi } from "vitest";
import { Cliente } from "../../../clientes/models/cliente.model";
import { ClienteService } from "../../../clientes/services/cliente.service";
import { Embarcacao } from "../../../embarcacoes/models/embarcacao.model";
import { EmbarcacaoService } from "../../../embarcacoes/services/embarcacao.service";
import { Contrato } from "../../models/contrato.model";
import { ContratoService } from "../../services/contrato.service";
import { ContratoListagemComponent } from "./contrato-listagem.component";

describe("ContratoListagemComponent", () => {
  let fixture: ComponentFixture<ContratoListagemComponent>;

  const contratoServiceMock = {
    listar: vi.fn(),
  };
  const clienteServiceMock = {
    listarTodosAtivos: vi.fn(),
  };
  const embarcacaoServiceMock = {
    listarTodas: vi.fn(),
  };
  const breakpointObserverMock = {
    observe: vi.fn().mockReturnValue(of({ matches: false, breakpoints: {} })),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    contratoServiceMock.listar.mockReturnValue(of(paginaComContrato()));
    clienteServiceMock.listarTodosAtivos.mockReturnValue(of([criarCliente()]));
    embarcacaoServiceMock.listarTodas.mockReturnValue(of([criarEmbarcacao()]));

    TestBed.configureTestingModule({
      imports: [ContratoListagemComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        {
          provide: ContratoService,
          useValue: contratoServiceMock,
        },
        {
          provide: ClienteService,
          useValue: clienteServiceMock,
        },
        {
          provide: EmbarcacaoService,
          useValue: embarcacaoServiceMock,
        },
        {
          provide: BreakpointObserver,
          useValue: breakpointObserverMock,
        },
      ],
    });

    await TestBed.compileComponents();
  });

  it("deve carregar opções e contratos", () => {
    criarComponente();

    expect(clienteServiceMock.listarTodosAtivos).toHaveBeenCalledOnce();
    expect(embarcacaoServiceMock.listarTodas).toHaveBeenCalledOnce();
    expect(contratoServiceMock.listar).toHaveBeenCalledWith({
      pagina: 0,
      tamanho: 10,
      status: undefined,
      clienteId: undefined,
      embarcacaoId: undefined,
    });
    expect(fixture.componentInstance["contratos"]()).toHaveLength(1);
    expect(fixture.componentInstance["totalElementos"]()).toBe(1);
  });

  it("deve aplicar os três filtros simultaneamente", () => {
    criarComponente();
    const componente = fixture.componentInstance;

    componente["filtroStatus"].setValue("ATIVO");
    componente["filtroClienteId"].setValue("cliente-1");
    componente["filtroEmbarcacaoId"].setValue("embarcacao-1");
    contratoServiceMock.listar.mockClear();

    componente["aplicarFiltros"]();

    expect(contratoServiceMock.listar).toHaveBeenCalledWith({
      pagina: 0,
      tamanho: 10,
      status: "ATIVO",
      clienteId: "cliente-1",
      embarcacaoId: "embarcacao-1",
    });
  });

  it("deve limpar todos os filtros", () => {
    criarComponente();
    const componente = fixture.componentInstance;

    componente["filtroStatus"].setValue("SUSPENSO");
    componente["filtroClienteId"].setValue("cliente-1");
    componente["filtroEmbarcacaoId"].setValue("embarcacao-1");
    contratoServiceMock.listar.mockClear();

    componente["limparFiltros"]();

    expect(componente["filtroStatus"].value).toBe("TODOS");
    expect(componente["filtroClienteId"].value).toBe("");
    expect(componente["filtroEmbarcacaoId"].value).toBe("");
    expect(contratoServiceMock.listar).toHaveBeenCalledWith({
      pagina: 0,
      tamanho: 10,
      status: undefined,
      clienteId: undefined,
      embarcacaoId: undefined,
    });
  });

  it("deve manter somente embarcações do cliente selecionado", () => {
    embarcacaoServiceMock.listarTodas.mockReturnValue(
      of([
        criarEmbarcacao(),
        criarEmbarcacao({
          id: "embarcacao-2",
          proprietarioId: "cliente-2",
          nome: "Maré Alta",
        }),
      ]),
    );
    criarComponente();
    const componente = fixture.componentInstance;

    componente["filtroClienteId"].setValue("cliente-1");

    expect(componente["embarcacoesFiltradas"]()).toEqual([
      expect.objectContaining({ id: "embarcacao-1" }),
    ]);
  });

  it("deve permitir edição somente para contrato em rascunho", () => {
    criarComponente();
    const componente = fixture.componentInstance;

    expect(componente["podeEditar"](criarContrato())).toBe(true);
    expect(componente["podeEditar"](criarContrato({ status: "ATIVO" }))).toBe(
      false,
    );
  });

  it("deve exibir mensagem quando a listagem falhar", () => {
    contratoServiceMock.listar.mockReturnValue(
      throwError(() => new Error("Falha")),
    );

    criarComponente();

    expect(fixture.componentInstance["contratos"]()).toEqual([]);
    expect(fixture.componentInstance["mensagemErro"]()).toBe(
      "Não foi possível carregar os contratos.",
    );
  });

  function criarComponente(): void {
    fixture = TestBed.createComponent(ContratoListagemComponent);
    fixture.detectChanges();
  }

  function paginaComContrato() {
    return {
      content: [criarContrato()],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      numberOfElements: 1,
      first: true,
      last: true,
      empty: false,
    };
  }

  function criarCliente(): Cliente {
    return {
      id: "cliente-1",
      nome: "João da Silva",
      ativo: true,
    } as Cliente;
  }

  function criarEmbarcacao(alteracoes: Partial<Embarcacao> = {}): Embarcacao {
    return {
      id: "embarcacao-1",
      proprietarioId: "cliente-1",
      nome: "Aurora",
      numeroInscricao: "PR-123456",
      ativa: true,
      ...alteracoes,
    } as Embarcacao;
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
});
