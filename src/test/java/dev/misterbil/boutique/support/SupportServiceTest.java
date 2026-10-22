package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Reponse;
import dev.misterbil.boutique.support.domaine.SupportService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Post 1.1 : le service métier se teste avec un mock du port (Mockito). Aucun appel au LLM.
 */
class SupportServiceTest {

	private final AssistantSupport assistant = mock(AssistantSupport.class);
	private final SupportService support = new SupportService(assistant);

	@Test
	void renvoie_la_reponse_de_l_assistant() {
		when(assistant.repondre(any())).thenReturn(new Reponse("Commande 42 : expédiée"));

		assertThat(support.traiter("Où est ma commande 42 ?")).isEqualTo(new Reponse("Commande 42 : expédiée"));
	}
}
