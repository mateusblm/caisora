export type PerfilUsuario =
  | 'ADMINISTRADOR_PLATAFORMA'
  | 'ADMINISTRADOR_MARINA'
  | 'GERENTE'
  | 'ATENDENTE'
  | 'FINANCEIRO';

export type ModuloSistema =
  | 'DASHBOARD'
  | 'CLIENTES'
  | 'EMBARCACOES'
  | 'VAGAS'
  | 'OCUPACOES'
  | 'MOVIMENTACOES'
  | 'CONTRATOS'
  | 'PAINEL_TV'
  | 'USUARIOS'
  | 'CHECKLIST_SAIDA'
  | 'CONFIGURACOES';

export type AcaoSistema =
  | 'VISUALIZAR'
  | 'CRIAR'
  | 'EDITAR'
  | 'ALTERAR_STATUS'
  | 'INICIAR'
  | 'CONCLUIR'
  | 'CANCELAR'
  | 'CONFIGURAR';

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
  modulosAtivos: ModuloSistema[];
  permissoes: string[];
}

export interface RespostaLogin {
  tokenAcesso: string;
  tipoToken: string;
  expiraEm: number;
  usuario: UsuarioAutenticado;
}
