package dev.misterbil.boutique.support.web;

import dev.misterbil.boutique.client.domaine.Client;
import dev.misterbil.boutique.client.domaine.Session;
import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import dev.misterbil.boutique.support.domaine.Reponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * Post 2.1 : le contrôleur passe la question au port et renvoie le texte.
 * Post 2.3 : la conversation est rangée sous l'identifiant de la session.
 * Post 2.5 : Spring MVC envoie un Flux en Server-Sent Events, un événement « data: » par fragment.
 */
class AssistantControllerTest {

	private final AssistantSupport assistant = mock(AssistantSupport.class);
	private final Session session = mock(Session.class);
	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AssistantController(assistant, session)).build();

	@Test
	void la_question_part_au_port_et_la_reponse_revient_en_texte() throws Exception {
		when(session.clientConnecte()).thenReturn(new Client("paul", false));
		when(assistant.repondre(new Question("paul", "Où est ma commande 42 ?"))).thenReturn(new Reponse("Expédiée."));

		mvc.perform(get("/assistant").param("question", "Où est ma commande 42 ?"))
				.andExpect(content().string("Expédiée."));
	}

	@Test
	void la_reponse_en_direct_part_en_server_sent_events() throws Exception {
		when(session.clientConnecte()).thenReturn(new Client("paul", false));
		when(assistant.repondreEnDirect(new Question("paul", "Retour ?"))).thenReturn(Flux.just("Tu as ", "30 jours", "."));

		var resultat = mvc.perform(get("/assistant/direct").param("question", "Retour ?"))
				.andExpect(request().asyncStarted())
				.andReturn();

		mvc.perform(asyncDispatch(resultat))
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
				.andExpect(content().string("data:Tu as \n\ndata:30 jours\n\ndata:.\n\n"));
	}
}
