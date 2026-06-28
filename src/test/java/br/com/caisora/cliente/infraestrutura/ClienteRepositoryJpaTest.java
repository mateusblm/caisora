package br.com.caisora.cliente.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.caisora.TestcontainersConfiguration;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

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
class ClienteRepositoryJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ClienteRepository clienteRepository;

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
    }

    @Test
    void deveSalvarCliente() {
        Cliente cliente = criarCliente(
                organizacaoA,
                "Joao da Silva",
                "52998224725"
        );

        Cliente salvo = clienteRepository.save(cliente);
        entityManager.flush();

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getOrganizacao().getId())
                .isEqualTo(organizacaoA.getId());
        assertThat(salvo.getNome())
                .isEqualTo("Joao da Silva");
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void deveBuscarClientePorIdEOrganizacao() {
        Cliente salvo = clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Joao da Silva",
                        "52998224725"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var resultado =
                clienteRepository.findByIdAndOrganizacaoId(
                        salvo.getId(),
                        organizacaoA.getId()
                );

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId())
                .isEqualTo(salvo.getId());
    }

    @Test
    void naoDeveEncontrarClienteDeOutraOrganizacao() {
        Cliente salvo = clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Joao da Silva",
                        "52998224725"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var resultado =
                clienteRepository.findByIdAndOrganizacaoId(
                        salvo.getId(),
                        organizacaoB.getId()
                );

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveListarSomenteClientesDaOrganizacao() {
        clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Joao da Silva",
                        "52998224725"
                )
        );

        clienteRepository.save(
                criarCliente(
                        organizacaoB,
                        "Maria Souza",
                        "11144477735"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
                clienteRepository.findAllByOrganizacaoId(
                        organizacaoA.getId(),
                        PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getNome())
                .isEqualTo("Joao da Silva");
        assertThat(
                pagina.getContent()
                        .get(0)
                        .getOrganizacao()
                        .getId()
        ).isEqualTo(organizacaoA.getId());
    }

    @Test
    void deveFiltrarClientesAtivos() {
        Cliente ativo = criarCliente(
                organizacaoA,
                "Cliente Ativo",
                "52998224725"
        );

        Cliente inativo = criarCliente(
                organizacaoA,
                "Cliente Inativo",
                "11144477735"
        );

        inativo.inativar();

        clienteRepository.save(ativo);
        clienteRepository.save(inativo);

        entityManager.flush();
        entityManager.clear();

        var pagina =
                clienteRepository.findAllByOrganizacaoIdAndAtivo(
                        organizacaoA.getId(),
                        true,
                        PageRequest.of(0, 10)
                );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getNome())
                .isEqualTo("Cliente Ativo");
    }

    @Test
    void deveBuscarClientePorNomeIgnorandoMaiusculas() {
        clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Joao da Silva",
                        "52998224725"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var pagina =
                clienteRepository
                        .findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
                                organizacaoA.getId(),
                                "JOAO",
                                PageRequest.of(0, 10)
                        );

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getNome())
                .isEqualTo("Joao da Silva");
    }

    @Test
    void deveIdentificarDocumentoExistenteNaOrganizacao() {
        clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Joao da Silva",
                        "52998224725"
                )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existe =
                clienteRepository.existsByOrganizacaoIdAndCpfCnpj(
                        organizacaoA.getId(),
                        "52998224725"
                );

        assertThat(existe).isTrue();
    }

    @Test
    void devePermitirMesmoDocumentoEmOrganizacoesDiferentes() {
        Cliente clienteA = criarCliente(
                organizacaoA,
                "Joao Marina A",
                "52998224725"
        );

        Cliente clienteB = criarCliente(
                organizacaoB,
                "Joao Marina B",
                "52998224725"
        );

        clienteRepository.save(clienteA);
        clienteRepository.save(clienteB);

        entityManager.flush();

        assertThat(clienteA.getId()).isNotNull();
        assertThat(clienteB.getId()).isNotNull();
    }

    @Test
    void naoDevePermitirDocumentoDuplicadoNaMesmaOrganizacao() {
        Cliente clienteA = criarCliente(
                organizacaoA,
                "Joao Um",
                "52998224725"
        );

        Cliente clienteB = criarCliente(
                organizacaoA,
                "Joao Dois",
                "52998224725"
        );

        clienteRepository.save(clienteA);
        entityManager.flush();

        assertThatThrownBy(() -> {
            clienteRepository.save(clienteB);
            entityManager.flush();
        }).isInstanceOfAny(
                DataIntegrityViolationException.class,
                PersistenceException.class
        );
    }

    @Test
    void deveIgnorarOProprioIdNaVerificacaoDeDocumento() {
        Cliente salvo = clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Joao da Silva",
                        "52998224725"
                )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existeOutro =
                clienteRepository
                        .existsByOrganizacaoIdAndCpfCnpjAndIdNot(
                                organizacaoA.getId(),
                                "52998224725",
                                salvo.getId()
                        );

        assertThat(existeOutro).isFalse();
    }

    @Test
    void deveEncontrarOutroClienteComMesmoDocumento() {
        Cliente primeiro = clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Primeiro Cliente",
                        "52998224725"
                )
        );

        Cliente segundo = clienteRepository.save(
                criarCliente(
                        organizacaoA,
                        "Segundo Cliente",
                        "11144477735"
                )
        );

        entityManager.flush();
        entityManager.clear();

        boolean existeOutro =
                clienteRepository
                        .existsByOrganizacaoIdAndCpfCnpjAndIdNot(
                                organizacaoA.getId(),
                                primeiro.getCpfCnpj(),
                                segundo.getId()
                        );

        assertThat(existeOutro).isTrue();
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
}