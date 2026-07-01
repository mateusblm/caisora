import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  environment
} from '../../../../environments/environment';
import {
  PainelTvOperacional
} from '../models/painel-tv.model';

@Injectable({
  providedIn: 'root'
})
export class PainelTvService {

  private readonly endpoint =
    `${environment.apiUrl}/movimentacoes/painel-tv`;

  constructor(
    private readonly http: HttpClient
  ) {
  }

  buscar(): Observable<PainelTvOperacional> {
    return this.http.get<PainelTvOperacional>(
      this.endpoint
    );
  }
}
