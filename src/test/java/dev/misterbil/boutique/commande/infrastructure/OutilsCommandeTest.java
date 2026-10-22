package dev.misterbil.boutique.commande.infrastructure;

import dev.misterbil.boutique.commande.domaine.Commande;
import dev.misterbil.boutique.commande.domaine.Commandes;
import dev.misterbil.boutique.commande.domaine.StatutCommande;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 2.4 : le LLM n'exécute pas de code. Il renvoie une demande d'appel d'outil ; l'application exécute la méthode
 * et renvoie le résultat au modèle, qui produit la réponse finale.
 */
class OutilsCommandeTest {

	static final Commandes COMMANDES = numero -> Optional.of(new Commande("42", "paul", StatutCommande.EXPEDIEE))
			.filter(c -> c.numero().equals(numero));

	/** ChatModel de test : 1er appel, demande d'outil ; 2e appel, réponse construite avec le résultat de l'outil. */
	static class FauxModeleOutil implements ChatModel {

		final List<Prompt> prompts = new ArrayList<>();

		/** Options de type ToolCallingChatOptions, comme un vrai ChatModel. Sinon, ToolCallingAdvisor saute la boucle d'outils. */
		@Override
		public ChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			if (prompts.size() == 1) {
				var demande = new AssistantMessage.ToolCall("appel-1", "function", "getStatutCommande", "{\"numero\":\"42\"}");
				return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(demande)).build())));
			}
			var resultat = (ToolResponseMessage) prompt.getInstructions().getLast();
			return new ChatResponse(List.of(new Generation(new AssistantMessage(
					"Votre commande 42 est : " + resultat.getResponses().getFirst().responseData()))));
		}
	}

	private String demander(FauxModeleOutil modele, String clientId) {
		return ChatClient.builder(modele).build().prompt()
				.user("Où en est ma commande 42 ?")
				.tools(new OutilsCommande(COMMANDES))
				.toolContext(Map.of(OutilsCommande.CLIENT_ID, clientId))
				.call().content();
	}

	@Test
	void le_modele_demande_l_outil_et_l_application_l_execute() {
		var modele = new FauxModeleOutil();

		var reponse = demander(modele, "paul");

		assertThat(modele.prompts).hasSize(2); // demande d'outil, puis réponse finale
		assertThat(modele.prompts.getFirst().getInstructions().getLast().getText()).isEqualTo("Où en est ma commande 42 ?");
		assertThat(reponse).isEqualTo("Votre commande 42 est : \"EXPEDIEE\"");
	}

	@Test
	void la_commande_d_un_autre_client_reste_introuvable() {
		var reponse = demander(new FauxModeleOutil(), "eve");

		assertThat(reponse).contains("INTROUVABLE");
	}

	@Test
	void le_contexte_n_est_pas_envoye_au_modele() {
		var modele = new FauxModeleOutil();

		demander(modele, "paul");

		assertThat(modele.prompts.getFirst().getContents()).doesNotContain("paul");
		assertThat(modele.prompts.getFirst().getOptions().toString()).doesNotContain("paul");
	}
}
