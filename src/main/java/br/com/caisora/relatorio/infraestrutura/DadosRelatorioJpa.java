package br.com.caisora.relatorio.infraestrutura;

import br.com.caisora.relatorio.dominio.DadosRelatorio;
import br.com.caisora.vaga.dominio.*;
import br.com.caisora.movimentacao.dominio.*;
import br.com.caisora.contrato.dominio.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class DadosRelatorioJpa implements DadosRelatorio {
    private final EntityManager em;
    public DadosRelatorioJpa(EntityManager em) {
        this.em=em;
    }
    public List<VagaOcupada> vagas(UUID org,String setor,TipoVaga tipo) {
        var q=em.createQuery("""
   select v.setor, v.tipo,
    case when exists (select o.id from Ocupacao o where o.vaga.id=v.id and o.organizacao.id=:org
      and o.status=br.com.caisora.ocupacao.dominio.StatusOcupacao.ATIVA) then true else false end
   from Vaga v where v.organizacao.id=:org and v.ativa=true
   """+(setor==null?"":" and lower(v.setor)=lower(:setor)")+(tipo==null?"":" and v.tipo=:tipo"),Object[].class).setParameter("org",org);
        if(setor!=null) q.setParameter("setor",setor);
        if(tipo!=null) q.setParameter("tipo",tipo);
        return q.getResultList().stream().map(row->new VagaOcupada((String)row[0],(TipoVaga)row[1],(Boolean)row[2])).toList();
    }
    public List<Movimentacao> movimentacoes(UUID org,Instant inicio,Instant fim,TipoMovimentacao tipo) {
        var q=em.createQuery("select m from Movimentacao m where m.organizacao.id=:org and m.agendadaPara>=:inicio and m.agendadaPara<:fim"+
        (tipo==null?"":" and m.tipo=:tipo"),Movimentacao.class).setParameter("org",org).setParameter("inicio",inicio).setParameter("fim",fim);
        if(tipo!=null) q.setParameter("tipo",tipo);
        return q.getResultList();
    }
    public List<Contrato> contratos(UUID org,StatusContrato status,PeriodicidadeContrato periodicidade,LocalDate inicio,LocalDate fim) {
        var q=em.createQuery("select c from Contrato c where c.organizacao.id=:org"+
        (status==null?"":" and c.status=:status")+(periodicidade==null?"":" and c.periodicidade=:periodicidade")+
        (inicio==null?"":" and c.dataInicio>=:inicio")+(fim==null?"":" and c.dataInicio<=:fim"),Contrato.class).setParameter("org",org);
        if(status!=null) q.setParameter("status",status);
        if(periodicidade!=null) q.setParameter("periodicidade",periodicidade);
        if(inicio!=null) q.setParameter("inicio",inicio);
        if(fim!=null) q.setParameter("fim",fim);
        return q.getResultList();
    }
}
