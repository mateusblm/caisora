import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { RelatoriosComponent } from './relatorios.component';
import { RelatoriosService } from './relatorios.service';
import { AutenticacaoService } from '../../core/autenticacao/autenticacao.service';
describe('RelatoriosComponent', () => {
  const service = { ocupacao: vi.fn(), movimentacoes: vi.fn(), contratos: vi.fn() };
  const auth = { temPermissao: vi.fn() };
  beforeEach(() => {
    vi.resetAllMocks();
    auth.temPermissao.mockReturnValue(true);
    TestBed.configureTestingModule({
      imports: [RelatoriosComponent],
      providers: [
        { provide: RelatoriosService, useValue: service },
        { provide: AutenticacaoService, useValue: auth },
      ],
    });
  });
  it('envia filtros de ocupação e exibe intervalo sem vagas', () => {
    service.ocupacao.mockReturnValue(
      of({ totalVagasAtivas: 0, ocupadas: 0, disponiveis: 0, taxaOcupacao: 0, grupos: [] }),
    );
    const f = TestBed.createComponent(RelatoriosComponent);
    f.componentInstance['setor'] = ' Norte ';
    f.componentInstance['tipoVaga'] = 'SECA';
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(service.ocupacao).toHaveBeenCalledWith({ setor: 'Norte', tipo: 'SECA' });
    expect(f.nativeElement.textContent).toContain('Nenhuma vaga ativa');
  });
  it('mostra duração sem amostras e regra de pontualidade', () => {
    service.movimentacoes.mockReturnValue(
      of({
        total: 0,
        porSituacao: { CONCLUIDA: 0 },
        concluidas: 0,
        canceladas: 0,
        tempoMedioExecucaoSegundos: null,
        amostrasDuracao: 0,
        concluidasNoPrazo: 0,
        percentualNoPrazo: 0,
        regraPontualidade: 'Conclusão até o horário agendado',
      }),
    );
    const f = TestBed.createComponent(RelatoriosComponent);
    f.componentInstance['tipo'] = 'movimentacoes';
    f.componentInstance['inicio'] = '2026-10-01';
    f.componentInstance['fim'] = '2026-10-02';
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(service.movimentacoes).toHaveBeenCalledWith({ inicio: '2026-10-01', fim: '2026-10-02' });
    expect(f.nativeElement.textContent).toContain('Sem dados suficientes');
    expect(f.nativeElement.textContent).toContain('Conclusão até o horário agendado');
  });
  it('apresenta valores contratuais por periodicidade', () => {
    service.contratos.mockReturnValue(
      of({
        total: 2,
        porSituacao: { ATIVO: 2 },
        percentualPorSituacao: { ATIVO: 100 },
        porPeriodicidade: { MENSAL: 1, ANUAL: 1 },
        valoresBasePorPeriodicidade: { MENSAL: 100, ANUAL: 1200 },
      }),
    );
    const f = TestBed.createComponent(RelatoriosComponent);
    f.componentInstance['tipo'] = 'contratos';
    f.componentInstance['consultar']();
    f.detectChanges();
    const text = f.nativeElement.textContent;
    expect(text).toContain('mensal');
    expect(text).toContain('anual');
    expect(text).toContain('não representam pagamentos recebidos');
    expect(text).toContain('sem total financeiro geral');
  });
  it('escolhe somente relatório permitido pelo módulo de negócio', () => {
    auth.temPermissao.mockImplementation((m: string) => m === 'CONTRATOS');
    const f = TestBed.createComponent(RelatoriosComponent);
    expect(f.componentInstance['tipo']).toBe('contratos');
  });
  it('trata acesso negado sem manter resultado anterior', () => {
    service.ocupacao.mockReturnValue(throwError(() => ({ error: { mensagem: 'Acesso negado' } })));
    const f = TestBed.createComponent(RelatoriosComponent);
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Acesso negado');
    expect(f.componentInstance['carregando']()).toBe(false);
  });
});
