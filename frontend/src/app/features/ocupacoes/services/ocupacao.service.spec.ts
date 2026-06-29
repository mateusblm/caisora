import {
  TestBed
} from '@angular/core/testing';

import {
  provideHttpClient
} from '@angular/common/http';

import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import {
  environment
} from '../../../../environments/environment';

import {
  AtualizarOcupacao,
  CriarOcupacao,
  Ocupacao
} from '../models/ocupacao.model';

import {
  OcupacaoService
} from './ocupacao.service';

describe('OcupacaoService', () => {

  let service: OcupacaoService;
  let httpTesting:
    HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(
      OcupacaoService
    );

    httpTesting = TestBed.inject(
      HttpTestingController
    );
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it(
    'deve listar ocupações com paginação',
    () => {
      service.listar({
        pagina: 0,
        tamanho: 10
      }).subscribe();

      const requisicao =
        httpTesting.expectOne(
          (request) =>
            request.url
              === `${environment.apiUrl}/ocupacoes`
            && request.params.get('page')
              === '0'
            && request.params.get('size')
              === '10'
            && request.params.get('sort')
              === 'inicioEm,desc'
        );

      expect(
        requisicao.request.method
      ).toBe('GET');

      requisicao.flush(
        paginaVazia()
      );
    }
  );

  it(
    'deve priorizar embarcação nos filtros',
    () => {
      service.listar({
        pagina: 0,
        tamanho: 10,
        embarcacaoId: 'embarcacao-1',
        vagaId: 'vaga-1',
        status: 'ATIVA'
      }).subscribe();

      const requisicao =
        httpTesting.expectOne(
          `${environment.apiUrl}/ocupacoes`
            + '?page=0'
            + '&size=10'
            + '&sort=inicioEm,desc'
            + '&embarcacaoId=embarcacao-1'
        );

      expect(
        requisicao.request.params.has(
          'vagaId'
        )
      ).toBe(false);

      expect(
        requisicao.request.params.has(
          'status'
        )
      ).toBe(false);

      requisicao.flush(
        paginaVazia()
      );
    }
  );

  it(
    'deve priorizar vaga quando não houver embarcação',
    () => {
      service.listar({
        pagina: 0,
        tamanho: 10,
        vagaId: 'vaga-1',
        status: 'ATIVA'
      }).subscribe();

      const requisicao =
        httpTesting.expectOne(
          (request) =>
            request.params.get('vagaId')
              === 'vaga-1'
        );

      expect(
        requisicao.request.params.has(
          'status'
        )
      ).toBe(false);

      requisicao.flush(
        paginaVazia()
      );
    }
  );

  it(
    'deve filtrar por status',
    () => {
      service.listar({
        pagina: 1,
        tamanho: 20,
        status: 'ENCERRADA'
      }).subscribe();

      const requisicao =
        httpTesting.expectOne(
          (request) =>
            request.params.get('page')
              === '1'
            && request.params.get('size')
              === '20'
            && request.params.get('status')
              === 'ENCERRADA'
        );

      requisicao.flush(
        paginaVazia(1, 20)
      );
    }
  );

  it(
    'deve buscar ocupação por id',
    () => {
      service.buscarPorId(
        'ocupacao-1'
      ).subscribe();

      const requisicao =
        httpTesting.expectOne(
          `${environment.apiUrl}`
            + '/ocupacoes/ocupacao-1'
        );

      expect(
        requisicao.request.method
      ).toBe('GET');

      requisicao.flush(
        criarOcupacao()
      );
    }
  );

  it(
    'deve criar ocupação',
    () => {
      const dados: CriarOcupacao = {
        embarcacaoId: 'embarcacao-1',
        vagaId: 'vaga-1',
        inicioEm:
          '2026-06-29T20:00:00.000Z',
        fimPrevistoEm: null,
        observacoes: 'Teste'
      };

      service.criar(dados).subscribe();

      const requisicao =
        httpTesting.expectOne(
          `${environment.apiUrl}/ocupacoes`
        );

      expect(
        requisicao.request.method
      ).toBe('POST');

      expect(
        requisicao.request.body
      ).toEqual(dados);

      requisicao.flush(
        criarOcupacao()
      );
    }
  );

  it(
    'deve atualizar ocupação',
    () => {
      const dados: AtualizarOcupacao = {
        fimPrevistoEm:
          '2026-07-10T20:00:00.000Z',
        observacoes:
          'Nova previsão'
      };

      service.atualizar(
        'ocupacao-1',
        dados
      ).subscribe();

      const requisicao =
        httpTesting.expectOne(
          `${environment.apiUrl}`
            + '/ocupacoes/ocupacao-1'
        );

      expect(
        requisicao.request.method
      ).toBe('PUT');

      expect(
        requisicao.request.body
      ).toEqual(dados);

      requisicao.flush(
        criarOcupacao()
      );
    }
  );

  it(
    'deve encerrar ocupação',
    () => {
      const encerradaEm =
        '2026-06-29T22:30:00.000Z';

      service.encerrar(
        'ocupacao-1',
        encerradaEm
      ).subscribe();

      const requisicao =
        httpTesting.expectOne(
          `${environment.apiUrl}`
            + '/ocupacoes/ocupacao-1'
            + '/encerramento'
        );

      expect(
        requisicao.request.method
      ).toBe('PATCH');

      expect(
        requisicao.request.body
      ).toEqual({
        encerradaEm
      });

      requisicao.flush(
        criarOcupacao()
      );
    }
  );

  it(
    'deve carregar todas as ocupações ativas',
    () => {
      service.listarTodasAtivas()
        .subscribe((ocupacoes) => {
          expect(ocupacoes).toHaveLength(2);
        });

      const primeira =
        httpTesting.expectOne(
          (request) =>
            request.params.get('page')
              === '0'
            && request.params.get('size')
              === '100'
            && request.params.get('status')
              === 'ATIVA'
        );

      primeira.flush({
        ...paginaVazia(0, 100),
        content: [
          criarOcupacao('ocupacao-1')
        ],
        totalElements: 2,
        totalPages: 2,
        numberOfElements: 1,
        last: false,
        empty: false
      });

      const segunda =
        httpTesting.expectOne(
          (request) =>
            request.params.get('page')
              === '1'
            && request.params.get('status')
              === 'ATIVA'
        );

      segunda.flush({
        ...paginaVazia(1, 100),
        content: [
          criarOcupacao('ocupacao-2')
        ],
        totalElements: 2,
        totalPages: 2,
        numberOfElements: 1,
        first: false,
        last: true,
        empty: false
      });
    }
  );

  function paginaVazia(
    number = 0,
    size = 10
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

  function criarOcupacao(
    id = 'ocupacao-1'
  ): Ocupacao {
    return {
      id,
      embarcacaoId: 'embarcacao-1',
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
      observacoes: 'Teste',
      organizacaoId: 'organizacao-1',
      criadaEm:
        '2026-06-29T20:00:00.000Z',
      atualizadaEm:
        '2026-06-29T20:00:00.000Z'
    };
  }
});
