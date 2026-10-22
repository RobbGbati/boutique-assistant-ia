package dev.misterbil.boutique.stock.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 2.6 : client JSON-RPC sur /mcp (Streamable HTTP) : initialize, tools/list, tools/call.
 * Pas besoin d'Ollama : le serveur MCP n'appelle pas le LLM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServeurMcpTest {

	@Value("${local.server.port}")
	int port;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> envoyer(String json, String session) throws Exception {
		var requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json, text/event-stream");
		if (session != null) {
			requete.header("Mcp-Session-Id", session);
		}
		return http.send(requete.POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void un_client_mcp_liste_puis_appelle_l_outil_stock() throws Exception {
		var init = envoyer("""
				{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
				"capabilities":{},"clientInfo":{"name":"test","version":"1"}}}""", null);
		var session = init.headers().firstValue("Mcp-Session-Id").orElseThrow();
		envoyer("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", session);

		var outils = envoyer("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}", session).body();
		var appel = envoyer("""
				{"jsonrpc":"2.0","id":3,"method":"tools/call",
				"params":{"name":"stock-produit","arguments":{"reference":"SKU-42"}}}""", session).body();

		assertThat(init.body()).contains("\"name\":\"boutique-stock\"");
		assertThat(outils).contains("\"name\":\"stock-produit\"")
				.contains("\"readOnlyHint\":true", "\"destructiveHint\":false");
		assertThat(appel).contains("\"text\":\"7\"");
	}
}
