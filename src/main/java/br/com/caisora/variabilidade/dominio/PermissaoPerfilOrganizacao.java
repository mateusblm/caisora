package br.com.caisora.variabilidade.dominio;

import br.com.caisora.usuario.dominio.PerfilUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "perfil_permissoes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_perfil_permissao",
                columnNames = {"organizacao_id", "perfil", "modulo", "acao"}
        )
)
public class PermissaoPerfilOrganizacao {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organizacao_id", nullable = false)
    private UUID organizacaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PerfilUsuario perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ModuloSistema modulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AcaoSistema acao;

    @Column(nullable = false)
    private boolean permitido;

    protected PermissaoPerfilOrganizacao() {
    }

    private PermissaoPerfilOrganizacao(
            UUID organizacaoId,
            PerfilUsuario perfil,
            ModuloSistema modulo,
            AcaoSistema acao,
            boolean permitido
    ) {
        this.organizacaoId = organizacaoId;
        this.perfil = perfil;
        this.modulo = modulo;
        this.acao = acao;
        this.permitido = permitido;
    }

    public static PermissaoPerfilOrganizacao criar(
            UUID organizacaoId,
            PerfilUsuario perfil,
            ModuloSistema modulo,
            AcaoSistema acao,
            boolean permitido
    ) {
        return new PermissaoPerfilOrganizacao(organizacaoId, perfil, modulo, acao, permitido);
    }


    public UUID getId() {
        return id;
    }

    public UUID getOrganizacaoId() {
        return organizacaoId;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public ModuloSistema getModulo() {
        return modulo;
    }

    public AcaoSistema getAcao() {
        return acao;
    }

    public boolean isPermitido() {
        return permitido;
    }

    public void alterar(boolean permitido) {
        this.permitido = permitido;
    }

    @PrePersist
    void prePersistir() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
