import { Injectable } from '@angular/core';

import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import {
  EMPTY,
  Observable,
  expand,
  map,
  reduce
} from 'rxjs';

import { environment } from
  '../../../../environments/environment';

import { Pagina } from
  '../../../shared/modelos/pagina.model';

import {
  AlterarStatusVaga,
  ConsultaVagas,
  DadosVaga,
  Vaga
} from '../models/vaga.model';

@Injectable({
  providedIn: 'root'
})
export class VagaService {

  private readonly endpoint =
    `${environment.apiUrl}/vagas`;

  constructor(
    private readonly http: HttpClient
  ) {
  }

  listar(
    consulta: ConsultaVagas
  ): Observable<Pagina<Vaga>> {

    let parametros = new HttpParams()
      .set('page', consulta.pagina)
      .set('size', consulta.tamanho)
      .set('sort', 'codigo,asc');

    /*
     * O controller processa somente um filtro
     * por vez, nesta ordem: codigo, setor,
     * tipo e status.
     */
    if (consulta.codigo?.trim()) {
      parametros = parametros.set(
        'codigo',
        consulta.codigo.trim()
      );
    } else if (consulta.setor?.trim()) {
      parametros = parametros.set(
        'setor',
        consulta.setor.trim()
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

    return this.http.get<Pagina<Vaga>>(
      this.endpoint,
      {
        params: parametros
      }
    );
  }

  listarTodas(): Observable<Vaga[]> {
      const tamanho = 100;

      return this.listar({
        pagina: 0,
        tamanho
      }).pipe(
        expand((pagina) =>
          pagina.last
            ? EMPTY
            : this.listar({
                pagina: pagina.number + 1,
                tamanho
              })
        ),

        map((pagina) => pagina.content),

        reduce(
          (todas, paginaAtual) => [
            ...todas,
            ...paginaAtual
          ],
          [] as Vaga[]
        )
      );
    }

  buscarPorId(
    id: string
  ): Observable<Vaga> {
    return this.http.get<Vaga>(
      `${this.endpoint}/${id}`
    );
  }

  criar(
    dados: DadosVaga
  ): Observable<Vaga> {
    return this.http.post<Vaga>(
      this.endpoint,
      dados
    );
  }

  atualizar(
    id: string,
    dados: DadosVaga
  ): Observable<Vaga> {
    return this.http.put<Vaga>(
      `${this.endpoint}/${id}`,
      dados
    );
  }

  alterarStatus(
    id: string,
    ativa: boolean
  ): Observable<Vaga> {
    const dados: AlterarStatusVaga = {
      ativa
    };

    return this.http.patch<Vaga>(
      `${this.endpoint}/${id}/status`,
      dados
    );
  }
}
