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
  PainelTvOperacional
} from '../models/painel-tv.model';
import { PainelTvService } from './painel-tv.service';

describe('PainelTvService', () => {

  let service: PainelTvService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(PainelTvService);
    httpTesting = TestBed.inject(
      HttpTestingController
    );
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('deve buscar o painel de TV', () => {
    const painel = criarPainel();

    service.buscar().subscribe(
      (resposta) => {
        expect(resposta).toEqual(painel);
      }
    );

    const requisicao = httpTesting.expectOne(
      `${environment.apiUrl}/movimentacoes/painel-tv`
    );

    expect(requisicao.request.method).toBe('GET');

    requisicao.flush(painel);
  });

  function criarPainel(): PainelTvOperacional {
    return {
      geradoEm: '2026-07-01T12:00:00Z',
      fusoHorario: 'America/Sao_Paulo',
      inicioDia: '2026-07-01T03:00:00Z',
      fimDia:
        '2026-07-02T02:59:59.999999999Z',
      atualizarAposSegundos: 15,
      resumo: {
        descidasParaAgua: 1,
        retiradasDaAgua: 2,
        transferenciasDeVaga: 0,
        deslocamentosInternos: 0,
        emExecucao: 1,
        alertas: 1
      },
      alertas: [],
      descidasParaAgua: [],
      retiradasDaAgua: [],
      transferenciasDeVaga: [],
      deslocamentosInternos: [],
      emExecucao: []
    };
  }
});
