import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import {
  AutenticacaoService
} from './autenticacao.service';

describe('AutenticacaoService', () => {

  let service: AutenticacaoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AutenticacaoService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(
      AutenticacaoService
    );

    httpMock = TestBed.inject(
      HttpTestingController
    );
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('deve criar o servico', () => {
    expect(service).toBeTruthy();
  });

  it('deve iniciar sem usuario autenticado', () => {
    expect(service.estaAutenticado())
      .toBe(false);

    expect(service.usuarioAtual())
      .toBeNull();
  });

  it('deve enviar codigo da marina no corpo do login sem header de organizacao', () => {
    service.autenticar({
      codigoOrganizacao: 'marina-teste',
      email: 'maria@marina.com',
      senha: 'SenhaForte123'
    }).subscribe();

    const requisicao =
      httpMock.expectOne(
        '/api/v1/autenticacao/login'
      );

    expect(requisicao.request.method)
      .toBe('POST');

    expect(
      requisicao.request.headers.has(
        'X-Organizacao-Id'
      )
    ).toBe(false);

    expect(requisicao.request.body)
      .toEqual({
        codigoOrganizacao: 'marina-teste',
        email: 'maria@marina.com',
        senha: 'SenhaForte123'
      });

    requisicao.flush({
      tokenAcesso: 'token.jwt',
      tipoToken: 'Bearer',
      expiraEm: 3600,
      usuario: {
        id: 'usuario-1',
        nome: 'Maria Silva',
        email: 'maria@marina.com',
        perfil: 'ADMINISTRADOR_MARINA',
        organizacaoId: 'organizacao-1',
        organizacaoNome: 'Marina Teste'
      }
    });
  });
});
