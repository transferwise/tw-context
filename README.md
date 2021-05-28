# tw-context
![Apache 2](https://img.shields.io/hexpm/l/plug.svg)
![Java 11](https://img.shields.io/badge/Java-11-blue.svg)
![Maven Central](https://badgen.net/maven/v/maven-central/com.transferwise.common/tw-context)

Defines and owns important concepts of
- entrypoints
- unit of work
- deadlines
- criticality

## Owner field

Many applications have multiple owners - on endpoints, jobs and other units of work.

It is useful to correlate Rollbar errors, logs and even metrics by specific owners.

`com.transferwise.common.context.TwContextAttributeChangeListenerTest.ownerCanBeSetWhenNameIsChanged` describes how
to make the application set the owner field.

But for services, to set an owner it is recommended to use `tw-context-ownership-starter` module instead.
Please consult with its [README](tw-context-ownership-starter/README.md) what it can do and how to use it.

## License
Copyright 2021 TransferWise Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.