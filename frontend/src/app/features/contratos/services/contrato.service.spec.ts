import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { environment } from "../../../../environments/environment";
import { Contrato, DadosContrato } from "../models/contrato.model";
import { ContratoService } from "./contrato.service";

describe("ContratoService", () => {
  let service: ContratoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ContratoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it("deve listar contratos com todos os filtros", () => {
    service
      .listar({
        pagina: 1,
        tamanho: 10,
        status: "ATIVO",
        clienteId: "cliente-1",
        embarcacaoId: "embarcacao-1",
      })
      .subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.url === `${environment.apiUrl}/contratos` &&
        request.params.get("page") === "1" &&
        request.params.get("size") === "10" &&
        request.params.get("sort") === "criadoEm,desc" &&
        request.params.get("status") === "ATIVO" &&
        request.params.get("clienteId") === "cliente-1" &&
        request.params.get("embarcacaoId") === "embarcacao-1",
    );

    expect(requisicao.request.method).toBe("GET");
    requisicao.flush(paginaVazia());
  });

  it("deve criar contrato", () => {
    const dados = criarDadosContrato();

    service.criar(dados).subscribe();

    const requisicao = httpTesting.expectOne(`${environment.apiUrl}/contratos`);
    expect(requisicao.request.method).toBe("POST");
    expect(requisicao.request.body).toEqual(dados);
    requisicao.flush(criarContrato());
  });

  it("deve atualizar contrato", () => {
    const dados = criarDadosContrato();

    service.atualizar("contrato-1", dados).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/contratos/contrato-1`,
    );
    expect(requisicao.request.method).toBe("PUT");
    expect(requisicao.request.body).toEqual(dados);
    requisicao.flush(criarContrato());
  });

  it("deve enviar contrato para assinatura com descrição normalizada", () => {
    service
      .enviarParaAssinatura("contrato-1", "  Revisado pelo gerente  ")
      .subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/contratos/contrato-1/enviar-para-assinatura`,
    );
    expect(requisicao.request.method).toBe("POST");
    expect(requisicao.request.body).toEqual({
      descricao: "Revisado pelo gerente",
    });
    requisicao.flush({
      ...criarContrato(),
      status: "PENDENTE_ASSINATURA",
    });
  });

  it("deve ativar contrato", () => {
    const dados = {
      dataAssinatura: "2026-07-05",
      descricao: "Assinado presencialmente",
    };

    service.ativar("contrato-1", dados).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/contratos/contrato-1/ativar`,
    );
    expect(requisicao.request.method).toBe("POST");
    expect(requisicao.request.body).toEqual(dados);
    requisicao.flush({
      ...criarContrato(),
      status: "ATIVO",
    });
  });

  it("deve encerrar contrato", () => {
    const dados = {
      dataEncerramento: "2026-12-31",
      descricao: "Encerramento solicitado pelo cliente",
    };

    service.encerrar("contrato-1", dados).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/contratos/contrato-1/encerrar`,
    );
    expect(requisicao.request.method).toBe("POST");
    expect(requisicao.request.body).toEqual(dados);
    requisicao.flush({
      ...criarContrato(),
      status: "ENCERRADO",
    });
  });

  it("deve vincular ocupação", () => {
    service.vincularOcupacao("contrato-1", "ocupacao-1").subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/contratos/contrato-1/ocupacoes`,
    );
    expect(requisicao.request.method).toBe("POST");
    expect(requisicao.request.body).toEqual({
      ocupacaoId: "ocupacao-1",
    });
    requisicao.flush({
      id: "vinculo-1",
      contratoId: "contrato-1",
      ocupacaoId: "ocupacao-1",
      vagaId: "vaga-1",
      vagaCodigo: "A-01",
      inicioEm: "2026-07-05T12:00:00.000Z",
      fimEm: null,
      motivoFim: null,
      aberto: true,
    });
  });

  it("deve desvincular ocupação enviando motivo na query string", () => {
    service
      .desvincularOcupacao("contrato-1", "vinculo-1", "  Mudança de vaga  ")
      .subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.url ===
          `${environment.apiUrl}/contratos/contrato-1/ocupacoes/vinculo-1` &&
        request.params.get("motivo") === "Mudança de vaga",
    );
    expect(requisicao.request.method).toBe("DELETE");
    requisicao.flush({
      id: "vinculo-1",
      contratoId: "contrato-1",
      ocupacaoId: "ocupacao-1",
      vagaId: "vaga-1",
      vagaCodigo: "A-01",
      inicioEm: "2026-07-05T12:00:00.000Z",
      fimEm: "2026-08-01T12:00:00.000Z",
      motivoFim: "Mudança de vaga",
      aberto: false,
    });
  });

  it("deve listar histórico com paginação", () => {
    service.listarHistorico("contrato-1", 0, 25).subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.url ===
          `${environment.apiUrl}/contratos/contrato-1/historico` &&
        request.params.get("page") === "0" &&
        request.params.get("size") === "25" &&
        request.params.get("sort") === "realizadoEm,desc",
    );
    expect(requisicao.request.method).toBe("GET");
    requisicao.flush(paginaVazia(25));
  });

  function paginaVazia(size = 10) {
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size,
      number: 0,
      numberOfElements: 0,
      first: true,
      last: true,
      empty: true,
    };
  }

  function criarDadosContrato(): DadosContrato {
    return {
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
    };
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
