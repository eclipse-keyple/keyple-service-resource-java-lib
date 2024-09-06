# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.1.0] - 2024-09-06
### Added
- Added the method `SmartCard matches(SmartCard smartCard)` to `CardResourceProfileExtension` SPI.
  When using the card resource with pool plugins, this method allows the potentially smart card selected during reader 
  allocation process to be directly validated by the card profile extension.
  This feature enables network exchanges optimizations.
### Upgraded
- Keyple Service Lib `3.2.3` -> `3.3.0` (source code not impacted)

## [3.0.2] - 2024-06-25
### Changed
- Logging improvement.
### Upgraded
- Keyple Service Lib `3.2.1` -> `3.2.3` (source code not impacted)

## [3.0.1] - 2024-04-12
### Changed
- Java source and target levels `1.6` -> `1.8`
### Upgraded
- Keypop Reader API `2.0.0` -> `2.0.1`
- Keyple Common API `2.0.0` -> `2.0.1`
- Keyple Service Lib `3.0.0` -> `3.2.1`
- Keyple Util Lib `2.3.1` -> `2.4.0`
- Gradle `6.8.3` -> `7.6.4`

## [3.0.0] - 2023-11-28
:warning: Major version! Following the migration of the "Calypsonet Terminal" APIs to the
[Eclipse Keypop project](https://keypop.org), this library now implements Keypop interfaces.
### Added
- Added project status badges on `README.md` file.
### Changed
- Signature of method `CardResourceProfileExtension.matches(...)`:
  - previous: `SmartCard matches(CardReader reader, CardSelectionManager cardSelectionManager)`
  - current: `SmartCard matches(CardReader reader, ReaderApiFactory readerApiFactory)`
### Fixed
- CI: code coverage report when releasing.
### Upgraded
- Calypsonet Terminal Reader API `1.2.0` -> Keypop Reader API `2.0.0`
- Keyple Service Library `2.2.0` -> `3.0.0`
- Keyple Util Library `2.3.0` -> `2.3.1` (source code not impacted)

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
- `CHANGELOG.md` file (issue [eclipse-keyple/keyple#6]).
- CI: Forbid the publication of a version already released (issue [#4]).
### Changed
- Useless check removed in configurator builder (issue [#9])
### Fixed
- Sonar issues (issue [#6]).

## [2.0.0] - 2021-10-06
This is the initial release.
It follows the extraction of Keyple 1.0 components contained in the `eclipse-keyple/keyple-java` repository to dedicated repositories.
It also brings many major API changes.

[unreleased]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/3.1.0...HEAD
[3.1.0]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/3.0.2...3.1.0
[3.0.2]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/3.0.1...3.0.2
[3.0.1]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/3.0.0...3.0.1
[3.0.0]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/2.1.1...3.0.0
[2.1.1]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/2.1.0...2.1.1
[2.1.0]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/2.0.2...2.1.0
[2.0.2]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/2.0.1...2.0.2
[2.0.1]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/releases/tag/2.0.0

[#9]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/issues/9
[#6]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/issues/6
[#4]: https://github.com/eclipse-keyple/keyple-service-resource-java-lib/issues/4

[eclipse-keyple/keyple#6]: https://github.com/eclipse-keyple/keyple/issues/6