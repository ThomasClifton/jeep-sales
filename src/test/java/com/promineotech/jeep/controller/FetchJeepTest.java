package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import com.promineotech.jeep.controller.support.FetchJeepTestSupport;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

@ActiveProfiles("test")
@Sql(scripts = {
    "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
    "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, 
    config = @SqlConfig(encoding = "utf-8")
)

class FetchJeepTest extends FetchJeepTestSupport{

  @Test
  void testThatJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {
    // GIVEN: a valid model, trim, uri
    JeepModel model = JeepModel.WRANGLER;
    String trim = "Sport";
    String uri = String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);
    
    // WHEN: a connection is made to the uri
    ResponseEntity<List<Jeep>> response = getRestTemplate().exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

    System.out.println(response);
    // THEN: a success (OK - 220) code is returned
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    
    //And: the actual list is the same as the expected list
    List<Jeep> actual = response.getBody();
    List<Jeep> expected = buildExpected();
    
    //actual.forEach(jeep -> jeep.setModelPK(0));
    
    assertThat(actual).isEqualTo(expected);
  }
  
  @Test
  void testThatAnErrorMessageIsReturnedWhenAnUnkownTrimIsSupplied() {
    // GIVEN: a valid model, trim, uri
    JeepModel model = JeepModel.WRANGLER;
    String trim = "unknownvalue";
    String uri = String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);
    
    // WHEN: a connection is made to the uri
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

    
    // THEN: a not found (404) code is returned
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    
    //And: the actual list is the same as the expected list
    Map<String, Object> error = response.getBody();
    
    assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
  }
  
  @ParameterizedTest
  @MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
  void testThatAnErrorMessageIsReturnedWhenAnInvalidTrimIsSupplied(String model, String trim, String reason) {
    // GIVEN: a valid model, trim, uri
    String uri = String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);
    
    // WHEN: a connection is made to the uri
    ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

    
    // THEN: a not found (404) code is returned
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    
    //And: the actual list is the same as the expected list
    Map<String, Object> error = response.getBody();
    
    assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);
  }
  
  static Stream<Arguments> parametersForInvalidInput(){
    // @formatter:off
    return Stream.of(
        arguments("WRANGLER", "@ji3#%", "Trim contains non-alpha-numeric characters"),
        arguments("WRANGLER", "C".repeat(31), "Trim string too long"),
        arguments("INVALID", "sport", "Model is not an enum value")
    );
    // @formatter:on
  }

  protected void assertErrorMessageValid(Map<String, Object> error, HttpStatus status) {
    //@formatter:off
    assertThat(error)
      .containsKey("message")
      .containsEntry("status code", status.value())
      .containsKey("uri")
      .containsKey("timestamp")
      .containsEntry("reason", status.getReasonPhrase());
    // @formatter:on
  }

  private List<Jeep> buildExpected() {
    List<Jeep> list = new LinkedList<>();
    
    // @formatter:off
    list.add(Jeep.builder()
        .modelId(JeepModel.WRANGLER)
        .trimLevel("Sport")
        .numDoors(2)
        .wheelSize(17)
        .basePrice(new BigDecimal("28475.00"))
        .build());
    list.add(Jeep.builder()
        .modelId(JeepModel.WRANGLER)
        .trimLevel("Sport")
        .numDoors(4)
        .wheelSize(17)
        .basePrice(new BigDecimal("31975.00"))
        .build());
    // @formatter:on
    
    Collections.sort(list);
    return list;
  }
}
