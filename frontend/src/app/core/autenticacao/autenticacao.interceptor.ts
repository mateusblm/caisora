import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';

import { inject } from '@angular/core';
import { Router } from '@angular/router';

import {
  catchError,
  throwError
} from 'rxjs';

import { environment } from
  '../../../environments/environment';

import {
  AutenticacaoService
} from './autenticacao.service';

export const autenticacaoInterceptor:
  HttpInterceptorFn = (requisicao, proximo) => {

    const autenticacaoService =
      inject(AutenticacaoService);

    const router = inject(Router);

    const requisicaoApi =
      requisicao.url.startsWith(
        environment.apiUrl
      )
      || requisicao.url.includes(
        `${environment.apiUrl}/`
      );

    const requisicaoLogin =
      requisicao.url.includes(
        '/autenticacao/login'
      );

    const token =
      autenticacaoService.obterToken();

    let requisicaoAutenticada = requisicao;

    if (
      requisicaoApi
      && !requisicaoLogin
      && token
    ) {
      requisicaoAutenticada =
        requisicao.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
    }

    return proximo(
      requisicaoAutenticada
    ).pipe(
      catchError((erro: unknown) => {
        const sessaoNaoAutorizada =
          erro instanceof HttpErrorResponse
          && erro.status === 401
          && requisicaoApi
          && !requisicaoLogin;

        if (sessaoNaoAutorizada) {
          const rotaAtual =
            router.url.startsWith('/login')
              ? '/dashboard'
              : router.url;

          autenticacaoService.encerrarSessao();

          void router.navigate(
            ['/login'],
            {
              queryParams: {
                sessao: 'expirada',
                retorno: rotaAtual
              }
            }
          );
        }

        return throwError(() => erro);
      })
    );
  };