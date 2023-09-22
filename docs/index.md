# tw-context Documentation

Defines and owns important concepts of
- entrypoints
- unit of work
- deadlines
- criticality

## Owner field

Many applications have multiple owners - on endpoints, jobs and other units of work.

It is useful to correlate Rollbar errors, logs and even metrics by specific owners.

`com.transferwise.common.context.TwContextAttributePutListenerTest.ownerCanBeSetWhenNameIsChanged` describes how
to make the application set the owner field.

But for services, to set an owner it is recommended to use `tw-context-ownership-starter` module instead.
Please consult with its [README](https://github.com/transferwise/tw-context/blob/master/tw-context-ownership-starter/README.md) what it can do and how to use it.