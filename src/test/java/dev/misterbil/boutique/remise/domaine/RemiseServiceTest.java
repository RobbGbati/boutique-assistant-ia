package dev.misterbil.boutique.remise.domaine;

import dev.misterbil.boutique.client.domaine.Client;
import dev.misterbil.boutique.client.domaine.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Post 1.6 : même si un LLM manipulé déclenche l'action, le contrôle d'accès côté code la refuse.
 */
class RemiseServiceTest {

	private final Session session = mock(Session.class);
	private final Panier panier = mock(Panier.class);
	private final RemiseService service = new RemiseService(session, panier);

	@Test
	void refuse_la_remise_a_un_client_qui_n_est_pas_employe() {
		when(session.clientConnecte()).thenReturn(new Client("c-1", false));

		assertThatThrownBy(service::appliquerRemiseEmploye).isInstanceOf(AccesRefuse.class);
		verify(panier, never()).appliquer(Remise.REMISE_EMPLOYE);
	}

	@Test
	void applique_la_remise_a_un_employe() {
		when(session.clientConnecte()).thenReturn(new Client("e-1", true));

		service.appliquerRemiseEmploye();

		verify(panier).appliquer(Remise.REMISE_EMPLOYE);
	}
}
