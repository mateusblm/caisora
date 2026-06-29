import {
  Component,
  signal
} from '@angular/core';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import {
  HttpErrorResponse
} from '@angular/common/http';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  finalize
} from 'rxjs';

import {
  MatButtonModule
} from '@angular/material/button';

import {
  MatFormFieldModule
} from '@angular/material/form-field';

import {
  MatIconModule
} from '@angular/material/icon';

import {
  MatInputModule
} from '@angular/material/input';

import {
  MatProgressSpinnerModule
} from '@angular/material/progress-spinner';

import {
  AutenticacaoService
} from '../../../../core/autenticacao/autenticacao.service';

import {
  ErroApi
} from '../../../../shared/modelos/erro-api.model';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  protected readonly carregando =
    signal(false);

  protected readonly ocultarSenha =
    signal(true);

  protected readonly mensagemErro =
    signal<string | null>(null);

  protected readonly formulario;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly autenticacaoService:
      AutenticacaoService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute
  ) {
    this.formulario =
      this.formBuilder.nonNullable.group({
        codigoOrganizacao: [
          '',
          [
            Validators.required
          ]
        ],
        email: [
          '',
          [
            Validators.required,
            Validators.email
          ]
        ],
        senha: [
          '',
          [
            Validators.required
          ]
        ]
      });

    const sessaoExpirada =
      this.activatedRoute.snapshot
        .queryParamMap
        .get('sessao') === 'expirada';

    if (sessaoExpirada) {
      this.mensagemErro.set(
        'Sua sessão expirou. Entre novamente.'
      );
    }
  }

  protected entrar(): void {
    this.mensagemErro.set(null);

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();

      return;
    }

    const {
      codigoOrganizacao,
      email,
      senha
    } = this.formulario.getRawValue();

    this.carregando.set(true);

    this.autenticacaoService
      .autenticar(
        {
          codigoOrganizacao:
            codigoOrganizacao
              .trim()
              .toLowerCase(),
          email: email.trim(),
          senha
        }
      )
      .pipe(
        finalize(() =>
          this.carregando.set(false)
        )
      )
      .subscribe({
        next: () => {
          const retorno =
            this.activatedRoute.snapshot
              .queryParamMap
              .get('retorno');

          const retornoValido =
            retorno !== null
            && retorno.startsWith('/')
            && !retorno.startsWith('//')
            && retorno !== '/login';

          void this.router.navigateByUrl(
            retornoValido
              ? retorno
              : '/dashboard'
          );
        },
        error: (
          erro: HttpErrorResponse
        ) => {
          this.mensagemErro.set(
            this.obterMensagemErro(erro)
          );
        }
      });
  }

  protected alternarSenha(): void {
    this.ocultarSenha.update(
      (oculta) => !oculta
    );
  }

  private obterMensagemErro(
    erro: HttpErrorResponse
  ): string {
    const resposta =
      erro.error as ErroApi | null;

    if (
      resposta?.codigo ===
      'CREDENCIAIS_INVALIDAS'
    ) {
      return 'Codigo da marina, e-mail ou senha invalidos.';
    }

    if (
      resposta?.codigo ===
      'USUARIO_INATIVO'
    ) {
      return 'Este usuário está inativo.';
    }

    if (
      resposta?.codigo ===
      'ORGANIZACAO_INATIVA'
    ) {
      return 'Esta organização está inativa.';
    }

    if (resposta?.mensagem) {
      return resposta.mensagem;
    }

    if (erro.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível realizar o login.';
  }
}
