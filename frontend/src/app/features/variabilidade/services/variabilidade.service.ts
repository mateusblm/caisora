import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  ModuloSistema,
  PerfilUsuario
} from '../../../core/autenticacao/autenticacao.model';
import { ConfiguracaoVariabilidade } from '../models/variabilidade.model';

@Injectable({ providedIn: 'root' })
export class VariabilidadeService {
  private readonly endpoint = `${environment.apiUrl}/variabilidade`;

  constructor(private readonly http: HttpClient) {}

  buscarConfiguracao(): Observable<ConfiguracaoVariabilidade> {
    return this.http.get<ConfiguracaoVariabilidade>(
      `${this.endpoint}/configuracao`
    );
  }

  atualizarModulo(
    modulo: ModuloSistema,
    ativo: boolean
  ): Observable<ConfiguracaoVariabilidade> {
    return this.http.put<ConfiguracaoVariabilidade>(
      `${this.endpoint}/modulos/${modulo}`,
      { ativo }
    );
  }

  atualizarPermissoes(
    perfil: PerfilUsuario,
    permissoes: string[]
  ): Observable<ConfiguracaoVariabilidade> {
    return this.http.put<ConfiguracaoVariabilidade>(
      `${this.endpoint}/perfis/${perfil}/permissoes`,
      { permissoes }
    );
  }
}
