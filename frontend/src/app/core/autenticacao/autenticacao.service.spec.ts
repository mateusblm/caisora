import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting
} from '@angular/common/http/testing';

import {
  AutenticacaoService
} from './autenticacao.service';

describe('AutenticacaoService', () => {

  let service: AutenticacaoService;

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
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('deve criar o serviço', () => {
    expect(service).toBeTruthy();
  });

  it('deve iniciar sem usuário autenticado', () => {
    expect(service.estaAutenticado())
      .toBe(false);

    expect(service.usuarioAtual())
      .toBeNull();
  });
});