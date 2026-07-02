import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../../environments/environment";
import { Pagina } from "../../../shared/modelos/pagina.model";
import {
  AtivarContrato,
  ConsultaContratos,
  Contrato,
  DadosContrato,
  EncerrarContrato,
  HistoricoContrato,
  MotivoContrato,
  VinculoContratoOcupacao,
} from "../models/contrato.model";

@Injectable({ providedIn: "root" })
export class ContratoService {
  private readonly endpoint = `${environment.apiUrl}/contratos`;

  constructor(private readonly http: HttpClient) {}

  listar(consulta: ConsultaContratos): Observable<Pagina<Contrato>> {
    let parametros = new HttpParams()
      .set("page", consulta.pagina)
      .set("size", consulta.tamanho)
      .set("sort", "criadoEm,desc");

    if (consulta.status) {
      parametros = parametros.set("status", consulta.status);
    }
    if (consulta.clienteId) {
      parametros = parametros.set("clienteId", consulta.clienteId);
    }
    if (consulta.embarcacaoId) {
      parametros = parametros.set("embarcacaoId", consulta.embarcacaoId);
    }

    return this.http.get<Pagina<Contrato>>(this.endpoint, {
      params: parametros,
    });
  }

  buscarPorId(id: string): Observable<Contrato> {
    return this.http.get<Contrato>(`${this.endpoint}/${id}`);
  }

  criar(dados: DadosContrato): Observable<Contrato> {
    return this.http.post<Contrato>(this.endpoint, dados);
  }

  atualizar(id: string, dados: DadosContrato): Observable<Contrato> {
    return this.http.put<Contrato>(`${this.endpoint}/${id}`, dados);
  }

  enviarParaAssinatura(
    id: string,
    descricao: string | null,
  ): Observable<Contrato> {
    return this.executarAcaoComMotivo(id, "enviar-para-assinatura", descricao);
  }

  ativar(id: string, dados: AtivarContrato): Observable<Contrato> {
    return this.http.post<Contrato>(`${this.endpoint}/${id}/ativar`, dados);
  }

  suspender(id: string, descricao: string | null): Observable<Contrato> {
    return this.executarAcaoComMotivo(id, "suspender", descricao);
  }

  reativar(id: string, descricao: string | null): Observable<Contrato> {
    return this.executarAcaoComMotivo(id, "reativar", descricao);
  }

  solicitarEncerramento(
    id: string,
    descricao: string | null,
  ): Observable<Contrato> {
    return this.executarAcaoComMotivo(id, "solicitar-encerramento", descricao);
  }

  encerrar(id: string, dados: EncerrarContrato): Observable<Contrato> {
    return this.http.post<Contrato>(`${this.endpoint}/${id}/encerrar`, dados);
  }

  cancelar(id: string, descricao: string | null): Observable<Contrato> {
    return this.executarAcaoComMotivo(id, "cancelar", descricao);
  }

  listarOcupacoes(contratoId: string): Observable<VinculoContratoOcupacao[]> {
    return this.http.get<VinculoContratoOcupacao[]>(
      `${this.endpoint}/${contratoId}/ocupacoes`,
    );
  }

  vincularOcupacao(
    contratoId: string,
    ocupacaoId: string,
  ): Observable<VinculoContratoOcupacao> {
    return this.http.post<VinculoContratoOcupacao>(
      `${this.endpoint}/${contratoId}/ocupacoes`,
      { ocupacaoId },
    );
  }

  desvincularOcupacao(
    contratoId: string,
    vinculoId: string,
    motivo: string | null,
  ): Observable<VinculoContratoOcupacao> {
    let parametros = new HttpParams();
    if (motivo?.trim()) {
      parametros = parametros.set("motivo", motivo.trim());
    }

    return this.http.delete<VinculoContratoOcupacao>(
      `${this.endpoint}/${contratoId}/ocupacoes/${vinculoId}`,
      { params: parametros },
    );
  }

  listarHistorico(
    contratoId: string,
    pagina = 0,
    tamanho = 50,
  ): Observable<Pagina<HistoricoContrato>> {
    const parametros = new HttpParams()
      .set("page", pagina)
      .set("size", tamanho)
      .set("sort", "realizadoEm,desc");

    return this.http.get<Pagina<HistoricoContrato>>(
      `${this.endpoint}/${contratoId}/historico`,
      { params: parametros },
    );
  }

  private executarAcaoComMotivo(
    id: string,
    acao: string,
    descricao: string | null,
  ): Observable<Contrato> {
    const dados: MotivoContrato = {
      descricao: descricao?.trim() || null,
    };

    return this.http.post<Contrato>(`${this.endpoint}/${id}/${acao}`, dados);
  }
}
