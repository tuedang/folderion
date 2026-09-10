# Folderion

Filesystem-first record storage for Java, backed by **[OCFL](https://ocfl.io/1.1/spec/)** via
[`ocfl-java`](https://github.com/OCFL/ocfl-java) (flat layout).

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

Spec markdown that ocfl-java copies into the storage root is deleted after init (not required to run).

## Usage

```java
try (Bucket bucket = CentrisBucket.init(Path.of("buckets/centris"))) {
    new CentrisWriter(bucket).write(listing, images);
}
```

## Build

```bash
./gradlew test
```
