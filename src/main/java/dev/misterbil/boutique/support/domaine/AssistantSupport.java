package dev.misterbil.boutique.support.domaine;

/**
 * Post 1.1 : le port. Il parle métier et ne connaît aucun fournisseur de LLM.
 */
public interface AssistantSupport {

	Reponse repondre(Question question);

	/** Post 1.2 : la réponse du LLM, mappée dans un record et validée. */
	AnalyseTicket analyser(String ticket);
}
