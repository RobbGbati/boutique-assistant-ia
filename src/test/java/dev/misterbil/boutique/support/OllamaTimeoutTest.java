package dev.misterbil.boutique.support;

import com.sun.net.httpserver.HttpServer;
import dev.misterbil.boutique.support.domaine.StatutTraitement;
import dev.misterbil.boutique.support.domaine.SupportService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.4 : le timeout global de Spring Boot (spring.http.clients.read-timeout) interrompt-il un appel vers un Ollama lent ?
 */
@SpringBootTest(properties = {
		"spring.http.clients.read-timeout=1s",
		"spring.ai.retry.max-attempts=0"
})
class OllamaTimeoutTest {

	static final HttpServer serveur;

	static {
		try {
			serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		serveur.createContext("/", echange -> {
			echange.getRequestBody().readAllBytes();
			try {
				Thread.sleep(5_000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			echange.sendResponseHeaders(503, -1);
			echange.close();
		});
		serveur.start();
	}

	@DynamicPropertySource
	static void ollamaLent(DynamicPropertyRegistry registry) {
		registry.add("spring.ai.ollama.base-url", () -> "http://localhost:" + serveur.getAddress().getPort());
	}

	@AfterAll
	static void arreter() {
		serveur.stop(0);
	}

	@Autowired
	SupportService support;

	@Test
	void un_ollama_trop_lent_est_coupe() {
		long debut = System.nanoTime();
		var res = support.traiter("Où est ma commande 42 ?");
		long ms = (System.nanoTime() - debut) / 1_000_000;

		System.out.println(">>> Durée avec read-timeout=1s face à un serveur de 5 s : " + ms + " ms, statut " + res.statut());
		assertThat(res.statut()).isEqualTo(StatutTraitement.EN_ATTENTE_CONSEILLER);
	}
}
