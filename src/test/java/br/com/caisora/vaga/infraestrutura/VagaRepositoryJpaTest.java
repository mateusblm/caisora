package br.com.caisora.vaga.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.vaga.dominio.Vaga;
import br.com.caisora.vaga.dominio.VagaRepository;
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
class VagaRepositoryJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private OrganizacaoRepository organizacaoRepository;

    private Organizacao organizacaoA;

    private Organizacao organizacaoB;

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

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deveSalvarVaga() {
        Vaga vaga = criarVaga(
            organizacaoA,
            "A-01",
            TipoVaga.MOLHADA,
            "Pier A"
        );

        Vaga salva = vagaRepository.save(vaga);

        entityManager.flush();

        assertThat(salva.getId()).isNotNull();

        assertThat(salva.getOrganizacao().getId())
            .isEqualTo(organizacaoA.getId());

        assertThat(salva.getCodigo())
            .isEqualTo("A-01");

        assertThat(salva.getTipo())
            .isEqualTo(TipoVaga.MOLHADA);

        assertThat(salva.isAtiva()).isTrue();
    }

    @Test
    void deveBuscarVagaPorIdEOrganizacao() {
        Vaga salva = vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-02",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        entityManager.flush();
        entityManager.clear();

        var resultado =
            vagaRepository.findByIdAndOrganizacaoId(
                salva.getId(),
                organizacaoA.getId()
            );

        assertThat(resultado).isPresent();

        assertThat(resultado.get().getId())
            .isEqualTo(salva.getId());
    }

    @Test
    void naoDeveEncontrarVagaDeOutraOrganizacao() {
        Vaga salva = vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-03",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        entityManager.flush();
        entityManager.clear();

        var resultado =
            vagaRepository.findByIdAndOrganizacaoId(
                salva.getId(),
                organizacaoB.getId()
            );

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveListarSomenteVagasDaOrganizacao() {
        vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-04",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        vagaRepository.save(
            criarVaga(
                organizacaoB,
                "B-01",
                TipoVaga.SECA,
                "Pátio B"
            )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
            vagaRepository.findAllByOrganizacaoId(
                organizacaoA.getId(),
                PageRequest.of(0, 10)
            );

        assertThat(pagina.getContent())
            .extracting(Vaga::getCodigo)
            .containsExactly("A-04");
    }

    @Test
    void deveListarVagasPorStatus() {
        Vaga ativa = vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-05",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        Vaga inativa = vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-06",
                TipoVaga.SECA,
                "Pátio A"
            )
        );

        inativa.inativar();
        vagaRepository.save(inativa);

        entityManager.flush();
        entityManager.clear();

        var pagina =
            vagaRepository.findAllByOrganizacaoIdAndAtiva(
                organizacaoA.getId(),
                true,
                PageRequest.of(0, 10)
            );

        assertThat(pagina.getContent())
            .extracting(Vaga::getId)
            .containsExactly(ativa.getId());
    }

    @Test
    void deveBuscarVagasPorCodigoIgnorandoMaiusculas() {
        vagaRepository.save(
            criarVaga(
                organizacaoA,
                "PIER-A-07",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
            vagaRepository
                .findAllByOrganizacaoIdAndCodigoContainingIgnoreCase(
                    organizacaoA.getId(),
                    "pier-a",
                    PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent())
            .extracting(Vaga::getCodigo)
            .containsExactly("PIER-A-07");
    }

    @Test
    void deveBuscarVagasPorSetorIgnorandoMaiusculas() {
        vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-08",
                TipoVaga.MOLHADA,
                "Pier Norte"
            )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
            vagaRepository
                .findAllByOrganizacaoIdAndSetorContainingIgnoreCase(
                    organizacaoA.getId(),
                    "norte",
                    PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent())
            .extracting(Vaga::getSetor)
            .containsExactly("Pier Norte");
    }

    @Test
    void deveListarVagasPorTipo() {
        vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-09",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-10",
                TipoVaga.SECA,
                "Pátio A"
            )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
            vagaRepository.findAllByOrganizacaoIdAndTipo(
                organizacaoA.getId(),
                TipoVaga.SECA,
                PageRequest.of(0, 10)
            );

        assertThat(pagina.getContent())
            .extracting(Vaga::getCodigo)
            .containsExactly("A-10");
    }

    @Test
    void deveDetectarCodigoExistenteIgnorandoMaiusculas() {
        vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-11",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existe =
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCase(
                    organizacaoA.getId(),
                    "a-11"
                );

        assertThat(existe).isTrue();
    }

    @Test
    void deveDetectarCodigoDuplicadoExcetoPropriaVaga() {
        Vaga vagaA = vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-12",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        Vaga vagaB = vagaRepository.save(
            criarVaga(
                organizacaoA,
                "A-13",
                TipoVaga.SECA,
                "Pátio A"
            )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existeOutro =
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCaseAndIdNot(
                    organizacaoA.getId(),
                    "A-12",
                    vagaB.getId()
                );

        boolean existeDesconsiderandoPropria =
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCaseAndIdNot(
                    organizacaoA.getId(),
                    "A-12",
                    vagaA.getId()
                );

        assertThat(existeOutro).isTrue();

        assertThat(existeDesconsiderandoPropria)
            .isFalse();
    }

    @Test
    void devePermitirMesmoCodigoEmOrganizacoesDiferentes() {
        vagaRepository.save(
            criarVaga(
                organizacaoA,
                "COMPARTILHADA-01",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        vagaRepository.save(
            criarVaga(
                organizacaoB,
                "COMPARTILHADA-01",
                TipoVaga.SECA,
                "Pátio B"
            )
        );

        entityManager.flush();

        assertThat(
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCase(
                    organizacaoA.getId(),
                    "COMPARTILHADA-01"
                )
        ).isTrue();

        assertThat(
            vagaRepository
                .existsByOrganizacaoIdAndCodigoIgnoreCase(
                    organizacaoB.getId(),
                    "COMPARTILHADA-01"
                )
        ).isTrue();
    }

    @Test
    void naoDevePermitirCodigoDuplicadoNaMesmaOrganizacao() {
        vagaRepository.save(
            criarVaga(
                organizacaoA,
                "DUP-01",
                TipoVaga.MOLHADA,
                "Pier A"
            )
        );

        entityManager.flush();

        Vaga duplicada = criarVaga(
            organizacaoA,
            "DUP-01",
            TipoVaga.SECA,
            "Pátio A"
        );

        assertThatThrownBy(() -> {
            vagaRepository.save(duplicada);
            entityManager.flush();
        }).isInstanceOfAny(
            DataIntegrityViolationException.class,
            PersistenceException.class
        );
    }

    private Vaga criarVaga(
        Organizacao organizacao,
        String codigo,
        TipoVaga tipo,
        String setor
    ) {
        return new Vaga(
            organizacao,
            codigo,
            tipo,
            setor,
            "Localização de teste",
            new BigDecimal("12.50"),
            new BigDecimal("4.00"),
            new BigDecimal("1.50"),
            new BigDecimal("5.00"),
            new BigDecimal("9000.00"),
            true,
            true,
            "Vaga para testes"
        );
    }
}
