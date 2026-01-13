# Release operation

1. Update version
  1. Update version in project.clj
  2. Update pom.xml (re-create with lein pom)
  3. Update README.md (the "Installation" vector)
  4. Commit
2. Create a new release on Github pointing to that commit
  1. [Draft release](https://github.com/StudistCorporation/sleepydog/releases/new)
  2. From the Select tag dropdown, choose "Create new tag"
  3. Create tag matching the version in project.clj
    a. For example if it's "0.2.0" in project.clj, the tag should be "v0.2.0"
  4. Write release notes
  5. "Publish release"

Once the release is published, a Github Action will run to push the release to Clojars.

## Pre-releases

It can be useful to cut pre-releases for testing purposes. In that case, use versions with suffixes like "0.1.0-alpha1" or "0.2.1-rc2".

Pre-releases should be set as such when you create the release on Github, by checking the "Set as a pre-release" checkbox.
