package br.com.caisora.organizacao.aplicacao;

import br.com.caisora.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.caisora.organizacao.api.AtualizarOrganizacaoRequest;
import br.com.caisora.organizacao.api.CriarOrganizacaoRequest;
import br.com.caisora.organizacao.api.OrganizacaoResponse;
import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.organizacao.dominio.OrganizacaoRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizacaoService {

    private final OrganizacaoRepository organizacaoRepository;
    private final OrganizacaoMapper organizacaoMapper;

    public OrganizacaoService(
            OrganizacaoRepository organizacaoRepository,
            OrganizacaoMapper organizacaoMapper
    ) {
        this.organizacaoRepository = organizacaoRepository;
        this.organizacaoMapper = organizacaoMapper;
    }

    @Transactional
    public OrganizacaoResponse criar(CriarOrganizacaoRequest request) {
        Organizacao organizacao = organizacaoMapper.paraEntidade(request);
        Organizacao organizacaoSalva = organizacaoRepository.save(organizacao);
        return organizacaoMapper.paraResponse(organizacaoSalva);
    }

    @Transactional(readOnly = true)
    public Page<OrganizacaoResponse> listar(Pageable paginacao) {
        return organizacaoRepository.findAll(paginacao)
                .map(organizacaoMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public OrganizacaoResponse buscarPorId(UUID id) {
        return organizacaoMapper.paraResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public OrganizacaoResponse atualizar(UUID id, AtualizarOrganizacaoRequest request) {
        Organizacao organizacao = buscarEntidadePorId(id);
        organizacao.atualizar(
                request.nome(),
                request.razaoSocial(),
                request.documento(),
                request.email(),
                request.telefone(),
                request.ativa());

        return organizacaoMapper.paraResponse(organizacao);
    }

    @Transactional
    public void inativar(UUID id) {
        Organizacao organizacao = buscarEntidadePorId(id);
        organizacao.inativar();
    }

    private Organizacao buscarEntidadePorId(UUID id) {
        return organizacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organizacao nao encontrada"));
    }
}
