package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.commande.infrastructure.OutilsCommande;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static dev.misterbil.boutique.support.infrastructure.SpringAiAssistant.FICHE_DE_POSTE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 3.1 : le LLM ne connaît que ce qu'on lui envoie.
 * Même question, 10 fois en session neuve, avec la fiche de poste seule, puis avec la politique de retour entière
 * collée dans le prompt système. Mêmes conditions que le retest du 2.8 : l'outil commande est branché.
 * Le classement oui / non lit le premier mot : chaque réponse est affichée pour être relue à la main.
 * Les lignes « politique » et « chat » sont celles montrées dans le reel du post 3.1.
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
		var fiche = builder.clone()
				.defaultSystem(FICHE_DE_POSTE)
				.build();

		var politique = new ClassPathResource(
				"docs/politique-retours.md")
				.getContentAsString(UTF_8);

		var chat = builder.clone()
				.defaultSystem(FICHE_DE_POSTE
						+ "\n" + politique)
				.build();

		mesurer("Fiche seule", fiche, true);
		mesurer("Fiche + politique", chat, true);
		// Témoins : mêmes prompts, sans l'outil commande, pour voir si l'outil pèse sur la réponse et sur les tokens
		mesurer("Fiche seule, sans outil", fiche, false);
		mesurer("Fiche + politique, sans outil", chat, false);
	}

	private void mesurer(String nom, ChatClient chat, boolean avecOutil) {
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
