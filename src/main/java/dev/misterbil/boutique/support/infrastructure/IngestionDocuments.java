package dev.misterbil.boutique.support.infrastructure;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Post 3.3 : l'ingestion (ETL). Lire les documents de la boutique, les découper, les ranger dans PgVector.
 * Relancée à chaque démarrage : avant d'ajouter un fichier, on efface ses anciens morceaux.
 * Sans cette ligne, chaque redémarrage empile une copie (mesuré : 24 lignes, puis 48).
 * Transactionnel : si un fichier échoue (modèle absent, dimensions changées), les anciens morceaux restent.
 */
@Component
class IngestionDocuments {

	private static final Logger log = LoggerFactory.getLogger(IngestionDocuments.class);

	private final VectorStore vectorStore;

	IngestionDocuments(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	@Transactional
	void ingerer() throws IOException {
		var splitter = TokenTextSplitter.builder().build();
		for (Resource fichier : new PathMatchingResourcePatternResolver().getResources("classpath:docs/*.md")) {
			String nom = fichier.getFilename();
			var config = MarkdownDocumentReaderConfig.builder()
				.withAdditionalMetadata("fichier", nom)
				.build();
			var reader = new MarkdownDocumentReader(fichier, config);
			var morceaux = splitter.apply(reader.get());
			vectorStore.delete(new FilterExpressionBuilder().eq("fichier", nom).build());
			vectorStore.add(morceaux);
			log.info("{} : {} morceaux", nom, morceaux.size());
		}
	}

	/** Au démarrage de l'application. Désactivé dans les tests (src/test/resources/config/application.yml). */
	@Component
	@ConditionalOnBooleanProperty(name = "boutique.ingestion.au-demarrage", matchIfMissing = true)
	static class AuDemarrage implements ApplicationRunner {

		private final IngestionDocuments ingestion;

		AuDemarrage(IngestionDocuments ingestion) {
			this.ingestion = ingestion;
		}

		@Override
		public void run(ApplicationArguments args) throws IOException {
			ingestion.ingerer();
		}
	}
}
