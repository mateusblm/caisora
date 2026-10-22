package br.com.caisora.organizacao.dominio;
import static org.assertj.core.api.Assertions.*;
import br.com.caisora.organizacao.api.CoordenadasRequest;
import java.math.BigDecimal;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
class CoordenadasTest {
 @Test void aceitaLimitesERejeitaCoordenadasForaDaTerra(){
  var org=Organizacao.criar("Marina","marina",null,null,"marina@example.com",null);
  org.configurarCoordenadas(new BigDecimal("-90"),new BigDecimal("180"));assertThat(org.getLongitude()).isEqualByComparingTo("180");
  assertThatThrownBy(()->org.configurarCoordenadas(new BigDecimal("90.000001"),BigDecimal.ZERO)).isInstanceOf(RuntimeException.class);
  assertThatThrownBy(()->org.configurarCoordenadas(null,BigDecimal.ZERO)).isInstanceOf(RuntimeException.class);
 }
 @Test void validaPrecisaoEParObrigatorio(){try(var factory=Validation.buildDefaultValidatorFactory()){
  var validator=factory.getValidator();assertThat(validator.validate(new CoordenadasRequest(new BigDecimal("-25.123456"),new BigDecimal("-48.123456")))).isEmpty();
  assertThat(validator.validate(new CoordenadasRequest(new BigDecimal("1.1234567"),null))).hasSize(2);
 }}
}
