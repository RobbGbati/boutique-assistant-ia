package dev.misterbil.boutique.client.web;

import dev.misterbil.boutique.client.domaine.Client;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Démo seulement : simule la connexion d'un client pour essayer l'assistant à plusieurs.
 * En production, c'est le rôle de Spring Security. Point clé : l'identité est posée une fois,
 * côté serveur, et le chat ne peut pas la changer.
 */
@RestController
class ConnexionDemoController {

	@PostMapping("/demo/connexion")
	String connecter(@RequestParam String client, HttpSession session) {
		session.setAttribute(SessionHttp.ATTRIBUT, new Client(client, false));
		return "Connecté : " + client;
	}
}
