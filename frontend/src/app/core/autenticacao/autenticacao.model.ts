export type PerfilUsuario =
  | 'ADMINISTRADOR_PLATAFORMA'
  | 'ADMINISTRADOR_MARINA'
  | 'GERENTE'
  | 'ATENDENTE'
  | 'FINANCEIRO';

export interface SolicitacaoLogin {
  email: string;
  senha: string;
}

export interface UsuarioAutenticado {
  id: string;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  organizacaoId: string;
  organizacaoNome: string;
}

export interface RespostaLogin {
  tokenAcesso: string;
  tipoToken: string;
  expiraEm: number;
  usuario: UsuarioAutenticado;
}

export interface ErroCampoApi {
  campo: string;
  mensagem: string;
}

export interface ErroApi {
  instante?: string;
  status: number;
  erro?: string;
  codigo: string;
  mensagem: string;
  caminho?: string;
  errosCampos?: ErroCampoApi[];
}