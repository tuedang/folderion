# Folderion

Filesystem-first record storage for Java, backed by **[OCFL](https://ocfl.io/1.1/spec/)** via
[`ocfl-java`](https://github.com/OCFL/ocfl-java) (flat layout).

## Modules

| Module | Role |
|--------|------|
| `folderion` | Core OCFL buckets + Centris listing contract (`CentrisBucket`, writer/reader) |
| `folderion-crawler` | Crawl Centris via [Crawl4AI](https://docs.crawl4ai.com/) and persist into `CentrisBucket` |

## Layout

```text
buckets/
  centris/                            ← Folderion meta + OCFL storage root
    bucket.json
    schemas/centris.listing.v1.json
    0=ocfl_1.1                        ← required (NAMASTE)
    ocfl_layout.json                  ← required
    centris-27481461/                 ← record object (data + versions)
      inventory.json
      v1/content/record.json
      v2/content/…
  centris-work/                       ← OCFL workspace (sibling)
```

## Usage (library)

```java
try (Bucket bucket = CentrisBucket.INSTANCE.init(Path.of("buckets/centris"))) {
    new CentrisWriter(bucket).write(listing, images);
}
```

## Crawler

`folderion-crawler` scrapes a Centris search URL, extracts each listing with a **classpath JSON schema**
(`JsonCssExtractionStrategy`), then writes via `CentrisWriter` / `CentrisBucket`.

Schemas (edit without recompiling Java field maps):

- `folderion-crawler/src/main/resources/schemas/centris-search.extraction.json`
- `folderion-crawler/src/main/resources/schemas/centris-listing.extraction.json`

```bash
./gradlew :folderion-crawler:run --args="--bucket buckets/centris --max-listings 5"
```

Defaults:

- Crawl4AI: `http://192.168.68.57:11235` (`CRAWL4AI_URL` / `--crawl4ai-url`)
- Bearer token: `mytoken` (`CRAWL4AI_TOKEN` / `--token`)
- Search URL: the Centris houses query baked into `CentrisCrawlerApp`

## Build

```bash
./gradlew test
./gradlew :folderion:test
./gradlew :folderion-crawler:test
```
