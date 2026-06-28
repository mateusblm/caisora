package br.com.caisora.organizacao.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.organizacao.api.AtualizarOrganizacaoRequest;
import br.com.caisora.organizacao.api.CriarOrganizacaoRequest;
import br.com.caisora.organizacao.api.OrganizacaoResponse;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrganizacaoServiceTest {

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    private final OrganizacaoMapper organizacaoMapper = new OrganizacaoMapper();

    private OrganizacaoService organizacaoService;

    @BeforeEach
    void configurar() {
        organizacaoService = new OrganizacaoService(organizacaoRepository, organizacaoMapper);
    }

    @Test
    void deveCriarOrganizacaoAtivaComEmailNormalizado() {
        CriarOrganizacaoRequest request = new CriarOrganizacaoRequest(
                "Marina Teste",
                "Marina Teste LTDA",
                "12345678000199",
                "CONTATO@MARINATESTE.COM",
                "11999999999");

        when(organizacaoRepository.save(any(Organizacao.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        OrganizacaoResponse response = organizacaoService.criar(request);

        ArgumentCaptor<Organizacao> captor = ArgumentCaptor.forClass(Organizacao.class);
        verify(organizacaoRepository).save(captor.capture());

        assertThat(captor.getValue().getEmail()).isEqualTo("contato@marinateste.com");
        assertThat(captor.getValue().isAtiva()).isTrue();
        assertThat(response.email()).isEqualTo("contato@marinateste.com");
        assertThat(response.ativa()).isTrue();
    }

    @Test
    void deveBuscarOrganizacaoPorId() {
        UUID id = UUID.randomUUID();
        Organizacao organizacao = criarOrganizacaoPersistida(id);
        when(organizacaoRepository.findById(id)).thenReturn(Optional.of(organizacao));

        OrganizacaoResponse response = organizacaoService.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.nome()).isEqualTo("Marina Teste");
    }

    @Test
    void deveFalharAoBuscarOrganizacaoInexistente() {
        UUID id = UUID.randomUUID();
        when(organizacaoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizacaoService.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Organizacao nao encontrada");
    }

    @Test
    void deveAtualizarOrganizacao() {
        UUID id = UUID.randomUUID();
        Organizacao organizacao = criarOrganizacaoPersistida(id);
        when(organizacaoRepository.findById(id)).thenReturn(Optional.of(organizacao));

        AtualizarOrganizacaoRequest request = new AtualizarOrganizacaoRequest(
                "Marina Atualizada",
                "Marina Atualizada LTDA",
                "98765432000199",
                "NOVO@MARINA.COM",
                "11888888888",
                true);

        OrganizacaoResponse response = organizacaoService.atualizar(id, request);

        assertThat(response.nome()).isEqualTo("Marina Atualizada");
        assertThat(response.email()).isEqualTo("novo@marina.com");
        assertThat(response.ativa()).isTrue();
    }

    @Test
    void deveInativarOrganizacao() {
        UUID id = UUID.randomUUID();
        Organizacao organizacao = criarOrganizacaoPersistida(id);
        when(organizacaoRepository.findById(id)).thenReturn(Optional.of(organizacao));

        organizacaoService.inativar(id);

        assertThat(organizacao.isAtiva()).isFalse();
    }

    private Organizacao criarOrganizacaoPersistida(UUID id) {
        Organizacao organizacao = Organizacao.criar(
                "Marina Teste",
                "Marina Teste LTDA",
                "12345678000199",
                "contato@marinateste.com",
                "11999999999");
        ReflectionTestUtils.setField(organizacao, "id", id);
        ReflectionTestUtils.setField(organizacao, "criadaEm", Instant.parse("2026-06-28T20:00:00Z"));
        ReflectionTestUtils.setField(organizacao, "atualizadaEm", Instant.parse("2026-06-28T20:00:00Z"));
        return organizacao;
    }
}
