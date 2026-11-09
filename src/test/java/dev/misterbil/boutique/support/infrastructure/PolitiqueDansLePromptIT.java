package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.commande.infrastructure.OutilsCommande;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 3.1 : le LLM ne connaît que ce qu'on lui envoie.
 * Même question, 10 fois en session neuve, avec la fiche de poste seule, puis avec la politique de retour entière
 * collée dans le prompt système. Mêmes conditions que le retest du 2.8 : l'outil commande est branché.
 * Le classement oui / non lit le premier mot : chaque réponse est affichée pour être relue à la main.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class PolitiqueDansLePromptIT {

	static final String QUESTION = "Je peux renvoyer un article ouvert ?";
	static final int ESSAIS = 10;

	@Autowired
	ChatClient.Builder builder;

	@Autowired
	OutilsCommande outilsCommande;

	@Test
	void la_politique_dans_le_prompt_systeme() throws IOException {
		var politique = new ClassPathResource("docs/politique-retours.md").getContentAsString(StandardCharsets.UTF_8);

		mesurer("Fiche seule", SpringAiAssistant.FICHE_DE_POSTE, true);
		mesurer("Fiche + politique", SpringAiAssistant.FICHE_DE_POSTE + "\n" + politique, true);
		// Témoins : mêmes prompts, sans l'outil commande, pour voir si l'outil pèse sur la réponse et sur les tokens
		mesurer("Fiche seule, sans outil", SpringAiAssistant.FICHE_DE_POSTE, false);
		mesurer("Fiche + politique, sans outil", SpringAiAssistant.FICHE_DE_POSTE + "\n" + politique, false);
	}

	private void mesurer(String nom, String systeme, boolean avecOutil) {
		var chat = builder.clone().defaultSystem(systeme).build();
		Map<String, Integer> score = new TreeMap<>();
		for (int i = 1; i <= ESSAIS; i++) {
			var requete = chat.prompt().user(QUESTION);
			if (avecOutil) {
				requete = requete.tools(outilsCommande)
						.toolContext(Map.of(OutilsCommande.CLIENT_ID, "essai-" + i));
			}
			var reponse = requete.call().chatResponse();
			var texte = reponse.getResult().getOutput().getText();
			var tokensEntree = reponse.getMetadata().getUsage().getPromptTokens();

			var verdict = verdict(texte);
			score.merge(verdict, 1, Integer::sum);
			System.out.printf(">>> [%s] %2d %-5s entrée=%d | %s%n", nom, i, verdict, tokensEntree, texte.replace('\n', ' '));
		}
		System.out.println(">>> [" + nom + "] SCORE : " + score);
		assertThat(score.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(ESSAIS);
	}

	static String verdict(String texte) {
		var debut = texte.strip().toLowerCase(Locale.FRENCH);
		if (debut.startsWith("oui")) {
			return "OUI";
		}
		if (debut.startsWith("non")) {
			return "NON";
		}
		return "AUTRE";
	}
}
