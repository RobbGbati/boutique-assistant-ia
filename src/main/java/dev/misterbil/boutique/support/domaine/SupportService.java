package dev.misterbil.boutique.support.domaine;

import org.springframework.stereotype.Service;

/**
 * Service métier. Il dépend du port {@link AssistantSupport}, jamais de Spring AI.
 */
@Service
public class SupportService {

	private final AssistantSupport assistant;

	public SupportService(AssistantSupport assistant) {
		this.assistant = assistant;
	}

	public Reponse traiter(String messageClient) {
		return assistant.repondre(new Question(messageClient));
	}
}
