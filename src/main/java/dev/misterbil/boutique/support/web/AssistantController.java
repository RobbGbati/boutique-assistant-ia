package dev.misterbil.boutique.support.web;

import dev.misterbil.boutique.client.domaine.Session;
import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Post 2.1 : la porte d'entrée HTTP. Le contrôleur parle au port, pas au fournisseur.
 */
@RestController
class AssistantController {

	private final AssistantSupport assistant;
	private final Session session;

	AssistantController(AssistantSupport assistant, Session session) {
		this.assistant = assistant;
		this.session = session;
	}

	@GetMapping("/assistant")
	String repondre(@RequestParam String question) {
		return assistant.repondre(question(question)).texte();
	}

	/** Post 2.3 : l'identifiant du client vient de la session, jamais de la requête. */
	private Question question(String texte) {
		return new Question(session.clientConnecte().id(), texte);
	}
}
