package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.support.domaine.Question;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 2.2 : le prompt système (« fiche de poste ») est envoyé avec chaque question. analyser() le remplace par sa propre consigne.
 */
class FicheDePosteTest {

	private final List<Prompt> prompts = new ArrayList<>();
	private final ChatModel modele = prompt -> {
		prompts.add(prompt);
		return new ChatResponse(List.of(new Generation(new AssistantMessage(
				"{\"categorie\": \"LIVRAISON\", \"urgence\": 4, \"resume\": \"Colis en retard\"}"))));
	};

	private List<Message> messagesSysteme() {
		return prompts.getFirst().getInstructions().stream()
				.filter(m -> m.getMessageType() == MessageType.SYSTEM)
				.toList();
	}

	@Test
	void la_fiche_de_poste_part_avec_chaque_question() {
		AdapterDeTest.autourDe(modele).repondre(new Question("Où est ma commande 42 ?"));

		assertThat(messagesSysteme()).singleElement()
				.extracting(Message::getText).isEqualTo(SpringAiAssistant.FICHE_DE_POSTE);
	}

	@Test
	void l_analyse_remplace_la_fiche_par_sa_consigne() {
		AdapterDeTest.autourDe(modele).analyser("Ma commande 42 n'est pas arrivée.");

		assertThat(messagesSysteme()).singleElement()
				.extracting(Message::getText).asString()
				.startsWith("Analyse ce ticket")
				.doesNotContain("Boutique Bil");
	}
}
