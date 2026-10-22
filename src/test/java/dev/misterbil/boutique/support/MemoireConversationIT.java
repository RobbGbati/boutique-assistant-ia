package dev.misterbil.boutique.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.5, sur le modèle local : les tokens d'entrée grandissent à chaque appel,
 * sauf si l'historique est limité aux derniers échanges.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class MemoireConversationIT {

	@Autowired
	ChatClient.Builder builder;

	/** Tokens d'entrée de chaque appel, pour les 6 questions d'une même conversation. */
	private List<Integer> tokensEntree(ChatMemory memoire) {
		var chat = builder.clone()
				.defaultSystem("Tu es l'assistant du support d'une boutique en ligne. Réponds en une phrase courte.")
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memoire).build())
				.defaultOptions(ChatOptions.builder().temperature(0.0))
				.build();
		return MemoireConversationTest.QUESTIONS.stream()
				.map(question -> chat.prompt()
						.user(question)
						.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "client-42"))
						.call().chatResponse()
						.getMetadata().getUsage().getPromptTokens())
				.toList();
	}

	@Test
	void l_historique_fait_grandir_l_entree_et_la_fenetre_la_plafonne() {
		var complet = tokensEntree(MessageWindowChatMemory.builder().build());
		var limite = tokensEntree(MessageWindowChatMemory.builder().maxMessages(7).build());

		System.out.println(">>> Entrée, historique complet : " + complet);
		System.out.println(">>> Entrée, 3 derniers échanges : " + limite);
		assertThat(complet).isSortedAccordingTo(Integer::compare);
		assertThat(complet.getLast()).isGreaterThan(complet.getFirst() * 2);
		assertThat(limite.getLast()).isLessThan(complet.getLast());
	}
}
