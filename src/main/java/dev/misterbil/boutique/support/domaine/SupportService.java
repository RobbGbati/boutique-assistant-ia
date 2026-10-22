package dev.misterbil.boutique.support.domaine;

import org.springframework.stereotype.Service;

/**
 * Service métier. Il dépend du port {@link AssistantSupport}, jamais de Spring AI.
 */
@Service
public class SupportService {

	private static final int URGENCE_MAX = 5;

	private final AssistantSupport assistant;

	public SupportService(AssistantSupport assistant) {
		this.assistant = assistant;
	}

	public Reponse traiter(String messageClient) {
		return assistant.repondre(new Question(messageClient));
	}

	/**
	 * Post 1.3 : le routage, logique métier testable avec le port mocké.
	 * Urgence maximale ou catégorie inconnue : un humain prend le ticket.
	 */
	public Destination orienter(String ticket) {
		AnalyseTicket analyse = assistant.analyser(ticket);
		if (analyse.urgence() >= URGENCE_MAX) {
			return Destination.CONSEILLER;
		}
		return switch (analyse.categorie()) {
			case LIVRAISON -> Destination.SAV_LIVRAISON;
			case PAIEMENT -> Destination.COMPTABILITE;
			case RETOUR -> Destination.RETOURS;
			case AUTRE -> Destination.CONSEILLER;
		};
	}
}
