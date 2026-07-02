import { HttpErrorResponse } from "@angular/common/http";
import { Component, DestroyRef, OnInit, inject, signal } from "@angular/core";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatButtonModule } from "@angular/material/button";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatSelectModule } from "@angular/material/select";
import { MatSnackBar, MatSnackBarModule } from "@angular/material/snack-bar";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { catchError, finalize, forkJoin, of } from "rxjs";
import { ErroApi } from "../../../../shared/modelos/erro-api.model";
import { Cliente } from "../../../clientes/models/cliente.model";
import { ClienteService } from "../../../clientes/services/cliente.service";
import { Embarcacao } from "../../../embarcacoes/models/embarcacao.model";
import { EmbarcacaoService } from "../../../embarcacoes/services/embarcacao.service";
import { TipoVaga } from "../../../vagas/models/vaga.model";
import {
  Contrato,
  DadosContrato,
  PeriodicidadeContrato,
} from "../../models/contrato.model";
import { ContratoService } from "../../services/contrato.service";

@Component({
  selector: "app-contrato-formulario",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule,
  ],
  templateUrl: "./contrato-formulario.component.html",
  styleUrl: "./contrato-formulario.component.scss",
})
export class ContratoFormularioComponent implements OnInit {
  private readonly contratoService = inject(ContratoService);
  private readonly clienteService = inject(ClienteService);
  private readonly embarcacaoService = inject(EmbarcacaoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly contratoId = this.route.snapshot.paramMap.get("id");

  protected readonly modoEdicao = this.contratoId !== null;
  protected readonly contratoAtual = signal<Contrato | null>(null);
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly embarcacoes = signal<Embarcacao[]>([]);
  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly edicaoBloqueada = signal(false);
  protected readonly mensagemErro = signal<string | null>(null);

  protected readonly periodicidades: PeriodicidadeContrato[] = [
    "DIARIA",
    "MENSAL",
    "TRIMESTRAL",
    "SEMESTRAL",
    "ANUAL",
    "PERSONALIZADA",
  ];

  protected readonly tiposVaga: TipoVaga[] = [
    "MOLHADA",
    "SECA",
    "POITA",
    "OUTRA",
  ];

  protected readonly formulario = new FormGroup(
    {
      clienteId: new FormControl("", {
        nonNullable: true,
        validators: [Validators.required],
      }),
      embarcacaoId: new FormControl(
        { value: "", disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      tipoVagaContratada: new FormControl<TipoVaga | "">("", {
        nonNullable: true,
        validators: [Validators.required],
      }),
      periodicidade: new FormControl<PeriodicidadeContrato | "">("", {
        nonNullable: true,
        validators: [Validators.required],
      }),
      dataInicio: new FormControl("", {
        nonNullable: true,
        validators: [Validators.required],
      }),
      dataFim: new FormControl("", { nonNullable: true }),
      renovacaoAutomatica: new FormControl(false, {
        nonNullable: true,
      }),
      diasAvisoPrevio: new FormControl<number | null>(null, {
        validators: [Validators.min(0)],
      }),
      valorBase: new FormControl<number | null>(null, {
        validators: [Validators.required, Validators.min(0)],
      }),
      diaVencimento: new FormControl<number | null>(null, {
        validators: [
          Validators.required,
          Validators.min(1),
          Validators.max(31),
        ],
      }),
      observacoes: new FormControl("", {
        nonNullable: true,
        validators: [Validators.maxLength(2000)],
      }),
    },
    { validators: [this.validarDadosComerciais] },
  );

  ngOnInit(): void {
    this.configurarReacoes();
    this.carregarDados();
  }

  protected embarcacoesDoCliente(): Embarcacao[] {
    const clienteId = this.formulario.controls.clienteId.value;
    return this.embarcacoes().filter(
      (embarcacao) =>
        embarcacao.proprietarioId === clienteId &&
        (embarcacao.ativa ||
          embarcacao.id === this.contratoAtual()?.embarcacaoId),
    );
  }

  protected formatarNomeEmbarcacao(embarcacao: Embarcacao): string {
    const nome =
      embarcacao.nome || embarcacao.numeroInscricao || "Embarcação sem nome";
    const modelo = [embarcacao.fabricante, embarcacao.modelo]
      .filter(Boolean)
      .join(" ");
    return modelo ? `${nome} — ${modelo}` : nome;
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

  protected formatarTipoVaga(tipo: TipoVaga): string {
    const rotulos: Record<TipoVaga, string> = {
      MOLHADA: "Vaga molhada",
      SECA: "Vaga seca",
      POITA: "Poita",
      OUTRA: "Outra acomodação",
    };
    return rotulos[tipo];
  }

  protected salvar(): void {
    if (this.formulario.invalid || this.salvando() || this.edicaoBloqueada()) {
      this.formulario.markAllAsTouched();
      return;
    }

    const dados = this.montarDados();
    const operacao = this.contratoId
      ? this.contratoService.atualizar(this.contratoId, dados)
      : this.contratoService.criar(dados);

    this.salvando.set(true);
    this.mensagemErro.set(null);
    operacao
      .pipe(
        finalize(() => this.salvando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (contrato) => {
          this.snackBar.open(
            this.modoEdicao
              ? "Contrato atualizado com sucesso."
              : "Contrato criado com sucesso.",
            "Fechar",
            {
              duration: 3500,
              horizontalPosition: "center",
              verticalPosition: "bottom",
            },
          );
          void this.router.navigate(["/contratos", contrato.id]);
        },
        error: (erro: HttpErrorResponse) => {
          this.tratarErro(erro);
        },
      });
  }

  private configurarReacoes(): void {
    this.formulario.controls.clienteId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((clienteId) => {
        const controleEmbarcacao = this.formulario.controls.embarcacaoId;
        const embarcacaoAtual = this.embarcacoes().find(
          (embarcacao) => embarcacao.id === controleEmbarcacao.value,
        );

        if (embarcacaoAtual && embarcacaoAtual.proprietarioId !== clienteId) {
          controleEmbarcacao.setValue("");
        }

        if (clienteId && !this.edicaoBloqueada()) {
          controleEmbarcacao.enable({ emitEvent: false });
        } else {
          controleEmbarcacao.disable({ emitEvent: false });
        }
      });

    this.formulario.controls.periodicidade.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.formulario.updateValueAndValidity({
          emitEvent: false,
        });
      });

    this.formulario.controls.dataInicio.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.formulario.updateValueAndValidity({
          emitEvent: false,
        });
      });

    this.formulario.controls.dataFim.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.formulario.updateValueAndValidity({
          emitEvent: false,
        });
      });

    this.formulario.controls.renovacaoAutomatica.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((renovacaoAutomatica) => {
        if (!renovacaoAutomatica) {
          this.formulario.controls.diasAvisoPrevio.setValue(null);
        }
        this.formulario.updateValueAndValidity({
          emitEvent: false,
        });
      });
  }

  private carregarDados(): void {
    this.carregando.set(true);
    this.mensagemErro.set(null);

    forkJoin({
      clientes: this.clienteService.listarTodosAtivos(),
      embarcacoes: this.embarcacaoService.listarTodas(),
      contrato: this.contratoId
        ? this.contratoService.buscarPorId(this.contratoId)
        : of(null),
    })
      .pipe(
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ clientes, embarcacoes, contrato }) => {
          this.clientes.set(clientes);
          this.embarcacoes.set(embarcacoes);

          if (contrato) {
            this.contratoAtual.set(contrato);
            this.garantirOpcoesDoContrato(contrato);
            this.preencherFormulario(contrato);

            if (contrato.status !== "RASCUNHO") {
              this.edicaoBloqueada.set(true);
              this.formulario.disable();
              this.mensagemErro.set(
                "Somente contratos em rascunho podem ser editados.",
              );
            }
          }
        },
        error: (erro: HttpErrorResponse) => {
          const resposta = erro.error as ErroApi | null;
          this.mensagemErro.set(
            resposta?.mensagem ||
              "Não foi possível carregar os dados do contrato.",
          );
        },
      });
  }

  private garantirOpcoesDoContrato(contrato: Contrato): void {
    if (!this.clientes().some((cliente) => cliente.id === contrato.clienteId)) {
      this.clienteService
        .buscarPorId(contrato.clienteId)
        .pipe(
          catchError(() => of(null)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((cliente) => {
          if (cliente) {
            this.clientes.update((atuais) => [...atuais, cliente]);
          }
        });
    }

    if (
      !this.embarcacoes().some(
        (embarcacao) => embarcacao.id === contrato.embarcacaoId,
      )
    ) {
      this.embarcacaoService
        .buscarPorId(contrato.embarcacaoId)
        .pipe(
          catchError(() => of(null)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((embarcacao) => {
          if (embarcacao) {
            this.embarcacoes.update((atuais) => [...atuais, embarcacao]);
          }
        });
    }
  }

  private preencherFormulario(contrato: Contrato): void {
    this.formulario.patchValue({
      clienteId: contrato.clienteId,
      embarcacaoId: contrato.embarcacaoId,
      tipoVagaContratada: contrato.tipoVagaContratada,
      periodicidade: contrato.periodicidade,
      dataInicio: contrato.dataInicio,
      dataFim: contrato.dataFim ?? "",
      renovacaoAutomatica: contrato.renovacaoAutomatica,
      diasAvisoPrevio: contrato.diasAvisoPrevio,
      valorBase: contrato.valorBase,
      diaVencimento: contrato.diaVencimento,
      observacoes: contrato.observacoes ?? "",
    });
  }

  private montarDados(): DadosContrato {
    const valor = this.formulario.getRawValue();
    return {
      clienteId: valor.clienteId,
      embarcacaoId: valor.embarcacaoId,
      tipoVagaContratada: valor.tipoVagaContratada as TipoVaga,
      periodicidade: valor.periodicidade as PeriodicidadeContrato,
      dataInicio: valor.dataInicio,
      dataFim: valor.dataFim || null,
      renovacaoAutomatica: valor.renovacaoAutomatica,
      diasAvisoPrevio: valor.renovacaoAutomatica ? valor.diasAvisoPrevio : null,
      valorBase: Number(valor.valorBase),
      diaVencimento: Number(valor.diaVencimento),
      observacoes: valor.observacoes.trim() || null,
    };
  }

  private tratarErro(erro: HttpErrorResponse): void {
    const resposta = erro.error as ErroApi | null;
    const mensagem =
      resposta?.mensagem ||
      (erro.status === 0
        ? "Não foi possível conectar ao servidor."
        : "Não foi possível salvar o contrato.");
    this.mensagemErro.set(mensagem);
    this.snackBar.open(mensagem, "Fechar", {
      duration: 5000,
      horizontalPosition: "center",
      verticalPosition: "bottom",
    });
  }

  private validarDadosComerciais(
    controle: AbstractControl,
  ): ValidationErrors | null {
    const periodicidade = controle.get("periodicidade")?.value;
    const dataInicio = controle.get("dataInicio")?.value;
    const dataFim = controle.get("dataFim")?.value;
    const renovacaoAutomatica = controle.get("renovacaoAutomatica")?.value;
    const diasAvisoPrevio = controle.get("diasAvisoPrevio")?.value;

    const erros: ValidationErrors = {};

    if (periodicidade === "PERSONALIZADA" && !dataFim) {
      erros["dataFimObrigatoria"] = true;
    }

    if (dataInicio && dataFim && dataFim < dataInicio) {
      erros["periodoInvalido"] = true;
    }

    if (
      renovacaoAutomatica &&
      (diasAvisoPrevio === null || diasAvisoPrevio === undefined)
    ) {
      erros["avisoPrevioObrigatorio"] = true;
    }

    return Object.keys(erros).length > 0 ? erros : null;
  }
}
