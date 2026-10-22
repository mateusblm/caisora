import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReferenciasService } from './referencias.service';
describe('ReferenciasService', () => {
  let http: HttpTestingController;
  let service: ReferenciasService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ReferenciasService);
  });
  afterEach(() => http.verify());
  it('consulta os quatro conjuntos sem informar tenant ou chave pelo navegador', () => {
    for (const grupo of ['paises', 'municipios', 'feriados', 'mares'] as const) {
      service.consultar(grupo, { busca: 'Brasil' }).subscribe();
      const req = http.expectOne((r) => r.url === `/api/v1/referencias/${grupo}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.has('organizacaoId')).toBe(false);
      expect(req.request.params.has('key')).toBe(false);
      req.flush({ registros: [] });
    }
  });
  it('disponibilidade não consulta previsões nem consome créditos', () => {
    service.disponibilidadeMares().subscribe();
    http
      .expectOne('/api/v1/referencias/mares/disponibilidade')
      .flush({ habilitada: false, mensagem: 'Desabilitada' });
  });
});
