import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AmbienteService, Previsao } from './ambiente.service';
@Component({
  selector: 'app-ambiente',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
  ],
  templateUrl: './ambiente.component.html',
  styleUrl: './ambiente.component.scss',
})
export class AmbienteComponent {
  private readonly service = inject(AmbienteService);
  protected tipo: 'meteorologia' | 'maritimo' = 'meteorologia';
  protected readonly hoje = new Date().toISOString().slice(0, 10);
  protected readonly limite = new Date(Date.now() + 15 * 86400000).toISOString().slice(0, 10);
  protected inicio = this.hoje;
  protected fim = this.hoje;
  protected readonly carregando = signal(false);
  protected readonly erro = signal('');
  protected readonly resultado = signal<Previsao | null>(null);
  protected readonly tipoResultado = signal<'meteorologia' | 'maritimo'>('meteorologia');
  protected colunasResultado() {
    return this.tipoResultado() === 'meteorologia'
      ? [
          { campo: 'temperature_2m', nome: 'Temperatura' },
          { campo: 'wind_speed_10m', nome: 'Vento' },
          { campo: 'wind_direction_10m', nome: 'Direção do vento' },
          { campo: 'wind_gusts_10m', nome: 'Rajadas' },
          { campo: 'precipitation_probability', nome: 'Probabilidade de precipitação' },
        ]
      : [
          { campo: 'wave_height', nome: 'Altura das ondas' },
          { campo: 'wave_direction', nome: 'Direção das ondas' },
          { campo: 'wave_period', nome: 'Período das ondas' },
          { campo: 'swell_wave_height', nome: 'Altura da ondulação' },
          { campo: 'swell_wave_direction', nome: 'Direção da ondulação' },
        ];
  }
  protected consultar() {
    const tipo = this.tipo;
    this.carregando.set(true);
    this.erro.set('');
    this.resultado.set(null);
    this.service.consultar(tipo, this.inicio, this.fim).subscribe({
      next: (r) => {
        this.tipoResultado.set(tipo);
        this.resultado.set(r);
        this.carregando.set(false);
      },
      error: (e) => {
        this.erro.set(e.error?.mensagem || 'Não foi possível consultar o provedor.');
        this.carregando.set(false);
      },
    });
  }
}
