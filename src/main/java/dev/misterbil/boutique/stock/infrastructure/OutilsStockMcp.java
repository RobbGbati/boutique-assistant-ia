package dev.misterbil.boutique.stock.infrastructure;

import dev.misterbil.boutique.stock.domaine.Stock;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Post 2.6 : le service Stock exposé en MCP. Tout client MCP (Claude, un IDE...) peut l'appeler.
 * Attention : le serveur MCP n'a aucune authentification par défaut. À sécuriser comme toute API exposée.
 */
@Component
class OutilsStockMcp {

	private final Stock stock;

	OutilsStockMcp(Stock stock) {
		this.stock = stock;
	}

	// Par défaut, un outil MCP est annoncé comme destructif (destructiveHint=true) : on déclare qu'il ne fait que lire
	@McpTool(name = "stock-produit", description = "Quantité en stock d'un produit de la boutique",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
	int stockProduit(@McpToolParam(description = "Référence du produit, ex. SKU-42", required = true) String reference) {
		return stock.quantite(reference);
	}
}
