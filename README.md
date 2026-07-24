<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app
<div align="center">

# 💧 AquaTrack — Suivi Hydratation

**Application Android de suivi de l'hydratation quotidienne, en Jetpack Compose.**

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-8.10.1-3DDC84?logo=android&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.11.1-02303A?logo=gradle&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-orange)

</div>

---

## À propos

Ce dépôt contient un projet Android initialement **généré par Google AI Studio**, puis repris pour
être rendu fonctionnel en local.

Le projet livré par le générateur ne compilait pas. Le travail a consisté à diagnostiquer chaque
anomalie, à migrer la chaîne d'outils vers une version compatible avec l'IDE cible, à corriger le
code, puis à documenter l'ensemble.

> **📋 [Lire le journal des corrections → `CORRECTIONS.md`](CORRECTIONS.md)**
>
> 18 anomalies recensées : 7 bloquantes, 5 majeures, 6 mineures. Chaque entrée précise le symptôme
> exact, la cause et le correctif appliqué.

### La correction structurante

Le projet généré ciblait **AGP 9.1.1**, alors qu'Android Studio Meerkat plafonne à **AGP 8.10**. Il
a fallu rétrograder toute la chaîne d'outils — ce qui a mis au jour quatre régressions relevant d'un
même mécanisme : **AGP 9 faisait implicitement ce qu'AGP 8 exige de déclarer** (compilation du
Kotlin, alignement des cibles JVM, activation d'AndroidX, DSL `compileSdk`).

---

## Fonctionnalités

| | |
|---|---|
| 🎯 **Objectif quotidien** | 2 L, avec suivi de la progression en pourcentage |
| 💧 **Ajout rapide** | verres de 250 ml, animation d'éclaboussure au clic |
| 🌊 **Cercle animé** | jauge dessinée au `Canvas`, remplissage par double vague sinusoïdale |
| 🕘 **Historique du jour** | entrées horodatées, suppression unitaire avec animation de dissolution |
| 🏆 **Célébration** | message de motivation contextuel et félicitations à l'objectif atteint |
| 🔄 **Réinitialisation** | remise à zéro de la journée avec confirmation |
| 🎨 **Thème sombre** | palette turquoise, Material 3, affichage bord-à-bord |

---

## Stack technique

- **Kotlin 2.2.10** — coroutines, `StateFlow`
- **Jetpack Compose** — UI déclarative, Material 3, animations `Canvas` et `Animatable`
- **Architecture MVVM** — `ViewModel` + état exposé en `StateFlow`, UI sans état
- **Tests** — JUnit, Robolectric, Roborazzi (capture d'écran), Espresso

---

## Prérequis

| Composant | Version |
|---|---|
| Android Studio | Meerkat Feature Drop \| 2024.3.2 ou plus récent |
| JDK | 17 minimum (le JBR 21 embarqué convient) |
| Gradle | 8.11.1 — fourni par le wrapper, rien à installer |
| Android Gradle Plugin | 8.10.1 |
| SDK Platform | **API 36** (Android 16) |
| SDK Build Tools | 36.0.0 |
| `minSdk` | 24 (Android 7.0) |

---

## Démarrage

```bash
git clone <URL_DU_DEPOT>
cd suivi-hydratation
```

1. Ouvrir le dossier dans Android Studio (*File → Open*).
2. Installer **Android 16 (API 36)** via *Tools → SDK Manager → SDK Platforms*, et
   **Build Tools 36.0.0** dans l'onglet *SDK Tools*.
3. Vérifier le JDK du projet : *Settings → Build, Execution, Deployment → Build Tools → Gradle →
   Gradle JDK* (17 ou 21).
4. Synchroniser Gradle, puis lancer l'application sur un émulateur ou un appareil.

> **Aucun fichier `.env`, `google-services.json` ni keystore n'est nécessaire.** Le projet généré en
> réclamait trois — tous absents de l'export, tous devenus inutiles après les corrections #2, #3
> et #6.

### En ligne de commande

```bash
./gradlew assembleDebug        # APK dans app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # tests unitaires JVM + Robolectric
./gradlew lintDebug            # analyse statique
./gradlew connectedDebugAndroidTest   # tests instrumentés (appareil requis)
```

---

## Structure du projet

```
app/src/main/java/com/example/
├── MainActivity.kt              # Activity + UI Compose (écran, cercle animé, historique)
├── data/
│   └── HydrationLog.kt          # modèle d'une entrée de boisson
└── ui/
    ├── HydrationViewModel.kt    # état de la journée, StateFlow, logique métier
    └── theme/                   # palette turquoise, typographie, thème Material 3
```

---

## Limites connues

Ces points sont documentés et assumés — voir la section « Écarts fonctionnels » de
[`CORRECTIONS.md`](CORRECTIONS.md).

- **Pas de persistance.** Les données vivent en mémoire : elles survivent à une rotation d'écran,
  mais sont perdues à la fermeture de l'application. Le descripteur du générateur annonçait pourtant
  une « persistance locale », et les dépendances Room étaient déclarées sans qu'aucune `@Entity`,
  `@Dao` ni `@Database` n'ait été écrite.
- **Objectif plafonné.** Il est impossible de dépasser 2 L : le ViewModel écrête toute saisie
  au-delà de l'objectif. Comportement volontaire du code généré, discutable pour un tracker.
- **Objectif non modifiable.** `setDailyGoal()` existe dans le ViewModel mais n'est exposé par aucun
  élément d'interface.

---

## Signature de release

L'APK de release n'est signé que si un keystore est disponible :

```bash
export KEYSTORE_PATH=/chemin/vers/mon-keystore.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
./gradlew assembleRelease
```

Sans ces variables, la tâche produit un APK non signé au lieu d'échouer.

---

## Licence

Projet réalisé dans un cadre pédagogique.