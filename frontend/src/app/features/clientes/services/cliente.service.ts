import { Injectable } from '@angular/core';

import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import {
  EMPTY,
  expand,
  Observable,
  reduce
} from 'rxjs';

import { environment } from
  '../../../../environments/environment';

import { Pagina } from
  '../../../shared/modelos/pagina.model';

import {
  AlterarStatusCliente,
  Cliente,
  ConsultaClientes,
  DadosCliente
} from '../models/cliente.model';

@Injectable({
  providedIn: 'root'
})
export class ClienteService {

  private readonly endpoint =
    `${environment.apiUrl}/clientes`;

  constructor(
    private readonly http: HttpClient
  ) {
  }

  listar(
    consulta: ConsultaClientes
  ): Observable<Pagina<Cliente>> {

    let parametros = new HttpParams()
      .set('page', consulta.pagina)
      .set('size', consulta.tamanho)
      .set('sort', 'nome,asc');

    /*
     * O controller atual prioriza nome sobre status.
     * Por isso enviamos apenas um dos filtros.
     */
    if (consulta.nome?.trim()) {
      parametros = parametros.set(
        'nome',
        consulta.nome.trim()
      );
    } else if (consulta.ativo !== undefined) {
      parametros = parametros.set(
        'ativo',
        consulta.ativo
      );
    }

    return this.http.get<Pagina<Cliente>>(
      this.endpoint,
      {
        params: parametros
      }
    );
  }

  buscarPorId(
    id: string
  ): Observable<Cliente> {
    return this.http.get<Cliente>(
      `${this.endpoint}/${id}`
    );
  }

  criar(
    dados: DadosCliente
  ): Observable<Cliente> {
    return this.http.post<Cliente>(
      this.endpoint,
      dados
    );
  }

  atualizar(
    id: string,
    dados: DadosCliente
  ): Observable<Cliente> {
    return this.http.put<Cliente>(
      `${this.endpoint}/${id}`,
      dados
    );
  }

  alterarStatus(
    id: string,
    ativo: boolean
  ): Observable<Cliente> {
    const dados: AlterarStatusCliente = {
      ativo
    };

    return this.http.patch<Cliente>(
      `${this.endpoint}/${id}/status`,
      dados
    );
  }

    listarTodosAtivos(): Observable<Cliente[]> {
    const tamanhoPagina = 100;

    return this.listar({
        pagina: 0,
        tamanho: tamanhoPagina,
        ativo: true
    }).pipe(
        expand((pagina) =>
        pagina.last
            ? EMPTY
            : this.listar({
                pagina: pagina.number + 1,
                tamanho: tamanhoPagina,
                ativo: true
            })
        ),

        reduce(
        (clientes, pagina) => [
            ...clientes,
            ...pagina.content
        ],
        [] as Cliente[]
        )
    );
    }
}