package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AnalyseTicket;
import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Categorie;
import dev.misterbil.boutique.support.domaine.Destination;
import dev.misterbil.boutique.support.domaine.Reponse;
import dev.misterbil.boutique.support.domaine.SupportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Post 1.3, niveau 1 : port AssistantSupport mocké (Mockito). Aucun appel au LLM, résultat déterministe.
 */
class SupportServiceTest {

	private final AssistantSupport assistant = mock(AssistantSupport.class);
	private final SupportService support = new SupportService(assistant);

	@Test
	void renvoie_la_reponse_de_l_assistant() {
		when(assistant.repondre(any())).thenReturn(new Reponse("Commande 42 : expédiée"));

		assertThat(support.traiter("Où est ma commande 42 ?")).isEqualTo(new Reponse("Commande 42 : expédiée"));
	}

	/** Routage : chaque catégorie va à son équipe. */
	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({"LIVRAISON, SAV_LIVRAISON", "PAIEMENT, COMPTABILITE", "RETOUR, RETOURS", "AUTRE, CONSEILLER"})
	void oriente_selon_la_categorie(Categorie categorie, Destination attendue) {
		when(assistant.analyser(any())).thenReturn(new AnalyseTicket(categorie, 3, "Résumé"));

		assertThat(support.orienter("Un ticket")).isEqualTo(attendue);
	}

	@Test
	void une_urgence_maximale_va_a_un_conseiller() {
		when(assistant.analyser(any())).thenReturn(new AnalyseTicket(Categorie.LIVRAISON, 5, "Colis perdu"));

		assertThat(support.orienter("Un ticket")).isEqualTo(Destination.CONSEILLER);
	}

	@Test
	void une_analyse_impossible_va_a_un_conseiller() {
		when(assistant.analyser(any())).thenReturn(AnalyseTicket.aTrier());

		assertThat(support.orienter("Un ticket")).isEqualTo(Destination.CONSEILLER);
	}
}
