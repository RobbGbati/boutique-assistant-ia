package dev.misterbil.boutique.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Post 2.5, sur le modèle local : même question en call() puis en stream().
 * Run du 20 octobre 2026 (3 runs) : total ≈ 2,7 s dans les deux cas, premier fragment en 20 à 53 ms.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class StreamingIT {

	static final String QUESTION = "Explique en 150 mots comment se passe un retour d'article dans une boutique en ligne.";

	@Autowired
	ChatClient.Builder builder;

	@Test
	void le_streaming_ne_change_pas_le_total_mais_le_premier_mot() {
		var chat = builder.clone().defaultOptions(ChatOptions.builder().temperature(0.0)).build();
		for (int run = 1; run <= 3; run++) {
			var debut = System.nanoTime();
			chat.prompt().user(QUESTION).call().content();
			var bloquant = (System.nanoTime() - debut) / 1_000_000;

			var debutFlux = System.nanoTime();
			var premier = new long[] {-1};
			var morceaux = new AtomicInteger();
			chat.prompt().user(QUESTION).stream().content()
					.doOnNext(m -> {
						if (premier[0] < 0) {
							premier[0] = (System.nanoTime() - debutFlux) / 1_000_000;
						}
						morceaux.incrementAndGet();
					})
					.blockLast();
			var total = (System.nanoTime() - debutFlux) / 1_000_000;
			System.out.println(">>> Run " + run + " : call() " + bloquant + " ms | stream() premier morceau "
					+ premier[0] + " ms, total " + total + " ms, " + morceaux.get() + " morceaux");
		}
	}
}
