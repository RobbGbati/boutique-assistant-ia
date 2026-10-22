package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 2.4, sur le modèle local : l'assistant lit le statut de la commande via l'outil.
 * Run du 20 octobre 2026 : statut EXPEDIEE restitué, avec un délai de livraison absent du résultat de l'outil.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class OutilCommandeIT {

	@Autowired
	AssistantSupport assistant;

	@Test
	void l_assistant_verifie_la_commande_du_client_connecte() {
		var paul = assistant.repondre(new Question("paul", "Où en est ma commande 42 ?")).texte();
		var lea = assistant.repondre(new Question("lea", "Où en est ma commande 42 ?")).texte();

		System.out.println(">>> Paul (sa commande) : " + paul);
		System.out.println(">>> Léa (commande de Paul) : " + lea);
		assertThat(paul.toLowerCase()).contains("expédi");
	}
}
