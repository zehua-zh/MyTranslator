# MyTranslator

Application Android de traduction (anglais vers français) avec génération de petits exercices de vocabulaire.

## Fonctionnalités

- Traduction de texte anglais vers français
- Génération de challenges de vocabulaire (QCM)
- Écran de préférences (difficulté, taille de vocabulaire, mode simplifié)

## Technologies

- Java (Android)
- Android SDK 32
- Gradle (AGP 7.1.2)
- Gson

## Configuration API

- Utiliser `RAPIDAPI_KEY` pour la lemmatisation Twinword.
- Ajoutez `RAPIDAPI_KEY` dans `~/.gradle/gradle.properties`.

## Exécution en local

1. Ouvrir le dossier dans Android Studio puis lancer l'application sur émulateur/appareil.
2. Ou en ligne de commande :

```bash
./gradlew assembleDebug
```

APK généré :

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Structure du projet

- `app/` : code Android principal
- `app/src/main/java/com/example/mytranslator/` : logique de l'application
- `app/src/main/assets/` : fichiers de vocabulaire
- `app/src/main/res/` : layouts et ressources UI
