package dev.misterbil.boutique.support.web;

import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import dev.misterbil.boutique.support.domaine.Reponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Post 2.1 : le contrôleur passe la question au port et renvoie le texte.
 */
class AssistantControllerTest {

	private final AssistantSupport assistant = mock(AssistantSupport.class);
	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AssistantController(assistant)).build();

	@Test
	void la_question_part_au_port_et_la_reponse_revient_en_texte() throws Exception {
		when(assistant.repondre(new Question("Où est ma commande 42 ?"))).thenReturn(new Reponse("Expédiée."));

		mvc.perform(get("/assistant").param("question", "Où est ma commande 42 ?"))
				.andExpect(content().string("Expédiée."));
	}
}
