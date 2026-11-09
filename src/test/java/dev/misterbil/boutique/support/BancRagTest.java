package dev.misterbil.boutique.support;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 3.1 : le banc de questions de la série 3, reposé à chaque post.
 * Ce test ne parle pas au LLM : il vérifie que le banc reste cohérent avec le corpus (src/main/resources/docs).
 * Une question « hors corpus » (source null) attend un refus, pas une réponse.
 */
class BancRagTest {

	record Cas(String id, String question, String attendu, String source, String section) {

		boolean horsCorpus() {
			return source == null;
		}
	}

	static List<Cas> banc() throws IOException {
		try (var json = new ClassPathResource("banc-rag.json").getInputStream()) {
			return List.of(JsonMapper.builder().build().readValue(json, Cas[].class));
		}
	}

	@Test
	void le_banc_contient_vingt_questions_dont_quatre_hors_corpus() throws IOException {
		var banc = banc();

		assertThat(banc).hasSize(20);
		assertThat(banc).filteredOn(Cas::horsCorpus).hasSize(4);
		assertThat(new HashSet<>(banc.stream().map(Cas::id).toList())).hasSize(20);
	}

	@Test
	void la_question_fil_rouge_est_dans_le_banc() throws IOException {
		assertThat(banc()).anySatisfy(cas -> {
			assertThat(cas.question()).isEqualTo("Je peux renvoyer un article ouvert ?");
			assertThat(cas.section()).isEqualTo("Articles ouverts");
		});
	}

	/** Chaque source attendue existe et contient la section citée : c'est ce que le post 3.6 affichera sous la réponse. */
	@Test
	void chaque_source_attendue_existe_dans_le_corpus() throws IOException {
		for (var cas : banc()) {
			if (cas.horsCorpus()) {
				assertThat(cas.section()).as(cas.id()).isNull();
				continue;
			}
			var document = new ClassPathResource("docs/" + cas.source());
			assertThat(document.exists()).as(cas.id() + " : " + cas.source()).isTrue();
			assertThat(document.getContentAsString(StandardCharsets.UTF_8))
					.as(cas.id())
					.contains("## " + cas.section() + "\n");
		}
	}
}
