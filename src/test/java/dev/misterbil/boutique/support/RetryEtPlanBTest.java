package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.Question;
import com.sun.net.httpserver.HttpServer;
import dev.misterbil.boutique.support.domaine.StatutTraitement;
import dev.misterbil.boutique.support.domaine.SupportService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.4 : un serveur HTTP de test remplace Ollama et répond toujours 503.
 * Le nombre de requêtes reçues mesure le retry de Spring AI, puis l'effet du circuit breaker.
 */
@SpringBootTest(properties = {
		"spring.ai.retry.max-attempts=2",
		"spring.ai.retry.backoff.initial-interval=100ms",
		"resilience4j.circuitbreaker.instances.assistant.minimum-number-of-calls=3",
		"resilience4j.circuitbreaker.instances.assistant.sliding-window-size=3"
})
class RetryEtPlanBTest {

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
			System.out.println(">>> " + echange.getRequestMethod() + " " + echange.getRequestURI());
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

	@Autowired
	CircuitBreakerRegistry circuits;

	@BeforeEach
	void reinitialiser() {
		circuits.circuitBreaker("assistant").reset();
		requetes.set(0);
	}

	@Test
	void un_appel_en_echec_declenche_le_plan_b_et_on_compte_les_tentatives() {
		var res = support.traiter(new Question("client-test", "Où est ma commande 42 ?"));

		assertThat(res.statut()).isEqualTo(StatutTraitement.EN_ATTENTE_CONSEILLER);
		System.out.println(">>> Requêtes reçues pour 1 appel avec spring.ai.retry.max-attempts=2 : " + requetes.get());
	}

	@Test
	void le_circuit_ouvert_n_appelle_plus_le_llm() {
		for (int i = 0; i < 3; i++) {
			support.traiter(new Question("client-test", "Question " + i));
		}
		int avant = requetes.get();
		System.out.println(">>> État du circuit après 3 échecs : " + circuits.circuitBreaker("assistant").getState());

		var res = support.traiter(new Question("client-test", "Encore une question"));

		assertThat(res.statut()).isEqualTo(StatutTraitement.EN_ATTENTE_CONSEILLER);
		assertThat(requetes.get()).as("aucune requête quand le circuit est ouvert").isEqualTo(avant);
	}
}
