package dev.misterbil.boutique.support;

import dev.misterbil.boutique.support.domaine.AnalyseTicket;
import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Categorie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 1.3, niveau 3 : l'évaluation sur des tickets de référence.
 * Résultat : un score sur 10 tickets, comparé à un seuil (pas un succès ou un échec par cas).
 * Format et présence de l'information : vérifiés par le code. Absence d'invention : vérifiée par un modèle juge (bespoke-minicheck).
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#avecJuge")
class EvaluationIT {

	/** Seuil propre à ce jeu de tickets. Il sert à comparer deux versions du prompt. */
	static final int SEUIL = 7;

	record Reference(String ticket, Categorie attendue) {
	}

	static final List<Reference> REFERENCES = List.of(
			new Reference("Ma commande 1287 devait arriver mardi, le suivi n'a pas bougé depuis 5 jours.", Categorie.LIVRAISON),
			new Reference("Le livreur a laissé mon colis chez un voisin que je ne connais pas. Comment le récupérer ?", Categorie.LIVRAISON),
			new Reference("Je n'ai reçu que deux articles sur les trois commandés.", Categorie.LIVRAISON),
			new Reference("J'ai été débité deux fois de 49,90 € pour la même commande.", Categorie.PAIEMENT),
			new Reference("Ma carte a été refusée au paiement alors que mon solde est suffisant.", Categorie.PAIEMENT),
			new Reference("Le code promo BIENVENUE10 n'a pas été déduit du montant payé.", Categorie.PAIEMENT),
			new Reference("Le pull reçu est trop petit, je voudrais le renvoyer pour être remboursé.", Categorie.RETOUR),
			new Reference("Comment imprimer l'étiquette de retour pour la commande 5521 ?", Categorie.RETOUR),
			new Reference("Proposez-vous des cartes cadeaux pour Noël ?", Categorie.AUTRE),
			new Reference("Je voudrais supprimer mon compte client et mes données personnelles.", Categorie.AUTRE));

	@Autowired
	AssistantSupport assistant;

	@Autowired
	ChatClient.Builder builder;

	/** Le juge : réglages de la doc Spring AI pour bespoke-minicheck (réponse « yes » ou « no »). */
	private FactCheckingEvaluator juge() {
		return FactCheckingEvaluator.forBespokeMinicheck(builder.clone()
				.defaultOptions(OllamaChatOptions.builder().model("bespoke-minicheck").numPredict(2).temperature(0.0)));
	}

	/** Test témoin : le juge doit détecter une invention. Un juge qui répond toujours « yes » rendrait le score inutile. */
	@Test
	void le_juge_repere_une_invention() {
		var ticket = List.of(new Document("Le pull reçu est trop petit, je voudrais le renvoyer pour être remboursé."));

		assertThat(juge().evaluate(new EvaluationRequest(ticket, "Le client veut renvoyer un pull trop petit.")).isPass())
				.isTrue();
		assertThat(juge().evaluate(new EvaluationRequest(ticket, "Le client exige 200 € de dédommagement et menace de porter plainte.")).isPass())
				.isFalse();
	}

	@Test
	void le_score_sur_les_tickets_de_reference_atteint_le_seuil() {
		var juge = juge();

		int score = 0;
		for (var ref : REFERENCES) {
			AnalyseTicket analyse = assistant.analyser(ref.ticket());

			boolean format = !analyse.equals(AnalyseTicket.aTrier()); // mapping + Bean Validation passés
			boolean info = analyse.categorie() == ref.attendue();
			// Le ticket est le document, le résumé est l'affirmation à vérifier
			boolean rienInvente = format && juge.evaluate(
					new EvaluationRequest(List.of(new Document(ref.ticket())), analyse.resume())).isPass();

			boolean ok = format && info && rienInvente;
			if (ok) {
				score++;
			}
			System.out.printf(">>> %s format=%s info=%s rienInvente=%s | %s | %s%n",
					ok ? "OK  " : "RATÉ", format, info, rienInvente, analyse.categorie(), analyse.resume());
		}
		System.out.println(">>> SCORE : " + score + " / " + REFERENCES.size() + " (seuil " + SEUIL + ")");

		assertThat(score).isGreaterThanOrEqualTo(SEUIL);
	}
}
