# MangaBall Enhanced — extension for Komikku

An improved source for **[mangaball.com](https://mangaball.com)** (the site formerly at
`mangaball.net`) that adds a real advanced-search experience to Komikku:

| You asked for | What this extension gives you |
| --- | --- |
| **Enhanced search** | A query language inside the search box: `tag:romance -tag:ecchi sort:views status:ongoing pages:3`, plus an extra-keyword field and an optional multi-page search depth |
| **Sorting by genre** | *Sort by* (latest update / most viewed / top rated / recently added / title) **and** a local **Focus genre** ordering that floats your favourite genre to the top of the results |
| **Sorting by tags** | Local **Focus tag** ordering and **Result ordering → Best tag match first**, which ranks results by how many of your included tags they carry |
| **Including or excluding tags** | Every genre, theme, format and content tag is a **tri-state filter**: tap once = *include*, tap twice = *exclude*. Includes are combined with **AND / OR** logic, exclusions always subtract |

Everything else you expect is here too: latest updates, popular, details, chapter lists,
page reading, 40 language variants, and a few quality-of-life switches.

> **Not affiliated with MangaBall.** This is an unofficial, personal extension that talks to
> the site's public API. All content belongs to its respective owners.

---

## 1. Install it on your phone (no coding needed)

1. **Open this repository's Releases page** (GitHub app or browser):
   `Releases` → the newest release → download the file
   `tachiyomi-all.mangaball-enhanced-v1.6.x.apk`.
2. **Tap the downloaded file.** Android will warn you that the app is from an unknown source —
   allow installation for your browser/file manager when it asks. (The APK is signed, but it is
   not signed by the official Komikku key, so Android always shows this warning.)
3. **Open Komikku** → `More` (⋮) → **Extensions**.
4. Open the **Untrusted** tab, find **MangaBall Enhanced** and tap the **shield** icon.
   This is required once per install — the app does not know the key this APK was signed with.
5. Done. In Komikku go to **Browse**, and you will find `MangaBall (English)`,
   `MangaBall (Español)`, `MangaBall (Français)` … pick the one you want and optionally pin it
   to the top.

### Updating later

Whenever a new version is published, download the new APK and install it over the old one —
the signature stays the same, so Android updates it normally. Do **not** uninstall the old
version first (you would lose your library entries for these sources).

---

## 2. Using the filters (the important part)

Open a MangaBall source → tap the **Filter** icon in the search bar. From top to bottom:

| Filter | What it does |
| --- | --- |
| **Sort by** | Server-side ordering: Latest updated, Most viewed, Top rated, Recently added, Title. Tap a row to sort descending, tap it again for ascending. |
| **Result ordering (local)** | Re-orders the page you already downloaded, no extra requests: *Best tag match first*, *Focus genre first*, *Focus tag first*, *Title (A-Z)*, *Most viewed first*, *Most chapters first*. |
| **Focus genre** | Pick e.g. *Romance*; with *Focus genre first* ordering those titles jump to the top. |
| **Focus tag** | Same idea for themes/formats, e.g. *Martial Arts*, *Long Strip*. |
| **Type / Magazine demographic / Publication status** | Manga / Manhwa / Manhua / Comics, Shounen … Josei, Ongoing / Completed / Hiatus / Cancelled. |
| **Tag mode** | `AND` = results must contain **every** included tag, `OR` = **at least one** included tag. Excluded tags are always removed. |
| **18+ content from the server** | Include or exclude 18+ titles for this search. |
| **Extra keywords** | Additional words appended to whatever you type in the search box. |
| **Languages** | Tap a language once to **require** it, twice to **exclude** it. Titles are matched on the languages they actually have chapters in. |
| **Genres / Themes / Formats / Content warnings** | The tri-state tag lists — tap once to include (✓), twice to exclude (✗). |

Working example: Sort by *Most viewed*, Tag mode *AND*, **Romance** ✓ + **Drama** ✓,
**Ecchi** ✗, Language *English* ✓, Result ordering *Focus genre first*, Focus genre *Romance*.
That is "the most popular English romance-dramas without ecchi, romance first".

---

## 3. Enhanced search query language

Type these directly into the search box (they can be mixed with normal words):

| Token | Meaning | Example |
| --- | --- | --- |
| `tag:NAME`, `genre:NAME`, `theme:NAME`, `format:NAME` | Include a tag | `tag:action`, `genre:slice of life`, `format:"long strip"` |
| `-tag:NAME` | Exclude a tag | `-tag:ecchi`, `-theme:incest` |
| `sort:SORT` | `views`, `lastupdate`, `rating`, `created_at`, `name` | `sort:rating` |
| `order:asc` / `order:desc` | Sort direction | `order:asc` |
| `status:STATE` | `ongoing`, `completed`, `hiatus`, `cancelled` | `status:completed` |
| `type:TYPE` | `manga`, `manhwa`, `manhua`, `comics` | `type:manhwa` |
| `demo:GROUP` | `shounen`, `shoujo`, `seinen`, `josei` | `demo:seinen` |
| `lang:CODE` | Require result languages, comma separated | `lang:en,es` |
| `adult:safe` / `adult:all` | Skip or include 18+ titles | `adult:safe` |
| `mode:and` / `mode:or` | Tag logic | `mode:or` |
| `pages:N` (1–5) | Merge N result pages into one search | `pages:3` |

Example: `solo leveling tag:action tag:fantasy -tag:harem sort:views status:ongoing pages:2`

Quotes work for names with spaces: `tag:"martial arts"`.
Unknown tag names are ignored, and if a `tag:` name cannot be resolved the text is simply
treated as a normal keyword — searches never break because of a typo.

---

## 4. Source settings

Komikku → *MangaBall source* → ⚙️ (per-extension settings):

- **Hide 18+ titles** — asks the server to leave out 18+ results.
- **Only titles available in this source's language** — local filter on top of the results.
- **Show chapters in every language** — off by default: an English source lists English chapters only.
- **Use Cloudflare bypass client** — turn this on if searches fail with HTTP 403 or "Just a moment".
- **Search depth (1–5 pages)** — how many result pages one search merges (higher = slower, more results).
- **Scanlator blacklist** — comma separated group names whose chapters are hidden.

---

## 5. Languages / sources included

One source is created per site language, e.g. `MangaBall (English)`, `MangaBall (Español)`,
`MangaBall (Português (Brasil))`, `MangaBall (Français)`, `MangaBall (Deutsch)`,
`MangaBall (Italiano)`, `MangaBall (Русский)`, `MangaBall (العربية)`, `MangaBall (Indonesia)`,
`MangaBall (Tiếng Việt)`, `MangaBall (ไทย)`, `MangaBall (日本語)`, `MangaBall (한국어)`,
`MangaBall (中文)`, plus 25 more. Pin the one you read in; the others can stay unpinned.

---

## 6. How the builds work

This repository contains the full source, and GitHub builds the APK for you:

- `.github/workflows/build.yml` runs on every push: it sets up the Android SDK, builds the
  release APK, signs it with the keystore in `ci/mangaball.jks`, verifies the result,
  and publishes it to **Releases**. The build number automatically becomes the version code,
  so each release can be installed over the previous one.
- `build-info/apk-verification.txt` records the signature, version and manifest metadata of
  the newest APK.

```
extension/
  build.gradle.kts                                   build config + metadata
  src/main/AndroidManifest.xml                       the meta-data Komikku reads
  src/main/kotlin/eu/kanade/tachiyomi/extension/all/mangaball/
    MangaBallFactory.kt   creates one source per language
    MangaBall.kt          the source itself (browse, details, chapters, pages, settings)
    MangaBallQuery.kt     the search query language
    MangaBallFilters.kt   the filter UI
    MangaBallCatalog.kt   tag catalogue (learns new tags from the site)
    MangaBallApi.kt       API parsing helpers
tools/make_icon.py                                    generates the extension icon
```

Want to change something? Edit the file, then ask the assistant — every push rebuilds the APK
in about three minutes.

---

## 7. Troubleshooting

| Symptom | Fix |
| --- | --- |
| Source shows a red *untrusted* marker | Extensions → Untrusted → tap the shield next to *MangaBall Enhanced*. |
| Source is not listed in Browse at all | The site carries adult titles, so the extension is flagged as mixed content. Enable the NSFW/adult-sources toggle in Komikku's settings (Settings → Browse) and reopen Komikku. |
| Search returns nothing | Clear the included tags / switch Tag mode from AND to OR — very specific combinations genuinely have no results. |
| "HTTP 403" or a Cloudflare page | Enable **Use Cloudflare bypass client** in the source settings. |
| "App not installed" when updating | You are installing an APK with a different signature — uninstall the old extension first (the library entries can be re-added afterwards). |
| Chapters list is empty | The title may only have chapters in another language — enable **Show chapters in every language** or switch to that language's source. |
| Tags I expect are missing from the filter list | Type `tag:the name` in the search box — the tag list also learns new tags automatically from results. |

---

## 8. Notes

- Tags are sent to the API by **id**, which is why the filter lists are bundled with the
  extension and refreshed automatically when the site adds new tags.
- Language filtering is done locally, on the languages each title actually has chapters in —
  the API's own language parameter is not documented, and guessing it silently returned zero
  results.
- The extension is deliberately compact: everything it needs is provided by the reader app,
  so the APK is under 1 MB.
