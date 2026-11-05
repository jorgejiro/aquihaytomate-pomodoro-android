# Publicar una versión de ¡Aquí hay tomate!

El procedimiento completo para sacar una versión a Google Play y a F-Droid desde este Mac, sin pasar
por Play Console. Se estrenó con la 1.4.1 (2026-11-05). Copia el esquema de Constanza
(`~/dev/constanza-android/docs/release-process.md`), con las diferencias que se anotan abajo.

Ninguna clave vive en el repositorio. Las de firma están en Bitwarden Secrets Manager y `con-claves`
las inyecta solo durante el comando que las necesita; la de Play es un fichero fuera del repo.

---

## 0. Lo que tiene que haber en el Mac

| Qué | Cómo comprobarlo |
|---|---|
| Claves de firma en BWS | `con-claves \| rg AQUIHAYTOMATE` → `KEYSTORE_B64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` |
| Cuenta de servicio de Play | `~/.config/play/jjrmobileapps.json` (la de la cuenta de desarrollador, la misma que usa Constanza) |
| fastlane | `which fastlane` → `/opt/homebrew/bin/fastlane` |
| GitHub CLI | `gh auth status` |
| JDK | el `jbr` de Android Studio; el `Fastfile` ya fija `JAVA_HOME` |

Comprobación rápida, no publica nada:

```bash
con-claves 'fastlane validar'   # credenciales de Play válidas para com.jjrapps.aquihaytomate
```

---

## 1. Subir la versión (rama `chore/version-X.Y.Z`)

**Los cinco sitios de CLAUDE.md §2.7**, en el mismo commit:

1. `app/build.gradle.kts`: `versionCode` **+1** (siempre, aunque solo cambie el patch) y `versionName`.
2. `CHANGELOG.md`: sección `## [X.Y.Z] — AAAA-MM-DD (versionCode N)`.
3. `string-array` `changelog_X_Y_Z` en `values/strings.xml` **y** `values-es/strings.xml`.
4. Entrada nueva al principio de `ui/changelog/ChangelogCatalog.kt`.
5. `fastlane/metadata/android/{en-US,es-ES}/changelogs/<versionCode>.txt`: **500 caracteres como
   máximo**. Es a la vez el texto de «Novedades» de Play y el changelog de F-Droid. Se cierra con la
   línea de siempre: `No accounts, no cloud, no ads, no tracking.` / `Sin cuentas, sin nube, sin
   anuncios y sin seguimiento.`

Commit: `chore(version): X.Y.Z (versionCode N)`.

Antes del PR:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lint test
wc -m fastlane/metadata/android/*/changelogs/<versionCode>.txt
```

`ChangelogCatalogTest` falla si el catálogo no tiene la versión compilada; `StringsParityTest`, si
falta el array en un idioma.

---

## 2. PR y merge

```bash
git push -u origin chore/version-X.Y.Z
gh pr create --title "…" --body "…"
gh pr checks <n> --watch --interval 20      # CI: «Lint and unit tests», ~4 min
gh pr merge <n> --merge --delete-branch     # merge commit, nunca squash
git switch main && git pull
```

**Merge commit, no squash**: F-Droid compila commits concretos y el tag tiene que caer en `main`.

---

## 3. Compilar y comprobar

Desde `main` actualizado:

```bash
con-claves 'JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleRelease :app:bundleRelease'
```

Sale `app/build/outputs/{apk,bundle}/release/aquihaytomate-X.Y.Z-vcN-release.{apk,aab}`. El nombre
lleva la versión a propósito: el bundle de Bebe Agua se llamaba igual y se confundieron.

```bash
BT=$(ls -d ~/Library/Android/sdk/build-tools/* | tail -1)
A=app/build/outputs/apk/release/aquihaytomate-X.Y.Z-vcN-release.apk
$BT/aapt2 dump badging $A | head -1                  # versionCode y versionName
$BT/apksigner verify --print-certs $A | rg 'DN|SHA-256'
$BT/aapt2 dump permissions $A                        # comparar con la versión anterior
```

- **Clave de subida a Play**: `CN=Jorge, OU=Personal, O=JJRapps, L=Madrid, ST=Madrid, C=ES`, SHA-256
  `67e42f5ba85072c829cde58fc0b484a4b8bb349f1df6eb07dcf8fafb8784ec98`. Es el keystore de Bebe Agua pero
  **otra clave**: si sale otro certificado, Play rechaza el bundle (no publica nada, pero no sigas).
- **Permisos de la 1.4.1**: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`,
  `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `VIBRATE`, `WAKE_LOCK` y el
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` que añade AndroidX. **Ni `INTERNET` ni `USE_EXACT_ALARM`**:
  si aparecen, para y averigua de dónde vienen (el revisor de F-Droid ya cazó un `INTERNET` colado).

---

## 4. Publicar en Google Play

```bash
con-claves 'fastlane subir'
```

Vuelve a compilar el AAB firmado y lo publica en **producción** con estado `completed`: sale
directamente, sin borrador. Debe acabar con `Successfully finished the upload to Google Play`.

- **Solo sube el binario y las novedades de su `versionCode`.** La ficha (títulos, descripciones,
  capturas) se escribió a mano en Play Console y el `Fastfile` no la toca, a diferencia del de
  Constanza. El título real es `¡Aquí hay tomate! - Pomodoro` en los dos idiomas; no «corregirlo».
- Para dejarla preparada sin publicar: `con-claves 'fastlane subir track:internal release_status:draft'`.
- Play revisa la versión antes de que llegue a los móviles: de unas horas a un par de días.
- No definir `SUPPLY_JSON_KEY`: fastlane la lee por su cuenta y gana sin avisar a la clave del `Appfile`.

---

## 5. Publicar en F-Droid

```bash
git tag -a vX.Y.Z <sha-del-merge-en-main> -m "¡Aquí hay tomate! X.Y.Z"
git push origin vX.Y.Z
```

Nada más. La receta de `fdroiddata` tiene `UpdateCheckMode: Tags` y `AutoUpdateMode: Version`:
F-Droid detecta el tag, lee la versión de `app/build.gradle.kts`, compila y firma con **su** clave.
Tarda unos días. No hace falta GitHub Release (no se ha publicado ninguna).

Antes de etiquetar, que siga en pie lo que F-Droid necesita para compilar:

```bash
rg -n 'includeInApk = false' app/build.gradle.kts   # dependenciesInfo desactivado
rg -c foojay settings.gradle.kts                     # 0: sin el plugin foojay
```

Detalles en [`fdroid/LEEME.md`](fdroid/LEEME.md).

---

## 6. Dejar constancia

- En `CHANGELOG.md`, bajo la versión: «**Enviada a Google Play el AAAA-MM-DD.**»
- Lo que queda sin comprobar en un móvil real, en `docs/estado-del-proyecto.md` si cambia el estado.

## Historial

| Versión | versionCode | Fecha | Play | F-Droid |
|---|---|---|---|---|
| 1.4.1 | 9 | 2026-11-05 | `fastlane subir`, producción | tag `v1.4.1` sobre `727b616` |
