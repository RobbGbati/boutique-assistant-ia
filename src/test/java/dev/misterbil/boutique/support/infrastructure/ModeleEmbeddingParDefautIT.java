package dev.misterbil.boutique.support.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Post 3.2, le piège : la même configuration, sans spring.ai.ollama.embedding.model.
 * Spring AI demande alors son modèle par défaut à Ollama, absent de la machine.
 */
@SpringBootTest(properties = "spring.config.name=sans-modele-embedding")
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class ModeleEmbeddingParDefautIT {

	@Autowired
	EmbeddingModel embeddingModel;

	@Test
	void sansLaProprieteLePremierAppelEchoue() {
		assertThatThrownBy(() -> embeddingModel.embed(EmbeddingsIT.QUESTION))
				.satisfies(e -> System.out.println("Erreur : " + e.getClass().getSimpleName() + " : " + e.getMessage()));
	}
}
