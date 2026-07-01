import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';

import {
  Movimentacao
} from '../../../movimentacoes/models/movimentacao.model';
import {
  CartaoMovimentacaoOperacionalComponent
} from './cartao-movimentacao-operacional.component';

describe(
  'CartaoMovimentacaoOperacionalComponent',
  () => {
    let fixture: ComponentFixture<
      CartaoMovimentacaoOperacionalComponent
    >;

    beforeEach(async () => {
      await TestBed
        .configureTestingModule({
          imports: [
            CartaoMovimentacaoOperacionalComponent
          ],
          providers: [
            provideNoopAnimations(),
            provideRouter([])
          ]
        })
        .compileComponents();

      fixture = TestBed.createComponent(
        CartaoMovimentacaoOperacionalComponent
      );

      fixture.componentInstance.movimentacao =
        criarMovimentacao();

      fixture.componentInstance.agora =
        new Date(
          '2026-06-30T15:00:00Z'
        );

      fixture.detectChanges();
    });

    it(
      'deve exibir lançamento como descida para a água',
      () => {
        expect(
          fixture.nativeElement.textContent
        ).toContain(
          'Descer para a água'
        );
      }
    );

    it(
      'deve emitir evento ao iniciar',
      () => {
        const observador = vi.fn();

        fixture.componentInstance
          .iniciar
          .subscribe(observador);

        const botao =
          fixture.nativeElement.querySelector(
            '.acao-iniciar'
          ) as HTMLButtonElement;

        botao.click();

        expect(observador)
          .toHaveBeenCalledWith(
            fixture.componentInstance
              .movimentacao
          );
      }
    );

    function criarMovimentacao():
      Movimentacao {
      return {
        id: 'movimentacao-1',
        embarcacaoId: 'embarcacao-1',
        embarcacaoNome: 'Aurora',
        embarcacaoModelo: 'V33',
        proprietarioNome: 'João',
        tipo: 'LANCAMENTO',
        status: 'AGENDADA',
        prioridade: 'URGENTE',
        tipoPosicaoOrigem: 'VAGA',
        vagaOrigemId: 'vaga-1',
        vagaOrigemCodigo: 'A-01',
        descricaoOrigem: null,
        tipoPosicaoDestino: 'AGUA',
        vagaDestinoId: null,
        vagaDestinoCodigo: null,
        descricaoDestino: 'Rampa principal',
        agendadaPara:
          '2026-06-30T15:10:00Z',
        iniciadaEm: null,
        concluidaEm: null,
        canceladaEm: null,
        solicitadaPorId: 'usuario-1',
        solicitadaPorNome: 'Administrador',
        operadorResponsavelId: null,
        operadorResponsavelNome: null,
        observacoes: null,
        motivoCancelamento: null,
        versao: 0,
        organizacaoId: 'organizacao-1',
        criadaEm:
          '2026-06-30T14:00:00Z',
        atualizadaEm:
          '2026-06-30T14:00:00Z'
      };
    }
  }
);
