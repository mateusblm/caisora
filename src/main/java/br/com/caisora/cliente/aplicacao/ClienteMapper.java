package br.com.caisora.cliente.aplicacao;

import br.com.caisora.cliente.api.ClienteResponse;
import br.com.caisora.cliente.dominio.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public ClienteResponse paraResponse(Cliente cliente) {
        return new ClienteResponse(
            cliente.getId(),
            cliente.getTipoPessoa(),
            cliente.getNome(),
            cliente.getRazaoSocial(),
            cliente.getCpfCnpj(),
            cliente.getEmail(),
            cliente.getTelefone(),
            cliente.getCelular(),
            cliente.getObservacoes(),
            cliente.isAtivo(),
            cliente.getOrganizacao().getId(),
            cliente.getCriadoEm(),
            cliente.getAtualizadoEm()
        );
    }
}