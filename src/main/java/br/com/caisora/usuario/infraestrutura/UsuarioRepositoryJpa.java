package br.com.caisora.usuario.infraestrutura;

import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositoryJpa extends JpaRepository<Usuario, UUID>, UsuarioRepository {
}
