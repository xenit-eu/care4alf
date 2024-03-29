# Changelog

## Release 4.0.0 (2024-02-22)
This release adds support for Alfresco 7.4.
This release drops support for Alfresco 6.0, 6.1, 7.0, 7.1 & 7.2.
### ALFREDOPS
- [MSMVV-11](https://xenitsupport.jira.com/browse/MSMVV-11): Add support for Alfresco 7.4 (by removing logging module) & removed Harbor references
- [ALFREDOPS-855](https://xenitsupport.jira.com/browse/ALFREDOPS-855): Fixed GitHub CI/CD Sonartype configuration

## Release 3.0.0 (2023-01-09)
This release drops support for Alfresco 5.x.
This release adds support for Alfresco 7.3
### ALFREDOPS
- [839](https://xenitsupport.jira.com/browse/ALFREDOPS-839): Add support for Alfresco 7.3

## Release 2.5.0 (2022-04-27)
This release drops support for Alfresco versions in [7.0.0, 7.0.1.3).
### ALFREDOPS
- [818](https://xenitsupport.jira.com/browse/ALFREDOPS-818): Fix support for Alfresco versions >=7.0.1.3
- [820](https://xenitsupport.jira.com/browse/ALFREDOPS-820): Fix Jenkins build

## Release 2.4.0 (2021-11-03)
### ALFREDOPS
- [423](https://xenitsupport.jira.com/browse/ALFREDOPS-423): Change browser to accept noderefs as component parts
- [566](https://xenitsupport.jira.com/browse/ALFREDOPS-566): Fix export function for custom content model types and
  properties
- [588](https://xenitsupport.jira.com/browse/ALFREDOPS-588): Add basic content data components to export options
- [656](https://xenitsupport.jira.com/browse/ALFREDOPS-656): Comply with stricter content-security-policy
- [714](https://xenitsupport.jira.com/browse/ALFREDOPS-714): Change httpclient used for solr connections to work
  around https://alfresco.atlassian.net/browse/SEARCH-2289
- [733](https://xenitsupport.jira.com/browse/ALFREDOPS-733): Add section on the company to the Readme
- [746](https://xenitsupport.jira.com/browse/ALFREDOPS-746): Add Alfresco 7 support

## Release 2.3.0 (2020-03-19)
### ALFREDOPS
- [468](https://xenitsupport.jira.com/browse/ALFREDOPS-468): Added support for unlimited users licenses and changed return values in case of a missing license.

## Release 2.2.1 (2020-02-20)
- [443](https://xenitsupport.jira.com/browse/ALFREDOPS-443): Removed obsolete \<tt> tags for javadoc
- [457](https://xenitsupport.jira.com/browse/ALFREDOPS-457): Updated Alfresco 6.1 base war to 'org.alfresco:content-services:6.1.1@war'
- [447](https://xenitsupport.jira.com/browse/ALFREDOPS-447): Updated documentation

## Release 2.2.0 (2020-02-20)
### GENERIC
- Upgraded Gradle plugin to v5.0.2
- Changed com.fasterxml.jackson.core:jackson-databind:2.3.2 from shadowDeps to alfrescoProvided

### ALFREDOPS
- [391](https://xenitsupport.jira.com/browse/ALFREDOPS-391): Parallelize Jenkins build
- [395](https://xenitsupport.jira.com/browse/ALFREDOPS-395): Bulk Module: Add "Disable Auditable Policies" to File and Metadata mode
- [432](https://xenitsupport.jira.com/browse/ALFREDOPS-432): Fix Java 8 and 11 compatibility
- [431](https://xenitsupport.jira.com/browse/ALFREDOPS-431): Fix dummymail configuration instructions
- [433](https://xenitsupport.jira.com/browse/ALFREDOPS-433): PermissionImport: CompanyHome permissions aren't removed when using removeFirst=true

### USGNL
- [773](https://xenitsupport.jira.com/browse/USGNLSLA-773): Added support for perpetual licenses

## Release 2.1.3 (2019-12-06)
### ALFREDOPS
- [399](https://xenitsupport.jira.com/browse/ALFREDOPS-399): Fix missing cache metrics due to guava classpath conflicts

## Release 2.1.2 (2019-12-03)
### ALFREDOPS
- [390](https://xenitsupport.jira.com/browse/ALFREDOPS-390): Fix publishing amp without classifier

## Release 2.1.1 (2019-12-03)
### ALFREDOPS
- [390](https://xenitsupport.jira.com/browse/ALFREDOPS-390): Fix signing artifacts

## Release 2.1.0 (2019-11-29)
### GENERIC
- Add module.aliases to module.properties to improve the upgrade process

### ALFREDOPS
- [382](https://xenitsupport.jira.com/browse/ALFREDOPS-382): Fix Audit filtering on date and on document
- [324](https://xenitsupport.jira.com/browse/ALFREDOPS-324): Fix building without artifactory credentials
- [325](https://xenitsupport.jira.com/browse/ALFREDOPS-325): Get commons-csv from Alfresco nexus

### SPWPRI
- [31](https://xenitsupport.jira.com/browse/SPWPRI-31): Publish to Sonatype

## Release 2.0.1 (2019-10-30)
### ALFREDOPS
- [308](https://xenitsupport.jira.com/browse/ALFREDOPS-308): Finalize Jenkins run with a `composeDown` task
- [324](https://xenitsupport.jira.com/browse/ALFREDOPS-324): Update plugins and gradle wrapper
- [375](https://xenitsupport.jira.com/browse/ALFREDOPS-375): Fix early socket closure in dummy mail

## Release 2.0.0 (2019-08-20)
### ALFREDOPS
- [167](https://xenitsupport.jira.com/browse/ALFREDOPS-167): Make browser do locale-independent search
- [261](https://xenitsupport.jira.com/browse/ALFREDOPS-261): Update Gradle build
- [287](https://xenitsupport.jira.com/browse/ALFREDOPS-287): Shadow embedded dependencies to an internal package
- [288](https://xenitsupport.jira.com/browse/ALFREDOPS-288): Clean up osgi import packages
- [289](https://xenitsupport.jira.com/browse/ALFREDOPS-289): Clean up osgi export packages
- [290](https://xenitsupport.jira.com/browse/ALFREDOPS-290): C4A 2.0: Make sure dummy mail is properly tested
- [291](https://xenitsupport.jira.com/browse/ALFREDOPS-291): Care4alf can now be built on Windows
- [293](https://xenitsupport.jira.com/browse/ALFREDOPS-293): Replace deprecated Job interface with new Task interface
- [295](https://xenitsupport.jira.com/browse/ALFREDOPS-295): Remove Jenkins Archiving & Release

## Release 1.8.0 (2019-06-26)
### DSNYMETQUA
- [14](https://xenitsupport.jira.com/browse/DSNYMETQUA-14): Fix the scan breaking on database access issues, include these issues as part of the metadata integrity report; add the ability to scan a subset of the repository for testing purposes.
- [15](https://xenitsupport.jira.com/browse/DSNYMETQUA-15): Rework scan of orphaned files in the contentstore, fix false positive orphans.
- [16](https://xenitsupport.jira.com/browse/DSNYMETQUA-16): Improve UI and notification email, show summary rather than full report, add on-demand download and render of full report.

### ALFREDOPS
- [195](https://xenitsupport.jira.com/browse/ALFREDOPS-195): Fix empty emails getting added to dummy mail.

## Release 1.7.1 (2019-06-24)
### USGSUPPORT
- [698](https://xenitsupport.jira.com/browse/USGSUPPORT-698): USGSUPPORT-698 Fix permissions issue when retrieving license.users metrics

## Release 1.7.0 (2019-06-20)
### USGSUPPORT
- [698](https://xenitsupport.jira.com/browse/USGSUPPORT-698): USGSUPPORT-698 Add monitoring metrics license.users.max and license.users.authorized. Send License metrics every 10 minutes instead of once per day.

### ALFREDOPS
- [202](https://xenitsupport.jira.com/browse/ALFREDOPS-202): ALFREDOPS-202 Fix links to the Details page in the node browser for archived nodes
- [203](https://xenitsupport.jira.com/browse/ALFREDOPS-203): ALFREDOPS-203 Fix accepting full noderefs for other stores in browser

### BNPPARISBA
- [27](https://xenitsupport.jira.com/browse/BNPPARISBA-27): BNPPARISBA-27 Improvements to UI of audit module

## Release 1.6.2 (2019-05-02)
### ALFREDOPS
- [194](https://xenitsupport.jira.com/browse/ALFREDOPS-194): Fix compatibility issue with Dynamic Extensions 2.0. Care4Alf now no longer assumes Kotlin libraries will be provided.
- [195](https://xenitsupport.jira.com/browse/ALFREDOPS-195): Fix Dummy Mail using unsafe angular binding
- [174](https://xenitsupport.jira.com/browse/ALFREDOPS-174): Fix unclear success/error reporting when changing/adding a property in the nodebrowser. Additionally, fix a bug where it was possible to get the alfresco in an inconsistent state which prevented the nodebrowser from working (and therefore from repairing the state).

### USGSUPPORT
- [699](https://xenitsupport.jira.com/browse/USGSUPPORT-699): Fix issue with browser and export returning duplicates due to Paging/Sorting bug

## Release 1.6.1 (2019-03-29)
### DSNYMETQUA
- [12](https://xenitsupport.jira.com/browse/DSNYMETQUA-12): Indicate whether an integrity scan is currently running. Also allows listing all currently running quartz jobs.
- [13](https://xenitsupport.jira.com/browse/DSNYMETQUA-13): Fix bugs related to suboptimal config parsing, improve logging

## Release 1.6.0 (2019-03-12)
### ALFREDOPS
- [129](https://xenitsupport.jira.com/browse/ALFREDOPS-129): Comply with XEP-6 Git flow
- [171](https://xenitsupport.jira.com/browse/ALFREDOPS-171): Add the ability to correctly set the value of a multivalue property

### DSNYMETQUA
- [5](https://xenitsupport.jira.com/browse/DSNYMETQUA-5), [6](https://xenitsupport.jira.com/browse/DSNYMETQUA-6), [7](https://xenitsupport.jira.com/browse/DSNYMETQUA-7), [8](https://xenitsupport.jira.com/browse/DSNYMETQUA-8): Add integrity scan scheduled job that verifies the integrity of the metadata and sends an email notification

## Release 1.5.1 (2019-01-16)
### ALFREDOPS
- [165](https://xenitsupport.jira.com/browse/ALFREDOPS-165): Care4Alf tries to load Solr4 bean on Alfresco 4.2 that supports only Solr1

## Release 1.5.0 (2018-12-13)
### ALFREDOPS
- [152](https://xenitsupport.jira.com/browse/ALFREDOPS-152): Nodebrowser improvement: You can now choose the storeref to search in, as well as the transactional consistency of the query
- [148](https://xenitsupport.jira.com/browse/ALFREDOPS-148): SQL module now gives feedback on error
- [149](https://xenitsupport.jira.com/browse/ALFREDOPS-149): Fix SQL module breaking on newlines in query
- [162](https://xenitsupport.jira.com/browse/ALFREDOPS-162): Fix repeated pinging to SOLR
- [163](https://xenitsupport.jira.com/browse/ALFREDOPS-163): Fix incorrect UI update when deleting associations

## Release 1.4.0 (2018-09-21)
### ALFREDOPS
- [138](https://xenitsupport.jira.com/browse/ALFREDOPS-138): CSV escaping issues in c4a export
- [142](https://xenitsupport.jira.com/browse/ALFREDOPS-142): UI updates: Nodebrowser and SQL clarity
- [139](https://xenitsupport.jira.com/browse/ALFREDOPS-139): Fix Build
- [153](https://xenitsupport.jira.com/browse/ALFREDOPS-153): Care4alf: Underscores don't render in Chrome

### USGNLEXT
- [12](https://xenitsupport.jira.com/browse/USGNLEXT-12): Add DeleteAspectWorker to bulk
