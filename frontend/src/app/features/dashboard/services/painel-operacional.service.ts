import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  environment
} from '../../../../environments/environment';
import {
  PainelOperacional
} from '../models/painel-operacional.model';

@Injectable({
  providedIn: 'root'
})
export class PainelOperacionalService {

  private readonly endpoint =
    `${environment.apiUrl}`
    + '/movimentacoes/painel-operacional';

  constructor(
    private readonly http: HttpClient
  ) {
  }

  buscar(): Observable<PainelOperacional> {
    return this.http.get<PainelOperacional>(
      this.endpoint
    );
  }
}
