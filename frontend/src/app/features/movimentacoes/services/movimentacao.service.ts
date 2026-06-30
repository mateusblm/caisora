import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  EMPTY,
  Observable,
  expand,
  forkJoin,
  map,
  reduce
} from 'rxjs';

import { environment } from '../../../../environments/environment';
import { Pagina } from '../../../shared/modelos/pagina.model';
import {
  AtualizarMovimentacao,
  CancelarMovimentacao,
  ConcluirMovimentacao,
  ConsultaHistoricoMovimentacao,
  ConsultaMovimentacoes,
  CriarMovimentacao,
  HistoricoMovimentacao,
  IniciarMovimentacao,
  Movimentacao,
  PosicaoEmbarcacao,
  StatusMovimentacao,
  UsuarioOperador
} from '../models/movimentacao.model';

@Injectable({
  providedIn: 'root'
})
export class MovimentacaoService {

  private readonly endpoint =
    `${environment.apiUrl}/movimentacoes`;

  private readonly endpointEmbarcacoes =
    `${environment.apiUrl}/embarcacoes`;

  private readonly endpointUsuarios =
    `${environment.apiUrl}/usuarios`;

  constructor(
    private readonly http: HttpClient
  ) {
  }

  listar(
    consulta: ConsultaMovimentacoes
  ): Observable<Pagina<Movimentacao>> {
    let parametros = new HttpParams()
      .set('page', consulta.pagina)
      .set('size', consulta.tamanho)
      .set('sort', 'agendadaPara,asc');

    /*
     * O backend aceita somente um filtro funcional
     * por chamada. O frontend utiliza esta prioridade:
     *
     * status → tipo → embarcacaoId → período
     */
    if (consulta.status) {
      parametros = parametros.set(
        'status',
        consulta.status
      );
    } else if (consulta.tipo) {
      parametros = parametros.set(
        'tipo',
        consulta.tipo
      );
    } else if (consulta.embarcacaoId) {
      parametros = parametros.set(
        'embarcacaoId',
        consulta.embarcacaoId
      );
    } else if (consulta.inicio && consulta.fim) {
      parametros = parametros
        .set('inicio', consulta.inicio)
        .set('fim', consulta.fim);
    }

    return this.http.get<Pagina<Movimentacao>>(
      this.endpoint,
      { params: parametros }
    );
  }

  listarTodasAbertas():
    Observable<Movimentacao[]> {
    return forkJoin({
      agendadas:
        this.listarTodasPorStatus('AGENDADA'),
      emExecucao:
        this.listarTodasPorStatus('EM_EXECUCAO')
    }).pipe(
      map(
        ({ agendadas, emExecucao }) => [
          ...agendadas,
          ...emExecucao
        ]
      )
    );
  }

  listarUsuariosAtivos():
    Observable<UsuarioOperador[]> {
    const tamanho = 100;

    const listarPagina = (
      pagina: number
    ): Observable<Pagina<UsuarioOperador>> => {
      const parametros = new HttpParams()
        .set('page', pagina)
        .set('size', tamanho)
        .set('sort', 'nome,asc');

      return this.http.get<Pagina<UsuarioOperador>>(
        this.endpointUsuarios,
        { params: parametros }
      );
    };

    return listarPagina(0).pipe(
      expand(
        (pagina) =>
          pagina.last
            ? EMPTY
            : listarPagina(pagina.number + 1)
      ),
      map((pagina) => pagina.content),
      reduce(
        (todos, paginaAtual) => [
          ...todos,
          ...paginaAtual
        ],
        [] as UsuarioOperador[]
      ),
      map(
        (usuarios) =>
          usuarios
            .filter((usuario) => usuario.ativo)
            .sort(
              (primeiro, segundo) =>
                primeiro.nome.localeCompare(
                  segundo.nome,
                  'pt-BR'
                )
            )
      )
    );
  }

  buscarPorId(
    id: string
  ): Observable<Movimentacao> {
    return this.http.get<Movimentacao>(
      `${this.endpoint}/${id}`
    );
  }

  criar(
    dados: CriarMovimentacao
  ): Observable<Movimentacao> {
    return this.http.post<Movimentacao>(
      this.endpoint,
      dados
    );
  }

  atualizar(
    id: string,
    dados: AtualizarMovimentacao
  ): Observable<Movimentacao> {
    return this.http.put<Movimentacao>(
      `${this.endpoint}/${id}`,
      dados
    );
  }

  iniciar(
    id: string,
    dados: IniciarMovimentacao
  ): Observable<Movimentacao> {
    return this.http.patch<Movimentacao>(
      `${this.endpoint}/${id}/inicio`,
      dados
    );
  }

  concluir(
    id: string,
    dados: ConcluirMovimentacao
  ): Observable<Movimentacao> {
    return this.http.patch<Movimentacao>(
      `${this.endpoint}/${id}/conclusao`,
      dados
    );
  }

  cancelar(
    id: string,
    dados: CancelarMovimentacao
  ): Observable<Movimentacao> {
    return this.http.patch<Movimentacao>(
      `${this.endpoint}/${id}/cancelamento`,
      dados
    );
  }

  listarHistorico(
    movimentacaoId: string,
    consulta: ConsultaHistoricoMovimentacao
  ): Observable<Pagina<HistoricoMovimentacao>> {
    const parametros = new HttpParams()
      .set('page', consulta.pagina)
      .set('size', consulta.tamanho)
      .set('sort', 'ocorridoEm,desc');

    return this.http.get<Pagina<HistoricoMovimentacao>>(
      `${this.endpoint}/${movimentacaoId}/historico`,
      { params: parametros }
    );
  }

  buscarPosicaoEmbarcacao(
    embarcacaoId: string
  ): Observable<PosicaoEmbarcacao> {
    return this.http.get<PosicaoEmbarcacao>(
      `${this.endpointEmbarcacoes}/`
      + `${embarcacaoId}/posicao`
    );
  }

  private listarTodasPorStatus(
    status: StatusMovimentacao
  ): Observable<Movimentacao[]> {
    const tamanho = 100;

    return this.listar({
      pagina: 0,
      tamanho,
      status
    }).pipe(
      expand(
        (pagina) =>
          pagina.last
            ? EMPTY
            : this.listar({
              pagina: pagina.number + 1,
              tamanho,
              status
            })
      ),
      map((pagina) => pagina.content),
      reduce(
        (todas, paginaAtual) => [
          ...todas,
          ...paginaAtual
        ],
        [] as Movimentacao[]
      )
    );
  }
}
