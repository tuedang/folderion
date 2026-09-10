# Folderion

Filesystem-first record storage for Java: **one bucket = one record type**, **one folder = one record** (source of truth). Indexes are optional and rebuildable.

## Layout

```text
buckets/centris/
  bucket.json
  schemas/centris.listing.v1.json
  records/
    27481461/
      record.json
      README.md
      source/original.url
      media/images/...
      .state/content_fingerprint
```

## Usage

```java
import dev.folderion.bucket.*;

Folderion folderion = Folderion.create();
Bucket bucket = folderion.init(path, config, schema);

CommitResult result = bucket.commit(WritePlan.builder()
    .id("27481461")
    .record(payload)
    .sourceUrl("https://...")
    .readmeMarkdown("# ...")
    .images("images", List.of(...))
    .build());
```

Re-commit with the same content fingerprint → `UNCHANGED` (only `last_seen_at` updates).

Lean history: schema `historyFields` + `maxHistoryVersions`. When those fields change, prior
`record.json` is archived under `records/{id}/{id}_v1/` (then `_v2`, …) inside the record folder.
HEAD remains `records/{id}/` (no version suffix on the parent). Older versions beyond the max are
dropped. Media is not copied into version folders.

## Build

```bash
./gradlew test
```
