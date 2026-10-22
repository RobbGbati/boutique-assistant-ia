package dev.misterbil.boutique.client.web;

import dev.misterbil.boutique.client.domaine.Client;
import dev.misterbil.boutique.client.domaine.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Post 2.3 : le client connecté, lu dans la session HTTP. Le texte du chat ne peut pas le changer.
 */
@Component
class SessionHttp implements Session {

	static final String ATTRIBUT = "client";

	@Override
	public Client clientConnecte() {
		var client = RequestContextHolder.currentRequestAttributes().getAttribute(ATTRIBUT, RequestAttributes.SCOPE_SESSION);
		if (client instanceof Client c) {
			return c;
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connecte-toi d'abord : POST /demo/connexion?client=paul");
	}
}
