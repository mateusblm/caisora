import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
export type GrupoReferencia = 'paises' | 'municipios' | 'feriados' | 'mares';
export interface ConsultaReferencia {
  origem: string;
  fonte: string;
  consultadoEm: string;
  incompleta: boolean;
  registros: Record<string, string | number | null>[];
  datumSolicitado?: string | null;
  datumResposta?: string | null;
  copyright?: string;
  latitudeResposta?: number | null;
  longitudeResposta?: number | null;
}
export interface DisponibilidadeMares {
  habilitada: boolean;
  mensagem: string;
}
@Injectable({ providedIn: 'root' })
export class ReferenciasService {
  private readonly http = inject(HttpClient);
  consultar(grupo: GrupoReferencia, params: Record<string, string>) {
    return this.http.get<ConsultaReferencia>(`${environment.apiUrl}/referencias/${grupo}`, {
      params,
    });
  }
  disponibilidadeMares() {
    return this.http.get<DisponibilidadeMares>(
      `${environment.apiUrl}/referencias/mares/disponibilidade`,
    );
  }
}
