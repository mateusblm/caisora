package br.com.caisora.variabilidade.infraestrutura;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.variabilidade.dominio.AcaoSistema;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import br.com.caisora.variabilidade.dominio.PermissaoPerfilOrganizacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoPerfilOrganizacaoRepositoryJpa
        extends JpaRepository<PermissaoPerfilOrganizacao, UUID> {

    Optional<PermissaoPerfilOrganizacao> findByOrganizacaoIdAndPerfilAndModuloAndAcao(
            UUID organizacaoId,
            PerfilUsuario perfil,
            ModuloSistema modulo,
            AcaoSistema acao
    );

    List<PermissaoPerfilOrganizacao> findAllByOrganizacaoIdAndPerfil(
            UUID organizacaoId,
            PerfilUsuario perfil
    );
}
