package dev.misterbil.boutique.support;

import com.sun.net.httpserver.HttpServer;
import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import dev.misterbil.boutique.support.domaine.Reponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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
 * Post 2.5 : le flux passe par le même circuit breaker que repondre().
 * Un serveur HTTP de test remplace Ollama et répond toujours 503.
 */
@SpringBootTest(properties = {
		"spring.ai.retry.max-attempts=0",
		"resilience4j.circuitbreaker.instances.assistant.minimum-number-of-calls=3",
		"resilience4j.circuitbreaker.instances.assistant.sliding-window-size=3"
})
class StreamingPlanBTest {

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
	AssistantSupport assistant;

	@Autowired
	CircuitBreakerRegistry circuits;

	@BeforeEach
	void reinitialiser() {
		circuits.circuitBreaker("assistant").reset();
		requetes.set(0);
	}

	private Iterable<String> enDirect(String texte) {
		return assistant.repondreEnDirect(new Question("client-test", texte)).collectList().block();
	}

	@Test
	void un_flux_en_echec_renvoie_le_plan_b() {
		assertThat(enDirect("Où est ma commande 42 ?")).containsExactly(Reponse.indisponible().texte());
		assertThat(requetes.get()).isEqualTo(1);
	}

	@Test
	void le_circuit_ouvert_n_appelle_plus_le_llm() {
		for (int i = 0; i < 3; i++) {
			enDirect("Question " + i);
		}
		int avant = requetes.get();

		var fragments = enDirect("Encore une question");

		assertThat(circuits.circuitBreaker("assistant").getState()).isEqualTo(CircuitBreaker.State.OPEN);
		assertThat(fragments).containsExactly(Reponse.indisponible().texte());
		assertThat(requetes.get()).as("aucune requête quand le circuit est ouvert").isEqualTo(avant);
	}
}
