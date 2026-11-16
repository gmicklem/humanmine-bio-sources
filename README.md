HumanMine
============

Master: [![HumanMine bio sources CI](https://github.com/intermine/humanmine-bio-sources/workflows/HumanMine%20bio%20sources%20CI/badge.svg?branch=master)](https://github.com/intermine/humanmine-bio-sources/actions?query=workflow%3A%22HumanMine+bio+sources+CI%22)
[![Version](http://img.shields.io/badge/version-2.0.0-blue.svg?style=flat)](https://github.com/intermine/intermine/releases)
[![License](http://img.shields.io/badge/license-LGPL_2.1-blue.svg?style=flat)](https://github.com/intermine/intermine/blob/master/LICENSE)
[![Research software impact](http://depsy.org/api/package/pypi/intermine/badge.svg)](http://depsy.org/package/python/intermine)
[![Conda](https://anaconda.org/intermine/intermine/badges/installer/conda.svg)](https://anaconda.org/bioconda/intermine)

This is a repository of all the data parsers used to load human genomic data into an InterMine instance (HumanMine). These are humanmine-specific data, and in addition to the [core data parsers](http://intermine.readthedocs.io/en/latest/database/data-sources/library/) available in InterMine. See the [intermine-scripts](http://github.com/intermine/intermine-scripts) respository for the scripts we use to actually download the fly data.

* [HumanMine](http://www.humanmine.org)
* [User Docs](http://flymine.readthedocs.io/en/latest/)
* [InterMine](http://www.intermine.org)
* [InterMine Docs](http://intermine.readthedocs.io/en/latest/)

Building (InterMine 6.0 line)
------------------------

These data sources are built with Gradle 8.14 (the wrapper downloads it) against the `maint-2026` branch of
[InterMine](https://github.com/intermine/intermine), which becomes InterMine 6.0.0. They are Java 8 bytecode whatever JDK builds them.

* **JDK 17** runs Gradle (`export JAVA_HOME=...`; 21 also works). JDK 8 still works, with Gradle's deprecation notice.
* **InterMine artefacts.** `gradle.properties` sets `imVersion` and `bioVersion` to `5.1.1`, the version the `maint-2026` branch
  publishes until 6.0.0 is released. Until then publish them from an InterMine checkout of `maint-2026` into your local Maven repository
  (`~/.m2`), where `mavenLocal()` finds them. The checkout has no `gradle.properties` or `gradle-wrapper.properties` until
  `config/lib/install_intermine.py` has written them, so use that script (it also runs `publishToMavenLocal` in `plugin`, `intermine`, `bio`,
  `bio/sources` and `bio/postprocess`, in that order):

  ```bash
  cd intermine            # the maint-2026 checkout
  export PSQL_HOST=localhost PSQL_USER=test PSQL_PWD=test
  python3 -I config/lib/install_intermine.py
  ```

  To keep the artefacts apart from your own `~/.m2`, use `./gradlew -Dmaven.repo.local=/some/dir publishToMavenLocal` in each of those builds instead
  and add the same `-Dmaven.repo.local=/some/dir` to every command here.
  **When 6.0.0 is on Maven Central, change `systemProp.imVersion` and `systemProp.bioVersion` in `gradle.properties` to `6.0.+`**
  and nothing needs publishing locally any more.
* **Tests** run the converters against the `bio-test` database, which is described in `~/.intermine/intermine-bio-test.properties`
  (the file the InterMine CI writes: `config/ci-bio.properties` in this repository is an example, `config/init.sh` creates the
  database and user for PostgreSQL). The build copies that file into the test resources; it does not go into the jars.
* **Build and test** all sources, with Checkstyle: `./gradlew build`. One source: `./gradlew :bio-source-gtex:build`. `signor` is part of the
  build again (it had been left out of `settings.gradle` since May 2019, when its source was "temporarily moved to a branch"; HumanMine's
  `project.xml` still has it commented out). `DepmapSampleInfoConverterTest` compares with its target items; to regenerate them, uncomment its
  `writeItemsFile`, which writes to `build/`, not into the source tree.
* **Publish** to the local Maven repository, to build a mine against these sources: `./gradlew publishToMavenLocal`
  (`./gradlew install` is kept as an alias). The artefacts are `org.intermine:bio-source-<name>:4.3.0`, with a sources and a javadoc jar.
  In the mine's `project.xml` a `<source>` with `version="4.3.0"` resolves to them.
* **Maven Central** (a release manager's job, nothing here talks to the network): `SIGNING_KEY="$(gpg --armor --export-secret-keys <KEYID>)"
  SIGNING_PASSWORD=... ./gradlew centralBundle` signs the publications and writes `build/central-bundle/bio-sources-human-bundle.zip`, which is uploaded
  to the Central Portal as described in InterMine's `docs/RELEASING.md`. Signing is only done for `centralBundle`, never for local builds.

What a CI run needs until InterMine 6.0.0 is published
------------------------

The workflow in `.github/workflows` runs `./config/init.sh && ./config/run.sh` on a plain `ubuntu-latest` runner with PostgreSQL. `gradle.properties`
asks for InterMine `5.1.1` (`imVersion`, `bioVersion`), and that is the version string the `maint-2026` branch of
[intermine/intermine](https://github.com/intermine/intermine) publishes. Maven Central has no 5.1.1 and no 6.x yet (its newest `org.intermine:plugin`
is 5.1.0, which is another build), so a CI run resolves `org.intermine:plugin:5.1.1` and the `bio-core`, `intermine-*` artefacts only if they were put in
the runner's local Maven repository first. Until 6.0.0 is on Central, a run needs these steps before `./config/init.sh`:

1. JDK 17 (the workflow has it) and Python 3.10 or later: InterMine's `config/lib/install_intermine.py` uses `dataclasses` and `glob(root_dir=...)`.
   The workflow's "Set up python 2.7" step does not provide that; add a `actions/setup-python` step for Python 3.10+ (or run the script with an
   installed `python3` of that version).
2. A checkout of `maint-2026` of `intermine/intermine` (for example `actions/checkout` with `repository: intermine/intermine`, `ref: maint-2026`,
   `path: intermine`).
3. Publish its artefacts into the runner's `~/.m2` (the default `mavenLocal()` of this build reads), as in "InterMine artefacts" above:
   `PSQL_HOST=localhost PSQL_USER=test PSQL_PWD=test python3 -I config/lib/install_intermine.py` in that checkout (a few minutes; it also writes
   `~/.intermine/*.properties` unless they exist).
4. PostgreSQL with the `bio-test` database and the `test` user: `./config/init.sh` creates them on the runner (`sudo -u postgres createuser test`, ...),
   as before.

The tests and `checkstyleMain` then run against those artefacts. When 6.0.0 is on Central, change `systemProp.imVersion` and `systemProp.bioVersion`
in `gradle.properties` to `6.0.+` and steps 1 to 3 go away. The workflow files do not depend on the InterMine checkout; they are unchanged, so a run without these
steps fails at the first resolution of `org.intermine:plugin:5.1.1` (that is the expected failure until then).

A converter that is written for a mine keeps working as before. The dependency scopes of the published POMs are those of InterMine's own
bio sources (see "Dependency scopes in the bio-source POMs" in InterMine's `docs/UPGRADING-build.md`): the types in the converters'
signatures are `compile`, everything else is `runtime`, so a project that compiles against one of these jars declares the libraries
it imports itself.

Copyright and Licence
------------------------

Copyright (C) 2002-2018 InterMine

See [LICENSE](LICENSE) file for licensing information.

This product includes software developed by the
[Apache Software Foundation][apache]

Please cite
------------------------

**InterMine: a flexible data warehouse system for the integration and analysis of heterogeneous biological data.**<br/>
*Smith RN, Aleksic J, Butano D, Carr A, Contrino S, Hu F, Lyne M, Lyne R, Kalderimis A, Rutherford K, Stepan R, Sullivan J, Wakeling M, Watkins X, Micklem G.* <br/>
[Bioinformatics (2012) 28 (23): 3163-3165.](http://bioinformatics.oxfordjournals.org/content/28/23/3163.abstract) <br/>
[![doi](http://img.shields.io/badge/doi-10.1093%2Fbioinformatics%2Fbts577-blue.svg?style=flat)](http://bioinformatics.oxfordjournals.org/content/28/23/3163.abstract) 
[![pubmed](http://img.shields.io/badge/pubmed-23023984-blue.svg?style=flat)](http://www.ncbi.nlm.nih.gov/pubmed/23023984)


[readthedocs]: http://intermine.readthedocs.org/en/latest
[tutorial]: http://intermine.readthedocs.org/en/latest/get-started/tutorial
[psql]: http://www.postgresql.org
[java]: http://openjdk.java.net
[apache]: http://www.apache.org
[tomcat]: http://tomcat.apache.org/download-70.cgi
