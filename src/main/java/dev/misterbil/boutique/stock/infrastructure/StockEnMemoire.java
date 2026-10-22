package dev.misterbil.boutique.stock.infrastructure;

import dev.misterbil.boutique.stock.domaine.Stock;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Données de démonstration en mémoire.
 */
@Component
class StockEnMemoire implements Stock {

	private static final Map<String, Integer> QUANTITES = Map.of("SKU-42", 7, "SKU-7", 0);

	@Override
	public int quantite(String reference) {
		return QUANTITES.getOrDefault(reference, 0);
	}
}
