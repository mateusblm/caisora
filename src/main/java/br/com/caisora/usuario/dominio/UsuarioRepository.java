package br.com.caisora.usuario.dominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findByIdAndOrganizacaoId(UUID id, UUID organizacaoId);

    Page<Usuario> findAllByOrganizacaoId(UUID organizacaoId, Pageable paginacao);

    boolean existsByOrganizacaoIdAndEmail(UUID organizacaoId, String email);
}
