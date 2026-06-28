package br.com.caisora.organizacao.infraestrutura;

import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacaoRepositoryJpa extends JpaRepository<Organizacao, UUID>, OrganizacaoRepository {
}
