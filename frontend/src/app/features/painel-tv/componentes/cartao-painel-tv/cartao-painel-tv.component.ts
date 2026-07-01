import { NgClass } from '@angular/common';
import {
  Component,
  Input
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import {
  AcaoOperacionalPainelTv,
  MovimentacaoPainelTv,
  TipoAlertaPainelTv
} from '../../models/painel-tv.model';

@Component({
  selector: 'app-cartao-painel-tv',
  imports: [
    NgClass,
    MatIconModule
  ],
  templateUrl: './cartao-painel-tv.component.html',
  styleUrl: './cartao-painel-tv.component.scss'
})
export class CartaoPainelTvComponent {

  @Input({
    required: true
  })
  movimentacao!: MovimentacaoPainelTv;

  @Input()
  compacto = false;

  protected classesCartao(): string[] {
    return [
      `cartao-tv--${this.movimentacao.situacao
        .toLowerCase()}`,
      `cartao-tv--${this.movimentacao.prioridade
        .toLowerCase()}`,
      this.compacto
        ? 'cartao-tv--compacto'
        : ''
    ].filter(Boolean);
  }

  protected iconeAcao(): string {
    const icones: Record<
      AcaoOperacionalPainelTv,
      string
    > = {
      DESCER_PARA_AGUA: 'south',
      RETIRAR_DA_AGUA: 'north',
      RETORNAR_PARA_VAGA: 'keyboard_return',
      TRANSFERIR_DE_VAGA: 'swap_horiz',
      DESLOCAR_INTERNAMENTE: 'move_up'
    };

    return icones[
      this.movimentacao.acaoOperacional
    ];
  }

  protected rotuloAcao(): string {
    const rotulos: Record<
      AcaoOperacionalPainelTv,
      string
    > = {
      DESCER_PARA_AGUA: 'Descer para a água',
      RETIRAR_DA_AGUA: 'Retirar da água',
      RETORNAR_PARA_VAGA: 'Retornar para a vaga',
      TRANSFERIR_DE_VAGA: 'Transferir de vaga',
      DESLOCAR_INTERNAMENTE:
        'Deslocar internamente'
    };

    return rotulos[
      this.movimentacao.acaoOperacional
    ];
  }

  protected rotuloTempo(): string {
    switch (this.movimentacao.situacao) {
      case 'ATRASADA':
        return this.movimentacao.minutosAtraso === 1
          ? '1 min atrasada'
          : `${this.movimentacao.minutosAtraso} min atrasada`;

      case 'EM_EXECUCAO':
        return this.movimentacao.minutosEmExecucao === 1
          ? '1 min em operação'
          : `${this.movimentacao.minutosEmExecucao} min em operação`;

      case 'PROXIMA':
        if (
          this.movimentacao.minutosParaInicio <= 0
        ) {
          return 'Agora';
        }

        return `Em ${
          this.movimentacao.minutosParaInicio
        } min`;

      case 'AGENDADA':
        return this.formatarHora(
          this.movimentacao.agendadaPara
        );
    }
  }

  protected rotuloSituacao(): string {
    switch (this.movimentacao.situacao) {
      case 'ATRASADA':
        return 'Atrasada';
      case 'EM_EXECUCAO':
        return 'Em execução';
      case 'PROXIMA':
        return 'Próxima';
      case 'AGENDADA':
        return 'Agendada';
    }
  }

  protected formatarHora(
    valor: string
  ): string {
    const data = new Date(valor);

    if (Number.isNaN(data.getTime())) {
      return '--:--';
    }

    return new Intl.DateTimeFormat(
      'pt-BR',
      {
        hour: '2-digit',
        minute: '2-digit',
        timeZone: 'America/Sao_Paulo'
      }
    ).format(data);
  }

  protected possuiAlerta(
    alerta: TipoAlertaPainelTv
  ): boolean {
    return this.movimentacao.alertas
      .includes(alerta);
  }
}
