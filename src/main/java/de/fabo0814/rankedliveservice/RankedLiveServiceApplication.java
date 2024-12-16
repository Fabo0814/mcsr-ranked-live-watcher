package de.fabo0814.rankedliveservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.util.Collections;

@SpringBootApplication
@EnableWebSocket
public class RankedLiveServiceApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(RankedLiveServiceApplication.class);
		app.setDefaultProperties(Collections.singletonMap("server.port", "8080"));
		app.run(args);
	}

}
