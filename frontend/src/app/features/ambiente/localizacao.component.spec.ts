import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { LocalizacaoComponent } from './localizacao.component';
import { AmbienteService } from './ambiente.service';
describe('LocalizacaoComponent', () => {
  const service = { coordenadas: vi.fn(), salvar: vi.fn() };
  beforeEach(() => {
    vi.resetAllMocks();
    service.coordenadas.mockReturnValue(of({ latitude: null, longitude: null }));
    TestBed.configureTestingModule({
      imports: [LocalizacaoComponent],
      providers: [{ provide: AmbienteService, useValue: service }],
    });
  });
  it('carrega coordenadas ausentes sem criar uma localização fictícia', () => {
    const f = TestBed.createComponent(LocalizacaoComponent);
    f.detectChanges();
    expect(f.componentInstance['latitude']).toBeNull();
    expect(f.componentInstance['longitude']).toBeNull();
    expect(f.nativeElement.querySelector('input[name=latitude]').getAttribute('min')).toBe('-90');
  });
  it('salva coordenadas e informa erro de validação', () => {
    service.salvar.mockReturnValue(
      throwError(() => ({ error: { mensagem: 'Coordenadas inválidas' } })),
    );
    const f = TestBed.createComponent(LocalizacaoComponent);
    f.componentInstance['latitude'] = 91;
    f.componentInstance['longitude'] = 0;
    f.componentInstance['salvar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Coordenadas inválidas');
    expect(f.componentInstance['carregando']()).toBe(false);
  });
});
