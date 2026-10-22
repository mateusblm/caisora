import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
export interface Coordenadas {
  latitude: number | null;
  longitude: number | null;
}
export interface Previsao {
  origem: string;
  fonte: string;
  consultadoEm: string;
  fusoHorario: string;
  atualizacaoProvedor: string;
  incompleta: boolean;
  unidades: Record<string, string>;
  horas: { horario: string; valores: Record<string, number | null> }[];
}
@Injectable({ providedIn: 'root' })
export class AmbienteService {
  private readonly http = inject(HttpClient);
  coordenadas() {
    return this.http.get<Coordenadas>(`${environment.apiUrl}/minha-organizacao/coordenadas`);
  }
  salvar(coords: Coordenadas) {
    return this.http.put<Coordenadas>(
      `${environment.apiUrl}/minha-organizacao/coordenadas`,
      coords,
    );
  }
  consultar(tipo: 'meteorologia' | 'maritimo', inicio: string, fim: string) {
    return this.http.get<Previsao>(`${environment.apiUrl}/ambiente/${tipo}`, {
      params: { inicio, fim },
    });
  }
}
