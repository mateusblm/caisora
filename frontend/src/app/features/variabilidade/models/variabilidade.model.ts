import {
  AcaoSistema,
  ModuloSistema,
  PerfilUsuario
} from '../../../core/autenticacao/autenticacao.model';

export interface ModuloConfiguracao {
  modulo: ModuloSistema;
  nome: string;
  obrigatorio: boolean;
  ativo: boolean;
  acoes: AcaoSistema[];
}

export interface PerfilConfiguracao {
  perfil: PerfilUsuario;
  permissoes: string[];
}

export interface ConfiguracaoVariabilidade {
  modulos: ModuloConfiguracao[];
  perfis: PerfilConfiguracao[];
}
