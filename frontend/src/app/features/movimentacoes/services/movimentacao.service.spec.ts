import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import {
  AtualizarMovimentacao,
  CancelarMovimentacao,
  ConcluirMovimentacao,
  CriarMovimentacao,
  IniciarMovimentacao,
  Movimentacao
} from '../models/movimentacao.model';
import { MovimentacaoService } from './movimentacao.service';

describe('MovimentacaoService', () => {

  let service: MovimentacaoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(MovimentacaoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('deve listar movimentações com paginação', () => {
    service.listar({
      pagina: 0,
      tamanho: 20
    }).subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.url === `${environment.apiUrl}/movimentacoes`
        && request.params.get('page') === '0'
        && request.params.get('size') === '20'
        && request.params.get('sort') === 'agendadaPara,asc'
    );

    expect(requisicao.request.method).toBe('GET');
    requisicao.flush(paginaVazia());
  });

  it('deve priorizar o filtro por status', () => {
    service.listar({
      pagina: 0,
      tamanho: 20,
      status: 'AGENDADA',
      tipo: 'TRANSFERENCIA',
      embarcacaoId: 'embarcacao-1',
      inicio: '2026-06-30T00:00:00.000Z',
      fim: '2026-06-30T23:59:59.999Z'
    }).subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.params.get('status') === 'AGENDADA'
    );

    expect(requisicao.request.params.has('tipo')).toBe(false);
    expect(requisicao.request.params.has('embarcacaoId')).toBe(false);
    expect(requisicao.request.params.has('inicio')).toBe(false);
    expect(requisicao.request.params.has('fim')).toBe(false);

    requisicao.flush(paginaVazia());
  });

  it('deve priorizar o filtro por tipo quando não houver status', () => {
    service.listar({
      pagina: 0,
      tamanho: 20,
      tipo: 'TRANSFERENCIA',
      embarcacaoId: 'embarcacao-1'
    }).subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.params.get('tipo') === 'TRANSFERENCIA'
    );

    expect(
      requisicao.request.params.has('embarcacaoId')
    ).toBe(false);

    requisicao.flush(paginaVazia());
  });

  it('deve filtrar por embarcação', () => {
    service.listar({
      pagina: 0,
      tamanho: 20,
      embarcacaoId: 'embarcacao-1'
    }).subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.params.get('embarcacaoId') === 'embarcacao-1'
    );

    expect(requisicao.request.method).toBe('GET');
    requisicao.flush(paginaVazia());
  });

  it('deve filtrar por período completo', () => {
    const inicio = '2026-06-30T00:00:00.000Z';
    const fim = '2026-06-30T23:59:59.999Z';

    service.listar({
      pagina: 0,
      tamanho: 20,
      inicio,
      fim
    }).subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.params.get('inicio') === inicio
        && request.params.get('fim') === fim
    );

    expect(requisicao.request.method).toBe('GET');
    requisicao.flush(paginaVazia());
  });

  it('não deve enviar período incompleto', () => {
    service.listar({
      pagina: 0,
      tamanho: 20,
      inicio: '2026-06-30T00:00:00.000Z'
    }).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/movimentacoes`
        + '?page=0'
        + '&size=20'
        + '&sort=agendadaPara,asc'
    );

    expect(requisicao.request.params.has('inicio')).toBe(false);
    expect(requisicao.request.params.has('fim')).toBe(false);

    requisicao.flush(paginaVazia());
  });

  it('deve buscar movimentação por id', () => {
    service.buscarPorId('movimentacao-1').subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/movimentacoes/movimentacao-1`
    );

    expect(requisicao.request.method).toBe('GET');
    requisicao.flush(criarMovimentacao());
  });

  it('deve criar movimentação', () => {
    const dados: CriarMovimentacao = {
      embarcacaoId: 'embarcacao-1',
      tipo: 'LANCAMENTO',
      prioridade: 'ALTA',
      tipoPosicaoDestino: 'AGUA',
      vagaDestinoId: null,
      descricaoDestino: 'Área de lançamento',
      agendadaPara: '2026-07-01T13:00:00.000Z',
      operadorResponsavelId: null,
      observacoes: 'Preparar carreta'
    };

    service.criar(dados).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/movimentacoes`
    );

    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.body).toEqual(dados);

    requisicao.flush(criarMovimentacao());
  });

  it('deve atualizar movimentação', () => {
    const dados: AtualizarMovimentacao = {
      prioridade: 'URGENTE',
      tipoPosicaoDestino: 'AGUA',
      vagaDestinoId: null,
      descricaoDestino: 'Rampa principal',
      agendadaPara: '2026-07-01T14:00:00.000Z',
      operadorResponsavelId: 'usuario-2',
      observacoes: 'Cliente aguardando'
    };

    service.atualizar(
      'movimentacao-1',
      dados
    ).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/movimentacoes/movimentacao-1`
    );

    expect(requisicao.request.method).toBe('PUT');
    expect(requisicao.request.body).toEqual(dados);

    requisicao.flush(criarMovimentacao());
  });

  it('deve iniciar movimentação', () => {
    const dados: IniciarMovimentacao = {
      iniciadaEm: '2026-07-01T13:05:00.000Z',
      observacao: 'Operação iniciada'
    };

    service.iniciar(
      'movimentacao-1',
      dados
    ).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/movimentacoes/movimentacao-1/inicio`
    );

    expect(requisicao.request.method).toBe('PATCH');
    expect(requisicao.request.body).toEqual(dados);

    requisicao.flush({
      ...criarMovimentacao(),
      status: 'EM_EXECUCAO'
    });
  });

  it('deve concluir movimentação', () => {
    const dados: ConcluirMovimentacao = {
      concluidaEm: '2026-07-01T13:30:00.000Z',
      observacao: 'Operação concluída'
    };

    service.concluir(
      'movimentacao-1',
      dados
    ).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/movimentacoes/movimentacao-1/conclusao`
    );

    expect(requisicao.request.method).toBe('PATCH');
    expect(requisicao.request.body).toEqual(dados);

    requisicao.flush({
      ...criarMovimentacao(),
      status: 'CONCLUIDA'
    });
  });

  it('deve cancelar movimentação', () => {
    const dados: CancelarMovimentacao = {
      canceladaEm: '2026-07-01T12:30:00.000Z',
      motivo: 'Solicitação cancelada pelo cliente'
    };

    service.cancelar(
      'movimentacao-1',
      dados
    ).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/movimentacoes/movimentacao-1/cancelamento`
    );

    expect(requisicao.request.method).toBe('PATCH');
    expect(requisicao.request.body).toEqual(dados);

    requisicao.flush({
      ...criarMovimentacao(),
      status: 'CANCELADA'
    });
  });

  it('deve listar o histórico da movimentação', () => {
    service.listarHistorico(
      'movimentacao-1',
      {
        pagina: 0,
        tamanho: 10
      }
    ).subscribe();

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.url
          === `${environment.apiUrl}/movimentacoes/movimentacao-1/historico`
        && request.params.get('page') === '0'
        && request.params.get('size') === '10'
        && request.params.get('sort') === 'ocorridoEm,desc'
    );

    expect(requisicao.request.method).toBe('GET');
    requisicao.flush(paginaVazia(0, 10));
  });

  it('deve buscar a posição atual da embarcação', () => {
    service.buscarPosicaoEmbarcacao(
      'embarcacao-1'
    ).subscribe();

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/embarcacoes/embarcacao-1/posicao`
    );

    expect(requisicao.request.method).toBe('GET');

    requisicao.flush({
      id: 'posicao-1',
      embarcacaoId: 'embarcacao-1',
      embarcacaoNome: 'Aurora',
      embarcacaoModelo: 'V33',
      proprietarioNome: 'João',
      tipo: 'VAGA',
      vagaId: 'vaga-1',
      vagaCodigo: 'A-01',
      vagaSetor: 'Pier A',
      vagaLocalizacao: 'Corredor principal',
      descricaoLocal: null,
      movimentacaoOrigemId: null,
      versao: 0,
      organizacaoId: 'organizacao-1',
      criadaEm: '2026-06-30T12:00:00.000Z',
      atualizadaEm: '2026-06-30T12:00:00.000Z'
    });
  });


  it('deve reunir todas as movimentações abertas', () => {
    service.listarTodasAbertas().subscribe(
      (movimentacoes) => {
        expect(movimentacoes).toHaveLength(2);
        expect(movimentacoes[0].status).toBe('AGENDADA');
        expect(movimentacoes[1].status).toBe('EM_EXECUCAO');
      }
    );

    const agendadas = httpTesting.expectOne(
      (request) =>
        request.url === `${environment.apiUrl}/movimentacoes`
        && request.params.get('status') === 'AGENDADA'
    );

    agendadas.flush({
      ...paginaVazia(),
      content: [
        criarMovimentacao()
      ],
      totalElements: 1,
      numberOfElements: 1,
      empty: false
    });

    const emExecucao = httpTesting.expectOne(
      (request) =>
        request.url === `${environment.apiUrl}/movimentacoes`
        && request.params.get('status') === 'EM_EXECUCAO'
    );

    emExecucao.flush({
      ...paginaVazia(),
      content: [
        {
          ...criarMovimentacao(),
          id: 'movimentacao-2',
          status: 'EM_EXECUCAO'
        }
      ],
      totalElements: 1,
      numberOfElements: 1,
      empty: false
    });
  });

  it('deve listar somente usuários ativos', () => {
    service.listarUsuariosAtivos().subscribe(
      (usuarios) => {
        expect(usuarios).toHaveLength(1);
        expect(usuarios[0].nome).toBe('Ana');
      }
    );

    const requisicao = httpTesting.expectOne(
      (request) =>
        request.url === `${environment.apiUrl}/usuarios`
        && request.params.get('page') === '0'
        && request.params.get('size') === '100'
        && request.params.get('sort') === 'nome,asc'
    );

    expect(requisicao.request.method).toBe('GET');

    requisicao.flush({
      content: [
        {
          id: 'usuario-1',
          nome: 'Ana',
          email: 'ana@caisora.com',
          perfil: 'GERENTE',
          ativo: true,
          organizacaoId: 'organizacao-1',
          organizacaoNome: 'Marina Caisora',
          criadoEm: '2026-06-30T10:00:00.000Z',
          atualizadoEm: '2026-06-30T10:00:00.000Z'
        },
        {
          id: 'usuario-2',
          nome: 'Bruno',
          email: 'bruno@caisora.com',
          perfil: 'ATENDENTE',
          ativo: false,
          organizacaoId: 'organizacao-1',
          organizacaoNome: 'Marina Caisora',
          criadoEm: '2026-06-30T10:00:00.000Z',
          atualizadoEm: '2026-06-30T10:00:00.000Z'
        }
      ],
      totalElements: 2,
      totalPages: 1,
      size: 100,
      number: 0,
      numberOfElements: 2,
      first: true,
      last: true,
      empty: false
    });
  });

  function paginaVazia(
    number = 0,
    size = 20
  ) {
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size,
      number,
      numberOfElements: 0,
      first: number === 0,
      last: true,
      empty: true
    };
  }

  function criarMovimentacao(): Movimentacao {
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
      descricaoDestino: 'Área de lançamento',

      agendadaPara: '2026-07-01T13:00:00.000Z',
      iniciadaEm: null,
      concluidaEm: null,
      canceladaEm: null,

      solicitadaPorId: 'usuario-1',
      solicitadaPorNome: 'Administrador',
      operadorResponsavelId: null,
      operadorResponsavelNome: null,

      observacoes: 'Preparar carreta',
      motivoCancelamento: null,

      versao: 0,
      organizacaoId: 'organizacao-1',
      criadaEm: '2026-06-30T12:00:00.000Z',
      atualizadaEm: '2026-06-30T12:00:00.000Z'
    };
  }
});
