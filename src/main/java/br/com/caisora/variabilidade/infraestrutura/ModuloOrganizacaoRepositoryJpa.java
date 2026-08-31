package br.com.caisora.variabilidade.infraestrutura;

import br.com.caisora.variabilidade.dominio.ModuloOrganizacao;
import br.com.caisora.variabilidade.dominio.ModuloSistema;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuloOrganizacaoRepositoryJpa extends JpaRepository<ModuloOrganizacao, UUID> {
    Optional<ModuloOrganizacao> findByOrganizacaoIdAndModulo(UUID organizacaoId, ModuloSistema modulo);
    List<ModuloOrganizacao> findAllByOrganizacaoId(UUID organizacaoId);
}
