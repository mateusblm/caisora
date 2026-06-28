package br.com.caisora.cliente.dominio;

import br.com.caisora.organizacao.dominio.Organizacao;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "clientes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cliente_organizacao_cpf_cnpj",
            columnNames = {"organizacao_id", "cpf_cnpj"}
        )
    }
)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 20)
    private TipoPessoa tipoPessoa;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "razao_social", length = 200)
    private String razaoSocial;

    @Column(name = "cpf_cnpj", nullable = false, length = 14)
    private String cpfCnpj;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 20)
    private String celular;

    @Column(length = 2000)
    private String observacoes;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Cliente() {
    }

    public Cliente(
        Organizacao organizacao,
        TipoPessoa tipoPessoa,
        String nome,
        String razaoSocial,
        String cpfCnpj,
        String email,
        String telefone,
        String celular,
        String observacoes
    ) {
        this.organizacao = organizacao;
        this.tipoPessoa = tipoPessoa;
        this.nome = nome;
        this.razaoSocial = razaoSocial;
        this.cpfCnpj = cpfCnpj;
        this.email = email;
        this.telefone = telefone;
        this.celular = celular;
        this.observacoes = observacoes;
        this.ativo = true;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public void atualizarDados(
        TipoPessoa tipoPessoa,
        String nome,
        String razaoSocial,
        String cpfCnpj,
        String email,
        String telefone,
        String celular,
        String observacoes
    ) {
        this.tipoPessoa = tipoPessoa;
        this.nome = nome;
        this.razaoSocial = razaoSocial;
        this.cpfCnpj = cpfCnpj;
        this.email = email;
        this.telefone = telefone;
        this.celular = celular;
        this.observacoes = observacoes;
        this.atualizadoEm = Instant.now();
    }

    public void ativar() {
        this.ativo = true;
        this.atualizadoEm = Instant.now();
    }

    public void inativar() {
        this.ativo = false;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Organizacao getOrganizacao() {
        return organizacao;
    }

    public TipoPessoa getTipoPessoa() {
        return tipoPessoa;
    }

    public String getNome() {
        return nome;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getCelular() {
        return celular;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}