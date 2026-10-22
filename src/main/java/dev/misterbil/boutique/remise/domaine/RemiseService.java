package dev.misterbil.boutique.remise.domaine;

import dev.misterbil.boutique.client.domaine.Session;

/**
 * Post 1.6 : si un outil sensible doit exister, c'est le code qui vérifie, pas le modèle.
 */
public class RemiseService {

	private final Session session;
	private final Panier panier;

	public RemiseService(Session session, Panier panier) {
		this.session = session;
		this.panier = panier;
	}

	public void appliquerRemiseEmploye() {
		var client = session.clientConnecte();
		if (!client.estEmploye()) {
			throw new AccesRefuse();
		}
		panier.appliquer(Remise.REMISE_EMPLOYE);
	}
}
