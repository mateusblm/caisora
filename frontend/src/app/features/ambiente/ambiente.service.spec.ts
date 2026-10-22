import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AmbienteService } from './ambiente.service';
describe('AmbienteService', () => {
  let service: AmbienteService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AmbienteService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('consulta conjuntos externos separadamente e envia período', () => {
    service.consultar('meteorologia', '2026-10-22', '2026-10-23').subscribe();
    const weather = http.expectOne((r) => r.url === '/api/v1/ambiente/meteorologia');
    expect(weather.request.params.get('inicio')).toBe('2026-10-22');
    expect(weather.request.params.get('fim')).toBe('2026-10-23');
    expect(weather.request.params.has('organizacaoId')).toBe(false);
    weather.flush({});
    service.consultar('maritimo', '2026-10-22', '2026-10-22').subscribe();
    http.expectOne((r) => r.url === '/api/v1/ambiente/maritimo').flush({});
  });
  it('configura coordenadas da própria marina', () => {
    service.salvar({ latitude: -25, longitude: -48 }).subscribe();
    const req = http.expectOne('/api/v1/minha-organizacao/coordenadas');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ latitude: -25, longitude: -48 });
    req.flush({ latitude: -25, longitude: -48 });
  });
});
