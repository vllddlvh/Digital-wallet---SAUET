package io.reflectoring.digitalwallet;

import io.reflectoring.digitalwallet.application.domain.service.MoneyTransferProperties;
import io.reflectoring.digitalwallet.application.domain.model.Money;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DigitalWalletConfigurationProperties.class)
public class DigitalWalletConfiguration {

  /**
   * Adds a use-case-specific {@link MoneyTransferProperties} object to the application context. The properties
   * are read from the Spring-Boot-specific {@link DigitalWalletConfigurationProperties} object.
   */
  @Bean
  public MoneyTransferProperties moneyTransferProperties(DigitalWalletConfigurationProperties digitalWalletConfigurationProperties){
    return new MoneyTransferProperties(Money.of(digitalWalletConfigurationProperties.getTransferThreshold()));
  }

}
