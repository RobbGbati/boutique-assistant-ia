package dev.misterbil.boutique.support.domaine;

import reactor.core.publisher.Flux;

/**
 * Post 1.1 : le port. Il parle métier et ne connaît aucun fournisseur de LLM.
 */
public interface AssistantSupport {

	Reponse repondre(Question question);

	/**
	 * Post 2.5 : la réponse en flux de fragments, affichable pendant la génération.
	 * Compromis : le port dépend de Reactor (Flux), pour garder le code court.
	 */
	Flux<String> repondreEnDirect(Question question);

	/** Post 1.2 : la réponse du LLM, mappée dans un record et validée. */
	AnalyseTicket analyser(String ticket);
}
