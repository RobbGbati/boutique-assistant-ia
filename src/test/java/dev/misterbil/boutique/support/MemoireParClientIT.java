package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 2.3, sur le modèle local : deux clients écrivent en même temps, chacun retrouve sa conversation.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class MemoireParClientIT {

	static final String RAPPEL = "Pour le suivi de mon dossier, rappelle-moi mon prénom. Réponds juste le prénom.";

	@Autowired
	AssistantSupport assistant;

	@Test
	void chaque_client_retrouve_sa_conversation() {
		assistant.repondre(new Question("paul", "Bonjour, je m'appelle Paul."));
		assistant.repondre(new Question("lea", "Bonjour, je m'appelle Léa."));

		var paul = assistant.repondre(new Question("paul", RAPPEL)).texte();
		var lea = assistant.repondre(new Question("lea", RAPPEL)).texte();
		var inconnu = assistant.repondre(new Question("eve", RAPPEL)).texte();

		System.out.println(">>> Paul : " + paul + " | Léa : " + lea + " | Ève (aucun historique) : " + inconnu);
		assertThat(paul).contains("Paul").doesNotContain("Léa");
		assertThat(lea).contains("Léa").doesNotContain("Paul");
	}
}
