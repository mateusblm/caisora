import {
  NgClass
} from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MatProgressSpinnerModule
} from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';

import {
  Movimentacao,
  PrioridadeMovimentacao,
  TipoMovimentacao,
  TipoPosicaoEmbarcacao
} from '../../../movimentacoes/models/movimentacao.model';

export type ContextoCartaoOperacional =
  | 'ATRASADA'
  | 'EM_EXECUCAO'
  | 'PROXIMA'
  | 'CONCLUIDA';

@Component({
  selector: 'app-cartao-movimentacao-operacional',
  imports: [
    NgClass,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl:
    './cartao-movimentacao-operacional.component.html',
  styleUrl:
    './cartao-movimentacao-operacional.component.scss'
})
export class CartaoMovimentacaoOperacionalComponent {

  @Input({
    required: true
  })
  movimentacao!: Movimentacao;

  @Input()
  contexto: ContextoCartaoOperacional =
    'PROXIMA';

  @Input()
  agora = new Date();

  @Input()
  processando = false;

  @Input()
  bloqueado = false;

  @Output()
  readonly iniciar =
    new EventEmitter<Movimentacao>();

  @Output()
  readonly concluir =
    new EventEmitter<Movimentacao>();

  private readonly formatadorHora =
    new Intl.DateTimeFormat(
      'pt-BR',
      {
        hour: '2-digit',
        minute: '2-digit',
        timeZone: 'America/Sao_Paulo'
      }
    );

  protected classesCartao(): string[] {
    return [
      `cartao-operacao--${
        this.contexto.toLowerCase()
      }`,
      `cartao-operacao--prioridade-${
        this.movimentacao.prioridade
          .toLowerCase()
      }`
    ];
  }

  protected rotuloOperacao(): string {
    const rotulos: Record<
      TipoMovimentacao,
      string
    > = {
      LANCAMENTO: 'Descer para a água',
      RETIRADA: 'Subir para a vaga',
      RETORNO_PARA_VAGA: 'Retornar para a vaga',
      TRANSFERENCIA: 'Transferir de vaga',
      DESLOCAMENTO_INTERNO:
        'Deslocamento interno'
    };

    return rotulos[this.movimentacao.tipo];
  }

  protected iconeOperacao(): string {
    const icones: Record<
      TipoMovimentacao,
      string
    > = {
      LANCAMENTO: 'south',
      RETIRADA: 'north',
      RETORNO_PARA_VAGA: 'keyboard_return',
      TRANSFERENCIA: 'swap_horiz',
      DESLOCAMENTO_INTERNO: 'move_up'
    };

    return icones[this.movimentacao.tipo];
  }

  protected rotuloContexto(): string {
    switch (this.contexto) {
      case 'ATRASADA':
        return 'Atrasada';

      case 'EM_EXECUCAO':
        return 'Em execução';

      case 'CONCLUIDA':
        return 'Concluída';

      default:
        return 'Agendada';
    }
  }

  protected rotuloTempo(): string {
    if (this.contexto === 'EM_EXECUCAO') {
      return this.movimentacao.iniciadaEm
        ? `Há ${this.formatarDuracao(
          this.agora.getTime()
          - new Date(
            this.movimentacao.iniciadaEm
          ).getTime()
        )}`
        : 'Horário de início não informado';
    }

    if (this.contexto === 'CONCLUIDA') {
      return this.movimentacao.concluidaEm
        ? `Às ${this.formatarHora(
          this.movimentacao.concluidaEm
        )}`
        : 'Concluída';
    }

    const diferenca =
      new Date(
        this.movimentacao.agendadaPara
      ).getTime()
      - this.agora.getTime();

    if (diferenca < 0) {
      return `Há ${this.formatarDuracao(
        Math.abs(diferenca)
      )}`;
    }

    if (diferenca < 60_000) {
      return 'Agora';
    }

    return `Em ${this.formatarDuracao(
      diferenca
    )}`;
  }

  protected rotuloPrioridade(): string {
    const rotulos: Record<
      PrioridadeMovimentacao,
      string
    > = {
      NORMAL: 'Normal',
      ALTA: 'Alta',
      URGENTE: 'Urgente'
    };

    return rotulos[
      this.movimentacao.prioridade
    ];
  }

  protected descricaoEmbarcacao(): string {
    return [
      this.movimentacao.embarcacaoModelo,
      this.movimentacao.proprietarioNome
    ]
      .filter(Boolean)
      .join(' · ');
  }

  protected descricaoOrigem(): string {
    return this.descricaoPosicao(
      this.movimentacao.tipoPosicaoOrigem,
      this.movimentacao.vagaOrigemCodigo,
      this.movimentacao.descricaoOrigem
    );
  }

  protected descricaoDestino(): string {
    return this.descricaoPosicao(
      this.movimentacao.tipoPosicaoDestino,
      this.movimentacao.vagaDestinoCodigo,
      this.movimentacao.descricaoDestino
    );
  }

  protected formatarHora(
    valor: string
  ): string {
    const data = new Date(valor);

    if (Number.isNaN(data.getTime())) {
      return '--:--';
    }

    return this.formatadorHora.format(data);
  }

  protected solicitarInicio(): void {
    if (
      this.bloqueado
      || this.processando
      || this.movimentacao.status
        !== 'AGENDADA'
    ) {
      return;
    }

    this.iniciar.emit(this.movimentacao);
  }

  protected solicitarConclusao(): void {
    if (
      this.bloqueado
      || this.processando
      || this.movimentacao.status
        !== 'EM_EXECUCAO'
    ) {
      return;
    }

    this.concluir.emit(this.movimentacao);
  }

  private descricaoPosicao(
    tipo: TipoPosicaoEmbarcacao,
    vagaCodigo: string | null,
    descricao: string | null
  ): string {
    if (tipo === 'VAGA') {
      return vagaCodigo
        ? `Vaga ${vagaCodigo}`
        : 'Vaga não informada';
    }

    const rotulos: Record<
      TipoPosicaoEmbarcacao,
      string
    > = {
      VAGA: 'Vaga',
      AGUA: 'Água',
      PIER_ESPERA: 'Píer de espera',
      AREA_SERVICO: 'Área de serviço',
      EXTERNA: 'Área externa',
      DESCONHECIDA: 'Posição desconhecida'
    };

    const local = rotulos[tipo];

    return descricao?.trim()
      ? `${local} · ${descricao.trim()}`
      : local;
  }

  private formatarDuracao(
    milissegundos: number
  ): string {
    const minutos = Math.max(
      0,
      Math.floor(
        milissegundos / 60_000
      )
    );

    if (minutos < 1) {
      return 'menos de 1 min';
    }

    if (minutos < 60) {
      return `${minutos} min`;
    }

    const horas = Math.floor(
      minutos / 60
    );

    const minutosRestantes =
      minutos % 60;

    if (minutosRestantes === 0) {
      return `${horas} h`;
    }

    return `${horas} h `
      + `${minutosRestantes} min`;
  }
}
