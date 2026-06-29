import { Injectable } from '@angular/core';

import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import { Observable } from 'rxjs';

import { environment } from
  '../../../../environments/environment';

import { Pagina } from
  '../../../shared/modelos/pagina.model';

import {
  AlterarStatusEmbarcacao,
  ConsultaEmbarcacoes,
  DadosEmbarcacao,
  Embarcacao
} from '../models/embarcacao.model';

@Injectable({
  providedIn: 'root'
})
export class EmbarcacaoService {

  private readonly endpoint =
    `${environment.apiUrl}/embarcacoes`;

  constructor(
    private readonly http: HttpClient
  ) {
  }

  listar(
    consulta: ConsultaEmbarcacoes
  ): Observable<Pagina<Embarcacao>> {

    let parametros = new HttpParams()
      .set('page', consulta.pagina)
      .set('size', consulta.tamanho)
      .set('sort', 'nome,asc');

    /*
     * O controller processa um filtro principal
     * por vez, nesta ordem:
     * nome, proprietário, tipo e status.
     */
    if (consulta.nome?.trim()) {
      parametros = parametros.set(
        'nome',
        consulta.nome.trim()
      );
    } else if (consulta.proprietarioId) {
      parametros = parametros.set(
        'proprietarioId',
        consulta.proprietarioId
      );
    } else if (consulta.tipo) {
      parametros = parametros.set(
        'tipo',
        consulta.tipo
      );
    } else if (consulta.ativa !== undefined) {
      parametros = parametros.set(
        'ativa',
        consulta.ativa
      );
    }

    return this.http.get<
      Pagina<Embarcacao>
    >(
      this.endpoint,
      {
        params: parametros
      }
    );
  }

  buscarPorId(
    id: string
  ): Observable<Embarcacao> {
    return this.http.get<Embarcacao>(
      `${this.endpoint}/${id}`
    );
  }

  criar(
    dados: DadosEmbarcacao
  ): Observable<Embarcacao> {
    return this.http.post<Embarcacao>(
      this.endpoint,
      dados
    );
  }

  atualizar(
    id: string,
    dados: DadosEmbarcacao
  ): Observable<Embarcacao> {
    return this.http.put<Embarcacao>(
      `${this.endpoint}/${id}`,
      dados
    );
  }

  alterarStatus(
    id: string,
    ativa: boolean
  ): Observable<Embarcacao> {
    const dados: AlterarStatusEmbarcacao = {
      ativa
    };

    return this.http.patch<Embarcacao>(
      `${this.endpoint}/${id}/status`,
      dados
    );
  }
}