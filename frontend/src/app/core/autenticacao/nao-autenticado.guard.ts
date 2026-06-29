import { inject } from '@angular/core';

import {
  CanActivateFn,
  Router
} from '@angular/router';

import {
  AutenticacaoService
} from './autenticacao.service';

export const naoAutenticadoGuard:
  CanActivateFn = () => {

    const autenticacaoService =
      inject(AutenticacaoService);

    const router = inject(Router);

    if (!autenticacaoService.estaAutenticado()) {
      return true;
    }

    return router.createUrlTree([
      '/dashboard'
    ]);
  };