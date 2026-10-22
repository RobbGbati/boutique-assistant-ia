package dev.misterbil.boutique.support.web;

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

	AssistantController(AssistantSupport assistant) {
		this.assistant = assistant;
	}

	@GetMapping("/assistant")
	String repondre(@RequestParam String question) {
		return assistant.repondre(new Question(question)).texte();
	}
}
