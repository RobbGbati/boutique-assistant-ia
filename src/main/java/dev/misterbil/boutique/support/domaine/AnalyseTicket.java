package dev.misterbil.boutique.support.domaine;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Post 1.2 : la forme attendue d'une réponse de LLM, avec ses règles métier.
 */
public record AnalyseTicket(
		@NotNull Categorie categorie,
		@Min(1) @Max(5) int urgence,
		@NotBlank @Size(max = 200) String resume) {

	/** Valeur par défaut quand la réponse du LLM est inutilisable : le ticket part en revue humaine. */
	public static AnalyseTicket aTrier() {
		return new AnalyseTicket(Categorie.AUTRE, 3, "Analyse automatique impossible, revue humaine requise.");
	}
}
