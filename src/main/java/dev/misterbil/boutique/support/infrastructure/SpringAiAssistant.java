package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.support.domaine.AnalyseTicket;
import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import dev.misterbil.boutique.support.domaine.Reponse;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Post 1.1 : l'adapter. Seul endroit du code qui sait qu'un LLM existe.
 */
@Component
class SpringAiAssistant implements AssistantSupport {

	private static final Logger log = LoggerFactory.getLogger(SpringAiAssistant.class);

	private final ChatClient chat;
	private final Validator validator;

	SpringAiAssistant(ChatClient.Builder builder, Validator validator) {
		this.chat = builder.build();
		this.validator = validator;
	}

	@Override
	public Reponse repondre(Question q) {
		return new Reponse(chat.prompt()
				.user(q.texte())
				.call().content());
	}

	/**
	 * Post 1.2 : mapping (la forme), puis Bean Validation (le sens), puis valeur par défaut.
	 */
	@Override
	public AnalyseTicket analyser(String ticket) {
		AnalyseTicket analyse;
		try {
			analyse = chat.prompt()
					.user(ticket)
					.call()
					.entity(AnalyseTicket.class);
		}
		catch (RuntimeException e) {
			log.warn("Mapping impossible : {}", e.toString());
			return AnalyseTicket.aTrier();
		}
		var erreurs = validator.validate(analyse);
		if (!erreurs.isEmpty()) {
			log.warn("Validation échouée : {}", erreurs);
			return AnalyseTicket.aTrier();
		}
		return analyse;
	}
}
