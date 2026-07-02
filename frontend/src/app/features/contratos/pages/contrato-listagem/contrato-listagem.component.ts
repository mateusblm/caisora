import { BreakpointObserver } from "@angular/cdk/layout";
import { HttpErrorResponse } from "@angular/common/http";
import { Component, DestroyRef, OnInit, inject, signal } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { takeUntilDestroyed, toSignal } from "@angular/core/rxjs-interop";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatSelectModule } from "@angular/material/select";
import { RouterLink } from "@angular/router";
import { finalize, forkJoin, map } from "rxjs";
import { ErroApi } from "../../../../shared/modelos/erro-api.model";
import { Cliente } from "../../../clientes/models/cliente.model";
import { ClienteService } from "../../../clientes/services/cliente.service";
import { Embarcacao } from "../../../embarcacoes/models/embarcacao.model";
import { EmbarcacaoService } from "../../../embarcacoes/services/embarcacao.service";
import {
  Contrato,
  PeriodicidadeContrato,
  StatusContrato,
} from "../../models/contrato.model";
import { ContratoService } from "../../services/contrato.service";

@Component({
  selector: "app-contrato-listagem",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: "./contrato-listagem.component.html",
  styleUrl: "./contrato-listagem.component.scss",
})
export class ContratoListagemComponent implements OnInit {
  private readonly contratoService = inject(ContratoService);
  private readonly clienteService = inject(ClienteService);
  private readonly embarcacaoService = inject(EmbarcacaoService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly telaDesktop = toSignal(
    this.breakpointObserver
      .observe("(min-width: 980px)")
      .pipe(map((resultado) => resultado.matches)),
    { initialValue: false },
  );

  protected readonly filtroStatus = new FormControl<"TODOS" | StatusContrato>(
    "TODOS",
    { nonNullable: true },
  );
  protected readonly filtroClienteId = new FormControl("", {
    nonNullable: true,
  });
  protected readonly filtroEmbarcacaoId = new FormControl("", {
    nonNullable: true,
  });

  protected readonly contratos = signal<Contrato[]>([]);
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly embarcacoes = signal<Embarcacao[]>([]);
  protected readonly carregando = signal(false);
  protected readonly carregandoOpcoes = signal(false);
  protected readonly mensagemErro = signal<string | null>(null);
  protected readonly paginaAtual = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly totalElementos = signal(0);
  protected readonly tamanhoPagina = 10;

  protected readonly statusDisponiveis: StatusContrato[] = [
    "RASCUNHO",
    "PENDENTE_ASSINATURA",
    "ATIVO",
    "SUSPENSO",
    "EM_ENCERRAMENTO",
    "ENCERRADO",
    "CANCELADO",
  ];

  ngOnInit(): void {
    this.carregarOpcoes();
    this.carregarContratos();

    this.filtroClienteId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const embarcacaoSelecionada = this.embarcacoes().find(
          (embarcacao) => embarcacao.id === this.filtroEmbarcacaoId.value,
        );

        if (
          embarcacaoSelecionada &&
          this.filtroClienteId.value &&
          embarcacaoSelecionada.proprietarioId !== this.filtroClienteId.value
        ) {
          this.filtroEmbarcacaoId.setValue("");
        }
      });
  }

  protected aplicarFiltros(): void {
    this.carregarContratos(0);
  }

  protected limparFiltros(): void {
    this.filtroStatus.setValue("TODOS");
    this.filtroClienteId.setValue("");
    this.filtroEmbarcacaoId.setValue("");
    this.carregarContratos(0);
  }

  protected paginaAnterior(): void {
    if (this.paginaAtual() > 0) {
      this.carregarContratos(this.paginaAtual() - 1);
    }
  }

  protected proximaPagina(): void {
    if (this.paginaAtual() + 1 < this.totalPaginas()) {
      this.carregarContratos(this.paginaAtual() + 1);
    }
  }

  protected embarcacoesFiltradas(): Embarcacao[] {
    const clienteId = this.filtroClienteId.value;
    return this.embarcacoes().filter(
      (embarcacao) =>
        embarcacao.ativa &&
        (!clienteId || embarcacao.proprietarioId === clienteId),
    );
  }

  protected podeEditar(contrato: Contrato): boolean {
    return contrato.status === "RASCUNHO";
  }

  protected formatarStatus(status: StatusContrato): string {
    const rotulos: Record<StatusContrato, string> = {
      RASCUNHO: "Rascunho",
      PENDENTE_ASSINATURA: "Pendente de assinatura",
      ATIVO: "Ativo",
      SUSPENSO: "Suspenso",
      EM_ENCERRAMENTO: "Em encerramento",
      ENCERRADO: "Encerrado",
      CANCELADO: "Cancelado",
    };
    return rotulos[status];
  }

  protected formatarPeriodicidade(
    periodicidade: PeriodicidadeContrato,
  ): string {
    const rotulos: Record<PeriodicidadeContrato, string> = {
      DIARIA: "Diária",
      MENSAL: "Mensal",
      TRIMESTRAL: "Trimestral",
      SEMESTRAL: "Semestral",
      ANUAL: "Anual",
      PERSONALIZADA: "Personalizada",
    };
    return rotulos[periodicidade];
  }

  protected formatarTipoVaga(tipo: Contrato["tipoVagaContratada"]): string {
    const rotulos: Record<Contrato["tipoVagaContratada"], string> = {
      MOLHADA: "Vaga molhada",
      SECA: "Vaga seca",
      POITA: "Poita",
      OUTRA: "Outra",
    };
    return rotulos[tipo];
  }

  protected formatarMoeda(valor: number): string {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(valor);
  }

  protected formatarData(valor: string | null): string {
    if (!valor) {
      return "Sem término definido";
    }
    return new Intl.DateTimeFormat("pt-BR", {
      timeZone: "UTC",
    }).format(new Date(`${valor}T00:00:00Z`));
  }

  protected classeStatus(status: StatusContrato): string {
    return `status status--${status.toLowerCase()}`;
  }

  private carregarOpcoes(): void {
    this.carregandoOpcoes.set(true);
    forkJoin({
      clientes: this.clienteService.listarTodosAtivos(),
      embarcacoes: this.embarcacaoService.listarTodas(),
    })
      .pipe(
        finalize(() => this.carregandoOpcoes.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ clientes, embarcacoes }) => {
          this.clientes.set(clientes);
          this.embarcacoes.set(
            embarcacoes.filter((embarcacao) => embarcacao.ativa),
          );
        },
        error: () => {
          this.clientes.set([]);
          this.embarcacoes.set([]);
        },
      });
  }

  private carregarContratos(pagina = 0): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    const status = this.filtroStatus.value;
    this.contratoService
      .listar({
        pagina,
        tamanho: this.tamanhoPagina,
        status: status === "TODOS" ? undefined : status,
        clienteId: this.filtroClienteId.value || undefined,
        embarcacaoId: this.filtroEmbarcacaoId.value || undefined,
      })
      .pipe(
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (resposta) => {
          if (
            pagina > 0 &&
            resposta.content.length === 0 &&
            pagina >= resposta.totalPages
          ) {
            this.carregarContratos(Math.max(resposta.totalPages - 1, 0));
            return;
          }

          this.contratos.set(resposta.content);
          this.paginaAtual.set(resposta.number);
          this.totalPaginas.set(resposta.totalPages);
          this.totalElementos.set(resposta.totalElements);
        },
        error: (erro: HttpErrorResponse) => {
          const resposta = erro.error as ErroApi | null;
          this.contratos.set([]);
          this.mensagemErro.set(
            resposta?.mensagem || "Não foi possível carregar os contratos.",
          );
        },
      });
  }
}
