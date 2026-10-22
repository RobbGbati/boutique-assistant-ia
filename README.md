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

Plusieurs JDK installés : `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./mvnw test`.

---

dev_mister_bil. Je réfléchis, je code, je m'amuse.
