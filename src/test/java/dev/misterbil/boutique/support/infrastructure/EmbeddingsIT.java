package dev.misterbil.boutique.support.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 3.2 : un modèle d'embedding transforme un texte en vecteur ; deux textes de même sens ont des vecteurs proches.
 * Mesures affichées pour le script (modèle nomic-embed-text sur Ollama, similarité cosinus).
 */
@SpringBootTest
@EnabledIf("dev.misterbil.boutique.support.OllamaDisponible#enLocal")
class EmbeddingsIT {

	static final String QUESTION = "Je peux renvoyer un article ouvert ?";

	@Autowired
	EmbeddingModel embeddingModel;

	// Similarité cosinus : 1 = même direction (même sens), 0 = aucun rapport
	static double similarite(float[] a, float[] b) {
		double ab = 0, aa = 0, bb = 0;
		for (int i = 0; i < a.length; i++) {
			ab += a[i] * b[i];
			aa += a[i] * a[i];
			bb += b[i] * b[i];
		}
		return ab / (Math.sqrt(aa) * Math.sqrt(bb));
	}

	@Test
	void uneQuestionDevientUnVecteur() {
		float[] vecteur = embeddingModel.embed(QUESTION);
		System.out.printf("Dimensions : %d%nDébut : %.4f %.4f %.4f%n", vecteur.length, vecteur[0], vecteur[1], vecteur[2]);
		assertThat(vecteur).hasSize(768);
	}

	@Test
	void deuxPhrasesDeMemeSensSontProches() {
		float[] question = embeddingModel.embed(QUESTION);
		var phrases = List.of(
				"retour",
				"livraison",
				"paiement",
				"Un produit déballé peut-il être rendu ?", // même sens, aucun mot en commun
				"Je peux payer en trois fois ?");
		for (String phrase : phrases) {
			System.out.printf(Locale.ROOT, "%.3f  %s%n", similarite(question, embeddingModel.embed(phrase)), phrase);
		}
		double memeSens = similarite(question, embeddingModel.embed(phrases.get(3)));
		double autreSujet = similarite(question, embeddingModel.embed(phrases.get(4)));
		assertThat(memeSens).isGreaterThan(autreSujet);
	}

	record Section(String fichier, String titre, String texte) {
	}

	record Score(Section section, double valeur) {
	}

	// Chaque section « ## » du corpus, classée par proximité avec la question : la bonne section doit sortir en tête
	static List<Section> sectionsDuCorpus() throws IOException {
		var sections = new ArrayList<Section>();
		for (var doc : new PathMatchingResourcePatternResolver().getResources("classpath:docs/*.md")) {
			String[] blocs = doc.getContentAsString(UTF_8).split("\n## ");
			for (int i = 1; i < blocs.length; i++) {
				sections.add(new Section(doc.getFilename(), blocs[i].lines().findFirst().orElseThrow(), "## " + blocs[i]));
			}
		}
		return sections;
	}

	@Test
	void laBonneSectionSortEnTete() throws IOException {
		var sections = sectionsDuCorpus();
		// Code affiché dans le reel 3.2 (copie exacte)
		float[] question =
				embeddingModel.embed(QUESTION);
		var classement = new ArrayList<Score>();
		for (var s : sections) {
			String section = s.texte();
			float[] page =
					embeddingModel.embed(section);
			double score =
					similarite(question, page);
			classement.add(new Score(s, score));
		}
		classement.sort(Comparator.comparingDouble(Score::valeur).reversed());
		System.out.println("Sections du corpus : " + sections.size());
		for (var c : classement) {
			System.out.printf(Locale.ROOT, "%.3f  %s § %s%n", c.valeur(), c.section().fichier(), c.section().titre());
		}
		assertThat(classement.getFirst().section().titre()).isEqualTo("Articles ouverts");
	}

	// nomic-embed-text est entraîné avec des préfixes de tâche : « search_query: » pour la question, « search_document: » pour le texte
	@Test
	void avecLesPrefixesNomic() {
		float[] question = embeddingModel.embed("search_query: " + QUESTION);
		for (String phrase : List.of("retour", "livraison", "paiement", "Un produit déballé peut-il être rendu ?", "Je peux payer en trois fois ?")) {
			System.out.printf(Locale.ROOT, "préfixe %.3f  %s%n", similarite(question, embeddingModel.embed("search_document: " + phrase)), phrase);
		}
	}

	// Même classement, avec les préfixes exigés par la fiche du modèle (usage RAG recommandé par Nomic)
	@Test
	void laBonneSectionAvecLesPrefixesNomic() throws IOException {
		float[] question = embeddingModel.embed("search_query: " + QUESTION);
		var classement = new ArrayList<Score>();
		for (var s : sectionsDuCorpus()) {
			classement.add(new Score(s, similarite(question, embeddingModel.embed("search_document: " + s.texte()))));
		}
		classement.sort(Comparator.comparingDouble(Score::valeur).reversed());
		for (var c : classement) {
			System.out.printf(Locale.ROOT, "préfixe section %.3f  %s § %s%n", c.valeur(), c.section().fichier(), c.section().titre());
		}
	}
}
