package dev.misterbil.boutique.support.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Post 3.3, le piège : la table vector_store a été créée pour nomic-embed-text (768 nombres par texte).
 * On change de modèle d'embedding (all-minilm, 384 nombres) sans recréer la table.
 * initialize-schema fait un CREATE TABLE IF NOT EXISTS : la table existante garde vector(768).
 */
@SpringBootTest(properties = "spring.ai.ollama.embedding.model=all-minilm")
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#avecAllMinilm")
class ChangementDeModeleIT {

	@Autowired
	IngestionDocuments ingestion;

	@Autowired
	JdbcTemplate jdbc;

	// La table telle que l'a créée le premier démarrage avec nomic-embed-text (même SQL que PgVectorStore)
	@BeforeEach
	void tableCreeePourNomic() throws Exception {
		jdbc.execute("DROP TABLE IF EXISTS vector_store");
		jdbc.execute("CREATE TABLE vector_store (id uuid PRIMARY KEY, content text, metadata json, embedding vector(768))");
		jdbc.execute("CREATE INDEX IF NOT EXISTS spring_ai_vector_index ON vector_store USING HNSW (embedding vector_cosine_ops)");
		// Un ancien morceau de faq.md, rangé avec nomic-embed-text
		jdbc.update("INSERT INTO vector_store VALUES (gen_random_uuid(), 'ancien', '{\"fichier\": \"faq.md\"}', array_fill(0.1, ARRAY[768])::vector)");
	}

	@AfterEach
	void tableVidePourLesAutresTests() {
		jdbc.execute("TRUNCATE vector_store");
	}

	@Test
	void laTableGardeSesDimensionsEtLIngestionEchoue() {
		System.out.println("Colonne embedding : " + jdbc.queryForObject(
				"SELECT format_type(atttypid, atttypmod) FROM pg_attribute WHERE attrelid = 'vector_store'::regclass AND attname = 'embedding'",
				String.class));
		assertThatThrownBy(ingestion::ingerer)
				.satisfies(e -> {
					Throwable cause = e;
					while (cause.getCause() != null) {
						cause = cause.getCause();
					}
					System.out.println("Erreur : " + e.getClass().getSimpleName());
					System.out.println("Cause : " + cause.getClass().getSimpleName() + " : " + cause.getMessage());
				});
		// L'ingestion est transactionnelle : l'effacement de faq.md est annulé avec l'échec
		Integer restants = jdbc.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
		System.out.println("Lignes après l'échec : " + restants);
		assertThat(restants).isEqualTo(1);
	}
}
