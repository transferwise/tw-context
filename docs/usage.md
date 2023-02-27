# Usage

To use the library, first add the `mavenCentral` repository to your `repositories` block in your `build.gradle`:
```groovy
repositories {
  mavenCentral()
}
```
Then, add the `tw-context` library as a dependency in your `dependencies` block in your `build.gradle`:
```groovy
dependencies {
  implementation 'com.transferwise.common:tw-context:<VERSION>'
}
```
> Replace `<VERSION>` with the version of the library you want to use.
> You can also use `tw-context-starter` which autoconfigures some Spring beans. Or `tw-context-ownership-starter`  if you want to automatically set the `TwContexts`'s `owner` attribute.