export type TipoPessoa =
  | 'FISICA'
  | 'JURIDICA';

export interface Cliente {
  id: string;
  tipoPessoa: TipoPessoa;
  nome: string;
  razaoSocial: string | null;
  cpfCnpj: string;
  email: string | null;
  telefone: string | null;
  celular: string | null;
  observacoes: string | null;
  ativo: boolean;
  organizacaoId: string;
  criadoEm: string;
  atualizadoEm: string;
}

export interface DadosCliente {
  tipoPessoa: TipoPessoa;
  nome: string;
  razaoSocial: string | null;
  cpfCnpj: string;
  email: string | null;
  telefone: string | null;
  celular: string | null;
  observacoes: string | null;
}

export interface AlterarStatusCliente {
  ativo: boolean;
}

export type FiltroStatusCliente =
  | 'TODOS'
  | 'ATIVOS'
  | 'INATIVOS';

export interface ConsultaClientes {
  pagina: number;
  tamanho: number;
  nome?: string;
  ativo?: boolean;
}