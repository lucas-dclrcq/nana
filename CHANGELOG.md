# Changelog

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
