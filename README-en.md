<p style="text-align: center"><img src="assets/logo_180x.png" width="180" alt="PermPacks Logo"></p>
<p style="text-align: center"><b>PermPacks</b></p>

<p style="text-align: center">
  <span>English</span> | 
  <a href="README.md">中文</a>
</p>

A Paper plugin that distributes a layered resource-pack stack to players based on permissions. It supports both remote URLs and local files, ships with self-host and Amazon S3 upload backends, and can optionally block external packs for consistency.

### Highlights

- Permission-driven pack stack: filter packs from `packs.yml` by player permissions, load in ascending priority; later packs override earlier ones.
- Unified delivery for multiple sources: `url` and `file` items are unified into a single client request.
- Pluggable upload services: built-in `self_host` (embedded HTTP server) and `amazon_s3`; SHA-1 is auto-computed and reused to avoid duplicate uploads.
- External pack blocking: with ProtocolLib installed, block any resource pack not marked by this plugin.
- Hot reload and diagnostics: reload configs and redistribute online players; inspect pack details and player stack state.

### Requirements

- Paper 1.21+, Java 21
- Optional: ProtocolLib (recommended for external-pack blocking)

### Installation

1) Drop `PermPacks-<version>-all.jar` into `plugins/`
2) Start the server to generate `config.yml` and `packs.yml`
3) Edit configs as needed, then run `/permpacks reload packs` to apply

Note: the plugin creates cache/repository directories (e.g., `.filerepo/`). They are safe to delete; they will be re-created automatically.

### Configuration (`config.yml`)

```yaml
block_other_packs: false

file_upload:
  enabled: false
  cleanup: false
  service: self_host # amazon_s3, self_host
  self_host:
    host: 127.0.0.1
    port: 7077
  amazon_s3:
    endpoint: s3.amazonaws.com
    region: cn-north-1
    bucket: your-bucket-name
    directory: uploads
    access_key_id: ""
    secret_access_key: ""
    path_style_access: false
    chunked_encoding: true
```

- `block_other_packs`: if enabled, block any resource pack not marked by this plugin (requires ProtocolLib).
- `file_upload.enabled`: enable usage of `file` items in `packs.yml`; when disabled, `file` items are ignored.
- `file_upload.service`: choose `self_host` (embedded HTTP) or `amazon_s3`.
- `file_upload.cleanup`: on startup, remove unreferenced objects from the repository/bucket; keep only those referenced by current config.
- `self_host.*`: listen address and port for the embedded HTTP server.
- `amazon_s3.*`: S3 client and bucket settings; supports path-style addressing and chunked encoding.

### Packs (`packs.yml`)

Priority rules: default is 0; negatives allowed. Packs are loaded in ascending priority; items with equal priority are loaded in file order, with later items overriding earlier ones.

```yaml
example:
  permission: permpacks.pack.example
  priority: 10
  items:
    - url: https://example.com/high.zip
      hash: d41d8cd98f00b204e9800998ecf8427e
    - file: plugins/OtherPlugin/assets.zip
      hash: 900150983cd24fb0d6963f7d28e17f72
```

- `permission`: players owning this permission receive the pack (and its items).
- `priority`: lower loads earlier; later packs override earlier ones.
- `items.url`: direct download URL; providing official SHA-1 is recommended.
- `items.file`: local file path; requires upload service. The plugin validates, computes SHA-1, and uploads to self-host or S3 to generate an accessible URL.

Apply changes with: `/permpacks reload packs`.

### Commands & Permissions

- `/permpacks reload configs` (`permpacks.admin`): reload `config.yml` and `packs.yml`.
- `/permpacks reload packs` (`permpacks.admin`): re-cache and redistribute based on current config.
- `/permpacks info <PACK_ID>` (`permpacks.admin`): show permission and item details (URL/File and hashes) for a pack.
- `/permpacks check <PLAYER>` (`permpacks.admin`): compare player permissions with actually distributed packs; shows ✔/✘.

Aliases: `/ppacks`, `/pp`

### How It Works (Internals)

- Startup: `PermPacks` initializes `Options`, `BinaryCache`, and upload services (`SelfHostService`/`S3Service`), then reads and caches packs.
- Distribution: `Packer.distribute(player)` filters `Pack`s by permissions, builds a single request, and sends all items; each item is marked in `PackPacketTracker`.
- Blocking: when `block_other_packs` is enabled, `PackBlocker` (via ProtocolLib) blocks any outgoing resource pack not previously marked.
- Cleanup: with `file_upload.cleanup` enabled, unreferenced files/objects are cleaned up on self-host/S3 during startup.

### Build From Source

```bash
./gradlew build
```

- Artifact: `build/libs/PermPacks-<version>-all.jar`
- Dev server: `./gradlew runServer` (Paper 1.21)

### License

GPL-3.0. Contributions via Issues/PRs are welcome.


