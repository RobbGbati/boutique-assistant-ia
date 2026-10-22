package dev.misterbil.boutique.support.infrastructure;

import jakarta.validation.Validation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Construit l'adapter sur un ChatModel de test : aucun appel réseau, sortie déterministe.
 */
final class AdapterDeTest {

	private AdapterDeTest() {
	}

	static SpringAiAssistant autourDe(ChatModel modele) {
		return new SpringAiAssistant(ChatClient.builder(modele), Validation.buildDefaultValidatorFactory().getValidator());
	}
}
