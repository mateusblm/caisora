import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
export interface OcupacaoRelatorio {
  totalVagasAtivas: number;
  ocupadas: number;
  disponiveis: number;
  taxaOcupacao: number;
  grupos: {
    setor: string | null;
    tipo: string;
    totalVagasAtivas: number;
    ocupadas: number;
    disponiveis: number;
    taxaOcupacao: number;
  }[];
}
export interface MovimentacoesRelatorio {
  total: number;
  porSituacao: Record<string, number>;
  concluidas: number;
  canceladas: number;
  tempoMedioExecucaoSegundos: number | null;
  amostrasDuracao: number;
  concluidasNoPrazo: number;
  percentualNoPrazo: number;
  regraPontualidade: string;
}
export interface ContratosRelatorio {
  total: number;
  porSituacao: Record<string, number>;
  percentualPorSituacao: Record<string, number>;
  porPeriodicidade: Record<string, number>;
  valoresBasePorPeriodicidade: Record<string, number>;
}
@Injectable({ providedIn: 'root' })
export class RelatoriosService {
  private readonly http = inject(HttpClient);
  ocupacao(params: Record<string, string>) {
    return this.http.get<OcupacaoRelatorio>(`${environment.apiUrl}/relatorios/ocupacao`, {
      params,
    });
  }
  movimentacoes(params: Record<string, string>) {
    return this.http.get<MovimentacoesRelatorio>(`${environment.apiUrl}/relatorios/movimentacoes`, {
      params,
    });
  }
  contratos(params: Record<string, string>) {
    return this.http.get<ContratosRelatorio>(`${environment.apiUrl}/relatorios/contratos`, {
      params,
    });
  }
}
