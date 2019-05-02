# Changelog

## Release 1.6.3 (Unreleased)

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
