package br.com.caisora.contrato.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.autenticacao.aplicacao.UsuarioAutenticado;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.contrato.api.AtivarContratoRequest;
import br.com.caisora.contrato.api.CriarContratoRequest;
import br.com.caisora.contrato.api.VincularOcupacaoContratoRequest;
import br.com.caisora.contrato.dominio.Contrato;
import br.com.caisora.contrato.dominio.ContratoRepository;
import br.com.caisora.contrato.dominio.HistoricoContrato;
import br.com.caisora.contrato.dominio.HistoricoContratoRepository;
import br.com.caisora.contrato.dominio.PeriodicidadeContrato;
import br.com.caisora.contrato.dominio.SequenciaContratoRepository;
import br.com.caisora.contrato.dominio.StatusContrato;
import br.com.caisora.contrato.dominio.VinculoContratoOcupacao;
import br.com.caisora.contrato.dominio.VinculoContratoOcupacaoRepository;
import br.com.caisora.embarcacao.dominio.Embarcacao;
import br.com.caisora.embarcacao.dominio.EmbarcacaoRepository;
import br.com.caisora.embarcacao.dominio.TipoEmbarcacao;
import br.com.caisora.embarcacao.dominio.TipoPropulsao;
import br.com.caisora.ocupacao.dominio.Ocupacao;
import br.com.caisora.ocupacao.dominio.OcupacaoRepository;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import br.com.caisora.usuario.dominio.PerfilUsuario;
import br.com.caisora.usuario.dominio.Usuario;
import br.com.caisora.usuario.dominio.UsuarioRepository;
import br.com.caisora.vaga.dominio.TipoVaga;
import br.com.caisora.vaga.dominio.Vaga;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock private ContratoRepository contratoRepository;
    @Mock private VinculoContratoOcupacaoRepository vinculoRepository;
    @Mock private HistoricoContratoRepository historicoRepository;
    @Mock private SequenciaContratoRepository sequenciaRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private EmbarcacaoRepository embarcacaoRepository;
    @Mock private OcupacaoRepository ocupacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LeitorTokenJwt leitorTokenJwt;

    private ContratoService contratoService;

    @BeforeEach
    void configurar() {
        contratoService = new ContratoService(
            contratoRepository,
            vinculoRepository,
            historicoRepository,
            sequenciaRepository,
            organizacaoRepository,
            clienteRepository,
            embarcacaoRepository,
            ocupacaoRepository,
            usuarioRepository,
            new ContratoMapper(),
            leitorTokenJwt
        );
    }

    @Test
    void deveCriarContratoEmRascunhoComNumeroSequencial() {
        Contexto contexto = criarContexto();
        prepararContextoAutenticado(contexto);
        when(clienteRepository.findByIdAndOrganizacaoId(
            contexto.cliente().getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(contexto.cliente()));
        when(embarcacaoRepository.findByIdAndOrganizacaoId(
            contexto.embarcacao().getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(contexto.embarcacao()));
        when(sequenciaRepository.proximoNumero(
            contexto.organizacao().getId(),
            2026
        )).thenReturn(7L);
        when(contratoRepository.save(any(Contrato.class)))
            .thenAnswer(invocacao -> {
                Contrato contrato = invocacao.getArgument(0);
                ReflectionTestUtils.setField(
                    contrato,
                    "id",
                    UUID.randomUUID()
                );
                ReflectionTestUtils.setField(contrato, "versao", 0L);
                return contrato;
            });

        var response = contratoService.criar(
            criarRequest(
                contexto.cliente().getId(),
                contexto.embarcacao().getId()
            )
        );

        assertThat(response.numero()).isEqualTo("CTR-2026-000007");
        assertThat(response.status()).isEqualTo(StatusContrato.RASCUNHO);
        assertThat(response.clienteId()).isEqualTo(contexto.cliente().getId());
        verify(historicoRepository).save(any(HistoricoContrato.class));
    }

    @Test
    void naoDeveCriarContratoParaEmbarcacaoDeOutroCliente() {
        Contexto contexto = criarContexto();
        Cliente outroCliente = criarCliente(
            UUID.randomUUID(),
            contexto.organizacao(),
            "Outro cliente"
        );
        prepararContextoAutenticado(contexto);
        when(clienteRepository.findByIdAndOrganizacaoId(
            outroCliente.getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(outroCliente));
        when(embarcacaoRepository.findByIdAndOrganizacaoId(
            contexto.embarcacao().getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(contexto.embarcacao()));

        assertThatThrownBy(() -> contratoService.criar(
            criarRequest(
                outroCliente.getId(),
                contexto.embarcacao().getId()
            )
        ))
            .isInstanceOf(DadosInvalidosException.class)
            .hasMessage("A embarcacao nao pertence ao cliente informado");

        verify(contratoRepository, never()).save(any(Contrato.class));
    }

    @Test
    void deveAtivarContratoPendente() {
        Contexto contexto = criarContexto();
        Contrato contrato = criarContratoPersistido(contexto);
        contrato.enviarParaAssinatura();
        prepararContextoAutenticado(contexto);
        when(contratoRepository.findByIdAndOrganizacaoId(
            contrato.getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(contrato)).thenReturn(contrato);

        var response = contratoService.ativar(
            contrato.getId(),
            new AtivarContratoRequest(LocalDate.now(), null)
        );

        assertThat(response.status()).isEqualTo(StatusContrato.ATIVO);
        assertThat(response.dataAssinatura()).isEqualTo(LocalDate.now());
        verify(historicoRepository).save(any(HistoricoContrato.class));
    }

    @Test
    void naoDeveAtivarSegundoContratoDaMesmaEmbarcacao() {
        Contexto contexto = criarContexto();
        Contrato contrato = criarContratoPersistido(contexto);
        contrato.enviarParaAssinatura();
        prepararContextoAutenticado(contexto);
        when(contratoRepository.findByIdAndOrganizacaoId(
            contrato.getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(contrato));
        when(contratoRepository
            .existsByOrganizacaoIdAndEmbarcacaoIdAndStatusInAndIdNot(
                any(),
                any(),
                any(),
                any()
            )).thenReturn(true);

        assertThatThrownBy(() -> contratoService.ativar(
            contrato.getId(),
            new AtivarContratoRequest(LocalDate.now(), null)
        ))
            .isInstanceOf(ConflitoDadosException.class)
            .hasMessageContaining("outro contrato comercial ativo");
    }

    @Test
    void deveVincularOcupacaoAtivaDaMesmaEmbarcacao() {
        Contexto contexto = criarContexto();
        Contrato contrato = criarContratoPersistido(contexto);
        contrato.enviarParaAssinatura();
        contrato.ativar(LocalDate.now());
        Ocupacao ocupacao = criarOcupacao(contexto);
        prepararContextoAutenticado(contexto);
        when(contratoRepository.findByIdAndOrganizacaoId(
            contrato.getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(contrato));
        when(ocupacaoRepository.findByIdAndOrganizacaoId(
            ocupacao.getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(ocupacao));
        when(vinculoRepository.save(any(VinculoContratoOcupacao.class)))
            .thenAnswer(invocacao -> {
                VinculoContratoOcupacao vinculo = invocacao.getArgument(0);
                ReflectionTestUtils.setField(
                    vinculo,
                    "id",
                    UUID.randomUUID()
                );
                return vinculo;
            });

        var response = contratoService.vincularOcupacao(
            contrato.getId(),
            new VincularOcupacaoContratoRequest(ocupacao.getId())
        );

        assertThat(response.ocupacaoId()).isEqualTo(ocupacao.getId());
        assertThat(response.aberto()).isTrue();
        verify(historicoRepository).save(any(HistoricoContrato.class));
    }

    private void prepararContextoAutenticado(Contexto contexto) {
        when(leitorTokenJwt.obterUsuarioAutenticado()).thenReturn(
            new UsuarioAutenticado(
                contexto.usuario().getId(),
                "Administrador",
                "admin@marina.com",
                PerfilUsuario.ADMINISTRADOR_MARINA,
                contexto.organizacao().getId(),
                "Marina Teste"
            )
        );
        when(organizacaoRepository.findById(contexto.organizacao().getId()))
            .thenReturn(Optional.of(contexto.organizacao()));
        when(usuarioRepository.findByIdAndOrganizacaoId(
            contexto.usuario().getId(),
            contexto.organizacao().getId()
        )).thenReturn(Optional.of(contexto.usuario()));
    }

    private Contexto criarContexto() {
        Organizacao organizacao = Organizacao.criar(
            "Marina Teste",
            "marina-teste",
            "Marina Teste LTDA",
            "12345678000199",
            "contato@marina.com",
            "41999999999"
        );
        ReflectionTestUtils.setField(
            organizacao,
            "id",
            UUID.randomUUID()
        );

        Cliente cliente = criarCliente(
            UUID.randomUUID(),
            organizacao,
            "Joao da Silva"
        );
        Embarcacao embarcacao = new Embarcacao(
            organizacao,
            cliente,
            "Aurora",
            TipoEmbarcacao.LANCHA,
            "Schaefer",
            "V33",
            2024,
            "PR-123456",
            "BR-SCH12345A324",
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
            null
        );
        ReflectionTestUtils.setField(
            embarcacao,
            "id",
            UUID.randomUUID()
        );

        Usuario usuario = Usuario.criar(
            organizacao,
            "Administrador",
            "admin@marina.com",
            "hash",
            PerfilUsuario.ADMINISTRADOR_MARINA
        );
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());

        return new Contexto(
            organizacao,
            cliente,
            embarcacao,
            usuario
        );
    }

    private Cliente criarCliente(
        UUID id,
        Organizacao organizacao,
        String nome
    ) {
        Cliente cliente = new Cliente(
            organizacao,
            TipoPessoa.FISICA,
            nome,
            null,
            "52998224725",
            "cliente@email.com",
            null,
            "41999999999",
            null
        );
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }

    private Contrato criarContratoPersistido(Contexto contexto) {
        Contrato contrato = new Contrato(
            contexto.organizacao(),
            "CTR-2026-000001",
            contexto.cliente(),
            contexto.embarcacao(),
            TipoVaga.SECA,
            PeriodicidadeContrato.MENSAL,
            LocalDate.of(2026, 7, 1),
            null,
            true,
            30,
            new BigDecimal("1500.00"),
            10,
            null,
            contexto.usuario()
        );
        ReflectionTestUtils.setField(
            contrato,
            "id",
            UUID.randomUUID()
        );
        ReflectionTestUtils.setField(contrato, "versao", 0L);
        return contrato;
    }

    private Ocupacao criarOcupacao(Contexto contexto) {
        Vaga vaga = new Vaga(
            contexto.organizacao(),
            "A-01",
            TipoVaga.SECA,
            "Galpao A",
            "Corredor 1",
            new BigDecimal("12.00"),
            new BigDecimal("4.00"),
            new BigDecimal("2.00"),
            new BigDecimal("4.00"),
            new BigDecimal("7000.00"),
            false,
            true,
            null
        );
        ReflectionTestUtils.setField(vaga, "id", UUID.randomUUID());
        Ocupacao ocupacao = new Ocupacao(
            contexto.organizacao(),
            contexto.embarcacao(),
            vaga,
            Instant.now().minusSeconds(3600),
            null,
            null
        );
        ReflectionTestUtils.setField(
            ocupacao,
            "id",
            UUID.randomUUID()
        );
        return ocupacao;
    }

    private CriarContratoRequest criarRequest(
        UUID clienteId,
        UUID embarcacaoId
    ) {
        return new CriarContratoRequest(
            clienteId,
            embarcacaoId,
            TipoVaga.SECA,
            PeriodicidadeContrato.MENSAL,
            LocalDate.of(2026, 7, 1),
            null,
            true,
            30,
            new BigDecimal("1500.00"),
            10,
            "Contrato mensal"
        );
    }

    private record Contexto(
        Organizacao organizacao,
        Cliente cliente,
        Embarcacao embarcacao,
        Usuario usuario
    ) {
    }
}
