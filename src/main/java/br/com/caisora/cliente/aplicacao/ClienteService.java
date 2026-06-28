package br.com.caisora.cliente.aplicacao;

import br.com.caisora.autenticacao.aplicacao.LeitorTokenJwt;
import br.com.caisora.cliente.api.AlterarStatusClienteRequest;
import br.com.caisora.cliente.api.AtualizarClienteRequest;
import br.com.caisora.cliente.api.ClienteResponse;
import br.com.caisora.cliente.api.CriarClienteRequest;
import br.com.caisora.cliente.dominio.Cliente;
import br.com.caisora.cliente.dominio.ClienteRepository;
import br.com.caisora.cliente.dominio.TipoPessoa;
import br.com.caisora.compartilhado.excecao.ConflitoDadosException;
import br.com.caisora.compartilhado.excecao.DadosInvalidosException;
import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final ClienteMapper clienteMapper;
    private final LeitorTokenJwt leitorTokenJwt;

    public ClienteService(
        ClienteRepository clienteRepository,
        OrganizacaoRepository organizacaoRepository,
        ClienteMapper clienteMapper,
        LeitorTokenJwt leitorTokenJwt
    ) {
        this.clienteRepository = clienteRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.clienteMapper = clienteMapper;
        this.leitorTokenJwt = leitorTokenJwt;
    }

    @Transactional
    public ClienteResponse criar(CriarClienteRequest request) {
        UUID organizacaoId = obterOrganizacaoAutenticada();
        Organizacao organizacao = buscarOrganizacao(organizacaoId);

        String cpfCnpjNormalizado =
            normalizarDocumento(request.cpfCnpj());

        validarDados(
            request.tipoPessoa(),
            request.razaoSocial(),
            cpfCnpjNormalizado
        );

        validarDocumentoDisponivel(
            organizacaoId,
            cpfCnpjNormalizado
        );

        Cliente cliente = new Cliente(
            organizacao,
            request.tipoPessoa(),
            normalizarTextoObrigatorio(request.nome()),
            normalizarTextoOpcional(request.razaoSocial()),
            cpfCnpjNormalizado,
            normalizarEmail(request.email()),
            normalizarTelefone(request.telefone()),
            normalizarTelefone(request.celular()),
            normalizarTextoOpcional(request.observacoes())
        );

        Cliente clienteSalvo = clienteRepository.save(cliente);

        return clienteMapper.paraResponse(clienteSalvo);
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(Pageable paginacao) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        return clienteRepository
            .findAllByOrganizacaoId(
                organizacaoId,
                paginacao
            )
            .map(clienteMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listarPorStatus(
        boolean ativo,
        Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        return clienteRepository
            .findAllByOrganizacaoIdAndAtivo(
                organizacaoId,
                ativo,
                paginacao
            )
            .map(clienteMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> buscarPorNome(
        String nome,
        Pageable paginacao
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        return clienteRepository
            .findAllByOrganizacaoIdAndNomeContainingIgnoreCase(
                organizacaoId,
                nome.trim(),
                paginacao
            )
            .map(clienteMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(UUID id) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        Cliente cliente =
            buscarEntidadePorId(organizacaoId, id);

        return clienteMapper.paraResponse(cliente);
    }

    @Transactional
    public ClienteResponse atualizar(
        UUID id,
        AtualizarClienteRequest request
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        Cliente cliente =
            buscarEntidadePorId(organizacaoId, id);

        String cpfCnpjNormalizado =
            normalizarDocumento(request.cpfCnpj());

        validarDados(
            request.tipoPessoa(),
            request.razaoSocial(),
            cpfCnpjNormalizado
        );

        validarDocumentoDisponivelNaAtualizacao(
            organizacaoId,
            cpfCnpjNormalizado,
            id
        );

        cliente.atualizarDados(
            request.tipoPessoa(),
            normalizarTextoObrigatorio(request.nome()),
            normalizarTextoOpcional(request.razaoSocial()),
            cpfCnpjNormalizado,
            normalizarEmail(request.email()),
            normalizarTelefone(request.telefone()),
            normalizarTelefone(request.celular()),
            normalizarTextoOpcional(request.observacoes())
        );

        Cliente clienteSalvo = clienteRepository.save(cliente);

        return clienteMapper.paraResponse(clienteSalvo);
    }

    @Transactional
    public ClienteResponse alterarStatus(
        UUID id,
        AlterarStatusClienteRequest request
    ) {
        UUID organizacaoId = obterOrganizacaoAutenticada();

        Cliente cliente =
            buscarEntidadePorId(organizacaoId, id);

        if (Boolean.TRUE.equals(request.ativo())) {
            cliente.ativar();
        } else {
            cliente.inativar();
        }

        Cliente clienteSalvo = clienteRepository.save(cliente);

        return clienteMapper.paraResponse(clienteSalvo);
    }

    private Cliente buscarEntidadePorId(
        UUID organizacaoId,
        UUID clienteId
    ) {
        return clienteRepository
            .findByIdAndOrganizacaoId(
                clienteId,
                organizacaoId
            )
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Cliente nao encontrado"
                )
            );
    }

    private Organizacao buscarOrganizacao(
        UUID organizacaoId
    ) {
        return organizacaoRepository
            .findById(organizacaoId)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Organizacao nao encontrada"
                )
            );
    }

    private void validarDocumentoDisponivel(
        UUID organizacaoId,
        String cpfCnpj
    ) {
        boolean documentoExistente =
            clienteRepository
                .existsByOrganizacaoIdAndCpfCnpj(
                    organizacaoId,
                    cpfCnpj
                );

        if (documentoExistente) {
            throw new ConflitoDadosException(
                "Ja existe um cliente com este CPF ou CNPJ na organizacao"
            );
        }
    }

    private void validarDocumentoDisponivelNaAtualizacao(
        UUID organizacaoId,
        String cpfCnpj,
        UUID clienteId
    ) {
        boolean documentoExistente =
            clienteRepository
                .existsByOrganizacaoIdAndCpfCnpjAndIdNot(
                    organizacaoId,
                    cpfCnpj,
                    clienteId
                );

        if (documentoExistente) {
            throw new ConflitoDadosException(
                "Ja existe um cliente com este CPF ou CNPJ na organizacao"
            );
        }
    }

    private void validarDados(
        TipoPessoa tipoPessoa,
        String razaoSocial,
        String cpfCnpj
    ) {
        if (tipoPessoa == null) {
            throw new IllegalArgumentException(
                "Tipo de pessoa obrigatorio"
            );
        }

        if (
            tipoPessoa == TipoPessoa.JURIDICA
                && (razaoSocial == null || razaoSocial.isBlank())
        ) {
            throw new DadosInvalidosException(
                "RAZAO_SOCIAL_OBRIGATORIA",
                "Razao social obrigatoria para pessoa juridica"
            );
        }

        if (
            tipoPessoa == TipoPessoa.FISICA
                && cpfCnpj.length() != 11
        ) {
            throw new DadosInvalidosException(
                "CPF_INVALIDO",
                "CPF invalido"
            );
        }

        if (
            tipoPessoa == TipoPessoa.JURIDICA
                && cpfCnpj.length() != 14
        ) {
            throw new DadosInvalidosException(
            "CNPJ_INVALIDO",
            "CNPJ invalido"
            );
        }
    }

    private UUID obterOrganizacaoAutenticada() {
        return leitorTokenJwt
            .obterUsuarioAutenticado()
            .organizacaoId();
    }

    private String normalizarDocumento(
        String documento
    ) {
        if (documento == null || documento.isBlank()) {
            throw new DadosInvalidosException(
                "DOCUMENTO_OBRIGATORIO",
                "CPF ou CNPJ obrigatorio"
            );
        }

        return documento.replaceAll("\\D", "");
    }

    private String normalizarTelefone(
        String telefone
    ) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }

        return telefone.replaceAll("\\D", "");
    }

    private String normalizarEmail(
        String email
    ) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private String normalizarTextoObrigatorio(
        String texto
    ) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                "Texto obrigatorio"
            );
        }

        return texto.trim();
    }

    private String normalizarTextoOpcional(
        String texto
    ) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}