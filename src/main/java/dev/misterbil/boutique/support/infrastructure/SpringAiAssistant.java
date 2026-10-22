package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import dev.misterbil.boutique.support.domaine.Reponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Post 1.1 : l'adapter. Seul endroit du code qui sait qu'un LLM existe.
 */
@Component
class SpringAiAssistant implements AssistantSupport {

	private final ChatClient chat;

	SpringAiAssistant(ChatClient.Builder builder) {
		this.chat = builder.build();
	}

	@Override
	public Reponse repondre(Question q) {
		return new Reponse(chat.prompt()
				.user(q.texte())
				.call().content());
	}
}
