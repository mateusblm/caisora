package br.com.caisora.variabilidade.dominio;

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
        name = "organizacao_modulos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_organizacao_modulo",
                columnNames = {"organizacao_id", "modulo"}
        )
)
public class ModuloOrganizacao {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organizacao_id", nullable = false)
    private UUID organizacaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ModuloSistema modulo;

    @Column(nullable = false)
    private boolean ativo;

    protected ModuloOrganizacao() {
    }

    private ModuloOrganizacao(UUID organizacaoId, ModuloSistema modulo, boolean ativo) {
        this.organizacaoId = organizacaoId;
        this.modulo = modulo;
        this.ativo = ativo;
    }

    public static ModuloOrganizacao criar(UUID organizacaoId, ModuloSistema modulo, boolean ativo) {
        return new ModuloOrganizacao(organizacaoId, modulo, ativo);
    }


    public UUID getId() {
        return id;
    }

    public UUID getOrganizacaoId() {
        return organizacaoId;
    }

    public ModuloSistema getModulo() {
        return modulo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void alterar(boolean ativo) {
        this.ativo = ativo;
    }

    @PrePersist
    void prePersistir() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
