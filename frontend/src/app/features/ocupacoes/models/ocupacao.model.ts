import {
  TipoVaga
} from '../../vagas/models/vaga.model';

export type StatusOcupacao =
  | 'ATIVA'
  | 'ENCERRADA';

export type FiltroStatusOcupacao =
  | 'TODAS'
  | StatusOcupacao;

export interface Ocupacao {
  id: string;

  embarcacaoId: string;
  embarcacaoNome: string;
  embarcacaoModelo: string | null;
  proprietarioNome: string;

  vagaId: string;
  vagaCodigo: string;
  vagaTipo: TipoVaga;
  vagaSetor: string | null;
  vagaLocalizacao: string | null;

  status: StatusOcupacao;

  inicioEm: string;
  fimPrevistoEm: string | null;
  encerradaEm: string | null;

  observacoes: string | null;

  organizacaoId: string;
  criadaEm: string;
  atualizadaEm: string;
}

export interface CriarOcupacao {
  embarcacaoId: string;
  vagaId: string;
  inicioEm: string;
  fimPrevistoEm: string | null;
  observacoes: string | null;
}

export interface AtualizarOcupacao {
  fimPrevistoEm: string | null;
  observacoes: string | null;
}

export interface EncerrarOcupacao {
  encerradaEm: string;
}

export interface ConsultaOcupacoes {
  pagina: number;
  tamanho: number;

  embarcacaoId?: string;
  vagaId?: string;
  status?: StatusOcupacao;
}