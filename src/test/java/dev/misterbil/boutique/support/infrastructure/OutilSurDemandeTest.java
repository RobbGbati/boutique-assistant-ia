package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.support.domaine.Question;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 3.1 : l'outil commande n'est envoyé au modèle que si la question cite un numéro de commande.
 */
class OutilSurDemandeTest {

	@Test
	void le_numero_de_commande_est_reconnu() {
		assertThat(SpringAiAssistant.citeUneCommande("Où en est ma commande 42 ?")).isTrue();
		assertThat(SpringAiAssistant.citeUneCommande("Commande n° 7, elle arrive quand ?")).isTrue();
		assertThat(SpringAiAssistant.citeUneCommande("Je peux renvoyer un article ouvert ?")).isFalse();
		assertThat(SpringAiAssistant.citeUneCommande("Je peux annuler ma commande ?")).isFalse();
		assertThat(SpringAiAssistant.citeUneCommande("La livraison est gratuite dès 60 € ?")).isFalse();
	}

	@Test
	void l_outil_part_seulement_avec_une_commande() {
		List<Prompt> recus = new ArrayList<>();
		ChatModel modele = new ChatModel() {

			@Override
			public ChatResponse call(Prompt prompt) {
				recus.add(prompt);
				return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
			}

			// Spring AI 2.0.1 part de ces options (DefaultChatClientUtils) : comme celles d'Ollama, elles acceptent des outils
			@Override
			public ChatOptions getOptions() {
				return ToolCallingChatOptions.builder().build();
			}
		};
		var assistant = AdapterDeTest.autourDe(modele);

		assistant.repondre(new Question("paul", "Je peux renvoyer un article ouvert ?"));
		assistant.repondre(new Question("paul", "Où en est ma commande 42 ?"));

		assertThat(outils(recus.get(0))).isEmpty();
		assertThat(outils(recus.get(1))).containsExactly("getStatutCommande");
	}

	private static List<String> outils(Prompt prompt) {
		if (!(prompt.getOptions() instanceof ToolCallingChatOptions options) || options.getToolCallbacks() == null) {
			return List.of();
		}
		return options.getToolCallbacks().stream().map(outil -> outil.getToolDefinition().name()).toList();
	}
}
