# tw-context

## Owner field

Many applications have multiple owners - on endpoints, jobs and other unit of works.

It is useful to correlate Rollbar errors, logs and even metrics by specific owners.

`com.transferwise.common.context.TwContextAttributeChangeListenerTest.ownerCanBeSetWhenNameIsChanged` describes how
to make the application set the owner field.

But for services, to set an owner it is recommended to use `tw-context-ownership-starter` module instead.
Please consult with it's [README](tw-context-ownership-starter/README.md) what it can do and how to use it.