import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

interface ItemChecklist {
  id: string;
  descricao: string;
  concluido: boolean;
}

@Component({
  selector: 'app-checklist-saida',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatIconModule
  ],
  templateUrl: './checklist-saida.component.html',
  styleUrl: './checklist-saida.component.scss'
})
export class ChecklistSaidaComponent {
  protected readonly itens = signal<ItemChecklist[]>([
    { id: 'casco', descricao: 'Conferir estado externo do casco', concluido: false },
    { id: 'combustivel', descricao: 'Conferir nível de combustível informado', concluido: false },
    { id: 'pertences', descricao: 'Conferir pertences e itens soltos', concluido: false },
    { id: 'foto', descricao: 'Registrar evidência fotográfica da saída', concluido: false },
    { id: 'entrega', descricao: 'Confirmar responsável pela entrega', concluido: false }
  ]);

  protected readonly concluido = signal(false);

  protected alterarItem(id: string, valor: boolean): void {
    this.itens.update((itens) =>
      itens.map((item) => item.id === id ? { ...item, concluido: valor } : item)
    );
    this.concluido.set(false);
  }

  protected podeConcluir(): boolean {
    return this.itens().every((item) => item.concluido);
  }

  protected concluir(): void {
    if (this.podeConcluir()) {
      this.concluido.set(true);
    }
  }
}
