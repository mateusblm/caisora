import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners
} from '@angular/core';

import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import {
  provideRouter
} from '@angular/router';

import {
  provideAnimations
} from '@angular/platform-browser/animations';

import {
  routes
} from './app.routes';

import {
  autenticacaoInterceptor
} from './core/autenticacao/autenticacao.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    provideAnimations(),

    provideRouter(routes),

    provideHttpClient(
      withInterceptors([
        autenticacaoInterceptor
      ])
    )
  ]
};