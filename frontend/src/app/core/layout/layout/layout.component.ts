import { Component, inject } from '@angular/core';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { map } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';

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
export class LayoutComponent {

  private readonly breakpointObserver =
    inject(BreakpointObserver);

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
    }
  ];
}