package br.com.caisora.embarcacao.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class EmbarcacaoRepositoryJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EmbarcacaoRepository embarcacaoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OrganizacaoRepository organizacaoRepository;

    private Organizacao organizacaoA;
    private Organizacao organizacaoB;

    private Cliente proprietarioA;
    private Cliente proprietarioB;
    private Cliente proprietarioA2;

    @BeforeEach
    void configurar() {
        organizacaoA = organizacaoRepository.save(
                Organizacao.criar(
                        "Marina A",
                        "Marina A LTDA",
                        "11111111000111",
                        "contato@marinaa.com",
                        "41999999999"
                )
        );

        organizacaoB = organizacaoRepository.save(
                Organizacao.criar(
                        "Marina B",
                        "Marina B LTDA",
                        "22222222000122",
                        "contato@marinab.com",
                        "41988888888"
                )
        );

        proprietarioA = clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Joao Marina A",
                        "52998224725"
                )
        );

        proprietarioA2 = clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Maria Marina A",
                        "11144477735"
                )
        );

        proprietarioB = clienteRepository.save(
                criarCliente(
                        organizacaoB,
                        "Joao Marina B",
                        "52998224725"
                )
        );

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deveSalvarEmbarcacao() {
        Embarcacao embarcacao = criarEmbarcacao(
                organizacaoA,
                proprietarioA,
                "Aurora",
                "PR-100001",
                "CASCO-A-001"
        );

        Embarcacao salva =
                embarcacaoRepository.save(embarcacao);

        entityManager.flush();

        assertThat(salva.getId()).isNotNull();
        assertThat(salva.getOrganizacao().getId())
                .isEqualTo(organizacaoA.getId());
        assertThat(salva.getProprietario().getId())
                .isEqualTo(proprietarioA.getId());
        assertThat(salva.getNome()).isEqualTo("Aurora");
        assertThat(salva.isAtiva()).isTrue();
    }

    @Test
    void deveBuscarEmbarcacaoPorIdEOrganizacao() {
        Embarcacao salva = embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora",
                        "PR-100002",
                        "CASCO-A-002"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var resultado =
                embarcacaoRepository
                        .findByIdAndOrganizacaoId(
                                salva.getId(),
                                organizacaoA.getId()
                        );

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId())
                .isEqualTo(salva.getId());
    }

    @Test
    void naoDeveEncontrarEmbarcacaoDeOutraOrganizacao() {
        Embarcacao salva = embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora",
                        "PR-100003",
                        "CASCO-A-003"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var resultado =
                embarcacaoRepository
                        .findByIdAndOrganizacaoId(
                                salva.getId(),
                                organizacaoB.getId()
                        );

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveListarSomenteEmbarcacoesDaOrganizacao() {
        embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora",
                        "PR-100004",
                        "CASCO-A-004"
                )
        );

        embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoB,
                        proprietarioB,
                        "Horizonte",
                        "SC-200004",
                        "CASCO-B-004"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
                embarcacaoRepository.findAllByOrganizacaoId(
                        organizacaoA.getId(),
                        PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getNome())
                .isEqualTo("Aurora");
        assertThat(
                pagina.getContent()
                        .get(0)
                        .getOrganizacao()
                        .getId()
        ).isEqualTo(organizacaoA.getId());
    }

    @Test
    void deveFiltrarEmbarcacoesAtivas() {
        Embarcacao ativa = criarEmbarcacao(
                organizacaoA,
                proprietarioA,
                "Aurora",
                "PR-100005",
                "CASCO-A-005"
        );

        Embarcacao inativa = criarEmbarcacao(
                organizacaoA,
                proprietarioA,
                "Brisa",
                "PR-100006",
                "CASCO-A-006"
        );

        inativa.inativar();

        embarcacaoRepository.save(ativa);
        embarcacaoRepository.save(inativa);

        entityManager.flush();
        entityManager.clear();

        var pagina =
                embarcacaoRepository
                        .findAllByOrganizacaoIdAndAtiva(
                                organizacaoA.getId(),
                                true,
                                PageRequest.of(0, 10)
                        );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getNome())
                .isEqualTo("Aurora");
    }

    @Test
    void deveBuscarEmbarcacaoPorNomeIgnorandoMaiusculas() {
        embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora do Sul",
                        "PR-100007",
                        "CASCO-A-007"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
                embarcacaoRepository
                        .findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
                                organizacaoA.getId(),
                                "AURORA",
                                PageRequest.of(0, 10)
                        );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getNome())
                .isEqualTo("Aurora do Sul");
    }

    @Test
    void deveListarEmbarcacoesPorProprietario() {
        embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora",
                        "PR-100008",
                        "CASCO-A-008"
                )
        );

        embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA2,
                        "Brisa",
                        "PR-100009",
                        "CASCO-A-009"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
                embarcacaoRepository
                        .findAllByOrganizacaoIdAndProprietarioId(
                                organizacaoA.getId(),
                                proprietarioA.getId(),
                                PageRequest.of(0, 10)
                        );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(
                pagina.getContent()
                        .get(0)
                        .getProprietario()
                        .getId()
        ).isEqualTo(proprietarioA.getId());
    }

    @Test
    void deveListarEmbarcacoesPorTipo() {
        Embarcacao lancha = criarEmbarcacao(
                organizacaoA,
                proprietarioA,
                "Aurora",
                "PR-100010",
                "CASCO-A-010"
        );

        Embarcacao veleiro = new Embarcacao(
                organizacaoA,
                proprietarioA,
                "Vento Sul",
                TipoEmbarcacao.VELEIRO,
                "Beneteau",
                "Oceanis",
                2020,
                "PR-100011",
                "CASCO-A-011",
                "Paranagua",
                "BR",
                new BigDecimal("12.00"),
                new BigDecimal("3.80"),
                new BigDecimal("1.90"),
                new BigDecimal("2.00"),
                new BigDecimal("15.00"),
                new BigDecimal("6500.00"),
                10,
                TipoPropulsao.VELA_E_MOTOR,
                "Branca",
                null
        );

        embarcacaoRepository.save(lancha);
        embarcacaoRepository.save(veleiro);

        entityManager.flush();
        entityManager.clear();

        var pagina =
                embarcacaoRepository
                        .findAllByOrganizacaoIdAndTipo(
                                organizacaoA.getId(),
                                TipoEmbarcacao.VELEIRO,
                                PageRequest.of(0, 10)
                        );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getTipo())
                .isEqualTo(TipoEmbarcacao.VELEIRO);
    }

    @Test
    void deveIdentificarNumeroInscricaoExistente() {
        embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora",
                        "PR-100012",
                        "CASCO-A-012"
                )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existe =
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCase(
                                organizacaoA.getId(),
                                "pr-100012"
                        );

        assertThat(existe).isTrue();
    }

    @Test
    void deveIdentificarNumeroCascoExistente() {
        embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora",
                        "PR-100013",
                        "CASCO-A-013"
                )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existe =
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroCascoIgnoreCase(
                                organizacaoA.getId(),
                                "casco-a-013"
                        );

        assertThat(existe).isTrue();
    }

    @Test
    void devePermitirMesmaInscricaoEmOrganizacoesDiferentes() {
        Embarcacao embarcacaoA = criarEmbarcacao(
                organizacaoA,
                proprietarioA,
                "Aurora",
                "PR-COMPARTILHADA",
                "CASCO-A-014"
        );

        Embarcacao embarcacaoB = criarEmbarcacao(
                organizacaoB,
                proprietarioB,
                "Horizonte",
                "PR-COMPARTILHADA",
                "CASCO-B-014"
        );

        embarcacaoRepository.save(embarcacaoA);
        embarcacaoRepository.save(embarcacaoB);

        entityManager.flush();

        assertThat(embarcacaoA.getId()).isNotNull();
        assertThat(embarcacaoB.getId()).isNotNull();
    }

    @Test
    void naoDevePermitirInscricaoDuplicadaNaMesmaOrganizacao() {
        Embarcacao primeira = criarEmbarcacao(
                organizacaoA,
                proprietarioA,
                "Aurora",
                "PR-DUPLICADA",
                "CASCO-A-015"
        );

        Embarcacao segunda = criarEmbarcacao(
                organizacaoA,
                proprietarioA2,
                "Brisa",
                "PR-DUPLICADA",
                "CASCO-A-016"
        );

        embarcacaoRepository.save(primeira);
        entityManager.flush();

        assertThatThrownBy(() -> {
            embarcacaoRepository.save(segunda);
            entityManager.flush();
        }).isInstanceOfAny(
                DataIntegrityViolationException.class,
                PersistenceException.class
        );
    }

    @Test
    void naoDevePermitirCascoDuplicadoNaMesmaOrganizacao() {
        Embarcacao primeira = criarEmbarcacao(
                organizacaoA,
                proprietarioA,
                "Aurora",
                "PR-100017",
                "CASCO-DUPLICADO"
        );

        Embarcacao segunda = criarEmbarcacao(
                organizacaoA,
                proprietarioA2,
                "Brisa",
                "PR-100018",
                "CASCO-DUPLICADO"
        );

        embarcacaoRepository.save(primeira);
        entityManager.flush();

        assertThatThrownBy(() -> {
            embarcacaoRepository.save(segunda);
            entityManager.flush();
        }).isInstanceOfAny(
                DataIntegrityViolationException.class,
                PersistenceException.class
        );
    }

    @Test
    void deveIgnorarProprioIdNaVerificacaoDeInscricao() {
        Embarcacao salva = embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora",
                        "PR-100019",
                        "CASCO-A-019"
                )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existeOutra =
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCaseAndIdNot(
                                organizacaoA.getId(),
                                "pr-100019",
                                salva.getId()
                        );

        assertThat(existeOutra).isFalse();
    }

    @Test
    void deveEncontrarOutraEmbarcacaoComMesmaInscricao() {
        Embarcacao primeira = embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA,
                        "Aurora",
                        "PR-100020",
                        "CASCO-A-020"
                )
        );

        Embarcacao segunda = embarcacaoRepository.save(
                criarEmbarcacao(
                        organizacaoA,
                        proprietarioA2,
                        "Brisa",
                        "PR-100021",
                        "CASCO-A-021"
                )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existeOutra =
                embarcacaoRepository
                        .existsByOrganizacaoIdAndNumeroInscricaoIgnoreCaseAndIdNot(
                                organizacaoA.getId(),
                                primeira.getNumeroInscricao(),
                                segunda.getId()
                        );

        assertThat(existeOutra).isTrue();
    }

    private Cliente criarCliente(
            Organizacao organizacao,
            String nome,
            String cpf
    ) {
        return new Cliente(
                organizacao,
                TipoPessoa.FISICA,
                nome,
                null,
                cpf,
                nome.toLowerCase()
                        .replace(" ", ".")
                        + "@email.com",
                "4133334444",
                "41999998888",
                null
        );
    }

    private Embarcacao criarEmbarcacao(
            Organizacao organizacao,
            Cliente proprietario,
            String nome,
            String numeroInscricao,
            String numeroCasco
    ) {
        return new Embarcacao(
                organizacao,
                proprietario,
                nome,
                TipoEmbarcacao.LANCHA,
                "Schaefer",
                "V33",
                2023,
                numeroInscricao,
                numeroCasco,
                "Paranagua",
                "BR",
                new BigDecimal("10.33"),
                new BigDecimal("3.35"),
                new BigDecimal("0.95"),
                new BigDecimal("1.70"),
                new BigDecimal("3.60"),
                new BigDecimal("5200.00"),
                12,
                TipoPropulsao.MOTOR,
                "Branca",
                null
        );
    }
}
