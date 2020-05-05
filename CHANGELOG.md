# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.1] - 2020-05-05
### Changed
- Deadline can be now extended. Even when this would be very bad practice, it is sometimes needed during migration and optimization process.

For example in Payout Service `markCompleted` endpoint was timing out, but the transaction behind it finished.
When we introduced deadline interceptors, there, Ops were not able to complete those batches anymore, because the transactions were interrupted with DeadlineExceededException.

When deadline is exceeded, a special counter `tw.context.deadline.extended` is increased, to check quality of the service.