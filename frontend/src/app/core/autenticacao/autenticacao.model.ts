export type PerfilUsuario =
  | 'ADMINISTRADOR_PLATAFORMA'
  | 'ADMINISTRADOR_MARINA'
  | 'GERENTE'
  | 'ATENDENTE'
  | 'FINANCEIRO';

export interface SolicitacaoLogin {
  codigoOrganizacao: string;
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
