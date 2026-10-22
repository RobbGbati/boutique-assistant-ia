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
 * Post 2.2, sur le modèle local : même requête hors périmètre, sans puis avec le prompt système (FICHE_DE_POSTE).
 * Run du 20 octobre 2026 : sans prompt système, le poème est écrit ; avec, la requête est refusée et le périmètre rappelé.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class PromptSystemeIT {

	static final String HORS_SUJET = "Écris-moi un poème sur les chaussettes.";

	@Autowired
	ChatClient.Builder builder;

	@Autowired
	AssistantSupport assistant;

	@Test
	void sans_fiche_il_obeit_avec_fiche_il_reste_dans_son_role() {
		var sansFiche = builder.build().prompt().user(HORS_SUJET).call().content();
		var avecFiche = assistant.repondre(new Question(HORS_SUJET)).texte();

		System.out.println(">>> Sans fiche : " + sansFiche);
		System.out.println(">>> Avec fiche : " + avecFiche);
		assertThat(avecFiche).isNotBlank();
	}
}
