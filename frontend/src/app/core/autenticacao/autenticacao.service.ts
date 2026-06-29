import {
  Injectable,
  signal
} from '@angular/core';

import {
  HttpClient,
  HttpHeaders
} from '@angular/common/http';

import {
  Observable,
  tap
} from 'rxjs';

import { environment } from
  '../../../environments/environment';

import {
  RespostaLogin,
  SolicitacaoLogin,
  UsuarioAutenticado
} from './autenticacao.model';

@Injectable({
  providedIn: 'root'
})
export class AutenticacaoService {

  private readonly chaveToken =
    'caisora.token';

  private readonly chaveExpiracao =
    'caisora.expiracao';

  private readonly chaveUsuario =
    'caisora.usuario';

  private readonly endpoint =
    `${environment.apiUrl}/autenticacao`;

  private readonly usuarioSignal =
    signal<UsuarioAutenticado | null>(
      this.carregarUsuario()
    );

  readonly usuarioAtual =
    this.usuarioSignal.asReadonly();

  constructor(
    private readonly http: HttpClient
  ) {
  }

  autenticar(
    organizacaoId: string,
    solicitacao: SolicitacaoLogin
  ): Observable<RespostaLogin> {

    const headers = new HttpHeaders({
      'X-Organizacao-Id': organizacaoId
    });

    return this.http.post<RespostaLogin>(
      `${this.endpoint}/login`,
      solicitacao,
      {
        headers
      }
    ).pipe(
      tap((resposta) =>
        this.salvarSessao(resposta)
      )
    );
  }

  obterToken(): string | null {
    const token =
      localStorage.getItem(this.chaveToken);

    const expiracaoTexto =
      localStorage.getItem(this.chaveExpiracao);

    if (!token || !expiracaoTexto) {
      return null;
    }

    const expiracao =
      Number(expiracaoTexto);

    if (
      Number.isNaN(expiracao)
      || Date.now() >= expiracao
    ) {
      this.encerrarSessao();

      return null;
    }

    return token;
  }

  estaAutenticado(): boolean {
    return this.obterToken() !== null;
  }

  encerrarSessao(): void {
    localStorage.removeItem(this.chaveToken);
    localStorage.removeItem(this.chaveExpiracao);
    localStorage.removeItem(this.chaveUsuario);

    this.usuarioSignal.set(null);
  }

  private salvarSessao(
    resposta: RespostaLogin
  ): void {
    const expiracao =
      Date.now() + resposta.expiraEm * 1000;

    localStorage.setItem(
      this.chaveToken,
      resposta.tokenAcesso
    );

    localStorage.setItem(
      this.chaveExpiracao,
      expiracao.toString()
    );

    localStorage.setItem(
      this.chaveUsuario,
      JSON.stringify(resposta.usuario)
    );

    this.usuarioSignal.set(resposta.usuario);
  }

  private carregarUsuario():
    UsuarioAutenticado | null {

    const usuarioJson =
      localStorage.getItem(this.chaveUsuario);

    if (!usuarioJson) {
      return null;
    }

    try {
      return JSON.parse(
        usuarioJson
      ) as UsuarioAutenticado;
    } catch {
      localStorage.removeItem(
        this.chaveUsuario
      );

      return null;
    }
  }
}