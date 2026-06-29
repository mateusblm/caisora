import { of } from 'rxjs';

import {
  vi
} from 'vitest';

import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';

import {
  provideNoopAnimations
} from '@angular/platform-browser/animations';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  AutenticacaoService
} from '../../../../core/autenticacao/autenticacao.service';

import {
  LoginComponent
} from './login.component';

describe('LoginComponent', () => {

  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let autenticacaoService: {
    autenticar: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigateByUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    autenticacaoService = {
      autenticar: vi.fn().mockReturnValue(
        of({
          tokenAcesso: 'token.jwt',
          tipoToken: 'Bearer',
          expiraEm: 3600,
          usuario: {
            id: 'usuario-1',
            nome: 'Maria Silva',
            email: 'maria@marina.com',
            perfil: 'ADMINISTRADOR_MARINA',
            organizacaoId: 'organizacao-1',
            organizacaoNome: 'Marina Teste'
          }
        })
      )
    };

    router = {
      navigateByUrl: vi.fn()
    };

    await TestBed
      .configureTestingModule({
        imports: [
          LoginComponent
        ],
        providers: [
          provideNoopAnimations(),
          {
            provide: AutenticacaoService,
            useValue: autenticacaoService
          },
          {
            provide: Router,
            useValue: router
          },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                queryParamMap: {
                  get: () => null
                }
              }
            }
          }
        ]
      })
      .compileComponents();

    fixture = TestBed.createComponent(
      LoginComponent
    );

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve exibir campo Codigo da marina', () => {
    const texto =
      fixture.nativeElement.textContent;

    expect(texto).toContain(
      'Codigo da marina'
    );

    expect(texto).not.toContain(
      'Identificador da marina'
    );
  });

  it('deve enviar codigo da marina normalizado', () => {
    (component as any).formulario.setValue({
      codigoOrganizacao: ' MARINA-TESTE ',
      email: 'maria@marina.com',
      senha: 'SenhaForte123'
    });

    (component as any).entrar();

    expect(autenticacaoService.autenticar)
      .toHaveBeenCalledWith({
        codigoOrganizacao: 'marina-teste',
        email: 'maria@marina.com',
        senha: 'SenhaForte123'
      });

    expect(router.navigateByUrl)
      .toHaveBeenCalledWith('/dashboard');
  });

  it('deve validar codigo da marina obrigatorio', () => {
    (component as any).formulario.patchValue({
      codigoOrganizacao: '',
      email: 'maria@marina.com',
      senha: 'SenhaForte123'
    });

    (component as any).entrar();

    expect(autenticacaoService.autenticar)
      .not.toHaveBeenCalled();

    expect(
      (component as any).formulario.controls
        .codigoOrganizacao.touched
    ).toBe(true);
  });
});
