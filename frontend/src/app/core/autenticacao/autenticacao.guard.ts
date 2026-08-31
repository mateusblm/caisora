import { inject } from '@angular/core';

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

  return router.createUrlTree(['/dashboard']);
}

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
