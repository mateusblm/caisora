import { Routes } from '@angular/router';
import {
  autenticacaoFilhosGuard,
  autenticacaoGuard,
  variabilidadeGuard
} from './core/autenticacao/autenticacao.guard';
import { naoAutenticadoGuard } from './core/autenticacao/nao-autenticado.guard';
import { LayoutComponent } from './core/layout/layout/layout.component';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar | Caisora',
    canActivate: [naoAutenticadoGuard],
    loadComponent: () =>
      import('./features/autenticacao/pages/login/login.component')
        .then((componente) => componente.LoginComponent)
  },
  {
    path: 'painel-tv',
    title: 'Painel operacional TV | Caisora',
    canActivate: [autenticacaoGuard, variabilidadeGuard],
    data: { modulo: 'PAINEL_TV', acao: 'VISUALIZAR' },
    loadComponent: () =>
      import('./features/painel-tv/pages/painel-tv/painel-tv.component')
        .then((componente) => componente.PainelTvComponent)
  },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [autenticacaoGuard],
    canActivateChild: [autenticacaoFilhosGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        title: 'Dashboard | Caisora',
        data: { modulo: 'DASHBOARD', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard/dashboard.component')
            .then((componente) => componente.DashboardComponent)
      },
      {
        path: 'clientes/novo',
        title: 'Novo cliente | Caisora',
        data: { modulo: 'CLIENTES', acao: 'CRIAR' },
        loadComponent: () =>
          import('./features/clientes/pages/cliente-formulario/cliente-formulario.component')
            .then((componente) => componente.ClienteFormularioComponent)
      },
      {
        path: 'clientes/:id/editar',
        title: 'Editar cliente | Caisora',
        data: { modulo: 'CLIENTES', acao: 'EDITAR' },
        loadComponent: () =>
          import('./features/clientes/pages/cliente-formulario/cliente-formulario.component')
            .then((componente) => componente.ClienteFormularioComponent)
      },
      {
        path: 'clientes',
        title: 'Clientes | Caisora',
        data: { modulo: 'CLIENTES', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/clientes/pages/cliente-listagem/cliente-listagem.component')
            .then((componente) => componente.ClienteListagemComponent)
      },
      {
        path: 'embarcacoes/nova',
        title: 'Nova embarcação | Caisora',
        data: { modulo: 'EMBARCACOES', acao: 'CRIAR' },
        loadComponent: () =>
          import('./features/embarcacoes/pages/embarcacao-formulario/embarcacao-formulario.component')
            .then((componente) => componente.EmbarcacaoFormularioComponent)
      },
      {
        path: 'embarcacoes/:id/editar',
        title: 'Editar embarcação | Caisora',
        data: { modulo: 'EMBARCACOES', acao: 'EDITAR' },
        loadComponent: () =>
          import('./features/embarcacoes/pages/embarcacao-formulario/embarcacao-formulario.component')
            .then((componente) => componente.EmbarcacaoFormularioComponent)
      },
      {
        path: 'embarcacoes',
        title: 'Embarcações | Caisora',
        data: { modulo: 'EMBARCACOES', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/embarcacoes/pages/embarcacao-listagem/embarcacao-listagem.component')
            .then((componente) => componente.EmbarcacaoListagemComponent)
      },
      {
        path: 'vagas/nova',
        title: 'Nova vaga | Caisora',
        data: { modulo: 'VAGAS', acao: 'CRIAR' },
        loadComponent: () =>
          import('./features/vagas/pages/vaga-formulario/vaga-formulario.component')
            .then((componente) => componente.VagaFormularioComponent)
      },
      {
        path: 'vagas/:id/editar',
        title: 'Editar vaga | Caisora',
        data: { modulo: 'VAGAS', acao: 'EDITAR' },
        loadComponent: () =>
          import('./features/vagas/pages/vaga-formulario/vaga-formulario.component')
            .then((componente) => componente.VagaFormularioComponent)
      },
      {
        path: 'vagas',
        title: 'Vagas | Caisora',
        data: { modulo: 'VAGAS', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/vagas/pages/vaga-listagem/vaga-listagem.component')
            .then((componente) => componente.VagaListagemComponent)
      },
      {
        path: 'ocupacoes/nova',
        title: 'Nova ocupação | Caisora',
        data: { modulo: 'OCUPACOES', acao: 'CRIAR' },
        loadComponent: () =>
          import('./features/ocupacoes/pages/ocupacao-formulario/ocupacao-formulario.component')
            .then((componente) => componente.OcupacaoFormularioComponent)
      },
      {
        path: 'ocupacoes/:id/editar',
        title: 'Editar ocupação | Caisora',
        data: { modulo: 'OCUPACOES', acao: 'EDITAR' },
        loadComponent: () =>
          import('./features/ocupacoes/pages/ocupacao-formulario/ocupacao-formulario.component')
            .then((componente) => componente.OcupacaoFormularioComponent)
      },
      {
        path: 'ocupacoes',
        title: 'Ocupações | Caisora',
        data: { modulo: 'OCUPACOES', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/ocupacoes/pages/ocupacao-listagem/ocupacao-listagem.component')
            .then((componente) => componente.OcupacaoListagemComponent)
      },
      {
        path: 'movimentacoes/nova',
        title: 'Nova movimentação | Caisora',
        data: { modulo: 'MOVIMENTACOES', acao: 'CRIAR' },
        loadComponent: () =>
          import('./features/movimentacoes/pages/movimentacao-formulario/movimentacao-formulario.component')
            .then((componente) => componente.MovimentacaoFormularioComponent)
      },
      {
        path: 'movimentacoes/:id/editar',
        title: 'Editar movimentação | Caisora',
        data: { modulo: 'MOVIMENTACOES', acao: 'EDITAR' },
        loadComponent: () =>
          import('./features/movimentacoes/pages/movimentacao-formulario/movimentacao-formulario.component')
            .then((componente) => componente.MovimentacaoFormularioComponent)
      },
      {
        path: 'movimentacoes',
        title: 'Movimentações | Caisora',
        data: { modulo: 'MOVIMENTACOES', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/movimentacoes/pages/movimentacao-listagem/movimentacao-listagem.component')
            .then((componente) => componente.MovimentacaoListagemComponent)
      },
      {
        path: 'contratos/novo',
        title: 'Novo contrato | Caisora',
        data: { modulo: 'CONTRATOS', acao: 'CRIAR' },
        loadComponent: () =>
          import('./features/contratos/pages/contrato-formulario/contrato-formulario.component')
            .then((componente) => componente.ContratoFormularioComponent)
      },
      {
        path: 'contratos/:id/editar',
        title: 'Editar contrato | Caisora',
        data: { modulo: 'CONTRATOS', acao: 'EDITAR' },
        loadComponent: () =>
          import('./features/contratos/pages/contrato-formulario/contrato-formulario.component')
            .then((componente) => componente.ContratoFormularioComponent)
      },
      {
        path: 'contratos/:id',
        title: 'Detalhes do contrato | Caisora',
        data: { modulo: 'CONTRATOS', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/contratos/pages/contrato-detalhe/contrato-detalhe.component')
            .then((componente) => componente.ContratoDetalheComponent)
      },
      {
        path: 'contratos',
        title: 'Contratos | Caisora',
        data: { modulo: 'CONTRATOS', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/contratos/pages/contrato-listagem/contrato-listagem.component')
            .then((componente) => componente.ContratoListagemComponent)
      },
      {
        path: 'checklist-saida',
        title: 'Checklist de saída | Caisora',
        data: { modulo: 'CHECKLIST_SAIDA', acao: 'VISUALIZAR' },
        loadComponent: () =>
          import('./features/checklist-saida/pages/checklist-saida/checklist-saida.component')
            .then((componente) => componente.ChecklistSaidaComponent)
      },
      {
        path: 'configuracoes/variabilidade',
        title: 'Variabilidade | Caisora',
        data: { modulo: 'CONFIGURACOES', acao: 'CONFIGURAR' },
        loadComponent: () =>
          import('./features/variabilidade/pages/configuracao-variabilidade/configuracao-variabilidade.component')
            .then((componente) => componente.ConfiguracaoVariabilidadeComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
