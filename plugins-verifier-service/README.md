## Plugin Verifier Service

## Deprecation Notice

The _Plugin Verifier Service_ is deprecated and no longer under active development.

It has been replaced by a different implementation, except a single usage of `FeatureExtractorService`.

## About Project
Service used to off-load heavy verification tasks from [JetBrains Marketplace](https://plugins.jetbrains.com/):
- verify newly uploaded plugins against IDE builds using the *IntelliJ Plugin Verifier* tool (see `/intellij-plugin-verifier`)
- extract supported plugin features using the *IntelliJ Feature Extractor* tool (see `/intellij-feature-extractor`)

[JetBrains Marketplace](https://plugins.jetbrains.com/) *does not call* this service directly. Instead, this service periodically polls the queue of available tasks from the Marketplace,
executes them and sends results back: `VerifierService` polls JetBrains Marketplace using `VerifierServiceProtocol.requestScheduledVerifications` every minute,
schedules `VerifyPluginTask` and once they are completed, sends the results using `VerifierServiceProtocol.sendVerificationResult`.

The Verifier Services sends a set of available IDE builds to [JetBrains Marketplace](https://plugins.jetbrains.com/) for consideration (`AvailableIdeService`).
JetBrains Marketplace selects some IDE builds and verifies plugins against them.
  
### How to deploy
Read the [doc](https://jetbrains.team/p/intellij-plugin-verifier/documents/Plugin-Verifier-Service/a/Deploy-the-Plugin-Verifier-Service).

### How to run locally
Run `Start Plugin Verifier locally` configuration. There are many JVM properties specified (see all in the `Settings.kt`).
At least the following should be adjusted for your local environment (copy the run configuration locally):
- `-Dverifier.service.home.directory` — path to the service home directory, where all downloaded files, plugins, IDEs and temporary files are stored
- `-Dverifier.service.jdk.8.dir` — path to the JDK used as a fallback by Plugin Verifier
- `-Dverifier.service.plugins.repository.token` — permanent token used to authorize the service requests on JetBrains Marketplace
