package dev.misterbil.boutique.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Condition JUnit : les tests d'intégration ne tournent que si Ollama répond en local.
 */
final class OllamaDisponible {

	private OllamaDisponible() {
	}

	static boolean enLocal() {
		return modeles() != null;
	}

	/** Post 1.3 : l'évaluation a besoin du modèle juge, en plus d'Ollama. */
	static boolean avecJuge() {
		var modeles = modeles();
		return modeles != null && modeles.contains("\"bespoke-minicheck");
	}

	/** Liste brute des modèles installés (JSON de /api/tags), ou null si Ollama ne répond pas. */
	private static String modeles() {
		try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
			var requete = HttpRequest.newBuilder(URI.create("http://localhost:11434/api/tags")).build();
			var reponse = client.send(requete, HttpResponse.BodyHandlers.ofString());
			return reponse.statusCode() == 200 ? reponse.body() : null;
		}
		catch (Exception e) {
			return null;
		}
	}
}
