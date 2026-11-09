# Phabricator-Jenkins Plugin [![Build and Release](https://github.com/zryambus/phabricator-jenkins-plugin/actions/workflows/release.yml/badge.svg)](https://github.com/zryambus/phabricator-jenkins-plugin/actions/workflows/release.yml) [![Latest release](https://img.shields.io/github/v/release/zryambus/phabricator-jenkins-plugin)](https://github.com/zryambus/phabricator-jenkins-plugin/releases/latest)

This plugin provides [Phabricator][] integration with [Jenkins][]. It allows Jenkins to
report build status, unit test, lint and coverage information over Harbormaster (or via comments
if Harbormaster is not enabled).

> **_NOTE:_** This is a maintained fork of the archived [uber-archive/phabricator-jenkins-plugin](https://github.com/uber-archive/phabricator-jenkins-plugin).
> It tracks current Jenkins LTS releases, adds Pipeline support and uses the
> [Coverage plugin][] for coverage data instead of the retired Cobertura/JaCoCo plugins.

[Phabricator]: https://www.phacility.com/phabricator/
[Jenkins]: https://www.jenkins.io/
[Coverage plugin]: https://plugins.jenkins.io/coverage/

Table of Contents
=================
* [Requirements](#requirements)
* [Installation](#installation)
* [Configuration](#configuration)
  * [Phabricator Configuration](#phabricator-configuration)
  * [Jenkins Setup](#jenkins-setup)
* [Usage](#usage)
  * [Jenkins Job](#jenkins-job)
  * [Pipeline](#pipeline)
  * [Coverage](#coverage)
  * [Harbormaster](#harbormaster)
  * [Herald](#herald)
* [Advanced Usage](docs/advanced.md)
* [Test Your Configuration](#test-your-configuration)
* [Development](#development)
* [Releasing](#releasing)


Requirements
=============

* Jenkins **2.580.2** (LTS) or newer.
* Java **21** or newer on the Jenkins controller and agents.
* [Credentials plugin](https://plugins.jenkins.io/credentials/) (installed with Jenkins by default).
* Optional plugins:
  * [Pipeline](https://plugins.jenkins.io/workflow-aggregator/) — to use the `phabricatorStep` step.
  * [JUnit](https://plugins.jenkins.io/junit/) — to report unit test results to Harbormaster.
  * [Coverage][Coverage plugin] — to report coverage to Phabricator and Uberalls.
* [Arcanist](https://github.com/phacility/arcanist) is installed on the Jenkins nodes where the tests will be run (e.g. not just the master) the `arc` binary is in `$PATH` or configured explicitly in the global settings.

Installation
============

The plugin is not published to the Jenkins update center. Install it manually:

1. Download `phabricator-plugin.hpi` from the [latest release](https://github.com/zryambus/phabricator-jenkins-plugin/releases/latest).
2. In Jenkins, go to **Manage Jenkins** → **Plugins** → **Advanced settings**.
3. Under **Deploy Plugin**, choose the downloaded `.hpi` file and click **Deploy**.
4. Restart Jenkins.

Configuration
=============

Before the plugin can be used, a few configuration steps on your
Phabricator and Jenkins instances need to be completed.

Phabricator Configuration
-------------------------

In this section, you'll create a bot user in Phabricator and generate a Conduit API token. If you already have a bot user and a Conduit API token, skip to the "Jenkins Setup Section".

1. Create a bot user in Phabricator.
2. Click **Edit Settings** on the manage page for that user
3. Click **Conduit API Tokens** on the left of the settings page
4. Click **Generate API Token**, and accept.
5. Copy the token.

Jenkins Setup
-------------

1. Navigate to `https://ci.example.com/configure` with your base Jenkins URL in place of "ci.example.com".
2. Navigate to the **Phabricator** section and click the **Add** button. ![Add Credentials](/docs/add-credentials.png)
3. From the **Kind** dropdown, select **Phabricator Conduit Key**.
4. Enter the base URL for your Phabricator instance in the **Phabricator URL** field. For example `https://phabricator.example.com`.
5. Enter a description in the **Description** field for readability.![Configure Credentials](/docs/configure-credentials.png)
6. Paste the Conduit API token (created in the Phabricator Configuration section) in the **Conduit Token** field.
7. Click the **Add** button.
8. Click the **Save** button.

Usage
=====

Now that Jenkins and Phabricator are configured you can configure your Jenkins job and Harbormaster.

Jenkins Job
-----------

1. Navigate to the Jenkins job you want to integrate with Phabricator.
2. Click the **Configure** button.
3. Click the **Add Parameter** button and select **String Parameter**.
4. Enter `DIFF_ID` in the **Name** field of the parameter.
5. Repeat step 3.
6. Enter `PHID` in the **Name** field of the second parameter. ![Configure job parameters](/docs/configure-job-parameters.png)
7. If you want to apply the differential to your workspace before each test run, navigate to the **Build Environment** section and select the **Apply Phabricator Differential** checkbox. This resets to the base commit the differential was generated from. If you'd rather apply the patch to master, select the **Apply patch to master** checkbox.
![Enable build environment](/docs/configure-job-environment.png)
8. To report the build status to Phabricator after the test completes:
  1. Navigate to the **Post-build Actions** section.
  2. Click the **Add post-build action** button and select **Post to Phabricator**.
  3. Make sure the **Comment on Success** and **Comment with console link on Failure** checkboxes are selected.
  4. Optionally:
    1. If you have [Uberalls](https://github.com/uber/uberalls) enabled, enter a path to scan for Coverage reports.
    2. If you want to post additional text to Phabricator other than "Pass" and "Fail", select the **Add Custom Comment** checkbox. Then create a `.phabricator-comment` file and enter the text you want Jenkins to add to the build status comment in Phabricator.
![Add post-build action](/docs/configure-job-post-build.png)

Pipeline
--------

In Pipeline jobs, use the `phabricatorStep` step (shown as **Post to Phabricator** in the
Snippet Generator) instead of the post-build action. The job still needs the `DIFF_ID` and
`PHID` parameters. A build without a failure is reported as passed.

```groovy
pipeline {
    agent any
    parameters {
        string(name: 'DIFF_ID', defaultValue: '')
        string(name: 'PHID', defaultValue: '')
    }
    stages {
        stage('Test') {
            steps {
                sh './run-tests.sh'
            }
        }
    }
    post {
        always {
            phabricatorStep commentOnSuccess: true,
                            commentWithConsoleLinkOnFailure: true
        }
    }
}
```

Supported options: `commentOnSuccess`, `commentWithConsoleLinkOnFailure`, `customComment`,
`commentFile`, `commentSize`, `preserveFormatting`, `uberallsEnabled`, `coverageCheck`,
`coverageThreshold`, `minCoverageThreshold`, `coverageReportPattern`, `processLint`,
`lintFile`, `lintFileSize`, `sendPartialResults`.

The **Apply Phabricator Differential** build environment is available only in Freestyle jobs.
In Pipeline jobs, apply the patch yourself, for example with `arc patch --diff "$DIFF_ID"`.

Coverage
--------

Coverage is read from the [Coverage plugin][]. Publish your reports with the plugin
(the `recordCoverage` step in Pipeline, or the **Record code coverage results** post-build
action) before the Phabricator step runs. The line, branch, method, class, file and package
metrics are then sent to Phabricator and, if enabled, to [Uberalls](https://github.com/uber/uberalls).

If the Coverage plugin is not installed or the build has no coverage results, the plugin
falls back to parsing Cobertura, JaCoCo and Clover XML reports from the workspace that match
**Coverage Report Pattern** (default: `**/coverage*.xml, **/cobertura*.xml, **/jacoco*.xml`).

Harbormaster
------------

With Phabricator, Jenkins, and your Jenkins jobs configured it's time to configure a new Harbormaster build plan. This build plan will trigger the Jenkins job using a Herald rule that will be configured in the next section.

1. Navigate to `https://phabricator.example/harbormaster/plan/` with your base Phabricator URL in place of `phabricator.example`.
2. Click the **Create Build Plan** button in the top right corner of the page.
3. Enter a name for the build plan in the **Plan Name** field. For these instructions, we'll use "test-example" as the build name.
4. Click the **Create Build Plan** button.
5. Click the **Add Build Step** button.
6. Click the **Make HTTP Request** step.
7. Use this template URI to fill in the URI field for the build plan: `https://ci.example.com/buildByToken/buildWithParameters?job=test-example&DIFF_ID=${buildable.diff}&PHID=${target.phid}`

	Be sure to replace `https://ci.example.com` with the URI of your Jenkins instance and `test-example` with the name of your Jenkins job.

	If your Jenkins instance is exposed to the internet, make sure to install the [Build Token Root Plugin][] and fill in the `token` parameter.

[Build Token Root Plugin]: https://plugins.jenkins.io/build-token-root/

8. Click the **When Complete** dropdown menu and select **Wait For Message**.
9. Click the **Create Build Step** button.
![Harbormaster plan](/docs/harbormaster-plan.png)

Herald
------

With the build plans created it's time to create a Herald Rule to trigger the plans. The steps below will configure a Herald Rule to trigger the build plans on Differential Revisions to your repository.

1. Navigate to `https://phabricator.example/herald/` with your base Phabricator URL in place of `phabricator.example`.
2. Click the **Create Herald Rule** button in the top right corner of the page.
3. Select the **Differential Revisions** checkbox and click **Continue**.
4. Select the **Global** checkbox and click **Continue**.
5. Enter a name for the Herald Rule in the **Rule Name** field.
6. In the **Conditions** section, click the dropdown menu that says "Author" and select "Repository".
7. Enter your repository name in to the blank field in the **Conditions** section.
8. In the **Actions** section, click the dropdown menu that says "Add blocking reviewers" and select "Run build plans".
9. Enter the build plans that were created in the previous section in to the blank field in the **Action** section.
10. Click **Save Rule**.

![Herald rule](/docs/herald-rule.png)

Test Your Configuration
-----------------------

Try `arc diff`-ing on your repo. If everything goes well, you should see Jenkins
commenting on your diff:

![Example](/docs/uberalls-integration.png)

Advanced Usage
--------------

Now that you have build status and optionally coverage data set up, check out some
[advanced features](docs/advanced.md).

Development
-----------

The project is built with Maven and requires JDK 21.

Build the plugin and run the tests:
```bash
mvn verify
```

The plugin is written to `target/phabricator-plugin.hpi`.

Start a local Jenkins with the plugin installed:
```bash
mvn hpi:run
```

Open your browser to your [local instance](http://localhost:8080/jenkins).

Releasing
---------

1. Update the plugin version in `pom.xml` and commit it.
2. Push a tag `vX.Y.Z` (for example `v2.4.0`).

The [Build and Release](.github/workflows/release.yml) workflow builds the plugin, runs the tests
and publishes a GitHub release with the `.hpi` attached.

Pull Requests
-------------

Please open all pull requests and issues against
https://github.com/zryambus/phabricator-jenkins-plugin.

License
-------

MIT Licensed
