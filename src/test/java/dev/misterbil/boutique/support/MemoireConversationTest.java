package dev.misterbil.boutique.support;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Post 1.5 : avec une mémoire de conversation, tout l'historique repart au LLM à chaque appel.
 * Le ChatModel de test enregistre chaque prompt : on compte les messages envoyés, donc les tokens facturés.
 */
class MemoireConversationTest {

	static final List<String> QUESTIONS = List.of(
			"Où est ma commande ?",
			"Elle part quand ?",
			"Je peux changer l'adresse ?",
			"Et pour un retour ?",
			"Le remboursement prend combien de temps ?",
			"Merci, et le suivi ?");

	/** Joue les 6 questions d'une même conversation et renvoie les prompts reçus par le modèle. */
	private List<List<Message>> conversation(ChatMemory memoire) {
		var modele = new SortieStructureeSpringAiTest.FauxModele("Réponse de l'assistant.");
		var chat = ChatClient.builder(modele)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memoire).build())
				.build();
		for (var question : QUESTIONS) {
			chat.prompt()
					.user(question)
					.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "client-42"))
					.call().content();
		}
		return modele.prompts.stream().map(p -> p.getInstructions()).toList();
	}

	@Test
	void tout_l_historique_repart_a_chaque_appel() {
		// fenêtre par défaut : 20 messages, donc rien n'est retiré sur 6 échanges
		var prompts = conversation(MessageWindowChatMemory.builder().build());

		assertThat(prompts).extracting(List::size).containsExactly(1, 3, 5, 7, 9, 11);
	}

	@Test
	void historique_limite_aux_3_derniers_echanges() {
		// 3 échanges = 6 messages (question + réponse), plus la question en cours
		var prompts = conversation(MessageWindowChatMemory.builder().maxMessages(7).build());

		assertThat(prompts).extracting(List::size).containsExactly(1, 3, 5, 7, 7, 7);
		// au 6e appel, le plus ancien message envoyé est la 3e question
		assertThat(prompts.getLast().getFirst().getText()).isEqualTo(QUESTIONS.get(2));
		assertThat(prompts.getLast().getLast().getText()).isEqualTo(QUESTIONS.getLast());
	}

	@Test
	void sans_identifiant_de_conversation_l_appel_echoue() {
		var chat = ChatClient.builder(new SortieStructureeSpringAiTest.FauxModele("Réponse."))
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
				.build();

		assertThatThrownBy(() -> chat.prompt().user("Où est ma commande ?").call().content())
				.isInstanceOf(IllegalArgumentException.class);
	}
}
