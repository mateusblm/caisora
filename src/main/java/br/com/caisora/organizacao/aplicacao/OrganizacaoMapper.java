package br.com.caisora.organizacao.aplicacao;

import br.com.caisora.organizacao.api.CriarOrganizacaoRequest;
import br.com.caisora.organizacao.api.OrganizacaoResponse;
import br.com.caisora.organizacao.dominio.Organizacao;
import org.springframework.stereotype.Component;

@Component
public class OrganizacaoMapper {

    public Organizacao paraEntidade(CriarOrganizacaoRequest request) {
        return Organizacao.criar(
                request.nome(),
                request.razaoSocial(),
                request.documento(),
                request.email(),
                request.telefone());
    }

    public OrganizacaoResponse paraResponse(Organizacao organizacao) {
        return new OrganizacaoResponse(
                organizacao.getId(),
                organizacao.getNome(),
                organizacao.getRazaoSocial(),
                organizacao.getDocumento(),
                organizacao.getEmail(),
                organizacao.getTelefone(),
                organizacao.isAtiva(),
                organizacao.getCriadaEm(),
                organizacao.getAtualizadaEm());
    }
}
