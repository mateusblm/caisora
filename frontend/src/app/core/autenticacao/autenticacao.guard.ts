import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  ActivatedRouteSnapshot,
  CanActivateChildFn,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree
} from '@angular/router';

import {
  AcaoSistema,
  ModuloSistema
} from './autenticacao.model';
import {
  AutenticacaoService
} from './autenticacao.service';

function verificarAutenticacao(
  estado: RouterStateSnapshot
): boolean | UrlTree {
  const autenticacaoService = inject(AutenticacaoService);
  const router = inject(Router);

  if (autenticacaoService.estaAutenticado()) {
    return true;
  }

  return router.createUrlTree(
    ['/login'],
    {
      queryParams: {
        retorno: estado.url
      }
    }
  );
}

function verificarVariabilidade(
  rota: ActivatedRouteSnapshot
): boolean | UrlTree {
  const autenticacaoService = inject(AutenticacaoService);
  const router = inject(Router);

  const modulo = rota.data['modulo'] as ModuloSistema | undefined;
  const acao = rota.data['acao'] as AcaoSistema | undefined;

  if (!modulo || !acao) {
    return true;
  }

  if (autenticacaoService.temPermissao(modulo, acao)) {
    return true;
  }

  inject(MatSnackBar).open(
    `Seu perfil não tem permissão para ${descricaoAcao[acao]} em ${nomeModulo[modulo]}.`,
    'Fechar',
    {
      duration: 4000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    }
  );

  // Navegando dentro do app, so cancela e o usuario continua na tela em que estava.
  // Na primeira carga (URL digitada ou F5) nao existe tela anterior, entao manda pro dashboard,
  // a nao ser que o bloqueio seja do proprio dashboard, o que geraria um redirecionamento infinito.
  if (router.navigated || modulo === 'DASHBOARD') {
    return false;
  }

  return router.createUrlTree(['/dashboard']);
}

const nomeModulo: Record<ModuloSistema, string> = {
  DASHBOARD: 'Dashboard',
  CLIENTES: 'Clientes',
  EMBARCACOES: 'Embarcações',
  VAGAS: 'Vagas',
  OCUPACOES: 'Ocupações',
  MOVIMENTACOES: 'Movimentações',
  CONTRATOS: 'Contratos',
  PAINEL_TV: 'Painel TV',
  USUARIOS: 'Usuários',
  CHECKLIST_SAIDA: 'Checklist de saída',
  CONFIGURACOES: 'Configurações'
};

const descricaoAcao: Record<AcaoSistema, string> = {
  VISUALIZAR: 'visualizar',
  CRIAR: 'criar',
  EDITAR: 'editar',
  ALTERAR_STATUS: 'alterar status',
  INICIAR: 'iniciar',
  CONCLUIR: 'concluir',
  CANCELAR: 'cancelar',
  CONFIGURAR: 'configurar'
};

export const autenticacaoGuard: CanActivateFn = (_rota, estado) =>
  verificarAutenticacao(estado);

export const variabilidadeGuard: CanActivateFn = (rota, estado) => {
  const autenticado = verificarAutenticacao(estado);
  return autenticado === true
    ? verificarVariabilidade(rota)
    : autenticado;
};

export const autenticacaoFilhosGuard: CanActivateChildFn = (rota, estado) => {
  const autenticado = verificarAutenticacao(estado);
  return autenticado === true
    ? verificarVariabilidade(rota)
    : autenticado;
};
