package br.com.caisora.usuario.dominio;

import br.com.caisora.organizacao.dominio.Organizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
    }

    private Usuario(
            Organizacao organizacao,
            String nome,
            String email,
            String senhaHash,
            PerfilUsuario perfil
    ) {
        this.organizacao = organizacao;
        this.nome = nome;
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = true;
    }

    /**
     * Cria um usuario sempre vinculado a uma organizacao. O vinculo e a base do
     * isolamento multi-tenant e evita que o frontend escolha livremente outro tenant
     * nas operacoes futuras.
     */
    public static Usuario criar(
            Organizacao organizacao,
            String nome,
            String email,
            String senhaHash,
            PerfilUsuario perfil
    ) {
        return new Usuario(organizacao, nome, email, senhaHash, perfil);
    }

    /**
     * Atualiza apenas os dados editaveis do usuario. Organizacao e senha nao sao
     * alteradas por este fluxo para preservar isolamento de tenant e evitar troca
     * acidental de credenciais.
     */
    public void atualizar(String nome, PerfilUsuario perfil) {
        this.nome = nome;
        this.perfil = perfil;
    }

    /**
     * Altera o status operacional do usuario. A inativacao bloqueia autenticacao
     * quando o fluxo de login for implementado.
     */
    public void alterarStatus(boolean ativo) {
        this.ativo = ativo;
    }

    @PrePersist
    void prePersistir() {
        Instant agora = Instant.now();
        this.id = UUID.randomUUID();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void preAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
