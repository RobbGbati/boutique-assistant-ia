package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AnalyseTicket;
import dev.misterbil.boutique.support.domaine.Categorie;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.2 : ce que Spring AI fait (et ne fait pas) avec .entity() et validateSchema().
 * Le ChatModel de test renvoie des réponses fixées et enregistre chaque prompt reçu (un prompt reçu = un appel facturé).
 */
class SortieStructureeSpringAiTest {

	static final String VALIDE = "{\"categorie\": \"LIVRAISON\", \"urgence\": 4, \"resume\": \"Colis en retard\"}";

	/** ChatModel de test : renvoie les réponses dans l'ordre (la dernière se répète) et enregistre les prompts reçus. */
	static class FauxModele implements ChatModel {

		final List<Prompt> prompts = new ArrayList<>();
		private final List<String> reponses;

		FauxModele(String... reponses) {
			this.reponses = List.of(reponses);
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			var texte = reponses.get(Math.min(prompts.size(), reponses.size()) - 1);
			return new ChatResponse(List.of(new Generation(new AssistantMessage(texte))));
		}
	}

	@Test
	void entity_ajoute_le_format_attendu_au_prompt() {
		var modele = new FauxModele(VALIDE);

		ChatClient.builder(modele).build().prompt().user("Ma commande 42 ?").call().entity(AnalyseTicket.class);

		var format = new BeanOutputConverter<>(AnalyseTicket.class).getFormat();
		assertThat(modele.prompts.getFirst().getContents()).contains(format);
	}

	@Test
	void validateSchema_renvoie_l_erreur_au_modele_et_relance() {
		// 1re réponse hors schéma (catégorie inventée), 2e réponse correcte
		var modele = new FauxModele(VALIDE.replace("LIVRAISON", "VIP_URGENT"), VALIDE);

		var analyse = ChatClient.builder(modele).build().prompt().user("Ma commande 42 ?").call()
				.entity(AnalyseTicket.class, spec -> spec.validateSchema());

		System.out.println(">>> Prompt de la relance : " + modele.prompts.getLast().getContents());
		assertThat(analyse).isEqualTo(new AnalyseTicket(Categorie.LIVRAISON, 4, "Colis en retard"));
		assertThat(modele.prompts).hasSize(2); // la relance est un deuxième appel, donc des tokens en plus
		// le modèle reçoit l'erreur de validation pour se corriger
		assertThat(modele.prompts.getLast().getContents())
				.contains("Output JSON validation failed")
				.contains("LIVRAISON", "PAIEMENT", "RETOUR", "AUTRE");
	}

	@Test
	void validateSchema_ne_verifie_pas_les_regles_metier() {
		// JSON conforme au schéma, mais urgence à 12 sur 5
		var modele = new FauxModele(VALIDE.replace("\"urgence\": 4", "\"urgence\": 12"));

		var analyse = ChatClient.builder(modele).build().prompt().user("Ma commande 42 ?").call()
				.entity(AnalyseTicket.class, spec -> spec.validateSchema());

		assertThat(modele.prompts).hasSize(1); // aucune relance
		assertThat(analyse.urgence()).isEqualTo(12); // passe : seul Bean Validation l'arrête
	}
}
