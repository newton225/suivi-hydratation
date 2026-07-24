# Journal des corrections — Suivi Hydratation (AquaTrack)

Projet Android généré par **Google AI Studio**, repris, corrigé et rendu fonctionnel dans
Android Studio **Meerkat Feature Drop | 2024.3.2 Patch 1**.

| | |
|---|---|
| Projet d'origine | `suivi-hydratation.zip` (export AI Studio) |
| Module | `:app` — namespace `com.example` / applicationId `com.aistudio.hydratation.qxywtz` |
| Stack cible | Kotlin 2.2.10, Jetpack Compose, Material 3, **AGP 8.10.1**, Gradle 8.11.1 |
| IDE imposé | Android Studio Meerkat Feature Drop 2024.3.2 Patch 1 (build AI-243.26053.27) |
| Auteur des corrections | Isaac N'DRI |

---

## 0. Le problème principal : un projet ciblant une chaîne d'outils plus récente que l'IDE

C'est **la correction structurante de ce travail**, celle dont découlent plusieurs autres.

Le projet exporté par AI Studio ciblait **AGP 9.1.1**. Or chaque version d'Android Studio ne
supporte qu'une plage de versions d'AGP, et Meerkat Feature Drop 2024.3.2 s'arrête à **AGP 8.10**.

| Version d'Android Studio | Plage AGP supportée |
|---|---|
| Meerkat \| 2024.3.1 | 3.2 – 8.9 |
| **Meerkat Feature Drop \| 2024.3.2** | **3.2 – 8.10** |
| Narwhal \| 2025.1.1 | 3.2 – 8.11 |
| Otter 3 Feature Drop \| 2025.2.3 | … – 9.1 |

**Symptôme si on ouvre le projet tel quel :** échec de synchronisation dès la lecture des fichiers
de build, avec un message du type *« This project uses a newer version of the Android Gradle plugin
than this version of Android Studio supports »*, ou une erreur de parsing sur la DSL `compileSdk`.

**Décision retenue.** Rétrograder le projet vers **AGP 8.10.1 + Gradle 8.11.1**, plutôt que de
changer d'IDE. Cette rétrogradation n'est pas un simple changement de numéro de version : elle
impose quatre corrections de code de build (#12 à #15) détaillées plus bas, parce qu'AGP 9 avait
introduit des comportements qui n'existent pas en AGP 8.

### Environnement de build final

| Composant | Version |
|---|---|
| Android Gradle Plugin | **8.10.1** |
| Gradle | **8.11.1** (fourni par le wrapper) |
| JDK | 17 minimum — le JBR 21 embarqué dans Meerkat convient |
| Kotlin (KGP) | 2.2.10 — compatible AGP 8.x, inchangé |
| `compileSdk` / `targetSdk` | **36** (API 36 exige AGP ≥ 8.9.1) |
| `minSdk` | 24 |

---

## 1. Tableau récapitulatif

16 anomalies recensées : **8 bloquantes**, **3 majeures**, **5 mineures**.

| # | Sévérité | Anomalie | Fichier |
|---|---|---|---|
| 0 | 🔴 Bloquant | AGP 9.1.1 incompatible avec Android Studio Meerkat | `gradle/libs.versions.toml` |
| 1 | 🔴 Bloquant | Wrapper Gradle absent du projet | `gradle/wrapper/`, `gradlew` |
| 2 | 🔴 Bloquant | `debug.keystore` référencé mais absent | `app/build.gradle.kts` |
| 3 | 🔴 Bloquant | Keystore de release référencé mais absent | `app/build.gradle.kts` |
| 12 | 🔴 Bloquant | Plugin `kotlin-android` manquant (conséquence de #0) | `app/build.gradle.kts` |
| 13 | 🔴 Bloquant | DSL `compileSdk` réservée à AGP 9 (conséquence de #0) | `app/build.gradle.kts` |
| 14 | 🔴 Bloquant | Cibles JVM Java/Kotlin désalignées (conséquence de #0) | `app/build.gradle.kts` |
| 15 | 🔴 Bloquant | `android.useAndroidX` non défini (conséquence de #0) | `gradle.properties` |
| 4 | 🟠 Test KO | Test instrumenté avec assertion fausse | `ExampleInstrumentedTest.kt` |
| 5 | 🟠 Setup | SDK Platform 36 non installé par défaut | environnement |
| 6 | 🟡 Qualité | 3 plugins + 11 dépendances jamais utilisés | `build.gradle.kts`, `app/build.gradle.kts` |
| 7 | 🟡 Qualité | Nom de projet racine contenant une espace | `settings.gradle.kts` |
| 8 | 🟡 Qualité | `ViewModelProvider` instancié manuellement | `MainActivity.kt` |
| 9 | 🟠 UX | Flash blanc au démarrage sur appareil en thème clair | `themes.xml` |
| 10 | 🟡 Qualité | 6 imports inutilisés | `MainActivity.kt` |
| 11 | 🟡 Nettoyage | Dossier `assets/.aistudio/` étranger au module | racine |

---

## 2. Corrections liées à la rétrogradation AGP 9 → AGP 8

Quatre corrections (#12 à #15) relèvent d'un même mécanisme : **AGP 9 faisait implicitement ce
qu'AGP 8 exige de déclarer**. Compilation du Kotlin, alignement des cibles JVM, activation
d'AndroidX — autant de comportements devenus automatiques en AGP 9, que le générateur pouvait donc
légitimement omettre, et qu'il faut réintroduire un par un en rétrogradant.

### 🔴 #0 — Version d'AGP

```diff
  # gradle/libs.versions.toml
- agp = "9.1.1"
+ agp = "8.10.1"
```

### 🔴 #12 — Plugin `kotlin-android` manquant

**Symptôme.** Sans cette correction, la compilation « réussit » mais ne produit aucun `.class`
Kotlin. L'erreur finale est déroutante :

```
Unresolved class: com.example.MainActivity
```

ou, à l'installation, `ClassNotFoundException: Didn't find class "com.example.MainActivity"`.

**Cause.** **AGP 9.0 a introduit le support Kotlin intégré**, activé par défaut : le plugin
`org.jetbrains.kotlin.android` n'y est plus nécessaire, et le générateur AI Studio l'a donc
légitimement omis. Mais **AGP 8.x ne compile pas le Kotlin tout seul**. En rétrogradant, il faut
réintroduire ce plugin, qui n'était même pas déclaré dans le catalogue de versions.

**Correctif.** Ajout de l'alias dans le catalogue :

```toml
# gradle/libs.versions.toml
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

puis application dans les deux fichiers de build :

```kotlin
// build.gradle.kts (racine)
alias(libs.plugins.kotlin.android) apply false

// app/build.gradle.kts
alias(libs.plugins.kotlin.android)
```

> C'est le piège classique quand on rétrograde un projet généré pour AGP 9 : l'absence du plugin
> Kotlin ressemble à un oubli du générateur, alors que c'est le comportement normal d'AGP 9.

### 🔴 #13 — DSL `compileSdk` réservée à AGP 9

**Symptôme.** Erreur de configuration Gradle :

```
Unresolved reference: release
```

sur la ligne `compileSdk { version = release(36) { minorApiLevel = 1 } }`.

**Cause.** Cette syntaxe correspond aux *minor API levels* (ici **API 36.1**, Android 16 QPR1),
une notion introduite par AGP 9. AGP 8.10 ne connaît que la forme scalaire, et son niveau d'API
maximal est 36.

**Correctif.**

```diff
- compileSdk { version = release(36) { minorApiLevel = 1 } }
+ compileSdk = 36
```

Conséquence pratique : c'est désormais le **SDK Platform 36** (et non 36.1) qu'il faut installer,
ce qui simplifie la mise en place (voir #5).

### 🔴 #14 — Cibles JVM Java et Kotlin désalignées

**Symptôme.**

```
Inconsistent JVM-target compatibility detected for tasks
'compileDebugJavaWithJavac' (11) and 'compileDebugKotlin' (1.8).
```

**Cause.** `compileOptions` fixe Java 11, mais en AGP 8.x le compilateur Kotlin conserve sa cible
par défaut. AGP 9 alignait les deux automatiquement grâce au support Kotlin intégré ; ce filet de
sécurité disparaît avec la rétrogradation.

**Correctif.** Alignement explicite via la DSL Kotlin 2.x :

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_11
  }
}
```

### 🔴 #15 — `android.useAndroidX` non défini

**Symptôme.** Échec de la résolution des dépendances :

```
Configuration `:app:debugRuntimeClasspath` contains AndroidX dependencies, but the
`android.useAndroidX` property is not enabled, which may cause runtime issues.
Set `android.useAndroidX=true` in the `gradle.properties` file and retry.
```

suivi de la liste — très longue — de toutes les dépendances AndroidX détectées.

**Cause.** La propriété `android.useAndroidX` vaut **`true` par défaut à partir d'AGP 9.0.0
seulement**. Sur toutes les versions antérieures, y compris AGP 8.10.1, sa valeur par défaut est
`false` et elle doit être déclarée explicitement.

Le projet généré par AI Studio ciblait AGP 9 : son `gradle.properties` pouvait légitimement omettre
cette ligne. La rétrogradation vers AGP 8.10 la rend obligatoire. C'est la **quatrième** régression
de la même famille que #12, #13 et #14 : un comportement implicite d'AGP 9 qu'AGP 8 exige de
déclarer.

**Correctif.** Ajout dans `gradle.properties` :

```properties
android.useAndroidX=true
```

**`android.enableJetifier` n'est volontairement pas ajouté.** Jetifier ne sert qu'à réécrire à la
volée d'anciennes bibliothèques Support Library (`android.support.*`) vers AndroidX. Le projet n'en
utilise aucune, et activer Jetifier ralentirait fortement chaque build sans aucun bénéfice.

---

## 3. Corrections indépendantes de la version d'AGP

### 🔴 #1 — Wrapper Gradle absent

**Symptôme.** L'archive ne contient ni `gradlew`, ni `gradlew.bat`, ni
`gradle/wrapper/gradle-wrapper.jar`, ni `gradle/wrapper/gradle-wrapper.properties`. Le projet n'est
donc pas buildable en ligne de commande, et rien ne garantit la version de Gradle utilisée.

**Cause.** L'export AI Studio ne joint pas le wrapper (fichiers binaires exclus de l'archive).

**Correctif.** Ajout des quatre fichiers du wrapper, avec la distribution épinglée sur la version
exigée à la fois par AGP 8.10.1 et par Android Studio Meerkat :

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
```

**Vérification.** `./gradlew --version` doit afficher `Gradle 8.11.1`.

### 🔴 #2 — `debug.keystore` référencé mais absent

**Symptôme.**

```
Keystore file '<projet>/debug.keystore' not found for signing config 'debugConfig'.
```

**Cause.** Le projet déclarait une configuration de signature `debugConfig` pointant vers
`${rootDir}/debug.keystore`, et l'appliquait au buildType `debug` :

```kotlin
create("debugConfig") {
    storeFile = file("${rootDir}/debug.keystore")   // <-- ce fichier n'existe pas
    storePassword = "android"
    keyAlias = "androiddebugkey"
    keyPassword = "android"
}
// ...
debug { signingConfig = signingConfigs.getByName("debugConfig") }
```

Ce keystore est généré côté AI Studio et **volontairement exclu par le `.gitignore`** du projet
(ligne `debug.keystore`). Il n'est donc jamais présent dans l'export. Le README d'origine demandait
d'ailleurs de supprimer cette ligne manuellement : c'est une correction attendue.

**Correctif.** Suppression de la config `debugConfig` et du bloc `debug { ... }`. AGP retombe alors
sur le keystore de debug standard (`~/.android/debug.keystore`), généré automatiquement.

### 🔴 #3 — Keystore de release référencé mais absent

**Symptôme.** Même erreur que #2 sur `assembleRelease` / `bundleRelease`, avec `my-upload-key.jks`.
`storePassword` et `keyPassword` provenaient de variables d'environnement non définies, donc `null`.

**Correctif.** La configuration de release n'est déclarée **que si le keystore existe réellement** :

```kotlin
signingConfigs {
    val releaseKeystore = file(System.getenv("KEYSTORE_PATH") ?: "$rootDir/my-upload-key.jks")
    if (releaseKeystore.exists()) {
        create("release") {
            storeFile = releaseKeystore
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
}

buildTypes {
    release {
        // ...
        signingConfigs.findByName("release")?.let { signingConfig = it }
    }
}
```

### 🟠 #4 — Test instrumenté avec assertion fausse

**Symptôme.**

```
expected:<com.example> but was:<com.aistudio.hydratation.qxywtz>
```

**Cause.** Le test comparait `appContext.packageName` au **`namespace`** (`com.example`), alors que
la valeur au runtime est l'**`applicationId`**. Depuis AGP 7, ce sont deux notions indépendantes :
le `namespace` sert à générer la classe `R` et l'espace de noms Kotlin/Java, l'`applicationId`
identifie l'application sur l'appareil et sur le Play Store.

**Correctif.** Assertion alignée sur l'`applicationId`, avec commentaire explicatif.

### 🟠 #5 — SDK Platform 36 non installé

**Symptôme.** `Failed to find target with hash string 'android-36'`.

**Correctif (environnement).** *Tools → SDK Manager → SDK Platforms* → installer **Android 16
(API 36)**, ainsi que **Build Tools 36.0.0** dans l'onglet *SDK Tools*.

> Grâce à la correction #13, l'API 36.1 (Android 16 QPR1) n'est plus nécessaire.

### 🟡 #6 — Plugins et dépendances inutilisés

Trois plugins et onze dépendances n'étaient importés nulle part dans le code. Deux d'entre eux
exigent des fichiers absents de l'archive :

| Élément | Problème |
|---|---|
| plugin `google-services` | attend un `google-services.json` absent (contourné par `MissingGoogleServicesStrategy.WARN` et `googleServices.missing.passthrough=true`) |
| plugin `secrets` | attend un fichier `.env` absent (retombe sur `.env.example`) |
| plugin `ksp` | ne servait qu'à `room-compiler` et `moshi-kotlin-codegen` |
| Room (`room-runtime`, `room-ktx`, `room-compiler`) | **aucune `@Entity`, `@Dao` ni `@Database` n'existe dans le projet** |
| Retrofit / Moshi / OkHttp / logging-interceptor | aucun appel réseau dans l'application |
| Firebase BOM, `firebase-ai`, `firebase-appcheck-recaptcha` | aucune initialisation Firebase, aucun appel Gemini |

**Cause.** Le générateur produit un `build.gradle.kts` « au cas où », à partir du descripteur
`metadata.json` qui déclare la capacité `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` — jamais
implémentée dans le code livré.

**Correctif.** Plugins retirés, dépendances **commentées et non supprimées**, selon la convention
déjà employée par le générateur (« *Some unused dependencies are commented out below instead of
being removed* »). Les alias restent dans `gradle/libs.versions.toml` : une réactivation ne demande
que de décommenter une ligne.

**Bénéfice.** Suppression du traitement KSP, build sensiblement plus rapide, et disparition de deux
dépendances à des fichiers de configuration absents.

### 🟡 #7 — Nom de projet racine contenant une espace

`rootProject.name = "Suivi Hydratation"` → `"SuiviHydratation"`. Une espace se propage aux chemins
d'artefacts générés et gêne certains outils en ligne de commande et de CI. Le nom affiché à
l'utilisateur ne change pas : il vient de `res/values/strings.xml` (`app_name`).

### 🟡 #8 — `ViewModelProvider` instancié manuellement

`ViewModelProvider(this)[HydrationViewModel::class.java]` dans `onCreate` remplacé par le délégué
idiomatique `by viewModels()`, qui crée le ViewModel paresseusement et le rattache correctement au
cycle de vie de l'Activity.

### 🟠 #9 — Flash blanc au démarrage

**Symptôme.** Sur un appareil configuré en thème clair, un écran blanc apparaît brièvement au
lancement avant que Compose ne dessine son interface sombre.

**Cause.** `Theme.MyApplication` héritait de `android:Theme.DeviceDefault.NoActionBar` sans
redéfinir `android:windowBackground`. Le fond de fenêtre suivait donc le thème système, alors que
l'application force un thème sombre côté Compose (`MyApplicationTheme(darkTheme = true)`).

**Correctif.**

```xml
<style name="Theme.MyApplication" parent="android:Theme.DeviceDefault.NoActionBar">
    <item name="android:windowBackground">@color/dark_background</item>
</style>
```

`@color/dark_background` (`#FF0B1416`) reprend exactement la valeur de `DarkBackground` dans
`ui/theme/Color.kt`.

### 🟡 #10 — Imports inutilisés

Six imports morts supprimés dans `MainActivity.kt` :
`animation.core.Animatable` (le code utilise le nom pleinement qualifié),
`foundation.clickable`, `foundation.layout.fillMaxHeight`,
`material.icons.automirrored.rounded.List`, `material.icons.rounded.History`,
`ui.theme.TurquoiseTertiary`.

### 🟡 #11 — Dossier `assets/.aistudio/`

Dossier de métadonnées de l'éditeur AI Studio, placé à la racine hors du module `:app`, sans effet
sur le build. Supprimé.

---

## 4. Écarts fonctionnels constatés (arbitrage nécessaire)

Ces points ne sont **pas des erreurs de compilation** mais des écarts entre ce que le projet
annonce et ce qu'il fait. Ils sont documentés sans être corrigés unilatéralement, car ils relèvent
du cahier des charges.

### 4.1 — Impossible de dépasser l'objectif de 2 L

`HydrationViewModel.addWater()` plafonne le total au `dailyGoal` et refuse toute saisie au-delà,
l'interface affichant *« Limite autorisée de 2L déjà atteinte ! »*. Ce n'est pas un bug — c'est codé
volontairement, avec écrêtage de la dernière saisie — mais c'est un choix produit discutable pour un
tracker d'hydratation, où dépasser son objectif est un cas normal.

### 4.2 — Objectif quotidien non modifiable

`setDailyGoal()` et `setCustomDate()` existent dans le ViewModel mais ne sont exposés par aucun
élément d'interface. L'objectif est figé à 2000 ml.

---

## 5. Points de vigilance restants

Ces éléments n'ont **pas** été modifiés, mais sont les premiers suspects si la synchronisation ou
le build échoue encore. Chaque entrée donne le correctif exact à appliquer.

1. ~~**`androidx.core:core-ktx:1.18.0`**~~ — **point levé.** La résolution du
   `debugRuntimeClasspath` s'est effectuée sans erreur de `compileSdk`, confirmant que cette
   version est compatible avec `compileSdk = 36`. Aucune action requise.

2. **Plugin Roborazzi 1.59.0.** Version récente, susceptible d'exiger Gradle 9. C'est un outil de
   test par capture d'écran, sans impact sur l'application. Correctif si blocage : commenter
   `alias(libs.plugins.roborazzi)` dans les deux fichiers de build, retirer les quatre dépendances
   `roborazzi*`, et supprimer `app/src/test/java/com/example/GreetingScreenshotTest.kt`.

3. **Compose BOM 2024.09.00** (Compose 1.7), associée à un compilateur Compose Kotlin 2.2.10 plus
   récent. La résolution confirme les versions attendues (`compose.runtime:1.7.1`,
   `material3:1.3.0`, `foundation:1.7.0`) : `Modifier.animateItem()` est bien disponible. Si un
   avertissement de version d'exécution Compose apparaît malgré tout, monter la BOM dans le
   catalogue — **après** un premier build réussi, jamais avant.

4. **`material-icons-extended`.** Artefact déprécié par Google. Il fonctionne avec la BOM actuelle ;
   une migration vers Material Symbols serait à prévoir lors d'une montée de version.

5. **Cache de configuration.** `org.gradle.configuration-cache=true` est actif dans
   `gradle.properties`. Le retrait des plugins tiers (#6) réduit fortement le risque, mais en cas
   d'erreur *« configuration cache problems found »*, passer temporairement la propriété à `false`
   pour isoler le problème.

6. **`.env.example` / `metadata.json`.** Conservés à titre documentaire. Le plugin `secrets` ayant
   été retiré (#6), `.env.example` n'est plus lu par le build, et `GEMINI_API_KEY` n'est référencée
   nulle part dans le code Kotlin.

---

## 6. Procédure de vérification

```bash
# 1. Version de Gradle résolue par le wrapper
./gradlew --version              # -> Gradle 8.11.1, JVM 17 ou 21

# 2. Configuration du projet et résolution des dépendances
./gradlew :app:dependencies --configuration debugRuntimeClasspath

# 3. Compilation debug
./gradlew :app:assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk

# 4. Tests unitaires JVM (JUnit + Robolectric + Roborazzi)
./gradlew :app:testDebugUnitTest

# 5. Analyse statique
./gradlew :app:lintDebug

# 6. Tests instrumentés (émulateur ou appareil connecté requis)
./gradlew :app:connectedDebugAndroidTest
```

**Validation fonctionnelle manuelle**

| Scénario | Attendu |
|---|---|
| Lancement de l'application | aucun flash blanc, fond sombre immédiat (#9) |
| Appui sur « Ajouter 250 ml » | animation de vague, total et pourcentage mis à jour |
| Total porté à 2000 ml | boîte de dialogue de félicitations affichée **une seule fois** |
| Appui supplémentaire à 2000 ml | Toast « Limite autorisée de 2L déjà atteinte » (cf. §4.1) |
| Suppression d'une entrée de l'historique | animation de dissolution, total recalculé |
| Bouton « Reset » puis confirmation | historique vidé, compteur à 0 |
| Rotation de l'écran | données conservées (ViewModel) |
