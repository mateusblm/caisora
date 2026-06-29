export type TipoEmbarcacao =
  | 'LANCHA'
  | 'VELEIRO'
  | 'CATAMARA'
  | 'IATE'
  | 'MOTO_AQUATICA'
  | 'BOTE'
  | 'CANOA'
  | 'ESCUNA'
  | 'TRAINEIRA'
  | 'PESQUEIRO'
  | 'FLUTUANTE'
  | 'OUTRA';

export type TipoPropulsao =
  | 'MOTOR'
  | 'VELA'
  | 'VELA_E_MOTOR'
  | 'REMO'
  | 'SEM_PROPULSAO'
  | 'OUTRA';

export interface Embarcacao {
  id: string;
  proprietarioId: string;
  proprietarioNome: string;
  nome: string | null;
  tipo: TipoEmbarcacao;
  fabricante: string | null;
  modelo: string | null;
  anoFabricacao: number | null;
  numeroInscricao: string | null;
  numeroCasco: string | null;
  portoInscricao: string | null;
  codigoPaisBandeira: string | null;
  comprimentoTotalMetros: number;
  bocaMetros: number;
  caladoMetros: number | null;
  pontalMetros: number | null;
  alturaTotalMetros: number | null;
  pesoKg: number | null;
  capacidadePessoas: number | null;
  tipoPropulsao: TipoPropulsao;
  corPredominante: string | null;
  observacoes: string | null;
  ativa: boolean;
  organizacaoId: string;
  criadaEm: string;
  atualizadaEm: string;
}

export interface DadosEmbarcacao {
  proprietarioId: string;
  nome: string | null;
  tipo: TipoEmbarcacao;
  fabricante: string | null;
  modelo: string | null;
  anoFabricacao: number | null;
  numeroInscricao: string | null;
  numeroCasco: string | null;
  portoInscricao: string | null;
  codigoPaisBandeira: string | null;
  comprimentoTotalMetros: number;
  bocaMetros: number;
  caladoMetros: number | null;
  pontalMetros: number | null;
  alturaTotalMetros: number | null;
  pesoKg: number | null;
  capacidadePessoas: number | null;
  tipoPropulsao: TipoPropulsao;
  corPredominante: string | null;
  observacoes: string | null;
}

export interface AlterarStatusEmbarcacao {
  ativa: boolean;
}

export type FiltroStatusEmbarcacao =
  | 'TODAS'
  | 'ATIVAS'
  | 'INATIVAS';

export type FiltroTipoEmbarcacao =
  | 'TODAS'
  | TipoEmbarcacao;

export interface ConsultaEmbarcacoes {
  pagina: number;
  tamanho: number;
  nome?: string;
  ativa?: boolean;
  proprietarioId?: string;
  tipo?: TipoEmbarcacao;
}