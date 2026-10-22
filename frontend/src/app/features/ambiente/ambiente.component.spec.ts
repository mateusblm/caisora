import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AmbienteComponent } from './ambiente.component';
import { AmbienteService } from './ambiente.service';
describe('AmbienteComponent', () => {
  const service = { consultar: vi.fn() };
  beforeEach(() => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({
      imports: [AmbienteComponent],
      providers: [{ provide: AmbienteService, useValue: service }],
    });
  });
  it('identifica fonte, horário UTC e dados incompletos sem exibir zero fictício', () => {
    service.consultar.mockReturnValue(
      of({
        origem: 'Open-Meteo',
        fonte: 'https://open-meteo.com/en/docs',
        consultadoEm: '2026-10-22T10:00:00Z',
        atualizacaoProvedor: 'Atualização não informada',
        incompleta: true,
        unidades: { temperature_2m: '°C' },
        horas: [{ horario: '2026-10-22T11:00:00Z', valores: { temperature_2m: null } }],
      }),
    );
    const f = TestBed.createComponent(AmbienteComponent);
    f.componentInstance['consultar']();
    f.detectChanges();
    const text = f.nativeElement.textContent;
    expect(text).toContain('Resposta incompleta');
    expect(text).toContain('Open-Meteo');
    expect(text).toContain('UTC');
    expect(text).toContain('—');
    expect(text).toContain('não garantem segurança');
    expect(f.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
  });
  it('mostra erro do backend e permite nova consulta', () => {
    service.consultar.mockReturnValue(
      throwError(() => ({ error: { mensagem: 'Provedor indisponível' } })),
    );
    const f = TestBed.createComponent(AmbienteComponent);
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Provedor indisponível');
    expect(f.componentInstance['carregando']()).toBe(false);
    expect(f.componentInstance['resultado']()).toBeNull();
  });
  it('seleciona condições marítimas em requisição separada', () => {
    service.consultar.mockReturnValue(of({ horas: [], unidades: {}, origem: 'Open-Meteo' }));
    const f = TestBed.createComponent(AmbienteComponent);
    f.componentInstance['tipo'] = 'maritimo';
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(service.consultar.mock.calls[0][0]).toBe('maritimo');
    expect(f.nativeElement.textContent).toContain('Não há dados para o período');
  });
});
