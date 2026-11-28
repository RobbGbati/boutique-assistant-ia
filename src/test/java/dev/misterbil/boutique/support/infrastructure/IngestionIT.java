package dev.misterbil.boutique.support.infrastructure;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 3.3 : l'ingestion range les documents de la boutique dans PgVector (Postgres démarré par Docker Compose).
 * Mesures affichées pour le script.
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class IngestionIT {

	@Autowired
	IngestionDocuments ingestion;

	@Autowired
	VectorStore vectorStore;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void tableVide() {
		jdbc.execute("TRUNCATE vector_store");
	}

	int lignes() {
		return jdbc.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
	}

	@Test
	void lesDocumentsSontRangesDansLaTable() throws Exception {
		long debut = System.nanoTime();
		ingestion.ingerer();
		System.out.printf("Ingestion : %d lignes en %d ms%n", lignes(), (System.nanoTime() - debut) / 1_000_000);
		System.out.println("Colonne embedding : " + jdbc.queryForObject(
				"SELECT format_type(atttypid, atttypmod) FROM pg_attribute WHERE attrelid = 'vector_store'::regclass AND attname = 'embedding'",
				String.class));
		jdbc.query("SELECT metadata->>'fichier', metadata->>'title', length(content), left(content, 70) FROM vector_store ORDER BY 1, 2",
				(rs, i) -> {
					System.out.printf("%-22s | %-28s | %4d car. | %s%n", rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4));
					return null;
				});
		assertThat(lignes()).isPositive();
	}

	// Nos sections sont courtes (83 à 326 caractères) : le découpeur (800 tokens par morceau par défaut) n'a rien à couper
	@Test
	void leDecoupeurNeCoupePasLesSectionsCourtes() throws Exception {
		int lues = 0;
		int morceaux = 0;
		for (var fichier : new PathMatchingResourcePatternResolver().getResources("classpath:docs/*.md")) {
			var sections = new MarkdownDocumentReader(fichier, MarkdownDocumentReaderConfig.defaultConfig()).get();
			lues += sections.size();
			morceaux += TokenTextSplitter.builder().build().apply(sections).size();
		}
		System.out.printf("Lecture : %d sections, découpage : %d morceaux%n", lues, morceaux);
		assertThat(morceaux).isEqualTo(lues);
	}

	@Test
	void relancerLIngestionNeDoublePasLaTable() throws Exception {
		ingestion.ingerer();
		int premiere = lignes();
		ingestion.ingerer();
		int deuxieme = lignes();
		System.out.printf("1re ingestion : %d lignes, 2e ingestion : %d lignes%n", premiere, deuxieme);
		assertThat(deuxieme).isEqualTo(premiere);
	}

	@Test
	void laRechercheTrouveLaBonneSection() throws Exception {
		ingestion.ingerer();
		var resultats = vectorStore.similaritySearch(
				SearchRequest.builder().query(EmbeddingsIT.QUESTION).topK(3).build());
		resultats.forEach(d -> System.out.printf(Locale.ROOT, "%.3f  %s § %s%n",
				d.getScore(), d.getMetadata().get("fichier"), d.getMetadata().get("title")));
		assertThat(resultats.getFirst().getMetadata()).containsEntry("title", "Articles ouverts");
	}
}
