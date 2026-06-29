import { inject } from '@angular/core';

import {
  CanActivateChildFn,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree
} from '@angular/router';

import {
  AutenticacaoService
} from './autenticacao.service';

function verificarAcesso(
  estado: RouterStateSnapshot
): boolean | UrlTree {

  const autenticacaoService =
    inject(AutenticacaoService);

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

export const autenticacaoGuard:
  CanActivateFn = (_rota, estado) =>
    verificarAcesso(estado);

export const autenticacaoFilhosGuard:
  CanActivateChildFn = (_rota, estado) =>
    verificarAcesso(estado);