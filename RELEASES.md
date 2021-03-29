# Release Process

This document describes the release process of SDA Dropwizard Commons.

## Happy Path

The release process is automated.
It generates new releases for all merges to `master` or `release/2.x.x` and uploads all releases to Maven Central.
New releases are created based on Semantic Versioning (see also [CONTRIBUTING.md](CONTRIBUTING.md)).

As soon as a Pull Request was merged, the following actions happen in order:
1. It executes the Workflow to test the code ([java-ci.yml](.github/workflows/java-ci.yml)).
2. It executes the Workflow to determine if a new semantic version must be created ([java-ci.yml](.github/workflows/java-ci.yml)).
3. It creates a new release resulting in a new Git tag and a GitHub Release ([java-ci.yml](.github/workflows/java-ci.yml)).
4. The release creation triggers a Workflow for the upload ([publish-release-maven-central.yml](.github/workflows/publish-release-maven-central.yml)).
   1. Build and sign all artifacts (*.pom, *.jar, *-sources.jar, *-javadoc.jar).
   2. Create a new staging repository at https://oss.sonatype.org to allow parallel releases and increased stability.
   3. Publish the signed artifacts to the staging repository.
   4. Close the staging repository (using a [Gradle Plugin](https://github.com/Codearte/gradle-nexus-staging-plugin)).
   5. Release the staging repository (using a [Gradle Plugin](https://github.com/Codearte/gradle-nexus-staging-plugin)).

## Unhappy Paths

The release process can fail on different progress levels:
1. During the `master` build due to flaky tests.
2. During the upload due to network issues.
3. During the close or release of the staging repository due to network issues or other exceptions in the release plugin.

> Basic rule: Check https://oss.sonatype.org before restarting a release workflow.

### The `master` build failed and didn't create a release.

> Don't do concurrent builds for multiple commits on the same release branch (`master` and `release/1.x.x` are independent).

**Action:** It is safe to retry the build or to merge a new PR (all unreleased commits are included in the new release).

### The upload workflow fails to build or sign the artifacts.

> Everything that happens before creating a stating repository is safe to restart. 

**Action:** Restart the workflow. Broken code is unlikely, but if present, prepare a PR to resolve the issue. 

### One of the `publishMavenPublicationToMavenCentralRepository` gradle tasks failed.

**Action:** Login to https://oss.sonatype.org, remove the staging repository and restart the workflow.

### The `closeRepository` or `releaseRepository` gradle task failed.

> The upload succeeded but the release process couldn't be finished.

**Action:** Login to https://oss.sonatype.org, check if the contents of the repository look valid, close and release the repository manually.
            **DO NOT** restart the workflow; it is ok that it failed, as long as the release worked correctly.

Issues in the checks that sonatype executes are unlikely, but if present, discuss the issues and prepare a PR to resolve the issue.
