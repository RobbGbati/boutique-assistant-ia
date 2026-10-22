package dev.misterbil.boutique.commande.infrastructure;

import dev.misterbil.boutique.commande.domaine.Commande;
import dev.misterbil.boutique.commande.domaine.Commandes;
import dev.misterbil.boutique.commande.domaine.StatutCommande;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Données de démonstration en mémoire : la commande 42 appartient à Paul, la 7 à Léa.
 */
@Component
class CommandesEnMemoire implements Commandes {

	private static final Map<String, Commande> COMMANDES = Map.of(
			"42", new Commande("42", "paul", StatutCommande.EXPEDIEE),
			"7", new Commande("7", "lea", StatutCommande.EN_PREPARATION));

	@Override
	public Optional<Commande> parNumero(String numero) {
		return Optional.ofNullable(COMMANDES.get(numero));
	}
}
