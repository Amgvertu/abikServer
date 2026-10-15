package info.prorabka.abik.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AbikServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AbikServerApplication.class, args);
	}
}