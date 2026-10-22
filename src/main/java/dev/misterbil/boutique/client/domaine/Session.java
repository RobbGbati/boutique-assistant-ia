package dev.misterbil.boutique.client.domaine;

/**
 * Identité de l'utilisateur authentifié. Elle vient de la sécurité de l'application, jamais du chat.
 */
public interface Session {

	Client clientConnecte();
}
