# boutique-assistant-ia

Projet fil rouge de la série IA de **dev_mister_bil** : une boutique en ligne en Spring Boot,
à laquelle on ajoute un assistant de support basé sur un LLM. Chaque post de la série correspond à un tag git.

Stack : Java 25, Spring Boot 4.1.1, Spring AI 2.0.1, Ollama en local (modèle `llama3.2`).

## Démarrer

Prérequis : Java 25 et [Ollama](https://ollama.com).

```bash
ollama pull llama3.2          # modèle de 2 Go, exécuté en local, sans clé d'API
./mvnw spring-boot:run
```

## Naviguer post par post

Chaque post a son tag. Le code d'un tag est exactement celui du post.

```bash
git tag                              # liste des posts
git checkout post-1-4                # code à la fin du post 1.4
git diff post-1-3 post-1-4           # changements apportés par le post 1.4
git log --oneline                    # un commit par post
```

Chaque classe indique le post qui l'a introduite ou modifiée : rechercher `Post 2.4` liste tout le code de ce post.

| Post | Tag | Code | Tests |
|---|---|---|---|
| 1.1 Le LLM n'est pas ton domaine | `post-1-1` | `support/domaine/AssistantSupport` (port), `support/infrastructure/SpringAiAssistant` (adapter) | `SupportServiceTest` |
| 1.2 Une réponse de LLM est une donnée non fiable | `post-1-2` | `AnalyseTicket` (record + Bean Validation), `SpringAiAssistant.analyser` | `AnalyseTicketValidationTest`, `AnalyseTicketRepliTest`, `SortieStructureeSpringAiTest`, `ModesSortieStructureeIT` |
| 1.3 Tester du code qui ne répond jamais pareil | `post-1-3` | `SupportService.orienter`, consigne v2 dans `SpringAiAssistant` | `SupportServiceTest` (niveau 1), `SpringAiAssistantIT` (niveau 2), `EvaluationIT` (niveau 3) |

## Architecture

Architecture hexagonale par contexte métier : le `domaine` ne dépend d'aucun framework d'IA ; `infrastructure` et `web` dépendent du domaine, jamais l'inverse.

```
src/main/java/dev/misterbil/boutique/
BoutiqueAssistantIaApplication.java
support/                 assistant de support client
  domaine/               port AssistantSupport, SupportService, records métier
  infrastructure/        adapter Spring AI (seule classe qui appelle le LLM)
```

## Lancer les tests

| Commande | Plugin | Périmètre |
|---|---|---|
| `./mvnw test` | Surefire | Classes `*Test` : tests unitaires sur un `ChatModel` de test. Aucun appel à un LLM. |
| `./mvnw verify` | Surefire + Failsafe | Ajoute les classes `*IT`, exécutées contre Ollama (`http://localhost:11434`, modèle `llama3.2`). |

- Les `*IT` sont conditionnés par `@EnabledIf("…OllamaDisponible#enLocal")` : désactivés si `GET /api/tags` ne répond pas en 1 s. `EvaluationIT` exige en plus le modèle `bespoke-minicheck` (`OllamaDisponible#avecJuge`).
- Sortie du modèle non déterministe : les `*IT` vérifient des invariants (réponse non vide, mapping, statut d'outil) et affichent les valeurs mesurées sur la sortie standard, préfixées par `>>>`.
- `ModesSortieStructureeIT` (exploration du post 1.2) peut dépasser 15 min : la longueur de génération de `llama3.2` n'est pas bornée (`num_predict` non défini).
- Plusieurs JDK installés : `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./mvnw verify`.

## Constats mesurés

Conditions : Ollama en local, `llama3.2` (3,2 B paramètres, Q4_K_M), Apple M4 Pro, Spring AI 2.0.1, octobre 2026.

| Post | Constat | Test |
|---|---|---|
| 1.2 | `validateSchema()` valide la structure JSON, pas les contraintes Bean Validation : une `urgence` hors `[1, 5]` passe le schéma, `@Min`/`@Max` la rejettent. | `SortieStructureeSpringAiTest`, `ModesSortieStructureeIT` |
| 1.3 | Évaluation sur 10 tickets de référence (juge `bespoke-minicheck`) : 2/10 en sortie structurée par défaut, 5/10 en sortie native (`useProviderStructuredOutput()`), 9/10 avec la consigne v2 et `temperature = 0`. | `EvaluationIT` |

---

dev_mister_bil. Je réfléchis, je code, je m'amuse.
