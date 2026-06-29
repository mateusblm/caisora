import {
  Injectable
} from '@angular/core';

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

import {
  environment
} from '../../../../environments/environment';

import {
  Pagina
} from '../../../shared/modelos/pagina.model';

import {
  AtualizarOcupacao,
  ConsultaOcupacoes,
  CriarOcupacao,
  EncerrarOcupacao,
  Ocupacao
} from '../models/ocupacao.model';

@Injectable({
  providedIn: 'root'
})
export class OcupacaoService {

  private readonly endpoint =
    `${environment.apiUrl}/ocupacoes`;

  constructor(
    private readonly http: HttpClient
  ) {
  }

  listar(
    consulta: ConsultaOcupacoes
  ): Observable<Pagina<Ocupacao>> {
    let parametros = new HttpParams()
      .set(
        'page',
        consulta.pagina
      )
      .set(
        'size',
        consulta.tamanho
      )
      .set(
        'sort',
        'inicioEm,desc'
      );

    /*
     * O controller processa somente um
     * filtro funcional por chamada, nesta
     * prioridade:
     *
     * embarcacaoId → vagaId → status
     */
    if (consulta.embarcacaoId) {
      parametros = parametros.set(
        'embarcacaoId',
        consulta.embarcacaoId
      );
    } else if (consulta.vagaId) {
      parametros = parametros.set(
        'vagaId',
        consulta.vagaId
      );
    } else if (consulta.status) {
      parametros = parametros.set(
        'status',
        consulta.status
      );
    }

    return this.http.get<Pagina<Ocupacao>>(
      this.endpoint,
      {
        params: parametros
      }
    );
  }

  listarTodasAtivas(): Observable<Ocupacao[]> {
    const tamanho = 100;

    return this.listar({
        pagina: 0,
        tamanho,
        status: 'ATIVA'
    }).pipe(
        expand((pagina) =>
        pagina.last
            ? EMPTY
            : this.listar({
                pagina: pagina.number + 1,
                tamanho,
                status: 'ATIVA'
            })
        ),

        map((pagina) => pagina.content),

        reduce(
        (todas, paginaAtual) => [
            ...todas,
            ...paginaAtual
        ],
        [] as Ocupacao[]
        )
    );
    }

  buscarPorId(
    id: string
  ): Observable<Ocupacao> {
    return this.http.get<Ocupacao>(
      `${this.endpoint}/${id}`
    );
  }

  criar(
    dados: CriarOcupacao
  ): Observable<Ocupacao> {
    return this.http.post<Ocupacao>(
      this.endpoint,
      dados
    );
  }

  atualizar(
    id: string,
    dados: AtualizarOcupacao
  ): Observable<Ocupacao> {
    return this.http.put<Ocupacao>(
      `${this.endpoint}/${id}`,
      dados
    );
  }

  encerrar(
    id: string,
    encerradaEm: string
  ): Observable<Ocupacao> {
    const dados: EncerrarOcupacao = {
      encerradaEm
    };

    return this.http.patch<Ocupacao>(
      `${this.endpoint}/${id}/encerramento`,
      dados
    );
  }
}