package dev.misterbil.boutique.support.infrastructure;

import dev.misterbil.boutique.support.domaine.AnalyseTicket;
import dev.misterbil.boutique.support.domaine.AssistantSupport;
import dev.misterbil.boutique.support.domaine.Question;
import dev.misterbil.boutique.support.domaine.Reponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

/**
 * Post 1.1 : l'adapter. Seul endroit du code qui sait qu'un LLM existe.
 */
@Component
class SpringAiAssistant implements AssistantSupport {

	private static final Logger log = LoggerFactory.getLogger(SpringAiAssistant.class);

	/**
	 * Post 2.2 : le prompt système, vu comme la « fiche de poste » de l'assistant : rôle, périmètre, ton, refus, interdits.
	 * Il oriente le modèle sans rien garantir. Ce n'est ni un secret ni un contrôle de sécurité (OWASP LLM07) :
	 * les règles métier restent dans le code.
	 */
	static final String FICHE_DE_POSTE = """
			Tu es l'assistant du support client de la boutique en ligne Boutique Bil.
			Tu aides uniquement sur : les commandes, la livraison, les retours et les paiements.
			Tu réponds en français, en trois phrases maximum, avec un ton poli et direct.
			Si la demande sort de ce périmètre, tu refuses poliment et tu rappelles ce que tu sais faire.
			Tu n'inventes jamais un statut de commande, un délai ou un prix.
			""";

	// Post 1.3 : consigne v2. La v1 (catégories sans définition, résumé libre) donnait 5/10 à l'évaluation.
	private static final String CONSIGNE_ANALYSE = """
			Analyse ce ticket du support d'une boutique en ligne.
			Catégorie, une seule :
			- LIVRAISON : l'acheminement du colis (retard, suivi, perte, contenu manquant, lieu de livraison).
			- PAIEMENT : l'argent (débit, remboursement, moyen de paiement, prix ou réduction appliqués).
			- RETOUR : renvoyer ou échanger un article, et la procédure de retour.
			- AUTRE : tout le reste, par exemple le compte client ou une question générale.
			Urgence : de 1 (faible) à 5 (critique).
			Résumé : une phrase de 200 caractères maximum qui reformule ce que dit le client.
			N'ajoute aucun fait absent du ticket. Ne donne ni conseil ni consigne.
			""";

	private final ChatClient chat;
	private final Validator validator;

	SpringAiAssistant(ChatClient.Builder builder, Validator validator) {
		this.chat = builder
				.defaultSystem(FICHE_DE_POSTE) // post 2.2 : prompt système envoyé avec chaque appel
				.build();
		this.validator = validator;
	}

	/**
	 * Post 1.4 : le circuit breaker et le plan B posés sur l'adapter.
	 * Post 1.5 : chaque appel journalise ses tokens d'entrée et de sortie (base de facturation chez un fournisseur cloud).
	 */
	@Override
	@CircuitBreaker(name = "assistant", fallbackMethod = "planB")
	public Reponse repondre(Question q) {
		var reponse = chat.prompt()
				.user(q.texte())
				.call().chatResponse();
		var usage = reponse.getMetadata().getUsage();
		log.info("Tokens : entrée={} sortie={} total={}",
				usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
		return new Reponse(reponse.getResult().getOutput().getText());
	}

	Reponse planB(Question q, Throwable e) {
		log.warn("Plan B déclenché : {}", e.toString());
		return Reponse.indisponible();
	}

	/**
	 * Post 1.2 : mapping (la forme), puis Bean Validation (le sens), puis valeur par défaut.
	 * Post 1.3 : sortie structurée native d'Ollama. L'évaluation (EvaluationIT) a montré qu'en mode
	 * par défaut llama3.2 tronquait le JSON 7 fois sur 10 ; en natif, 10 analyses valides sur 10.
	 * Bean Validation reste indispensable : le schéma ne vérifie pas les règles métier.
	 * Post 2.2 : .system() remplace le prompt système par défaut pour cet appel (tâche de classification).
	 */
	@Override
	public AnalyseTicket analyser(String ticket) {
		AnalyseTicket analyse;
		try {
			analyse = chat.prompt()
					.system(CONSIGNE_ANALYSE)
					.user(ticket)
					.options(ChatOptions.builder().temperature(0.0)) // classer, pas créer : on limite la variation
					.call()
					.entity(AnalyseTicket.class, spec -> spec.useProviderStructuredOutput());
		}
		catch (RuntimeException e) {
			log.warn("Mapping impossible : {}", e.toString());
			return AnalyseTicket.aTrier();
		}
		var erreurs = validator.validate(analyse);
		if (!erreurs.isEmpty()) {
			log.warn("Validation échouée : {}", erreurs);
			return AnalyseTicket.aTrier();
		}
		return analyse;
	}
}
