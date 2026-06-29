export type TipoVaga =
  | 'MOLHADA'
  | 'SECA'
  | 'POITA'
  | 'OUTRA';

export interface Vaga {
  id: string;
  codigo: string;
  tipo: TipoVaga;
  setor: string | null;
  localizacao: string | null;
  comprimentoMaximoMetros: number;
  bocaMaximaMetros: number;
  caladoMaximoMetros: number | null;
  alturaMaximaMetros: number | null;
  pesoMaximoKg: number | null;
  possuiAgua: boolean;
  possuiEnergia: boolean;
  observacoes: string | null;
  ativa: boolean;
  organizacaoId: string;
  criadaEm: string;
  atualizadaEm: string;
}

export interface DadosVaga {
  codigo: string;
  tipo: TipoVaga;
  setor: string | null;
  localizacao: string | null;
  comprimentoMaximoMetros: number;
  bocaMaximaMetros: number;
  caladoMaximoMetros: number | null;
  alturaMaximaMetros: number | null;
  pesoMaximoKg: number | null;
  possuiAgua: boolean;
  possuiEnergia: boolean;
  observacoes: string | null;
}

export interface AlterarStatusVaga {
  ativa: boolean;
}

export type FiltroStatusVaga =
  | 'TODAS'
  | 'ATIVAS'
  | 'INATIVAS';

export type FiltroTipoVaga =
  | 'TODAS'
  | TipoVaga;

export interface ConsultaVagas {
  pagina: number;
  tamanho: number;
  codigo?: string;
  setor?: string;
  tipo?: TipoVaga;
  ativa?: boolean;
}
