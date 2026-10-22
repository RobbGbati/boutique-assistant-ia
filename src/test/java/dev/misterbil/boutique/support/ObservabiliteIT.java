package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 2.7, sur le modèle local : après un appel, les métriques Micrometer de Spring AI contiennent la durée, le modèle et les tokens.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class ObservabiliteIT {

	@Autowired
	AssistantSupport assistant;

	@Autowired
	MeterRegistry metriques;

	@Test
	void chaque_appel_est_mesure() {
		assistant.repondre(new Question("paul", "Où en est ma commande 42 ?"));

		var appels = metriques.get("gen_ai.client.operation").timer();
		var tokens = metriques.get("gen_ai.client.token.usage").tag("gen_ai.token.type", "total").counter();
		System.out.println(">>> gen_ai.client.operation : " + appels.count() + " appel(s), max "
				+ appels.max(java.util.concurrent.TimeUnit.MILLISECONDS) + " ms | tokens total : " + tokens.count());
		assertThat(appels.count()).isPositive();
		assertThat(tokens.count()).isPositive();
	}
}
