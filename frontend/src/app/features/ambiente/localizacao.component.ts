import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AmbienteService } from './ambiente.service';
@Component({
  selector: 'app-localizacao',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './localizacao.component.html',
  styleUrl: './localizacao.component.scss',
})
export class LocalizacaoComponent {
  private readonly service = inject(AmbienteService);
  protected latitude: number | null = null;
  protected longitude: number | null = null;
  protected readonly carregando = signal(true);
  protected readonly mensagem = signal('');
  constructor() {
    this.service.coordenadas().subscribe({
      next: (c) => {
        this.latitude = c.latitude;
        this.longitude = c.longitude;
        this.carregando.set(false);
      },
      error: () => {
        this.mensagem.set('Não foi possível consultar a localização.');
        this.carregando.set(false);
      },
    });
  }
  protected salvar() {
    this.carregando.set(true);
    this.service.salvar({ latitude: this.latitude, longitude: this.longitude }).subscribe({
      next: () => {
        this.mensagem.set('Localização salva.');
        this.carregando.set(false);
      },
      error: (e) => {
        this.mensagem.set(e.error?.mensagem || 'Não foi possível salvar.');
        this.carregando.set(false);
      },
    });
  }
}
