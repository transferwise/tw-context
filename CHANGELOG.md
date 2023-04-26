# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# [0.11.2] - 2023-04-26

### Changed

- Use new Spring Boot ^2.7 way of declaring auto-configurations so that `tw-context-starter` can be used with Spring Boot 3 applications.

## [0.11.1] - 2021-12-01

### Changed

- Deadline and Criticality are put into MDC.
  Especially, missing deadline makes debugging some timeout issues hard.

## [0.11.0] - 2021-05-27

### Changed

- Moving the minimum platform to JDK 11.
- Open sourcing it.

## [0.10.2] - 2021-04-21

### Changed

- Using a faster form for creating `java.time.Instant`-s. We are only interested in millisecond precision and getting nanoseconds seems to be
  relatively expensive operation.

## [0.10.1] - 2021-04-05

### Added

- NoOpTimeoutCustomizer for unit tests.

## [0.10.0] - 2021-04-01

### Added

- A method to `TimeoutCustomizer` for cases where creating a Duration is considered too expensive.

## [0.9.3] - 2021-03-20

### Fixed

- Restored deprecated `DefaultUnitOfWorkManager(MeterRegistry meterRegistry)` constructor to support some older libs.

## [0.9.2] - 2021-03-14

### Changed

- Small optimization around `TwContext` interceptors traversal.

## [0.9.1] - 2021-03-09

### Fixed

- Restored backward compatibility in `TwContextMetricsTemplate`.

## [0.9.0] - 2021-03-01

### Changed

- Small CPU optimizations

### Added

- MeterCache to circumvent Micrometer inefficiencies.

## [0.8.0] - 2021-02-25

### Added

- `MdcRestoringEntryPointInterceptor` for automatically cleaning MDC when exiting from the outmost entrypoint.

## [0.7.0] - 2020-09-31

### Added

- `UnitOfWorkManager.createEntryPoint` has method with `owner` attribute.
- `TwContext.isEntrypoint()` tells if we are running under some entrypoint.

### Changed

- Upgraded external libs.

## [0.6.1] - 2020-09-02

### Added

- Restored `com.transferwise.common.context.TwContextMetricsTemplate.registerDeadlineExceeded`
  old version for backward compatibility reasons.

## [0.6.0] - 2020-09-02

### Added

- Added owner tag for deadline metrics.

## [0.5.0] - 2020-08-24

### Added

- Added properties applying to all timeouts. The idea, is that timeout values set for production can often be too low for development environments,
  but asking engineers to always set timeouts with if statements ("if develenv then..."), in every single client, is not feasible. So instead, we
  define properties `tw-context.core.timeoutMultiplier` and `tw-context.core.timeoutAdditive`, which can be set globally, lets say as environment
  variables in custom environment. Also, an application is able to overwrite `TimeoutCustomizer` bean for more granual control.

## [0.4.0] - 2020-06-30

### Changed

- Removed 0.3.2 from repositories as minor version needed to be changed instead of patch one.

## [0.3.2] - 2020-06-29

### Changed

- Using a separate TwContextClockHolder to be able to mock time in local tests.

## [0.3.0] - 2020-05-20

### Added

- TwContext now has entry point "Owner" as first class citizen, together with name and group.

- tw-context-ownership-starter module. A library to automatically set entry point owner, based on configuration or "handler" classes.

## [0.2.2] - 2020-05-20

### Changed

- Deadline exception also contains information how long has passed from the start of the unit of work. In servlet case it means from beginning of the
  request.

## [0.2.1] - 2020-05-05

### Changed

- Deadline can be now extended. Even when this would be very bad practice, it is sometimes needed during migration and optimization process.

For example in Payout Service `markCompleted` endpoint was timing out, but the transaction behind it finished. When we introduced deadline
interceptors, there, Ops were not able to complete those batches anymore, because the transactions were interrupted with DeadlineExceededException.

When deadline is exceeded, a special counter `tw.context.deadline.extended` is increased, to check quality of the service.