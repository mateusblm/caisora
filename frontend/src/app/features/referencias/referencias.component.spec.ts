import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ReferenciasComponent } from './referencias.component';
import { ReferenciasService, ConsultaReferencia, GrupoReferencia } from './referencias.service';
describe('ReferenciasComponent', () => {
  const service = { consultar: vi.fn(), disponibilidadeMares: vi.fn() };
  const resposta = (registros: ConsultaReferencia['registros']): ConsultaReferencia => ({
    origem: 'Provedor controlado',
    fonte: 'https://example.com/docs',
    consultadoEm: '2026-10-22T12:00:00Z',
    incompleta: false,
    registros,
  });
  beforeEach(() => {
    vi.resetAllMocks();
    service.disponibilidadeMares.mockReturnValue(
      of({ habilitada: false, mensagem: 'WorldTides desabilitada sem autorização' }),
    );
    TestBed.configureTestingModule({
      imports: [ReferenciasComponent],
      providers: [provideRouter([]), { provide: ReferenciasService, useValue: service }],
    });
  });
  function criar(grupo: GrupoReferencia) {
    TestBed.inject(ActivatedRoute).snapshot.data = { grupo };
    return TestBed.createComponent(ReferenciasComponent);
  }
  it('consulta países por nome e exibe fonte e códigos', () => {
    service.consultar.mockReturnValue(
      of(resposta([{ nome: 'Brasil', codigoM49: 76, codigoIso2: 'BR', codigoIso3: 'BRA' }])),
    );
    const f = criar('paises');
    f.componentInstance['busca'] = ' Brasil ';
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(service.consultar).toHaveBeenCalledWith('paises', { busca: 'Brasil' });
    expect(f.nativeElement.textContent).toContain('BRA');
    expect(f.nativeElement.textContent).toContain('Provedor controlado');
    expect(f.nativeElement.textContent).toContain('UTC');
  });
  it('normaliza UF e pesquisa municípios sem alterar cadastros', () => {
    service.consultar.mockReturnValue(
      of(resposta([{ nome: 'Florianópolis', siglaUf: 'SC', codigoIbge: 4205407 }])),
    );
    const f = criar('municipios');
    Object.assign(f.componentInstance, { uf: ' sc ', busca: 'Florian' });
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(service.consultar).toHaveBeenCalledWith('municipios', { uf: 'SC', busca: 'Florian' });
    expect(f.nativeElement.textContent).toContain('4205407');
  });
  it('distingue feriados nacionais e estaduais preservando a classificação', () => {
    service.consultar.mockReturnValue(
      of(
        resposta([
          { data: '2026-01-01', nome: 'Nome nacional', tipo: 'national' },
          { data: '2026-08-11', nome: 'Nome estadual', tipo: 'state', uf: 'SC' },
        ]),
      ),
    );
    const f = criar('feriados');
    f.componentInstance['ano'] = 2026;
    f.componentInstance['uf'] = 'SC';
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(service.consultar).toHaveBeenCalledWith('feriados', { ano: '2026', uf: 'SC' });
    expect(f.nativeElement.textContent).toContain('Nacional (national)');
    expect(f.nativeElement.textContent).toContain('Estadual (state)');
    expect(f.nativeElement.textContent).toContain('classificação');
  });
  it('trata loading e impede consulta repetida no formulário', () => {
    const subject = new Subject<ConsultaReferencia>();
    service.consultar.mockReturnValue(subject);
    const f = criar('paises');
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.querySelector('form button').disabled).toBe(true);
    expect(f.nativeElement.textContent).toContain('Consultando…');
    subject.next(resposta([]));
    f.detectChanges();
    expect(f.componentInstance['carregando']()).toBe(false);
  });
  it('apresenta vazio e resposta incompleta sem preencher campos ausentes', () => {
    service.consultar.mockReturnValue(of({ ...resposta([{ nome: 'Brasil' }]), incompleta: true }));
    const f = criar('paises');
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Resposta incompleta');
    expect(f.nativeElement.textContent).toContain('—');
    service.consultar.mockReturnValue(of(resposta([])));
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Nenhum registro encontrado');
  });
  it('falha limpa o resultado anterior e permite tentar novamente', () => {
    service.consultar.mockReturnValue(
      throwError(() => ({ error: { mensagem: 'Provedor indisponível' } })),
    );
    const f = criar('municipios');
    f.componentInstance['resultado'].set(resposta([{ nome: 'Anterior' }]));
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Provedor indisponível');
    expect(f.componentInstance['resultado']()).toBeNull();
    expect(f.componentInstance['carregando']()).toBe(false);
  });
  it('WorldTides desabilitada bloqueia formulário e não faz chamada de previsão', () => {
    const f = criar('mares');
    f.detectChanges();
    f.componentInstance['consultar']();
    expect(service.consultar).not.toHaveBeenCalled();
    expect(f.nativeElement.querySelector('form button').disabled).toBe(true);
    expect(f.nativeElement.textContent).toContain('sem autorização');
    expect(f.nativeElement.textContent).toContain('não substituem referências oficiais');
  });
  it('marés controladas mostram alta baixa altura unidade datum e copyright', () => {
    service.disponibilidadeMares.mockReturnValue(
      of({ habilitada: true, mensagem: 'Créditos autorizados' }),
    );
    service.consultar.mockReturnValue(
      of({
        ...resposta([
          { dataHora: '2026-10-22T00:00:00Z', tipo: 'High', altura: 1.2, unidade: 'm' },
          { dataHora: '2026-10-22T06:00:00Z', tipo: 'Low', altura: -0.3, unidade: 'm' },
        ]),
        datumResposta: 'MSL',
        copyright: 'Copyright de teste',
      }),
    );
    const f = criar('mares');
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Alta');
    expect(f.nativeElement.textContent).toContain('Baixa');
    expect(f.nativeElement.textContent).toContain('MSL');
    expect(f.nativeElement.textContent).toContain('Copyright de teste');
  });
  it('falha na disponibilidade permite nova verificação sem consultar marés', () => {
    service.disponibilidadeMares.mockReturnValue(throwError(() => new Error('falha')));
    const f = criar('mares');
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Verificar novamente');
    expect(service.consultar).not.toHaveBeenCalled();
  });
});
