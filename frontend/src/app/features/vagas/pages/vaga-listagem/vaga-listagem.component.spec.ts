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
  ActivatedRoute
} from '@angular/router';

import {
  Vaga
} from '../../models/vaga.model';

import {
  VagaService
} from '../../services/vaga.service';

import {
  VagaListagemComponent
} from './vaga-listagem.component';

describe('VagaListagemComponent', () => {

  let fixture:
    ComponentFixture<VagaListagemComponent>;

  let component: VagaListagemComponent;

  let vagaService: {
    listar: ReturnType<typeof vi.fn>;
    alterarStatus: ReturnType<typeof vi.fn>;
  };

  const vaga: Vaga = {
    id: 'vaga-1',
    codigo: 'A-01',
    tipo: 'MOLHADA',
    setor: 'A',
    localizacao: 'Pier 1',
    comprimentoMaximoMetros: 12,
    bocaMaximaMetros: 4,
    caladoMaximoMetros: null,
    alturaMaximaMetros: null,
    pesoMaximoKg: null,
    possuiAgua: true,
    possuiEnergia: false,
    observacoes: null,
    ativa: true,
    organizacaoId: 'org-1',
    criadaEm: '2026-06-29T00:00:00Z',
    atualizadaEm: '2026-06-29T00:00:00Z'
  };

  beforeEach(async () => {
    vagaService = {
      listar: vi.fn(),
      alterarStatus: vi.fn()
    };

    vagaService.listar.mockReturnValue(
      of({
        content: [
          vaga
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
        numberOfElements: 1,
        first: true,
        last: true,
        empty: false
      })
    );

    await TestBed
      .configureTestingModule({
        imports: [
          VagaListagemComponent
        ],
        providers: [
          provideNoopAnimations(),
          {
            provide: VagaService,
            useValue: vagaService
          },
          {
            provide: ActivatedRoute,
            useValue: {}
          }
        ]
      })
      .compileComponents();

    fixture = TestBed.createComponent(
      VagaListagemComponent
    );

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve carregar vagas ao iniciar', () => {
    expect(vagaService.listar)
      .toHaveBeenCalledWith({
        pagina: 0,
        tamanho: 10,
        codigo: undefined,
        setor: undefined,
        tipo: undefined,
        ativa: undefined
      });

    expect((component as any).vagas())
      .toEqual([
        vaga
      ]);
  });

  it('deve limpar filtros conflitantes ao pesquisar por codigo', () => {
    (component as any).busca.setValue('A-01');
    (component as any).filtroTipo.setValue('SECA');
    (component as any).filtroStatus.setValue('ATIVAS');

    (component as any).pesquisar();

    expect((component as any).filtroTipo.value)
      .toBe('TODAS');

    expect((component as any).filtroStatus.value)
      .toBe('TODAS');

    expect(vagaService.listar)
      .toHaveBeenCalledWith(
        expect.objectContaining({
          codigo: 'A-01',
          setor: undefined,
          tipo: undefined,
          ativa: undefined
        })
      );
  });
});
