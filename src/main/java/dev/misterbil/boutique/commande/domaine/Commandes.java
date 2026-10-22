package dev.misterbil.boutique.commande.domaine;

import java.util.Optional;

/**
 * Post 2.4 : le port vers les commandes de la boutique.
 */
public interface Commandes {

	Optional<Commande> parNumero(String numero);
}
