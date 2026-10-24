package dev.misterbil.boutique.support;

import dev.misterbil.boutique.commande.domaine.Commandes;
import dev.misterbil.boutique.commande.infrastructure.OutilsCommande;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérification pour la caption du post 2.4 : avec returnDirect, le résultat de l'outil
 * revient tel quel au client, sans phrase rédigée (donc sans délai inventé) par le modèle.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class ReturnDirectIT {

	static class OutilDirect {

		private final Commandes commandes;

		OutilDirect(Commandes commandes) {
			this.commandes = commandes;
		}

		@Tool(description = "Donne le statut d'une commande du client connecté", returnDirect = true)
		public String getStatutCommande(@ToolParam(description = "Numéro de la commande") String numero,
				ToolContext contexte) {
			var id = contexte.getContext().get(OutilsCommande.CLIENT_ID);
			return commandes.parNumero(numero)
					.filter(c -> c.appartientA(id))
					.map(c -> c.statut().name())
					.orElse("INTROUVABLE");
		}
	}

	@Autowired
	ChatClient.Builder builder;

	@Autowired
	Commandes commandes;

	@Test
	void le_resultat_de_l_outil_revient_sans_passer_par_le_modele() {
		var reponse = builder.build().prompt()
				.user("Où en est ma commande 42 ?")
				.tools(new OutilDirect(commandes))
				.toolContext(Map.of(OutilsCommande.CLIENT_ID, "paul"))
				.call()
				.content();

		System.out.println(">>> returnDirect : " + reponse);
		assertThat(reponse).contains("EXPEDIEE");
	}
}
