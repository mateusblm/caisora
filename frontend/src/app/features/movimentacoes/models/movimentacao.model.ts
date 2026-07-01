export type TipoMovimentacao =
  | 'LANCAMENTO'
  | 'RETIRADA'
  | 'RETORNO_PARA_VAGA'
  | 'TRANSFERENCIA'
  | 'DESLOCAMENTO_INTERNO';

export type StatusMovimentacao =
  | 'AGENDADA'
  | 'EM_EXECUCAO'
  | 'CONCLUIDA'
  | 'CANCELADA';

export type PrioridadeMovimentacao =
  | 'NORMAL'
  | 'ALTA'
  | 'URGENTE';

export type TipoPosicaoEmbarcacao =
  | 'VAGA'
  | 'AGUA'
  | 'PIER_ESPERA'
  | 'AREA_SERVICO'
  | 'EXTERNA'
  | 'DESCONHECIDA';

export type TipoEventoMovimentacao =
  | 'CRIADA'
  | 'ATUALIZADA'
  | 'REAGENDADA'
  | 'INICIADA'
  | 'CONCLUIDA'
  | 'CANCELADA';

export type FiltroStatusMovimentacao =
  | 'TODAS'
  | StatusMovimentacao;

export type FiltroTipoMovimentacao =
  | 'TODOS'
  | TipoMovimentacao;

export interface Movimentacao {
  id: string;
  embarcacaoId: string;
  embarcacaoNome: string;
  embarcacaoModelo: string | null;
  proprietarioNome: string;
  tipo: TipoMovimentacao;
  status: StatusMovimentacao;
  prioridade: PrioridadeMovimentacao;

  tipoPosicaoOrigem: TipoPosicaoEmbarcacao;
  vagaOrigemId: string | null;
  vagaOrigemCodigo: string | null;
  descricaoOrigem: string | null;

  tipoPosicaoDestino: TipoPosicaoEmbarcacao;
  vagaDestinoId: string | null;
  vagaDestinoCodigo: string | null;
  descricaoDestino: string | null;

  agendadaPara: string;
  iniciadaEm: string | null;
  concluidaEm: string | null;
  canceladaEm: string | null;

  solicitadaPorId: string;
  solicitadaPorNome: string;
  operadorResponsavelId: string | null;
  operadorResponsavelNome: string | null;

  observacoes: string | null;
  motivoCancelamento: string | null;

  versao: number;
  organizacaoId: string;
  criadaEm: string;
  atualizadaEm: string;
}

export interface CriarMovimentacao {
  embarcacaoId: string;
  tipo: TipoMovimentacao;
  prioridade: PrioridadeMovimentacao;
  tipoPosicaoDestino: TipoPosicaoEmbarcacao;
  vagaDestinoId: string | null;
  descricaoDestino: string | null;
  agendadaPara: string;
  operadorResponsavelId: string | null;
  observacoes: string | null;
}

export interface AtualizarMovimentacao {
  prioridade: PrioridadeMovimentacao;
  tipoPosicaoDestino: TipoPosicaoEmbarcacao;
  vagaDestinoId: string | null;
  descricaoDestino: string | null;
  agendadaPara: string;
  operadorResponsavelId: string | null;
  observacoes: string | null;
}

export interface IniciarMovimentacao {
  iniciadaEm: string;
  observacao: string | null;
}

export interface ConcluirMovimentacao {
  concluidaEm: string;
  observacao: string | null;
}

export interface CancelarMovimentacao {
  canceladaEm: string;
  motivo: string;
}

export interface ConsultaMovimentacoes {
  pagina: number;
  tamanho: number;
  status?: StatusMovimentacao;
  tipo?: TipoMovimentacao;
  embarcacaoId?: string;
  inicio?: string;
  fim?: string;
}

export interface ConsultaHistoricoMovimentacao {
  pagina: number;
  tamanho: number;
}

export interface HistoricoMovimentacao {
  id: string;
  movimentacaoId: string;
  tipoEvento: TipoEventoMovimentacao;
  statusAnterior: StatusMovimentacao | null;
  statusNovo: StatusMovimentacao | null;
  agendadaParaAnterior: string | null;
  agendadaParaNova: string | null;
  usuarioId: string;
  usuarioNome: string;
  observacao: string | null;
  dadosAnteriores: Record<string, unknown> | null;
  dadosNovos: Record<string, unknown> | null;
  organizacaoId: string;
  ocorridoEm: string;
}

export interface PosicaoEmbarcacao {
  id: string;
  embarcacaoId: string;
  embarcacaoNome: string;
  embarcacaoModelo: string | null;
  proprietarioNome: string;
  tipo: TipoPosicaoEmbarcacao;
  vagaId: string | null;
  vagaCodigo: string | null;
  vagaSetor: string | null;
  vagaLocalizacao: string | null;
  descricaoLocal: string | null;
  movimentacaoOrigemId: string | null;
  versao: number;
  organizacaoId: string;
  criadaEm: string;
  atualizadaEm: string;
}

export interface UsuarioOperador {
  id: string;
  nome: string;
  email: string;
  perfil: string;
  ativo: boolean;
  organizacaoId: string;
  organizacaoNome: string;
  criadoEm: string;
  atualizadoEm: string;
}

