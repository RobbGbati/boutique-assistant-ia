package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AnalyseTicket;
import dev.misterbil.boutique.support.domaine.Categorie;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.2 : un JSON valide peut porter une urgence à 12 ; seule la contrainte Bean Validation (@Max(5)) la rejette.
 */
class AnalyseTicketValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void une_urgence_a_12_est_rejetee() {
		var analyse = new AnalyseTicket(Categorie.LIVRAISON, 12, "Colis en retard");

		assertThat(validator.validate(analyse)).hasSize(1);
	}

	@Test
	void une_analyse_correcte_passe() {
		var analyse = new AnalyseTicket(Categorie.LIVRAISON, 4, "Colis en retard");

		assertThat(validator.validate(analyse)).isEmpty();
	}

	@Test
	void la_valeur_par_defaut_est_elle_meme_valide() {
		assertThat(validator.validate(AnalyseTicket.aTrier())).isEmpty();
	}
}
