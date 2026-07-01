import {
  Movimentacao
} from '../../movimentacoes/models/movimentacao.model';

export interface IndicadoresOperacionais {
  emExecucao: number;
  atrasadas: number;
  proximaHora: number;
  urgentes: number;
  semOperador: number;
  concluidasHoje: number;
}

export interface PainelOperacional {
  geradoEm: string;
  fusoHorario: string;
  inicioDia: string;
  fimDia: string;
  indicadores: IndicadoresOperacionais;
  atrasadas: Movimentacao[];
  emExecucao: Movimentacao[];
  proximosTrintaMinutos: Movimentacao[];
  proximasDuasHoras: Movimentacao[];
  restanteDia: Movimentacao[];
  concluidasRecentemente: Movimentacao[];
}
