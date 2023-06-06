# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Added project status badges on `README.md` file.
### Fixed
- CI: code coverage report when releasing.

## [2.1.1] - 2023-04-27
### Fixed
- The issue of resource allocation associated with regular (non-pool) plugins, happening when the resource usage 
  timeout was not explicitly configured.

## [2.1.0] - 2023-04-25
### Added
- The method `CardResource.getReaderExtension` to access the reader extension.
### Changed
- `CardResource` is now an interface.
### Upgraded
- "Calypsonet Terminal Reader API" to version `1.2.0`
- "Keyple Service Library" to version `2.2.0`
- "Keyple Util Library" to version `2.3.0`
- "Google Gson Library" (com.google.code.gson) to version `2.10.1`

## [2.0.2] - 2022-07-25
### Changed
- Usage of `CardReader` instead of `Reader`
### Upgraded
- "Keyple Service Library" to version `2.1.0`
- "Keyple Util Library" to version `2.1.0`

## [2.0.1] - 2021-11-22
### Added
- `CHANGELOG.md` file (issue [eclipse/keyple#6]).
- CI: Forbid the publication of a version already released (issue [#4]).
### Changed
- Useless check removed in configurator builder (issue [#9])
### Fixed
- Sonar issues (issue [#6]).

## [2.0.0] - 2021-10-06
This is the initial release.
It follows the extraction of Keyple 1.0 components contained in the `eclipse/keyple-java` repository to dedicated repositories.
It also brings many major API changes.

[unreleased]: https://github.com/eclipse/keyple-service-resource-java-lib/compare/2.1.1...HEAD
[2.1.1]: https://github.com/eclipse/keyple-service-resource-java-lib/compare/2.1.0...2.1.1
[2.1.0]: https://github.com/eclipse/keyple-service-resource-java-lib/compare/2.0.2...2.1.0
[2.0.2]: https://github.com/eclipse/keyple-service-resource-java-lib/compare/2.0.1...2.0.2
[2.0.1]: https://github.com/eclipse/keyple-service-resource-java-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/eclipse/keyple-service-resource-java-lib/releases/tag/2.0.0

[#9]: https://github.com/eclipse/keyple-service-resource-java-lib/issues/9
[#6]: https://github.com/eclipse/keyple-service-resource-java-lib/issues/6
[#4]: https://github.com/eclipse/keyple-service-resource-java-lib/issues/4

[eclipse/keyple#6]: https://github.com/eclipse/keyple/issues/6