package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.support.domaine.AnalyseTicket;
import dev.misterbil.boutique.support.domaine.Categorie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.2 : quand la réponse du LLM est inutilisable, l'adapter renvoie la valeur par défaut.
 * Le ChatModel de test renvoie une réponse fixée : aucun appel réseau, sortie déterministe.
 */
class AnalyseTicketRepliTest {

	/** ChatModel de test : renvoie le texte donné à chaque appel. */
	private SpringAiAssistant assistantQuiRepond(String texte) {
		return AdapterDeTest.autourDe(prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(texte)))));
	}

	/** Test témoin : prouve que le ChatModel de test est bien branché. Sinon, les tests suivants passeraient sans rien vérifier. */
	@Test
	void une_reponse_correcte_passe_telle_quelle() {
		var analyse = assistantQuiRepond("{\"categorie\": \"LIVRAISON\", \"urgence\": 4, \"resume\": \"Colis en retard\"}")
				.analyser("Ma commande 42 ?");

		assertThat(analyse).isEqualTo(new AnalyseTicket(Categorie.LIVRAISON, 4, "Colis en retard"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {
			// texte en trop autour du JSON
			"Voici l'analyse demandée ! {\"categorie\": \"LIVRAISON\", \"urgence\": 4, \"resume\": \"Colis en retard\"}",
			// catégorie inventée, absente de l'enum
			"{\"categorie\": \"VIP_URGENT\", \"urgence\": 4, \"resume\": \"Colis en retard\"}"
	})
	void un_mapping_impossible_donne_la_valeur_par_defaut(String reponseDuLlm) {
		var analyse = assistantQuiRepond(reponseDuLlm).analyser("Ma commande 42 ?");

		assertThat(analyse).isEqualTo(AnalyseTicket.aTrier());
	}

	@Test
	void une_regle_metier_violee_donne_la_valeur_par_defaut() {
		// JSON parfaitement valide, mais urgence à 12 sur 5
		var analyse = assistantQuiRepond("{\"categorie\": \"LIVRAISON\", \"urgence\": 12, \"resume\": \"Colis en retard\"}")
				.analyser("Ma commande 42 ?");

		assertThat(analyse).isEqualTo(AnalyseTicket.aTrier());
	}
}
