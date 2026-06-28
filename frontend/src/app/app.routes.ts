import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import(
        './features/inicio/pages/inicio/inicio.component'
      ).then(
        (componente) => componente.InicioComponent
      )
  },
  {
    path: '**',
    redirectTo: ''
  }
];