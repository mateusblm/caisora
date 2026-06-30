import { Routes } from '@angular/router';

import {
  autenticacaoFilhosGuard,
  autenticacaoGuard
} from './core/autenticacao/autenticacao.guard';
import { naoAutenticadoGuard } from './core/autenticacao/nao-autenticado.guard';
import { LayoutComponent } from './core/layout/layout/layout.component';

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
        (componente) => componente.LoginComponent
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
      },
      {
        path: 'clientes/novo',
        title: 'Novo cliente | Caisora',
        loadComponent: () =>
          import(
            './features/clientes/pages/cliente-formulario/cliente-formulario.component'
          ).then(
            (componente) =>
              componente.ClienteFormularioComponent
          )
      },
      {
        path: 'clientes/:id/editar',
        title: 'Editar cliente | Caisora',
        loadComponent: () =>
          import(
            './features/clientes/pages/cliente-formulario/cliente-formulario.component'
          ).then(
            (componente) =>
              componente.ClienteFormularioComponent
          )
      },
      {
        path: 'clientes',
        title: 'Clientes | Caisora',
        loadComponent: () =>
          import(
            './features/clientes/pages/cliente-listagem/cliente-listagem.component'
          ).then(
            (componente) =>
              componente.ClienteListagemComponent
          )
      },
      {
        path: 'embarcacoes',
        title: 'Embarcações | Caisora',
        loadComponent: () =>
          import(
            './features/embarcacoes/pages/embarcacao-listagem/embarcacao-listagem.component'
          ).then(
            (componente) =>
              componente.EmbarcacaoListagemComponent
          )
      },
      {
        path: 'embarcacoes/nova',
        title: 'Nova embarcação | Caisora',
        loadComponent: () =>
          import(
            './features/embarcacoes/pages/embarcacao-formulario/embarcacao-formulario.component'
          ).then(
            (componente) =>
              componente.EmbarcacaoFormularioComponent
          )
      },
      {
        path: 'embarcacoes/:id/editar',
        title: 'Editar embarcação | Caisora',
        loadComponent: () =>
          import(
            './features/embarcacoes/pages/embarcacao-formulario/embarcacao-formulario.component'
          ).then(
            (componente) =>
              componente.EmbarcacaoFormularioComponent
          )
      },
      {
        path: 'vagas',
        title: 'Vagas | Caisora',
        loadComponent: () =>
          import(
            './features/vagas/pages/vaga-listagem/vaga-listagem.component'
          ).then(
            (componente) =>
              componente.VagaListagemComponent
          )
      },
      {
        path: 'vagas/nova',
        title: 'Nova vaga | Caisora',
        loadComponent: () =>
          import(
            './features/vagas/pages/vaga-formulario/vaga-formulario.component'
          ).then(
            (componente) =>
              componente.VagaFormularioComponent
          )
      },
      {
        path: 'vagas/:id/editar',
        title: 'Editar vaga | Caisora',
        loadComponent: () =>
          import(
            './features/vagas/pages/vaga-formulario/vaga-formulario.component'
          ).then(
            (componente) =>
              componente.VagaFormularioComponent
          )
      },
      {
        path: 'ocupacoes/nova',
        title: 'Nova ocupação | Caisora',
        loadComponent: () =>
          import(
            './features/ocupacoes/pages/ocupacao-formulario/ocupacao-formulario.component'
          ).then(
            (componente) =>
              componente.OcupacaoFormularioComponent
          )
      },
      {
        path: 'ocupacoes/:id/editar',
        title: 'Editar ocupação | Caisora',
        loadComponent: () =>
          import(
            './features/ocupacoes/pages/ocupacao-formulario/ocupacao-formulario.component'
          ).then(
            (componente) =>
              componente.OcupacaoFormularioComponent
          )
      },
      {
        path: 'ocupacoes',
        title: 'Ocupações | Caisora',
        loadComponent: () =>
          import(
            './features/ocupacoes/pages/ocupacao-listagem/ocupacao-listagem.component'
          ).then(
            (componente) =>
              componente.OcupacaoListagemComponent
          )
      },
      {
        path: 'movimentacoes/nova',
        title: 'Nova movimentação | Caisora',
        loadComponent: () =>
          import(
            './features/movimentacoes/pages/movimentacao-formulario/movimentacao-formulario.component'
          ).then(
            (componente) =>
              componente.MovimentacaoFormularioComponent
          )
      },
      {
        path: 'movimentacoes/:id/editar',
        title: 'Editar movimentação | Caisora',
        loadComponent: () =>
          import(
            './features/movimentacoes/pages/movimentacao-formulario/movimentacao-formulario.component'
          ).then(
            (componente) =>
              componente.MovimentacaoFormularioComponent
          )
      },
      {
        path: 'movimentacoes',
        title: 'Movimentações | Caisora',
        loadComponent: () =>
          import(
            './features/movimentacoes/pages/movimentacao-listagem/movimentacao-listagem.component'
          ).then(
            (componente) =>
              componente.MovimentacaoListagemComponent
          )
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
