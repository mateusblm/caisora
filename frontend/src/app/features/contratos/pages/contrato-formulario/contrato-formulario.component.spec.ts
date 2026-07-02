import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import {
  ActivatedRoute,
  Router,
  convertToParamMap,
  provideRouter,
} from "@angular/router";
import { MatSnackBar } from "@angular/material/snack-bar";
import { of } from "rxjs";
import { vi } from "vitest";
import { Cliente } from "../../../clientes/models/cliente.model";
import { ClienteService } from "../../../clientes/services/cliente.service";
import { Embarcacao } from "../../../embarcacoes/models/embarcacao.model";
import { EmbarcacaoService } from "../../../embarcacoes/services/embarcacao.service";
import { Contrato } from "../../models/contrato.model";
import { ContratoService } from "../../services/contrato.service";
import { ContratoFormularioComponent } from "./contrato-formulario.component";

describe("ContratoFormularioComponent", () => {
  let fixture: ComponentFixture<ContratoFormularioComponent>;
  let router: Router;

  const contratoServiceMock = {
    buscarPorId: vi.fn(),
    criar: vi.fn(),
    atualizar: vi.fn(),
  };
  const clienteServiceMock = {
    listarTodosAtivos: vi.fn(),
    buscarPorId: vi.fn(),
  };
  const embarcacaoServiceMock = {
    listarTodas: vi.fn(),
    buscarPorId: vi.fn(),
  };
  const snackBarMock = {
    open: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    clienteServiceMock.listarTodosAtivos.mockReturnValue(of([criarCliente()]));
    clienteServiceMock.buscarPorId.mockReturnValue(of(criarCliente()));
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
    embarcacaoServiceMock.buscarPorId.mockReturnValue(of(criarEmbarcacao()));
    contratoServiceMock.criar.mockReturnValue(of(criarContrato()));
    contratoServiceMock.atualizar.mockReturnValue(of(criarContrato()));

    TestBed.configureTestingModule({
      imports: [ContratoFormularioComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({}),
            },
          },
        },
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
          provide: MatSnackBar,
          useValue: snackBarMock,
        },
      ],
    });

    TestBed.overrideComponent(ContratoFormularioComponent, {
      add: {
        providers: [
          {
            provide: MatSnackBar,
            useValue: snackBarMock,
          },
        ],
      },
    });

    await TestBed.compileComponents();
    router = TestBed.inject(Router);
  });

  it("deve carregar clientes e filtrar embarcações pelo proprietário", () => {
    criarComponente();
    const componente = fixture.componentInstance;

    componente["formulario"].controls.clienteId.setValue("cliente-1");

    expect(componente["clientes"]()).toHaveLength(1);
    expect(componente["embarcacoesDoCliente"]()).toEqual([
      expect.objectContaining({
        id: "embarcacao-1",
        proprietarioId: "cliente-1",
      }),
    ]);
  });

  it("deve limpar embarcação ao trocar para outro cliente", () => {
    criarComponente();
    const formulario = fixture.componentInstance["formulario"];

    formulario.controls.clienteId.setValue("cliente-1");
    formulario.controls.embarcacaoId.setValue("embarcacao-1");
    formulario.controls.clienteId.setValue("cliente-2");

    expect(formulario.controls.embarcacaoId.value).toBe("");
  });

  it("deve exigir data final na periodicidade personalizada", () => {
    criarComponente();
    const formulario = fixture.componentInstance["formulario"];

    preencherFormularioValido(formulario);
    formulario.controls.periodicidade.setValue("PERSONALIZADA");
    formulario.controls.dataFim.setValue("");

    expect(formulario.hasError("dataFimObrigatoria")).toBe(true);
    expect(formulario.invalid).toBe(true);
  });

  it("deve exigir aviso prévio quando houver renovação automática", () => {
    criarComponente();
    const formulario = fixture.componentInstance["formulario"];

    preencherFormularioValido(formulario);
    formulario.controls.renovacaoAutomatica.setValue(true);
    formulario.controls.diasAvisoPrevio.setValue(null);

    expect(formulario.hasError("avisoPrevioObrigatorio")).toBe(true);
  });

  it("deve criar contrato válido e navegar para os detalhes", () => {
    criarComponente();
    const componente = fixture.componentInstance;
    const navigateSpy = vi.spyOn(router, "navigate").mockResolvedValue(true);

    preencherFormularioValido(componente["formulario"]);
    componente["salvar"]();

    expect(contratoServiceMock.criar).toHaveBeenCalledWith({
      clienteId: "cliente-1",
      embarcacaoId: "embarcacao-1",
      tipoVagaContratada: "SECA",
      periodicidade: "MENSAL",
      dataInicio: "2026-07-05",
      dataFim: null,
      renovacaoAutomatica: true,
      diasAvisoPrevio: 30,
      valorBase: 1800,
      diaVencimento: 10,
      observacoes: "Contrato inicial",
    });
    expect(navigateSpy).toHaveBeenCalledWith(["/contratos", "contrato-1"]);
    expect(snackBarMock.open).toHaveBeenCalled();
  });

  function criarComponente(): void {
    fixture = TestBed.createComponent(ContratoFormularioComponent);
    fixture.detectChanges();
  }

  function preencherFormularioValido(
    formulario: ContratoFormularioComponent["formulario"],
  ): void {
    formulario.setValue({
      clienteId: "cliente-1",
      embarcacaoId: "embarcacao-1",
      tipoVagaContratada: "SECA",
      periodicidade: "MENSAL",
      dataInicio: "2026-07-05",
      dataFim: "",
      renovacaoAutomatica: true,
      diasAvisoPrevio: 30,
      valorBase: 1800,
      diaVencimento: 10,
      observacoes: "Contrato inicial",
    });
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
      proprietarioNome: "João da Silva",
      nome: "Aurora",
      numeroInscricao: "PR-123456",
      fabricante: "Fibrafort",
      modelo: "Focker 320",
      ativa: true,
      ...alteracoes,
    } as Embarcacao;
  }

  function criarContrato(): Contrato {
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
      observacoes: "Contrato inicial",
      criadoPorId: "usuario-1",
      criadoPorNome: "Administrador",
      organizacaoId: "organizacao-1",
      criadoEm: "2026-07-05T12:00:00.000Z",
      atualizadoEm: "2026-07-05T12:00:00.000Z",
      versao: 0,
    };
  }
});
