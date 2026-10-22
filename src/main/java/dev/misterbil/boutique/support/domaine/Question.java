package dev.misterbil.boutique.support.domaine;

/**
 * Une question posée à l'assistant.
 * Post 2.3 : {@code clientId} range la conversation. Il vient de la session, jamais du texte du client.
 */
public record Question(String clientId, String texte) {
}
