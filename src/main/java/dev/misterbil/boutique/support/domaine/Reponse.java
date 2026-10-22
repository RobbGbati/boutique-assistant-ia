package dev.misterbil.boutique.support.domaine;

/**
 * Réponse de l'assistant. {@code disponible = false} signale le plan B (post 1.4).
 */
public record Reponse(String texte, boolean disponible) {

	public Reponse(String texte) {
		this(texte, true);
	}

	public static Reponse indisponible() {
		return new Reponse("Notre assistant est indisponible. Un conseiller te répond sous 24 h.", false);
	}
}
