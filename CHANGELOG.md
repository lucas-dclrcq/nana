# Changelog

## [0.10.0](https://github.com/lucas-dclrcq/nana/compare/v0.9.0...v0.10.0) (2026-08-22)


### Features

* allow using flaresolverr to bypass ddos guard ([7c5437c](https://github.com/lucas-dclrcq/nana/commit/7c5437c6770f8b435d22868a06b3d30b0e75c979))


### Bug Fixes

* disable flaresolverr in it tests ([5069ee8](https://github.com/lucas-dclrcq/nana/commit/5069ee89e6af5ee7b64edeed2183e4c93f4dbf98))

## [0.9.0](https://github.com/lucas-dclrcq/nana/compare/v0.8.0...v0.9.0) (2026-08-18)


### Features

* log anna http erors ([c42d135](https://github.com/lucas-dclrcq/nana/commit/c42d135673f5b9f3d78e8454a1572cfa28c6db46))


### Bug Fixes

* **deps:** update dependency io.quarkiverse.quinoa:quarkus-quinoa to v2.9.0 ([5e66603](https://github.com/lucas-dclrcq/nana/commit/5e66603e1565a665b4a2d7e1ee2cdc752220f86c))
* **deps:** update quarkus.platform.version to v3.38.2 ([731c537](https://github.com/lucas-dclrcq/nana/commit/731c537811b1e51d6142019cfaa0f392c37c8536))

## [0.8.0](https://github.com/lucas-dclrcq/nana/compare/v0.7.0...v0.8.0) (2026-08-18)


### Features

* set browser like user agent to bypass ddos protection ([d5f1713](https://github.com/lucas-dclrcq/nana/commit/d5f1713e6935399457d9d495bdc4a288529fb7b0))

## [0.7.0](https://github.com/lucas-dclrcq/nana/compare/v0.6.0...v0.7.0) (2026-08-10)


### Features

* add smallrye health ([da94d8c](https://github.com/lucas-dclrcq/nana/commit/da94d8c61676547e29825a28018f6e23e87e2dea))
* expose prometheus metrics ([f50a518](https://github.com/lucas-dclrcq/nana/commit/f50a518a8b88df4d3b84ddf9bad766cffe0d5824))


### Bug Fixes

* fix E2E test ([3a6183e](https://github.com/lucas-dclrcq/nana/commit/3a6183e5d92594d70f5dcfc8baa2fe3fe169cc04))
* fix native image build because of hibernate reactive ([af7a6a7](https://github.com/lucas-dclrcq/nana/commit/af7a6a7aa7415203cb005046a8bd1fe8a48c9467))
* replace test data support with flyway to reset db between tests ([e919949](https://github.com/lucas-dclrcq/nana/commit/e919949320a3f22aa11b3484872fe114012ffa83))

## [0.6.0](https://github.com/lucas-dclrcq/nana/compare/v0.5.0...v0.6.0) (2026-08-07)


### Features

* add docker compose example ([3107030](https://github.com/lucas-dclrcq/nana/commit/3107030f45d498fbace9e7bdc59a1bf0db454840))


### Bug Fixes

* **deps:** update dependency io.quarkiverse.quinoa:quarkus-quinoa to v2.8.4 ([ed5b801](https://github.com/lucas-dclrcq/nana/commit/ed5b80116a7f15cbd687e104ad2e043b2643e8f6))
* **deps:** update dependency org.jsoup:jsoup to v1.23.1 ([031b512](https://github.com/lucas-dclrcq/nana/commit/031b51254d5a01f3c844e02327238f01110488be))


### Documentation

* document user identification in README ([5db5650](https://github.com/lucas-dclrcq/nana/commit/5db56509c5178c01eb57342295eee2ebd1b5b948))

## [0.5.0](https://github.com/lucas-dclrcq/nana/compare/v0.4.0...v0.5.0) (2026-07-26)


### Features

* show fast download quota ([dc1a82b](https://github.com/lucas-dclrcq/nana/commit/dc1a82b1d46501b36dd5192d3d82da55660c930b))


### Bug Fixes

* **deps:** update dependency vue-router to v5 ([a7211ce](https://github.com/lucas-dclrcq/nana/commit/a7211ceba68b016f076675e21ce0c052e81c98a2))
* search should work without a secret key ([9f8e0df](https://github.com/lucas-dclrcq/nana/commit/9f8e0df1898dd018f58cacefe96572f2d719692d))

## [0.4.0](https://github.com/lucas-dclrcq/nana/compare/v0.3.0...v0.4.0) (2026-07-25)


### Features

* add configuration of allowed formats ([685ff2c](https://github.com/lucas-dclrcq/nana/commit/685ff2c4b4f04de2e0e93f26fadf09202f339923))


### Bug Fixes

* add missing dependency for quarkus reactive data ([19db353](https://github.com/lucas-dclrcq/nana/commit/19db353d8e3e6d3ea22fd42561d5739335ebd1c4))

## [0.3.0](https://github.com/lucas-dclrcq/nana/compare/v0.2.0...v0.3.0) (2026-07-25)


### Features

* **config:** simplyfy db url to avoid having to setup two url (blocking / reactive) ([dcd7f66](https://github.com/lucas-dclrcq/nana/commit/dcd7f66c229c2041fb5bc1cfcd093701a65ff56d))

## [0.2.0](https://github.com/lucas-dclrcq/nana/compare/v0.1.0...v0.2.0) (2026-07-25)


### Features

* use SSE events to make the frontend react to download events in real time ([1f8fb0a](https://github.com/lucas-dclrcq/nana/commit/1f8fb0af2eae85974af557c82ae9b41720f70434))
* **webui:** internationalize frontend (English + French) ([aaadd95](https://github.com/lucas-dclrcq/nana/commit/aaadd959d8891f2df31a66facbc64a70ed77aa35))


### Bug Fixes

* make max domain index coherent with tests ([56a8a5c](https://github.com/lucas-dclrcq/nana/commit/56a8a5c4ad091b2f8f9aa0e6d82098f09237ff76))


### Documentation

* rewrite readme + add screenshots ([83a54b0](https://github.com/lucas-dclrcq/nana/commit/83a54b0dd27257537476de7a57bad617c0977e56))
