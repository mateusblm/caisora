package br.com.caisora.cliente.dominio;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface ClienteRepository {

    Cliente save(Cliente cliente);

    Optional<Cliente> findByIdAndOrganizacaoId(
        UUID id,
        UUID organizacaoId
    );

    Page<Cliente> findAllByOrganizacaoId(
        UUID organizacaoId,
        Pageable paginacao
    );

    Page<Cliente> findAllByOrganizacaoIdAndAtivo(
        UUID organizacaoId,
        boolean ativo,
        Pageable paginacao
    );

    Page<Cliente> findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
        UUID organizacaoId,
        String nome,
        Pageable paginacao
    );

    boolean existsByOrganizacaoIdAndCpfCnpj(
        UUID organizacaoId,
        String cpfCnpj
    );

    boolean existsByOrganizacaoIdAndCpfCnpjAndIdNot(
        UUID organizacaoId,
        String cpfCnpj,
        UUID id
    );
}