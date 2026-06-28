package br.com.caisora.cliente.infraestrutura;

import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositoryJpa
    extends JpaRepository<Cliente, UUID>, ClienteRepository {
}