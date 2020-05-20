# tw-context

## Owner field

Many applications have multiple owners - on endpoints, jobs and other unit of works.

It is useful to correlate Rollbar errors, logs and even metrics by specific owners.

`com.transferwise.common.context.TwContextAttributeChangeListenerTest.ownerCanBeSetWhenNameIsChanged` describes how
to make the application set the owner field.