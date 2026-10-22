package dev.misterbil.boutique.support.domaine;

/**
 * Post 1.4 : ce que devient un message client, avec ou sans assistant disponible.
 */
public record ResultatTraitement(StatutTraitement statut, Reponse reponse) {
}
