package br.com.caisora.organizacao.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "organizacoes")
public class Organizacao {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 200)
    private String razaoSocial;

    @Column(length = 30)
    private String documento;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String telefone;

    @Column(nullable = false)
    private boolean ativa;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(nullable = false)
    private Instant atualizadaEm;

    protected Organizacao() {
    }

    private Organizacao(String nome, String razaoSocial, String documento, String email, String telefone) {
        this.nome = nome;
        this.razaoSocial = razaoSocial;
        this.documento = documento;
        this.email = normalizarEmail(email);
        this.telefone = telefone;
        this.ativa = true;
    }

    public static Organizacao criar(
            String nome,
            String razaoSocial,
            String documento,
            String email,
            String telefone
    ) {
        return new Organizacao(nome, razaoSocial, documento, email, telefone);
    }

    public void atualizar(
            String nome,
            String razaoSocial,
            String documento,
            String email,
            String telefone,
            boolean ativa
    ) {
        this.nome = nome;
        this.razaoSocial = razaoSocial;
        this.documento = documento;
        this.email = normalizarEmail(email);
        this.telefone = telefone;
        this.ativa = ativa;
    }

    public void inativar() {
        this.ativa = false;
    }

    @PrePersist
    void prePersistir() {
        Instant agora = Instant.now();
        this.id = UUID.randomUUID();
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    @PreUpdate
    void preAtualizar() {
        this.atualizadaEm = Instant.now();
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
