package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.Question;
import com.sun.net.httpserver.HttpServer;
import dev.misterbil.boutique.support.domaine.SupportService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Post 1.4 : nombre de relances de Spring AI vers Ollama avec max-attempts par défaut.
 * Les délais de backoff sont réduits pour accélérer le test ; max-attempts garde sa valeur par défaut.
 */
@SpringBootTest(properties = {
		"spring.ai.retry.backoff.initial-interval=10ms",
		"spring.ai.retry.backoff.multiplier=1",
		"spring.ai.retry.backoff.max-interval=10ms"
})
class OllamaRetryParDefautTest {

	static final AtomicInteger requetes = new AtomicInteger();
	static final HttpServer serveur;

	static {
		try {
			serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		serveur.createContext("/", echange -> {
			requetes.incrementAndGet();
			echange.getRequestBody().readAllBytes();
			echange.sendResponseHeaders(503, -1);
			echange.close();
		});
		serveur.start();
	}

	@DynamicPropertySource
	static void ollamaEnPanne(DynamicPropertyRegistry registry) {
		registry.add("spring.ai.ollama.base-url", () -> "http://localhost:" + serveur.getAddress().getPort());
	}

	@AfterAll
	static void arreter() {
		serveur.stop(0);
	}

	@Autowired
	SupportService support;

	@Test
	void compter_les_appels_par_defaut() {
		support.traiter(new Question("client-test", "Où est ma commande 42 ?"));
		System.out.println(">>> Appels reçus avec le retry par défaut : " + requetes.get());
	}
}
