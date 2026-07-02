import { HttpErrorResponse } from "@angular/common/http";
import {
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatSelectModule } from "@angular/material/select";
import { MatSnackBar, MatSnackBarModule } from "@angular/material/snack-bar";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { Observable, filter, finalize, forkJoin, switchMap } from "rxjs";
import { ErroApi } from "../../../../shared/modelos/erro-api.model";
import { Ocupacao } from "../../../ocupacoes/models/ocupacao.model";
import { OcupacaoService } from "../../../ocupacoes/services/ocupacao.service";
import {
  DadosDialogoAcaoContrato,
  DialogoAcaoContratoComponent,
  ResultadoDialogoAcaoContrato,
} from "../../componentes/dialogo-acao-contrato/dialogo-acao-contrato.component";
import {
  Contrato,
  HistoricoContrato,
  PeriodicidadeContrato,
  StatusContrato,
  TipoEventoContrato,
  VinculoContratoOcupacao,
} from "../../models/contrato.model";
import { ContratoService } from "../../services/contrato.service";

@Component({
  selector: "app-contrato-detalhe",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule,
  ],
  templateUrl: "./contrato-detalhe.component.html",
  styleUrl: "./contrato-detalhe.component.scss",
})
export class ContratoDetalheComponent implements OnInit {
  private readonly contratoService = inject(ContratoService);
  private readonly ocupacaoService = inject(OcupacaoService);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly contratoId = this.route.snapshot.paramMap.get("id") ?? "";

  protected readonly contrato = signal<Contrato | null>(null);
  protected readonly vinculos = signal<VinculoContratoOcupacao[]>([]);
  protected readonly historico = signal<HistoricoContrato[]>([]);
  protected readonly ocupacoesAtivas = signal<Ocupacao[]>([]);
  protected readonly carregando = signal(true);
  protected readonly mensagemErro = signal<string | null>(null);
  protected readonly acaoEmAndamento = signal<string | null>(null);
  protected readonly ocupacaoSelecionadaId = new FormControl("", {
    nonNullable: true,
  });

  protected readonly vinculoAberto = computed(
    () => this.vinculos().find((vinculo) => vinculo.aberto) ?? null,
  );

  protected readonly ocupacoesDisponiveis = computed(() => {
    const contrato = this.contrato();
    const vinculoAberto = this.vinculoAberto();
    if (!contrato || vinculoAberto) {
      return [];
    }
    return this.ocupacoesAtivas().filter(
      (ocupacao) => ocupacao.embarcacaoId === contrato.embarcacaoId,
    );
  });

  ngOnInit(): void {
    this.carregarDados();
  }

  protected podeEditar(): boolean {
    return this.contrato()?.status === "RASCUNHO";
  }

  protected podeEnviarParaAssinatura(): boolean {
    return this.contrato()?.status === "RASCUNHO";
  }

  protected podeAtivar(): boolean {
    return this.contrato()?.status === "PENDENTE_ASSINATURA";
  }

  protected podeSuspender(): boolean {
    return this.contrato()?.status === "ATIVO";
  }

  protected podeReativar(): boolean {
    return this.contrato()?.status === "SUSPENSO";
  }

  protected podeSolicitarEncerramento(): boolean {
    const status = this.contrato()?.status;
    return status === "ATIVO" || status === "SUSPENSO";
  }

  protected podeEncerrar(): boolean {
    return this.contrato()?.status === "EM_ENCERRAMENTO";
  }

  protected podeCancelar(): boolean {
    const status = this.contrato()?.status;
    return status === "RASCUNHO" || status === "PENDENTE_ASSINATURA";
  }

  protected podeGerenciarOcupacao(): boolean {
    return this.contrato()?.status === "ATIVO";
  }

  protected solicitarEnvioParaAssinatura(): void {
    this.executarComDialogo(
      "enviar",
      {
        titulo: "Enviar contrato para assinatura?",
        mensagem: "Os dados comerciais deixarão de ser editáveis.",
        detalhe:
          "Revise cliente, embarcação, vigência e valor antes de continuar.",
        textoConfirmacao: "Enviar para assinatura",
        icone: "draw",
        tom: "padrao",
        solicitarDescricao: true,
        rotuloDescricao: "Observação do envio",
      },
      (resultado) =>
        this.contratoService.enviarParaAssinatura(
          this.contratoId,
          resultado.descricao,
        ),
      "Contrato enviado para assinatura.",
    );
  }

  protected solicitarAtivacao(): void {
    this.executarComDialogo(
      "ativar",
      {
        titulo: "Ativar contrato?",
        mensagem: "Confirme a data em que o contrato foi assinado.",
        textoConfirmacao: "Ativar contrato",
        icone: "task_alt",
        tom: "sucesso",
        solicitarData: true,
        rotuloData: "Data da assinatura",
        dataInicial: this.hoje(),
        solicitarDescricao: true,
        rotuloDescricao: "Observação da ativação",
      },
      (resultado) =>
        this.contratoService.ativar(this.contratoId, {
          dataAssinatura: resultado.data!,
          descricao: resultado.descricao,
        }),
      "Contrato ativado com sucesso.",
    );
  }

  protected solicitarSuspensao(): void {
    this.executarComDialogo(
      "suspender",
      {
        titulo: "Suspender contrato?",
        mensagem:
          "O vínculo comercial permanecerá existente, mas ficará suspenso.",
        textoConfirmacao: "Suspender",
        icone: "pause_circle",
        tom: "perigo",
        solicitarDescricao: true,
        rotuloDescricao: "Motivo da suspensão",
        descricaoObrigatoria: true,
      },
      (resultado) =>
        this.contratoService.suspender(this.contratoId, resultado.descricao),
      "Contrato suspenso.",
    );
  }

  protected solicitarReativacao(): void {
    this.executarComDialogo(
      "reativar",
      {
        titulo: "Reativar contrato?",
        mensagem: "O contrato voltará ao estado ativo.",
        textoConfirmacao: "Reativar",
        icone: "play_circle",
        tom: "sucesso",
        solicitarDescricao: true,
        rotuloDescricao: "Observação da reativação",
      },
      (resultado) =>
        this.contratoService.reativar(this.contratoId, resultado.descricao),
      "Contrato reativado.",
    );
  }

  protected solicitarInicioEncerramento(): void {
    this.executarComDialogo(
      "solicitar-encerramento",
      {
        titulo: "Iniciar encerramento?",
        mensagem: "O contrato passará para o estado Em encerramento.",
        detalhe:
          "A ocupação não será encerrada até a conclusão formal do contrato.",
        textoConfirmacao: "Iniciar encerramento",
        icone: "pending_actions",
        tom: "perigo",
        solicitarDescricao: true,
        rotuloDescricao: "Motivo do encerramento",
        descricaoObrigatoria: true,
      },
      (resultado) =>
        this.contratoService.solicitarEncerramento(
          this.contratoId,
          resultado.descricao,
        ),
      "Encerramento solicitado.",
    );
  }

  protected solicitarConclusaoEncerramento(): void {
    this.executarComDialogo(
      "encerrar",
      {
        titulo: "Encerrar contrato?",
        mensagem:
          "Esta ação finaliza o vínculo comercial e o vínculo de ocupação aberto.",
        textoConfirmacao: "Encerrar contrato",
        icone: "event_busy",
        tom: "perigo",
        solicitarData: true,
        rotuloData: "Data de encerramento",
        dataInicial: this.hoje(),
        solicitarDescricao: true,
        rotuloDescricao: "Descrição do encerramento",
      },
      (resultado) =>
        this.contratoService.encerrar(this.contratoId, {
          dataEncerramento: resultado.data!,
          descricao: resultado.descricao,
        }),
      "Contrato encerrado.",
    );
  }

  protected solicitarCancelamento(): void {
    this.executarComDialogo(
      "cancelar",
      {
        titulo: "Cancelar contrato?",
        mensagem: "O contrato será cancelado antes da ativação.",
        textoConfirmacao: "Cancelar contrato",
        icone: "cancel",
        tom: "perigo",
        solicitarDescricao: true,
        rotuloDescricao: "Motivo do cancelamento",
        descricaoObrigatoria: true,
      },
      (resultado) =>
        this.contratoService.cancelar(this.contratoId, resultado.descricao),
      "Contrato cancelado.",
    );
  }

  protected vincularOcupacao(): void {
    const ocupacaoId = this.ocupacaoSelecionadaId.value;
    if (!ocupacaoId || this.acaoEmAndamento()) {
      return;
    }

    this.acaoEmAndamento.set("vincular-ocupacao");
    this.contratoService
      .vincularOcupacao(this.contratoId, ocupacaoId)
      .pipe(
        finalize(() => this.acaoEmAndamento.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.ocupacaoSelecionadaId.setValue("");
          this.exibirSucesso("Ocupação vinculada ao contrato.");
          this.carregarDados();
        },
        error: (erro: HttpErrorResponse) =>
          this.exibirErro(erro, "Não foi possível vincular a ocupação."),
      });
  }

  protected solicitarDesvinculo(vinculo: VinculoContratoOcupacao): void {
    this.executarComDialogo(
      "desvincular-ocupacao",
      {
        titulo: "Desvincular ocupação?",
        mensagem: `A vaga ${vinculo.vagaCodigo} deixará de estar vinculada ao contrato.`,
        textoConfirmacao: "Desvincular",
        icone: "link_off",
        tom: "perigo",
        solicitarDescricao: true,
        rotuloDescricao: "Motivo do desvínculo",
        descricaoObrigatoria: true,
      },
      (resultado) =>
        this.contratoService.desvincularOcupacao(
          this.contratoId,
          vinculo.id,
          resultado.descricao,
        ),
      "Ocupação desvinculada.",
    );
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
      OUTRA: "Outra acomodação",
    };
    return rotulos[tipo];
  }

  protected formatarEvento(evento: TipoEventoContrato): string {
    const rotulos: Record<TipoEventoContrato, string> = {
      CRIADO: "Contrato criado",
      EDITADO: "Dados atualizados",
      ENVIADO_PARA_ASSINATURA: "Enviado para assinatura",
      ATIVADO: "Contrato ativado",
      SUSPENSO: "Contrato suspenso",
      REATIVADO: "Contrato reativado",
      ENCERRAMENTO_SOLICITADO: "Encerramento solicitado",
      ENCERRADO: "Contrato encerrado",
      CANCELADO: "Contrato cancelado",
      OCUPACAO_VINCULADA: "Ocupação vinculada",
      OCUPACAO_DESVINCULADA: "Ocupação desvinculada",
    };
    return rotulos[evento];
  }

  protected formatarMoeda(valor: number): string {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(valor);
  }

  protected formatarData(valor: string | null): string {
    if (!valor) {
      return "Não informada";
    }
    return new Intl.DateTimeFormat("pt-BR", {
      timeZone: "UTC",
    }).format(new Date(`${valor}T00:00:00Z`));
  }

  protected formatarDataHora(valor: string): string {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
      timeZone: "America/Sao_Paulo",
    }).format(new Date(valor));
  }

  protected classeStatus(status: StatusContrato): string {
    return `status status--${status.toLowerCase()}`;
  }

  protected descricaoOcupacao(ocupacao: Ocupacao): string {
    return `${ocupacao.vagaCodigo} — ${ocupacao.embarcacaoNome}`;
  }

  private carregarDados(): void {
    if (!this.contratoId) {
      this.mensagemErro.set("Contrato não informado.");
      this.carregando.set(false);
      return;
    }

    this.carregando.set(true);
    this.mensagemErro.set(null);
    forkJoin({
      contrato: this.contratoService.buscarPorId(this.contratoId),
      vinculos: this.contratoService.listarOcupacoes(this.contratoId),
      historico: this.contratoService.listarHistorico(this.contratoId, 0, 100),
      ocupacoes: this.ocupacaoService.listarTodasAtivas(),
    })
      .pipe(
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ contrato, vinculos, historico, ocupacoes }) => {
          this.contrato.set(contrato);
          this.vinculos.set(vinculos);
          this.historico.set(historico.content);
          this.ocupacoesAtivas.set(ocupacoes);
        },
        error: (erro: HttpErrorResponse) => {
          const resposta = erro.error as ErroApi | null;
          this.mensagemErro.set(
            resposta?.mensagem || "Não foi possível carregar o contrato.",
          );
        },
      });
  }

  private executarComDialogo<T>(
    chave: string,
    dados: DadosDialogoAcaoContrato,
    operacao: (resultado: ResultadoDialogoAcaoContrato) => Observable<T>,
    mensagemSucesso: string,
  ): void {
    if (this.acaoEmAndamento()) {
      return;
    }

    this.dialog
      .open<
        DialogoAcaoContratoComponent,
        DadosDialogoAcaoContrato,
        ResultadoDialogoAcaoContrato
      >(DialogoAcaoContratoComponent, {
        data: dados,
        width: "calc(100vw - 32px)",
        maxWidth: "520px",
        autoFocus: false,
        restoreFocus: true,
      })
      .afterClosed()
      .pipe(
        filter(
          (resultado): resultado is ResultadoDialogoAcaoContrato =>
            resultado?.confirmado === true,
        ),
        switchMap((resultado) => {
          this.acaoEmAndamento.set(chave);
          return operacao(resultado).pipe(
            finalize(() => this.acaoEmAndamento.set(null)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.exibirSucesso(mensagemSucesso);
          this.carregarDados();
        },
        error: (erro: HttpErrorResponse) =>
          this.exibirErro(erro, "Não foi possível concluir a ação."),
      });
  }

  private exibirSucesso(mensagem: string): void {
    this.snackBar.open(mensagem, "Fechar", {
      duration: 3500,
      horizontalPosition: "center",
      verticalPosition: "bottom",
    });
  }

  private exibirErro(erro: HttpErrorResponse, mensagemPadrao: string): void {
    const resposta = erro.error as ErroApi | null;
    const mensagem =
      resposta?.mensagem ||
      (erro.status === 0
        ? "Não foi possível conectar ao servidor."
        : mensagemPadrao);
    this.snackBar.open(mensagem, "Fechar", {
      duration: 5000,
      horizontalPosition: "center",
      verticalPosition: "bottom",
    });
  }

  private hoje(): string {
    const agora = new Date();
    const ano = agora.getFullYear();
    const mes = String(agora.getMonth() + 1).padStart(2, "0");
    const dia = String(agora.getDate()).padStart(2, "0");
    return `${ano}-${mes}-${dia}`;
  }
}
