package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.3, niveau 2 : intégration avec le modèle local.
 * Assertions sur le contrat de l'adapter (réponse non vide, mapping), pas sur le texte généré.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class SpringAiAssistantIT {

	@Autowired
	AssistantSupport assistant;

	@Autowired
	ChatClient.Builder builder;

	@Test
	void l_adapter_obtient_une_reponse() {
		var reponse = assistant.repondre(new Question("Réponds en une phrase : quel est le délai de livraison standard en France ?"));

		assertThat(reponse.texte()).isNotBlank();
	}

	/** Post 1.2 : le JSON se mappe dans le record, et le résultat respecte les règles métier. */
	@Test
	void l_analyse_d_un_ticket_se_mappe_dans_le_record() {
		var analyse = assistant.analyser("Ma commande 42 devait arriver il y a 10 jours, toujours rien. Je veux une réponse aujourd'hui.");

		System.out.println(">>> Analyse : " + analyse);
		assertThat(analyse.categorie()).isNotNull();
		assertThat(analyse.urgence()).isBetween(1, 5);
	}

}
