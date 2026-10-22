package dev.misterbil.boutique.commande.infrastructure;

import dev.misterbil.boutique.commande.domaine.Commandes;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Post 2.4 : l'outil que le LLM peut demander. C'est l'application qui l'exécute.
 * Post 1.6 : l'identité vient du contexte posé par le code (la session), jamais du chat.
 */
@Component
public class OutilsCommande {

	public static final String CLIENT_ID = "clientId";

	private final Commandes commandes;

	public OutilsCommande(Commandes commandes) {
		this.commandes = commandes;
	}

	@Tool(description = "Donne le statut d'une commande du client connecté")
	public String getStatutCommande(@ToolParam(description = "Numéro de la commande") String numero,
			ToolContext contexte) {
		var id = contexte.getContext().get(CLIENT_ID);
		return commandes.parNumero(numero)
				.filter(c -> c.appartientA(id))
				.map(c -> c.statut().name())
				.orElse("INTROUVABLE");
	}
}
