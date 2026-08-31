import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  AcaoSistema,
  ModuloSistema,
  PerfilUsuario
} from '../../../../core/autenticacao/autenticacao.model';
import { AutenticacaoService } from '../../../../core/autenticacao/autenticacao.service';
import {
  ConfiguracaoVariabilidade,
  PerfilConfiguracao
} from '../../models/variabilidade.model';
import { VariabilidadeService } from '../../services/variabilidade.service';

@Component({
  selector: 'app-configuracao-variabilidade',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSlideToggleModule
  ],
  templateUrl: './configuracao-variabilidade.component.html',
  styleUrl: './configuracao-variabilidade.component.scss'
})
export class ConfiguracaoVariabilidadeComponent implements OnInit {
  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly mensagem = signal<string | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly configuracao = signal<ConfiguracaoVariabilidade | null>(null);
  protected readonly perfilSelecionado = signal<PerfilUsuario>('ATENDENTE');
  protected readonly permissoesSelecionadas = signal<Set<string>>(new Set());



  protected readonly perfilAtual = computed<PerfilConfiguracao | undefined>(() =>
    this.configuracao()?.perfis.find(
      (perfil) => perfil.perfil === this.perfilSelecionado()
    )
  );

  constructor(
    private readonly variabilidadeService: VariabilidadeService,
    private readonly autenticacaoService: AutenticacaoService
  ) {}

  ngOnInit(): void {
    this.carregar();
  }

  protected alterarModulo(modulo: ModuloSistema, ativo: boolean): void {
    this.salvando.set(true);
    this.limparMensagens();

    this.variabilidadeService.atualizarModulo(modulo, ativo).subscribe({
      next: (configuracao) => {
        this.aplicarConfiguracao(configuracao);
        this.atualizarSessao();
        this.mensagem.set('Configuração de módulo atualizada.');
        this.salvando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível atualizar o módulo.');
        this.salvando.set(false);
      }
    });
  }

  protected selecionarPerfil(perfil: PerfilUsuario): void {
    this.perfilSelecionado.set(perfil);
    this.sincronizarPermissoesSelecionadas();
  }

  protected chavePermissao(modulo: ModuloSistema, acao: AcaoSistema): string {
    return `${modulo}:${acao}`;
  }

  protected possuiPermissao(modulo: ModuloSistema, acao: AcaoSistema): boolean {
    return this.permissoesSelecionadas().has(this.chavePermissao(modulo, acao));
  }

  protected alternarPermissao(
    modulo: ModuloSistema,
    acao: AcaoSistema,
    permitido: boolean
  ): void {
    const novaLista = new Set(this.permissoesSelecionadas());
    const chave = this.chavePermissao(modulo, acao);

    if (permitido) {
      novaLista.add(chave);
    } else {
      novaLista.delete(chave);
    }

    this.permissoesSelecionadas.set(novaLista);
  }

  protected salvarPermissoes(): void {
    this.salvando.set(true);
    this.limparMensagens();

    this.variabilidadeService.atualizarPermissoes(
      this.perfilSelecionado(),
      Array.from(this.permissoesSelecionadas())
    ).subscribe({
      next: (configuracao) => {
        this.aplicarConfiguracao(configuracao);
        this.atualizarSessao();
        this.mensagem.set('Permissões do perfil atualizadas.');
        this.salvando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível atualizar as permissões.');
        this.salvando.set(false);
      }
    });
  }


  protected rotuloAcao(acao: AcaoSistema): string {
    switch (acao) {
      case 'VISUALIZAR': return 'Visualizar';
      case 'CRIAR': return 'Criar';
      case 'EDITAR': return 'Editar';
      case 'ALTERAR_STATUS': return 'Alterar status';
      case 'INICIAR': return 'Iniciar';
      case 'CONCLUIR': return 'Concluir';
      case 'CANCELAR': return 'Cancelar';
      case 'CONFIGURAR': return 'Configurar';
    }
  }

  protected nomePerfil(perfil: PerfilUsuario): string {
    switch (perfil) {
      case 'ADMINISTRADOR_MARINA': return 'Administrador da marina';
      case 'GERENTE': return 'Gerente';
      case 'ATENDENTE': return 'Atendente';
      case 'FINANCEIRO': return 'Financeiro';
      case 'ADMINISTRADOR_PLATAFORMA': return 'Administrador da plataforma';
    }
  }

  private carregar(): void {
    this.carregando.set(true);
    this.variabilidadeService.buscarConfiguracao().subscribe({
      next: (configuracao) => {
        this.aplicarConfiguracao(configuracao);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar a configuração da marina.');
        this.carregando.set(false);
      }
    });
  }

  private aplicarConfiguracao(configuracao: ConfiguracaoVariabilidade): void {
    this.configuracao.set(configuracao);
    const perfilExiste = configuracao.perfis.some(
      (perfil) => perfil.perfil === this.perfilSelecionado()
    );

    if (!perfilExiste && configuracao.perfis.length > 0) {
      this.perfilSelecionado.set(configuracao.perfis[0].perfil);
    }

    this.sincronizarPermissoesSelecionadas();
  }

  private sincronizarPermissoesSelecionadas(): void {
    const perfil = this.configuracao()?.perfis.find(
      (item) => item.perfil === this.perfilSelecionado()
    );
    this.permissoesSelecionadas.set(new Set(perfil?.permissoes ?? []));
  }

  private atualizarSessao(): void {
    this.autenticacaoService.atualizarUsuarioAtual().subscribe({
      error: () => {
        // A configuração administrativa já foi salva; a sessão será atualizada no próximo acesso.
      }
    });
  }

  private limparMensagens(): void {
    this.mensagem.set(null);
    this.erro.set(null);
  }
}
