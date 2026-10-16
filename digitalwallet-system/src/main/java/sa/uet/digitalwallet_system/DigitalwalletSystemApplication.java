package sa.uet.digitalwallet_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
public class DigitalwalletSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalwalletSystemApplication.class, args);
	}

}
