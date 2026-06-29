package br.com.caisora.ocupacao.infraestrutura;

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
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.ocupacao.dominio.StatusOcupacao;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.vaga.dominio.Vaga;
import br.com.caisora.vaga.dominio.VagaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.Instant;
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
class OcupacaoRepositoryJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrganizacaoRepository
        organizacaoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EmbarcacaoRepository
        embarcacaoRepository;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private OcupacaoRepository
        ocupacaoRepository;

    private Organizacao organizacaoA;

    private Organizacao organizacaoB;

    private Cliente clienteA;

    private Cliente clienteB;

    @BeforeEach
    void configurar() {
        organizacaoA =
            organizacaoRepository.save(
                Organizacao.criar(
                    "Marina A",
                    "marina-a",
                    "Marina A LTDA",
                    "11111111000111",
                    "contato@marinaa.com",
                    "41999999999"
                )
            );

        organizacaoB =
            organizacaoRepository.save(
                Organizacao.criar(
                    "Marina B",
                    "marina-b",
                    "Marina B LTDA",
                    "22222222000122",
                    "contato@marinab.com",
                    "41988888888"
                )
            );

        clienteA = clienteRepository.save(
            criarCliente(
                organizacaoA,
                "Cliente A",
                "52998224725",
                "clientea@email.com"
            )
        );

        clienteB = clienteRepository.save(
            criarCliente(
                organizacaoB,
                "Cliente B",
                "11144477735",
                "clienteb@email.com"
            )
        );

        entityManager.flush();
    }

    @Test
    void deveSalvarOcupacaoAtiva() {
        Embarcacao embarcacao =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-01"
            );

        Vaga vaga = salvarVaga(
            organizacaoA,
            "A-01"
        );

        Ocupacao ocupacao =
            criarOcupacao(
                organizacaoA,
                embarcacao,
                vaga
            );

        Ocupacao salva =
            ocupacaoRepository.save(
                ocupacao
            );

        entityManager.flush();

        assertThat(salva.getId())
            .isNotNull();

        assertThat(salva.getStatus())
            .isEqualTo(
                StatusOcupacao.ATIVA
            );

        assertThat(
            salva.getOrganizacao().getId()
        ).isEqualTo(organizacaoA.getId());

        assertThat(
            salva.getEmbarcacao().getId()
        ).isEqualTo(embarcacao.getId());

        assertThat(salva.getVaga().getId())
            .isEqualTo(vaga.getId());
    }

    @Test
    void deveBuscarPorIdEOrganizacao() {
        Embarcacao embarcacao =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-02"
            );

        Vaga vaga = salvarVaga(
            organizacaoA,
            "A-02"
        );

        Ocupacao salva =
            ocupacaoRepository.save(
                criarOcupacao(
                    organizacaoA,
                    embarcacao,
                    vaga
                )
            );

        entityManager.flush();
        entityManager.clear();

        var resultado =
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    salva.getId(),
                    organizacaoA.getId()
                );

        assertThat(resultado).isPresent();

        assertThat(resultado.get().getId())
            .isEqualTo(salva.getId());
    }

    @Test
    void naoDeveEncontrarOcupacaoDeOutraOrganizacao() {
        Embarcacao embarcacao =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-03"
            );

        Vaga vaga = salvarVaga(
            organizacaoA,
            "A-03"
        );

        Ocupacao salva =
            ocupacaoRepository.save(
                criarOcupacao(
                    organizacaoA,
                    embarcacao,
                    vaga
                )
            );

        entityManager.flush();
        entityManager.clear();

        var resultado =
            ocupacaoRepository
                .findByIdAndOrganizacaoId(
                    salva.getId(),
                    organizacaoB.getId()
                );

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveListarSomenteOcupacoesDaOrganizacao() {
        Embarcacao embarcacaoA =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-04"
            );

        Vaga vagaA = salvarVaga(
            organizacaoA,
            "A-04"
        );

        Embarcacao embarcacaoB =
            salvarEmbarcacao(
                organizacaoB,
                clienteB,
                "VENTO-04"
            );

        Vaga vagaB = salvarVaga(
            organizacaoB,
            "B-04"
        );

        Ocupacao ocupacaoA =
            ocupacaoRepository.save(
                criarOcupacao(
                    organizacaoA,
                    embarcacaoA,
                    vagaA
                )
            );

        ocupacaoRepository.save(
            criarOcupacao(
                organizacaoB,
                embarcacaoB,
                vagaB
            )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
            ocupacaoRepository
                .findAllByOrganizacaoId(
                    organizacaoA.getId(),
                    PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent())
            .extracting(Ocupacao::getId)
            .containsExactly(
                ocupacaoA.getId()
            );
    }

    @Test
    void deveListarPorStatus() {
        Embarcacao embarcacaoAtiva =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-05"
            );

        Vaga vagaAtiva = salvarVaga(
            organizacaoA,
            "A-05"
        );

        Embarcacao embarcacaoEncerrada =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-06"
            );

        Vaga vagaEncerrada = salvarVaga(
            organizacaoA,
            "A-06"
        );

        Ocupacao ativa =
            ocupacaoRepository.save(
                criarOcupacao(
                    organizacaoA,
                    embarcacaoAtiva,
                    vagaAtiva
                )
            );

        Ocupacao encerrada =
            criarOcupacao(
                organizacaoA,
                embarcacaoEncerrada,
                vagaEncerrada
            );

        encerrada.encerrar(
            encerrada.getInicioEm()
                .plusSeconds(1800)
        );

        ocupacaoRepository.save(encerrada);

        entityManager.flush();
        entityManager.clear();

        var pagina =
            ocupacaoRepository
                .findAllByOrganizacaoIdAndStatus(
                    organizacaoA.getId(),
                    StatusOcupacao.ATIVA,
                    PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent())
            .extracting(Ocupacao::getId)
            .containsExactly(ativa.getId());
    }

    @Test
    void deveListarHistoricoPorEmbarcacao() {
        Embarcacao embarcacao =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-07"
            );

        Vaga vagaA = salvarVaga(
            organizacaoA,
            "A-07"
        );

        Vaga vagaB = salvarVaga(
            organizacaoA,
            "A-08"
        );

        Ocupacao primeira =
            criarOcupacao(
                organizacaoA,
                embarcacao,
                vagaA
            );

        primeira.encerrar(
            primeira.getInicioEm()
                .plusSeconds(1800)
        );

        ocupacaoRepository.save(primeira);

        Ocupacao atual =
            ocupacaoRepository.save(
                criarOcupacao(
                    organizacaoA,
                    embarcacao,
                    vagaB
                )
            );

        entityManager.flush();
        entityManager.clear();

        var pagina =
            ocupacaoRepository
                .findAllByOrganizacaoIdAndEmbarcacaoId(
                    organizacaoA.getId(),
                    embarcacao.getId(),
                    PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent())
            .extracting(Ocupacao::getId)
            .containsExactlyInAnyOrder(
                primeira.getId(),
                atual.getId()
            );
    }

    @Test
    void deveListarHistoricoPorVaga() {
        Embarcacao embarcacaoA =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-08"
            );

        Embarcacao embarcacaoB =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-09"
            );

        Vaga vaga = salvarVaga(
            organizacaoA,
            "A-09"
        );

        Ocupacao primeira =
            criarOcupacao(
                organizacaoA,
                embarcacaoA,
                vaga
            );

        primeira.encerrar(
            primeira.getInicioEm()
                .plusSeconds(1800)
        );

        ocupacaoRepository.save(primeira);

        Ocupacao atual =
            ocupacaoRepository.save(
                criarOcupacao(
                    organizacaoA,
                    embarcacaoB,
                    vaga
                )
            );

        entityManager.flush();
        entityManager.clear();

        var pagina =
            ocupacaoRepository
                .findAllByOrganizacaoIdAndVagaId(
                    organizacaoA.getId(),
                    vaga.getId(),
                    PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent())
            .extracting(Ocupacao::getId)
            .containsExactlyInAnyOrder(
                primeira.getId(),
                atual.getId()
            );
    }

    @Test
    void deveEncontrarOcupacaoAtivaPorEmbarcacaoEVaga() {
        Embarcacao embarcacao =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-10"
            );

        Vaga vaga = salvarVaga(
            organizacaoA,
            "A-10"
        );

        Ocupacao salva =
            ocupacaoRepository.save(
                criarOcupacao(
                    organizacaoA,
                    embarcacao,
                    vaga
                )
            );

        entityManager.flush();
        entityManager.clear();

        var porEmbarcacao =
            ocupacaoRepository
                .findByOrganizacaoIdAndEmbarcacaoIdAndStatus(
                    organizacaoA.getId(),
                    embarcacao.getId(),
                    StatusOcupacao.ATIVA
                );

        var porVaga =
            ocupacaoRepository
                .findByOrganizacaoIdAndVagaIdAndStatus(
                    organizacaoA.getId(),
                    vaga.getId(),
                    StatusOcupacao.ATIVA
                );

        assertThat(porEmbarcacao)
            .isPresent()
            .get()
            .extracting(Ocupacao::getId)
            .isEqualTo(salva.getId());

        assertThat(porVaga)
            .isPresent()
            .get()
            .extracting(Ocupacao::getId)
            .isEqualTo(salva.getId());
    }

    @Test
    void deveDetectarConflitosAtivos() {
        Embarcacao embarcacao =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-11"
            );

        Vaga vaga = salvarVaga(
            organizacaoA,
            "A-11"
        );

        ocupacaoRepository.save(
            criarOcupacao(
                organizacaoA,
                embarcacao,
                vaga
            )
        );

        entityManager.flush();
        entityManager.clear();

        boolean embarcacaoOcupada =
            ocupacaoRepository
                .existsByOrganizacaoIdAndEmbarcacaoIdAndStatus(
                    organizacaoA.getId(),
                    embarcacao.getId(),
                    StatusOcupacao.ATIVA
                );

        boolean vagaOcupada =
            ocupacaoRepository
                .existsByOrganizacaoIdAndVagaIdAndStatus(
                    organizacaoA.getId(),
                    vaga.getId(),
                    StatusOcupacao.ATIVA
                );

        assertThat(embarcacaoOcupada)
            .isTrue();

        assertThat(vagaOcupada)
            .isTrue();
    }

    @Test
    void naoDevePermitirDuasOcupacoesAtivasParaMesmaEmbarcacao() {
        Embarcacao embarcacao =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-12"
            );

        Vaga vagaA = salvarVaga(
            organizacaoA,
            "A-12"
        );

        Vaga vagaB = salvarVaga(
            organizacaoA,
            "A-13"
        );

        ocupacaoRepository.save(
            criarOcupacao(
                organizacaoA,
                embarcacao,
                vagaA
            )
        );

        entityManager.flush();

        Ocupacao duplicada =
            criarOcupacao(
                organizacaoA,
                embarcacao,
                vagaB
            );

        assertThatThrownBy(() -> {
            ocupacaoRepository.save(duplicada);
            entityManager.flush();
        }).isInstanceOfAny(
            DataIntegrityViolationException.class,
            PersistenceException.class
        );
    }

    @Test
    void naoDevePermitirDuasOcupacoesAtivasParaMesmaVaga() {
        Embarcacao embarcacaoA =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-13"
            );

        Embarcacao embarcacaoB =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-14"
            );

        Vaga vaga = salvarVaga(
            organizacaoA,
            "A-14"
        );

        ocupacaoRepository.save(
            criarOcupacao(
                organizacaoA,
                embarcacaoA,
                vaga
            )
        );

        entityManager.flush();

        Ocupacao duplicada =
            criarOcupacao(
                organizacaoA,
                embarcacaoB,
                vaga
            );

        assertThatThrownBy(() -> {
            ocupacaoRepository.save(duplicada);
            entityManager.flush();
        }).isInstanceOfAny(
            DataIntegrityViolationException.class,
            PersistenceException.class
        );
    }

    @Test
    void devePermitirNovaOcupacaoAposEncerramento() {
        Embarcacao embarcacao =
            salvarEmbarcacao(
                organizacaoA,
                clienteA,
                "AURORA-15"
            );

        Vaga vagaA = salvarVaga(
            organizacaoA,
            "A-15"
        );

        Vaga vagaB = salvarVaga(
            organizacaoA,
            "A-16"
        );

        Ocupacao anterior =
            criarOcupacao(
                organizacaoA,
                embarcacao,
                vagaA
            );

        anterior.encerrar(
            anterior.getInicioEm()
                .plusSeconds(1800)
        );

        ocupacaoRepository.save(anterior);

        Ocupacao atual =
            ocupacaoRepository.save(
                criarOcupacao(
                    organizacaoA,
                    embarcacao,
                    vagaB
                )
            );

        entityManager.flush();

        assertThat(atual.getId())
            .isNotNull();

        assertThat(atual.getStatus())
            .isEqualTo(
                StatusOcupacao.ATIVA
            );
    }

    private Cliente criarCliente(
        Organizacao organizacao,
        String nome,
        String documento,
        String email
    ) {
        return new Cliente(
            organizacao,
            TipoPessoa.FISICA,
            nome,
            null,
            documento,
            email,
            "4133334444",
            "41999998888",
            "Cliente de teste"
        );
    }

    private Embarcacao salvarEmbarcacao(
        Organizacao organizacao,
        Cliente proprietario,
        String identificador
    ) {
        return embarcacaoRepository.save(
            new Embarcacao(
                organizacao,
                proprietario,
                identificador,
                TipoEmbarcacao.LANCHA,
                "Schaefer",
                "V33",
                2023,
                "PR-" + identificador,
                "CASCO-" + identificador,
                "Paranagua",
                "BR",
                new BigDecimal("10.00"),
                new BigDecimal("3.00"),
                new BigDecimal("1.00"),
                new BigDecimal("1.70"),
                new BigDecimal("3.00"),
                new BigDecimal("5000.00"),
                12,
                TipoPropulsao.MOTOR,
                "Branca",
                "Embarcação de teste"
            )
        );
    }

    private Vaga salvarVaga(
        Organizacao organizacao,
        String codigo
    ) {
        return vagaRepository.save(
            new Vaga(
                organizacao,
                codigo,
                TipoVaga.MOLHADA,
                "Pier A",
                "Corredor principal",
                new BigDecimal("12.00"),
                new BigDecimal("4.00"),
                new BigDecimal("1.50"),
                new BigDecimal("4.00"),
                new BigDecimal("7000.00"),
                true,
                true,
                "Vaga de teste"
            )
        );
    }

    private Ocupacao criarOcupacao(
        Organizacao organizacao,
        Embarcacao embarcacao,
        Vaga vaga
    ) {
        Instant inicio =
            Instant.now().minusSeconds(3600);

        return new Ocupacao(
            organizacao,
            embarcacao,
            vaga,
            inicio,
            inicio.plusSeconds(86400),
            "Ocupação de teste"
        );
    }
}
