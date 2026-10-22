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
		try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
			var requete = HttpRequest.newBuilder(URI.create("http://localhost:11434/api/tags")).build();
			return client.send(requete, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
		}
		catch (Exception e) {
			return false;
		}
	}
}
