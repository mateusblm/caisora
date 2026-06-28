package br.com.caisora.autenticacao.aplicacao;

import br.com.caisora.usuario.dominio.Usuario;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
public class GeradorTokenJwt {

    private final JwtEncoder jwtEncoder;
    private final long expiracaoSegundos;

    public GeradorTokenJwt(
            JwtEncoder jwtEncoder,
            @Value("${caisora.seguranca.jwt.expiracao-segundos}") long expiracaoSegundos
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expiracaoSegundos = expiracaoSegundos;
    }

    /**
     * Gera um JWT assinado pelo backend. O token contem identidade do usuario,
     * perfil e organizacao para que as proximas requisicoes nao dependam do header
     * temporario `X-Organizacao-Id`.
     */
    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(expiracaoSegundos))
                .claim("nome", usuario.getNome())
                .claim("email", usuario.getEmail())
                .claim("perfil", usuario.getPerfil().name())
                .claim("organizacaoId", usuario.getOrganizacao().getId().toString())
                .claim("organizacaoNome", usuario.getOrganizacao().getNome())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long obterExpiracaoSegundos() {
        return expiracaoSegundos;
    }
}
