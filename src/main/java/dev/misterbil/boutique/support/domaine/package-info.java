/**
 * Le cœur métier du support client. Aucune dépendance vers Spring AI ni vers un fournisseur de LLM.
 * <ul>
 *   <li>{@link dev.misterbil.boutique.support.domaine.AssistantSupport} : le port vers l'IA (post 1.1).</li>
 *   <li>{@link dev.misterbil.boutique.support.domaine.SupportService} : traiter un message, orienter un ticket.</li>
 * </ul>
 */
package dev.misterbil.boutique.support.domaine;
