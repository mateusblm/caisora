import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { RelatoriosService } from './relatorios.service';

describe('RelatoriosService — permanência histórica', () => {
  let http: HttpTestingController;
  let service: RelatoriosService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(RelatoriosService);
  });
  afterEach(() => http.verify());
  it('consulta endpoint próprio com filtros cumulativos sem aceitar organização do formulário', () => {
    service
      .permanenciaVagas({ inicio: '2026-10-10', fim: '2026-10-10', tipo: 'SECA', setor: 'Norte' })
      .subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/relatorios/permanencia-vagas');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().sort()).toEqual(['fim', 'inicio', 'setor', 'tipo']);
    expect(req.request.params.get('setor')).toBe('Norte');
    req.flush({ resumo: { quantidadeOcupacoes: 0, totalHorasOcupacao: 0 }, grupos: [] });
  });
});
