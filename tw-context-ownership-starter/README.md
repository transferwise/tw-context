# tw-context-ownership-starter

An implementation to automatically get `TwContext` `owner` attribute set, based on which entrypoint the code is running in.

By default it is configured to use Github teams names as `owner` value.

Example configuration.
```yaml
tw-context:
  ownership:
    entrypoint-to-owner-mappings:
      - "Jobs:testJob1:latam"
      - "Web:/v1/profile/1 (GET):profile-service"
    default-owner: "webapp-reliability"
``` 