# Change Log

All notable changes to this project will be documented in this file.

## 2.3.0

### Added

- Can specify a Maven `<archive>` configuration element to add additional manifest headers, such as for custom Java operators.

### Removed

- Support for versions of Java prior to 21.
- Support for versions of Maven prior to 3.9.9.

## 2.2.1

### Removed

- Support for Java code coverage. Cobertura does not work with
  versions of Java supported by Streaming 10.6 and newer.
- Support for versions of Java prior to 17.

## 2.2.0

### Added

- Support for upcoming Streaming releases, notably by allowing
  currently available newer JDKs for building and testing.

## 2.1.0

### Changed

- [SB-51347](https://jira.tibco.com/browse/SB-51347) EP-MAVEN: Build tibco-streaming-maven-plugin with JDK 11.

## 2.0.0

### Added

* Add incremental build support.

## 1.6.0

### Added

- [SB-48296](https://jira.tibco.com/browse/SB-48296) EP-MAVEN: detect duplicate dependencies

- Fragment dependencies are skipped if already included via other dependencies.

- [SB-48476](https://jira.tibco.com/browse/SB-48476) EP-MAVEN: stamp product version into archive and fragment archive manifests

- Added product version to manifest and `pom.properties` files.

### Changed

- [SB-48274](https://jira.tibco.com/browse/SB-48274) EP-MAVEN: unpack-fragment goal is a bit chatty.

- Unpacking info lines have been reduced to the debug log level.

- [SB-50168](https://jira.tibco.com/browse/SB-50168) Remove github pages plugin

### Fixed

- [SB-48215](https://jira.tibco.com/browse/SB-48215) EP-MAVEN: Unable to create report directory
- [SB-41668](https://jira.tibco.com/browse/SB-41668) EP-MAVEN: Warning when using ep-maven-plugin with mvn -T option

- Improvements to thread safety

- [SBGPP-83](https://jira.tibco.com/browse/SBGPP-83) TEST: StreamBaseException: Error loading resource.

- Added missing target/eventflow to the test classpath.

- [SB-48556](https://jira.tibco.com/browse/SB-48556) EP-MAVEN: confusing messages running against read-only product installation

- Improve error message if product directory is not found.

## 1.5.0 - September 19, 2019

Initial GitHub release
