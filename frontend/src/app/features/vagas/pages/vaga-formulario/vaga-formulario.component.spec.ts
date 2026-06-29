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
  VagaService
} from '../../services/vaga.service';

import {
  VagaFormularioComponent
} from './vaga-formulario.component';

describe('VagaFormularioComponent', () => {

  let fixture:
    ComponentFixture<VagaFormularioComponent>;

  let component: VagaFormularioComponent;

  let vagaService: {
    buscarPorId: ReturnType<typeof vi.fn>;
    criar: ReturnType<typeof vi.fn>;
    atualizar: ReturnType<typeof vi.fn>;
  };

  let router: {
    navigateByUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    vagaService = {
      buscarPorId: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn()
    };

    vagaService.criar.mockReturnValue(
      of({} as never)
    );

    router = {
      navigateByUrl: vi.fn()
    };

    await TestBed
      .configureTestingModule({
        imports: [
          VagaFormularioComponent
        ],
        providers: [
          provideNoopAnimations(),
          {
            provide: VagaService,
            useValue: vagaService
          },
          {
            provide: Router,
            useValue: router
          },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                paramMap: {
                  get: () => null
                }
              }
            }
          }
        ]
      })
      .compileComponents();

    fixture = TestBed.createComponent(
      VagaFormularioComponent
    );

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve enviar payload normalizado ao criar vaga', () => {
    (component as any).formulario.patchValue({
      codigo: ' a-01 ',
      tipo: 'MOLHADA',
      setor: ' Setor A ',
      localizacao: '',
      comprimentoMaximoMetros: 12,
      bocaMaximaMetros: 4,
      caladoMaximoMetros: null,
      alturaMaximaMetros: 8,
      pesoMaximoKg: null,
      possuiAgua: false,
      possuiEnergia: true,
      observacoes: '  Observacao  '
    });

    (component as any).salvar();

    expect(vagaService.criar)
      .toHaveBeenCalledWith({
        codigo: 'A-01',
        tipo: 'MOLHADA',
        setor: 'Setor A',
        localizacao: null,
        comprimentoMaximoMetros: 12,
        bocaMaximaMetros: 4,
        caladoMaximoMetros: null,
        alturaMaximaMetros: 8,
        pesoMaximoKg: null,
        possuiAgua: false,
        possuiEnergia: true,
        observacoes: 'Observacao'
      });

    expect(router.navigateByUrl)
      .toHaveBeenCalledWith('/vagas');
  });

  it('deve marcar campos obrigatorios quando formulario estiver invalido', () => {
    (component as any).salvar();

    expect(vagaService.criar)
      .not.toHaveBeenCalled();

    expect(
      (component as any).formulario.controls
        .codigo.touched
    ).toBe(true);
  });
});
