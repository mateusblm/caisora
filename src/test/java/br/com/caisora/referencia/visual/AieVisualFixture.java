package br.com.caisora.referencia.visual;

import br.com.caisora.organizacao.dominio.Organizacao;
import br.com.caisora.usuario.dominio.*;
import br.com.caisora.cliente.dominio.*;
import br.com.caisora.embarcacao.dominio.*;
import br.com.caisora.variabilidade.dominio.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.springframework.security.crypto.password.PasswordEncoder;

final class AieVisualFixture {
    record Usuarios(Usuario a,Usuario b,java.util.UUID embarcacaoId) {}
    static Usuarios criar(EntityManager em,PasswordEncoder encoder){
        var a=org(em,"aie-a","-27.59","-48.55");var b=org(em,"aie-b","-23.0","-43.0");
        var ua=Usuario.criar(a,"Administrador AIE","demo-aie@example.com",encoder.encode("Demo-AIE-2026"),PerfilUsuario.ADMINISTRADOR_MARINA);
        var ub=Usuario.criar(b,"Administrador AIE B","demo-aie-b@example.com",encoder.encode("Demo-AIE-2026"),PerfilUsuario.ADMINISTRADOR_MARINA);
        em.persist(ua);em.persist(ub);
        var cliente=new Cliente(a,TipoPessoa.FISICA,"Cliente teste AIE",null,"11111111111",null,null,null,null);em.persist(cliente);
        var barco=Embarcacao.builder().organizacao(a).proprietario(cliente).nome("Barco teste AIE")
                .tipo(TipoEmbarcacao.LANCHA).codigoPaisBandeira("PT").numeroInscricao("AIE-TESTE")
                .comprimentoTotalMetros(BigDecimal.TEN).bocaMetros(BigDecimal.ONE).tipoPropulsao(TipoPropulsao.MOTOR).construir();
        em.persist(barco);em.flush();return new Usuarios(ua,ub,barco.getId());
    }
    private static Organizacao org(EntityManager em,String slug,String lat,String lon){
        var org=Organizacao.criar("Marina teste "+slug,slug,null,null,slug+"@example.com",null);
        org.configurarCoordenadas(new BigDecimal(lat),new BigDecimal(lon));em.persist(org);
        em.persist(ModuloOrganizacao.criar(org.getId(),ModuloSistema.REFERENCIAS,true));return org;
    }
    static void navegador(int porta,String modo) throws Exception {
        var processo=new ProcessBuilder("node",java.nio.file.Path.of("scripts/validar-aie-browser.mjs").toAbsolutePath().toString(),"http://127.0.0.1:"+porta,modo).inheritIO().start();
        try{org.assertj.core.api.Assertions.assertThat(processo.waitFor(180,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            org.assertj.core.api.Assertions.assertThat(processo.exitValue()).isZero();}
        finally{if(processo.isAlive())processo.destroyForcibly();}
    }
}
