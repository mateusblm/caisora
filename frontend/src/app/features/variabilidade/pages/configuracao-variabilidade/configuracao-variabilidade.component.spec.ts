import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ConfiguracaoVariabilidadeComponent } from './configuracao-variabilidade.component';
import { VariabilidadeService } from '../../services/variabilidade.service';
import { AutenticacaoService } from '../../../../core/autenticacao/autenticacao.service';
describe('Configuração de módulos', () => {
  const config = {
    modulos: [
      {
        modulo: 'CONTRATOS',
        nome: 'Contratos',
        obrigatorio: false,
        ativo: true,
        acoes: ['VISUALIZAR'],
      },
    ],
    perfis: [],
  };
  const usuario = signal({ perfil: 'ADMINISTRADOR_MARINA' });
  const service = {
    buscarConfiguracao: vi.fn(),
    buscarModulosOrganizacao: vi.fn(),
    atualizarModuloOrganizacao: vi.fn(),
  };
  beforeEach(() => {
    vi.resetAllMocks();
    usuario.set({ perfil: 'ADMINISTRADOR_MARINA' });
    service.buscarConfiguracao.mockReturnValue(of(config));
    service.buscarModulosOrganizacao.mockReturnValue(of(config));
    service.atualizarModuloOrganizacao.mockReturnValue(of(config));
    TestBed.configureTestingModule({
      imports: [ConfiguracaoVariabilidadeComponent],
      providers: [
        { provide: VariabilidadeService, useValue: service },
        {
          provide: AutenticacaoService,
          useValue: { usuarioAtual: usuario, atualizarUsuarioAtual: () => of({}) },
        },
      ],
    });
  });
  it('administrador da marina vê módulos somente para leitura e mantém permissões', () => {
    const f = TestBed.createComponent(ConfiguracaoVariabilidadeComponent);
    f.detectChanges();
    expect(f.nativeElement.querySelector('mat-slide-toggle button').disabled).toBe(true);
    expect(f.nativeElement.textContent).toContain('Permissões por perfil');
    f.componentInstance['alterarModulo']('CONTRATOS', true);
    expect(service.atualizarModuloOrganizacao).not.toHaveBeenCalled();
  });
  it('plataforma altera módulos na organização carregada e não no campo editado depois', () => {
    usuario.set({ perfil: 'ADMINISTRADOR_PLATAFORMA' });
    const f = TestBed.createComponent(ConfiguracaoVariabilidadeComponent);
    f.detectChanges();
    f.componentInstance['organizacaoId'] = 'org-a';
    f.componentInstance['carregar']();
    f.componentInstance['organizacaoId'] = 'org-b';
    f.componentInstance['alterarModulo']('CONTRATOS', false);
    expect(service.atualizarModuloOrganizacao).toHaveBeenCalledWith('org-a', 'CONTRATOS', false);
    f.detectChanges();
    expect(f.nativeElement.textContent).not.toContain('Permissões por perfil');
  });
});
