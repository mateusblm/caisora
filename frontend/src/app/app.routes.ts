import { Routes } from '@angular/router';

import { LayoutComponent } from
  './core/layout/layout/layout.component';

export const routes: Routes = [
  {
    path: 'login',
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
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
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