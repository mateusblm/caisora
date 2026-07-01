import {
  PrioridadeMovimentacao,
  StatusMovimentacao,
  TipoMovimentacao,
  TipoPosicaoEmbarcacao
} from '../../movimentacoes/models/movimentacao.model';

export type AcaoOperacionalPainelTv =
  | 'DESCER_PARA_AGUA'
  | 'RETIRAR_DA_AGUA'
  | 'RETORNAR_PARA_VAGA'
  | 'TRANSFERIR_DE_VAGA'
  | 'DESLOCAR_INTERNAMENTE';

export type SituacaoPainelTv =
  | 'ATRASADA'
  | 'EM_EXECUCAO'
  | 'PROXIMA'
  | 'AGENDADA';

export type TipoAlertaPainelTv =
  | 'ATRASADA'
  | 'URGENTE'
  | 'PROXIMA'
  | 'SEM_OPERADOR';

export interface LocalMovimentacaoPainelTv {
  tipo: TipoPosicaoEmbarcacao;
  vagaCodigo: string | null;
  descricao: string | null;
  rotulo: string;
}

export interface MovimentacaoPainelTv {
  id: string;
  embarcacaoId: string;
  embarcacaoNome: string;
  embarcacaoModelo: string | null;
  proprietarioNome: string;
  tipo: TipoMovimentacao;
  acaoOperacional: AcaoOperacionalPainelTv;
  status: StatusMovimentacao;
  prioridade: PrioridadeMovimentacao;
  situacao: SituacaoPainelTv;
  origem: LocalMovimentacaoPainelTv;
  destino: LocalMovimentacaoPainelTv;
  agendadaPara: string;
  iniciadaEm: string | null;
  operadorResponsavelNome: string | null;
  observacoes: string | null;
  minutosAtraso: number;
  minutosParaInicio: number;
  minutosEmExecucao: number;
  alertas: TipoAlertaPainelTv[];
}

export interface ResumoPainelTv {
  descidasParaAgua: number;
  retiradasDaAgua: number;
  transferenciasDeVaga: number;
  deslocamentosInternos: number;
  emExecucao: number;
  alertas: number;
}

export interface PainelTvOperacional {
  geradoEm: string;
  fusoHorario: string;
  inicioDia: string;
  fimDia: string;
  atualizarAposSegundos: number;
  resumo: ResumoPainelTv;
  alertas: MovimentacaoPainelTv[];
  descidasParaAgua: MovimentacaoPainelTv[];
  retiradasDaAgua: MovimentacaoPainelTv[];
  transferenciasDeVaga: MovimentacaoPainelTv[];
  deslocamentosInternos: MovimentacaoPainelTv[];
  emExecucao: MovimentacaoPainelTv[];
}
