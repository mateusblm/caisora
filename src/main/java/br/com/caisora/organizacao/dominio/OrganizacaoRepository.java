package br.com.caisora.organizacao.dominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizacaoRepository {

    Organizacao save(Organizacao organizacao);

    Optional<Organizacao> findById(UUID id);

    Optional<Organizacao> findBySlugIgnoreCase(String slug);

    Page<Organizacao> findAll(Pageable paginacao);

    boolean existsById(UUID id);

    boolean existsBySlugIgnoreCase(String slug);
}
