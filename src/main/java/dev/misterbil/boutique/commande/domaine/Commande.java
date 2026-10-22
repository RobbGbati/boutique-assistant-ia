package dev.misterbil.boutique.commande.domaine;

public record Commande(String numero, String clientId, StatutCommande statut) {

	public boolean appartientA(Object client) {
		return clientId.equals(client);
	}
}
