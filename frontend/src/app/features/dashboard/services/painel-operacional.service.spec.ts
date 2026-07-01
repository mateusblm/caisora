import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
  environment
} from '../../../../environments/environment';
import {
  PainelOperacional
} from '../models/painel-operacional.model';
import {
  PainelOperacionalService
} from './painel-operacional.service';

describe('PainelOperacionalService', () => {

  let service: PainelOperacionalService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(
      PainelOperacionalService
    );

    httpTesting = TestBed.inject(
      HttpTestingController
    );
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('deve buscar o painel operacional', () => {
    const painel = criarPainel();

    service.buscar().subscribe(
      (resposta) => {
        expect(resposta).toEqual(painel);
      }
    );

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}`
      + '/movimentacoes/painel-operacional'
    );

    expect(requisicao.request.method).toBe('GET');

    requisicao.flush(painel);
  });

  function criarPainel(): PainelOperacional {
    return {
      geradoEm: '2026-06-30T23:30:00Z',
      fusoHorario: 'America/Sao_Paulo',
      inicioDia: '2026-06-30T03:00:00Z',
      fimDia:
        '2026-07-01T02:59:59.999999999Z',
      indicadores: {
        emExecucao: 1,
        atrasadas: 2,
        proximaHora: 3,
        urgentes: 1,
        semOperador: 1,
        concluidasHoje: 4
      },
      atrasadas: [],
      emExecucao: [],
      proximosTrintaMinutos: [],
      proximasDuasHoras: [],
      restanteDia: [],
      concluidasRecentemente: []
    };
  }
});
