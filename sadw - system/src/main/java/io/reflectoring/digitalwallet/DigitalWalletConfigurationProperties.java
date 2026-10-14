package io.reflectoring.digitalwallet;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "digitalwallet")
public class DigitalWalletConfigurationProperties {

  private long transferThreshold = Long.MAX_VALUE;

}
