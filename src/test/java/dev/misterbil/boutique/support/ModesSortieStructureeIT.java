package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AnalyseTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Exploration (post 1.2) : fiabilité des modes de sortie structurée de Spring AI sur le modèle local.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class ModesSortieStructureeIT {

	static final String TICKET = "Ma commande 42 devait arriver il y a 10 jours, toujours rien. Je veux une réponse aujourd'hui.";
	static final int ESSAIS = 5;

	@Autowired
	ChatClient.Builder builder;

	@Test
	void comparer_les_modes() {
		var chat = builder.build();
		mesurer("prompt (défaut)", () -> chat.prompt().user(TICKET).call().entity(AnalyseTicket.class));
		mesurer("validateSchema", () -> chat.prompt().user(TICKET).call()
				.entity(AnalyseTicket.class, spec -> spec.validateSchema()));
		mesurer("natif fournisseur", () -> chat.prompt().user(TICKET).call()
				.entity(AnalyseTicket.class, spec -> spec.useProviderStructuredOutput()));
	}

	private void mesurer(String mode, java.util.function.Supplier<AnalyseTicket> appel) {
		int ok = 0;
		for (int i = 0; i < ESSAIS; i++) {
			try {
				System.out.println(">>> " + mode + " : " + appel.get());
				ok++;
			}
			catch (RuntimeException e) {
				System.out.println(">>> " + mode + " : ÉCHEC " + e.getClass().getSimpleName());
			}
		}
		System.out.println(">>> BILAN " + mode + " : " + ok + "/" + ESSAIS);
	}
}
