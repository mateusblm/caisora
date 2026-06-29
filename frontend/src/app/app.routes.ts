import { Routes } from '@angular/router';

import { LayoutComponent } from
  './core/layout/layout/layout.component';

import {
  autenticacaoFilhosGuard,
  autenticacaoGuard
} from './core/autenticacao/autenticacao.guard';

import {
  naoAutenticadoGuard
} from './core/autenticacao/nao-autenticado.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar | Caisora',
    canActivate: [
      naoAutenticadoGuard
    ],
    loadComponent: () =>
      import(
        './features/autenticacao/pages/login/login.component'
      ).then(
        (componente) =>
          componente.LoginComponent
      )
  },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [
      autenticacaoGuard
    ],
    canActivateChild: [
      autenticacaoFilhosGuard
    ],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        title: 'Dashboard | Caisora',
        loadComponent: () =>
          import(
            './features/dashboard/pages/dashboard/dashboard.component'
          ).then(
            (componente) =>
              componente.DashboardComponent
          )
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];