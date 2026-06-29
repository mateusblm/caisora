import { TestBed } from '@angular/core/testing';

import {
  provideHttpClient
} from '@angular/common/http';

import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import {
  VagaService
} from './vaga.service';

describe('VagaService', () => {

  let service: VagaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        VagaService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(
      VagaService
    );

    httpMock = TestBed.inject(
      HttpTestingController
    );
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve listar usando somente o primeiro filtro aceito pelo backend', () => {
    service.listar({
      pagina: 1,
      tamanho: 10,
      codigo: ' A-01 ',
      setor: 'Norte',
      tipo: 'SECA',
      ativa: true
    }).subscribe();

    const requisicao =
      httpMock.expectOne((request) =>
        request.url === '/api/v1/vagas'
      );

    expect(requisicao.request.method)
      .toBe('GET');

    expect(
      requisicao.request.params.get('page')
    ).toBe('1');

    expect(
      requisicao.request.params.get('size')
    ).toBe('10');

    expect(
      requisicao.request.params.get('sort')
    ).toBe('codigo,asc');

    expect(
      requisicao.request.params.get('codigo')
    ).toBe('A-01');

    expect(
      requisicao.request.params.has('setor')
    ).toBe(false);

    expect(
      requisicao.request.params.has('tipo')
    ).toBe(false);

    expect(
      requisicao.request.params.has('ativa')
    ).toBe(false);

    requisicao.flush({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 1,
      numberOfElements: 0,
      first: false,
      last: true,
      empty: true
    });
  });

  it('deve enviar status quando filtros anteriores estiverem vazios', () => {
    service.listar({
      pagina: 0,
      tamanho: 10,
      ativa: false
    }).subscribe();

    const requisicao =
      httpMock.expectOne((request) =>
        request.url === '/api/v1/vagas'
      );

    expect(
      requisicao.request.params.get('ativa')
    ).toBe('false');

    requisicao.flush({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
      numberOfElements: 0,
      first: true,
      last: true,
      empty: true
    });
  });

  it('deve alterar status no endpoint da vaga', () => {
    service
      .alterarStatus('vaga-1', false)
      .subscribe();

    const requisicao =
      httpMock.expectOne(
        '/api/v1/vagas/vaga-1/status'
      );

    expect(requisicao.request.method)
      .toBe('PATCH');

    expect(requisicao.request.body)
      .toEqual({
        ativa: false
      });

    requisicao.flush({});
  });
});
