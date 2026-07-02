import { TipoVaga } from "../../vagas/models/vaga.model";

export type StatusContrato =
  | "RASCUNHO"
  | "PENDENTE_ASSINATURA"
  | "ATIVO"
  | "SUSPENSO"
  | "EM_ENCERRAMENTO"
  | "ENCERRADO"
  | "CANCELADO";

export type PeriodicidadeContrato =
  "DIARIA" | "MENSAL" | "TRIMESTRAL" | "SEMESTRAL" | "ANUAL" | "PERSONALIZADA";

export type TipoEventoContrato =
  | "CRIADO"
  | "EDITADO"
  | "ENVIADO_PARA_ASSINATURA"
  | "ATIVADO"
  | "SUSPENSO"
  | "REATIVADO"
  | "ENCERRAMENTO_SOLICITADO"
  | "ENCERRADO"
  | "CANCELADO"
  | "OCUPACAO_VINCULADA"
  | "OCUPACAO_DESVINCULADA";

export interface Contrato {
  id: string;
  numero: string;
  status: StatusContrato;
  clienteId: string;
  clienteNome: string;
  embarcacaoId: string;
  embarcacaoNome: string;
  tipoVagaContratada: TipoVaga;
  periodicidade: PeriodicidadeContrato;
  dataInicio: string;
  dataFim: string | null;
  dataAssinatura: string | null;
  dataAtivacao: string | null;
  dataEncerramento: string | null;
  renovacaoAutomatica: boolean;
  diasAvisoPrevio: number | null;
  valorBase: number;
  diaVencimento: number;
  observacoes: string | null;
  criadoPorId: string;
  criadoPorNome: string;
  organizacaoId: string;
  criadoEm: string;
  atualizadoEm: string;
  versao: number;
}

export interface DadosContrato {
  clienteId: string;
  embarcacaoId: string;
  tipoVagaContratada: TipoVaga;
  periodicidade: PeriodicidadeContrato;
  dataInicio: string;
  dataFim: string | null;
  renovacaoAutomatica: boolean;
  diasAvisoPrevio: number | null;
  valorBase: number;
  diaVencimento: number;
  observacoes: string | null;
}

export interface ConsultaContratos {
  pagina: number;
  tamanho: number;
  status?: StatusContrato;
  clienteId?: string;
  embarcacaoId?: string;
}

export interface MotivoContrato {
  descricao: string | null;
}

export interface AtivarContrato extends MotivoContrato {
  dataAssinatura: string;
}

export interface EncerrarContrato extends MotivoContrato {
  dataEncerramento: string;
}

export interface VinculoContratoOcupacao {
  id: string;
  contratoId: string;
  ocupacaoId: string;
  vagaId: string;
  vagaCodigo: string;
  inicioEm: string;
  fimEm: string | null;
  motivoFim: string | null;
  aberto: boolean;
}

export interface HistoricoContrato {
  id: string;
  contratoId: string;
  tipoEvento: TipoEventoContrato;
  statusAnterior: StatusContrato | null;
  statusNovo: StatusContrato | null;
  descricao: string | null;
  realizadoPorId: string;
  realizadoPorNome: string;
  realizadoEm: string;
}
