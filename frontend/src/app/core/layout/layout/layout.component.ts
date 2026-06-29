import {
  Component,
  computed,
  inject,
  OnInit
} from '@angular/core';

import {
  BreakpointObserver
} from '@angular/cdk/layout';

import {
  toSignal
} from '@angular/core/rxjs-interop';

import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';

import { map } from 'rxjs';

import {
  MatButtonModule
} from '@angular/material/button';

import {
  MatIconModule
} from '@angular/material/icon';

import {
  MatListModule
} from '@angular/material/list';

import {
  MatSidenavModule
} from '@angular/material/sidenav';

import {
  MatToolbarModule
} from '@angular/material/toolbar';

import {
  AutenticacaoService
} from '../../autenticacao/autenticacao.service';

import {
  PerfilUsuario
} from '../../autenticacao/autenticacao.model';

interface ItemMenu {
  rotulo: string;
  icone: string;
  rota: string;
}

@Component({
  selector: 'app-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent implements OnInit {

  private readonly breakpointObserver =
    inject(BreakpointObserver);

  private readonly autenticacaoService =
    inject(AutenticacaoService);

  private readonly router =
    inject(Router);

  protected readonly usuario =
    this.autenticacaoService.usuarioAtual;

  protected readonly telaPequena = toSignal(
    this.breakpointObserver
      .observe('(max-width: 900px)')
      .pipe(
        map((resultado) => resultado.matches)
      ),
    {
      initialValue: false
    }
  );

  protected readonly iniciaisUsuario =
    computed(() => {
      const nome =
        this.usuario()?.nome.trim();

      if (!nome) {
        return 'U';
      }

      const partes =
        nome.split(/\s+/);

      const primeiraInicial =
        partes[0].charAt(0);

      const ultimaInicial =
        partes.length > 1
          ? partes[partes.length - 1].charAt(0)
          : '';

      return (
        primeiraInicial + ultimaInicial
      ).toUpperCase();
    });

  protected readonly perfilUsuario =
    computed(() =>
      this.formatarPerfil(
        this.usuario()?.perfil
      )
    );

  protected readonly itensMenu: ItemMenu[] = [
    {
      rotulo: 'Dashboard',
      icone: 'dashboard',
      rota: '/dashboard'
    },
    {
      rotulo: 'Clientes',
      icone: 'groups',
      rota: '/clientes'
    },
    {
      rotulo: 'Embarcações',
      icone: 'directions_boat',
      rota: '/embarcacoes'
    },
    {
      rotulo: 'Vagas',
      icone: 'anchor',
      rota: '/vagas'
    }
  ];

  ngOnInit(): void {
    this.autenticacaoService
      .atualizarUsuarioAtual()
      .subscribe({
        error: () => {
          // O interceptor trata automaticamente o 401.
        }
      });
  }

  protected sair(): void {
    this.autenticacaoService.encerrarSessao();

    void this.router.navigateByUrl('/login');
  }

  private formatarPerfil(
    perfil?: PerfilUsuario
  ): string {
    switch (perfil) {
      case 'ADMINISTRADOR_PLATAFORMA':
        return 'Administrador da plataforma';

      case 'ADMINISTRADOR_MARINA':
        return 'Administrador da marina';

      case 'GERENTE':
        return 'Gerente';

      case 'ATENDENTE':
        return 'Atendente';

      case 'FINANCEIRO':
        return 'Financeiro';

      default:
        return 'Usuário';
    }
  }
}
