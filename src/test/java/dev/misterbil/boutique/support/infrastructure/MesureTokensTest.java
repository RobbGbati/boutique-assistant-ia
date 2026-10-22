package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.support.domaine.Question;
import dev.misterbil.boutique.support.domaine.Reponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.5 : l'adapter note les tokens de chaque appel.
 * Le ChatModel de test renvoie les valeurs mesurées sur Ollama (entrée 31, sortie 64).
 */
@ExtendWith(OutputCaptureExtension.class)
class MesureTokensTest {

	@Test
	void chaque_reponse_note_ses_tokens(CapturedOutput sortie) {
		var assistant = AdapterDeTest.autourDe(prompt -> ChatResponse.builder()
				.generations(List.of(new Generation(new AssistantMessage("Votre colis arrive demain."))))
				.metadata(ChatResponseMetadata.builder().usage(new DefaultUsage(31, 64)).build())
				.build());

		var reponse = assistant.repondre(new Question("client-test", "Où est ma commande ?"));

		assertThat(reponse).isEqualTo(new Reponse("Votre colis arrive demain."));
		assertThat(sortie).contains("Tokens : entrée=31 sortie=64 total=95");
	}
}
