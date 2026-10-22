import { TestBed } from '@angular/core/testing';
import { of, throwError, Subject } from 'rxjs';
import { vi } from 'vitest';
import { RelatoriosComponent } from './relatorios.component';
import { RelatoriosService } from './relatorios.service';
import { AutenticacaoService } from '../../core/autenticacao/autenticacao.service';
describe('RelatoriosComponent', () => {
  const service = {
    ocupacao: vi.fn(),
    movimentacoes: vi.fn(),
    contratos: vi.fn(),
    permanenciaVagas: vi.fn(),
  };
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
  const resultadoHistorico = () => ({
    resumo: {
      quantidadeOcupacoes: 2,
      permanenciaMediaSegundos: 7200,
      menorPermanenciaSegundos: 3600,
      maiorPermanenciaSegundos: 10800,
      totalHorasOcupacao: 4,
    },
    grupos: [
      {
        setor: 'Norte',
        tipo: 'SECA',
        indicadores: {
          quantidadeOcupacoes: 2,
          permanenciaMediaSegundos: 7200,
          menorPermanenciaSegundos: 3600,
          maiorPermanenciaSegundos: 10800,
          totalHorasOcupacao: 4,
        },
      },
    ],
  });
  it('envia todos os filtros históricos e mostra os indicadores do backend', () => {
    service.permanenciaVagas.mockReturnValue(of(resultadoHistorico()));
    const f = TestBed.createComponent(RelatoriosComponent);
    Object.assign(f.componentInstance, {
      tipo: 'permanencia-vagas',
      inicio: '2026-10-10',
      fim: '2026-10-10',
      setor: ' Norte ',
      tipoVaga: 'SECA',
    });
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(service.permanenciaVagas).toHaveBeenCalledWith({
      inicio: '2026-10-10',
      fim: '2026-10-10',
      setor: 'Norte',
      tipo: 'SECA',
    });
    expect(service.ocupacao).not.toHaveBeenCalled();
    const texto = f.nativeElement.querySelector('[data-testid=permanencia-resultado]').textContent;
    expect(texto).toContain('7,200.00');
    expect(texto).toContain('3,600.00');
    expect(texto).toContain('10,800.00');
    expect(texto).toContain('4.0000');
    expect(texto).toContain('Norte');
    expect(texto).toContain('Somente ocupações encerradas');
  });
  it('mostra carregamento e bloqueia envio até receber os dados históricos', () => {
    const resposta = new Subject<ReturnType<typeof resultadoHistorico>>();
    service.permanenciaVagas.mockReturnValue(resposta);
    const f = TestBed.createComponent(RelatoriosComponent);
    Object.assign(f.componentInstance, {
      tipo: 'permanencia-vagas',
      inicio: '2026-10-10',
      fim: '2026-10-10',
    });
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.querySelector('form button').disabled).toBe(true);
    expect(f.nativeElement.textContent).toContain('Calculando…');
    resposta.next(resultadoHistorico());
    f.detectChanges();
    expect(f.componentInstance['carregando']()).toBe(false);
  });
  it('exibe estado vazio histórico sem transformar média e extremos em zero', () => {
    service.permanenciaVagas.mockReturnValue(
      of({
        resumo: {
          quantidadeOcupacoes: 0,
          permanenciaMediaSegundos: null,
          menorPermanenciaSegundos: null,
          maiorPermanenciaSegundos: null,
          totalHorasOcupacao: 0,
        },
        grupos: [],
      }),
    );
    const f = TestBed.createComponent(RelatoriosComponent);
    f.componentInstance['tipo'] = 'permanencia-vagas';
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Nenhuma ocupação encerrada');
    expect(f.nativeElement.textContent).toContain('Sem grupos para o período');
    expect(f.nativeElement.querySelectorAll('dd').length).toBe(5);
    expect(f.nativeElement.textContent.match(/Sem dados/g)).toHaveLength(3);
  });
  it('trata falha histórica removendo dados da consulta anterior', () => {
    const f = TestBed.createComponent(RelatoriosComponent);
    f.componentInstance['tipo'] = 'permanencia-vagas';
    f.componentInstance['permanencia'].set(resultadoHistorico());
    service.permanenciaVagas.mockReturnValue(
      throwError(() => ({ error: { mensagem: 'Não foi possível consultar o histórico' } })),
    );
    f.componentInstance['consultar']();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Não foi possível consultar o histórico');
    expect(f.nativeElement.querySelector('[data-testid=permanencia-resultado]')).toBeNull();
    expect(f.componentInstance['carregando']()).toBe(false);
  });
  it('formulário histórico exige ambas as datas e impede cronologia invertida', async () => {
    const f = TestBed.createComponent(RelatoriosComponent);
    f.componentInstance['tipo'] = 'permanencia-vagas';
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('form button').disabled).toBe(true);
    const preencher = (nome: string, valor: string) => {
      const input = f.nativeElement.querySelector(`input[name=${nome}]`) as HTMLInputElement;
      input.value = valor;
      input.dispatchEvent(new Event('input', { bubbles: true }));
    };
    preencher('inicio', '2026-10-11');
    preencher('fim', '2026-10-10');
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('form button').disabled).toBe(true);
    preencher('fim', '2026-10-11');
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(f.nativeElement.querySelector('form button').disabled).toBe(false);
  });
  it('oferece a opção histórica somente com permissão de vagas e ocupações', async () => {
    auth.temPermissao.mockImplementation((m: string) => m === 'VAGAS' || m === 'CONTRATOS');
    const f = TestBed.createComponent(RelatoriosComponent);
    f.detectChanges();
    f.nativeElement.querySelector('mat-select[name=relatorio]').click();
    f.detectChanges();
    await f.whenStable();
    expect(document.body.textContent).not.toContain('Permanência histórica nas vagas');
  });
});
