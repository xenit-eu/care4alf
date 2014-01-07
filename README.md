# Care4Alf

Using Gradle, run gradle install to deploy on your local Alfresco. (requires Dynamic Extensions M6)

Add new module webscripts to the `xenit.care4alf.module` package. Make sure to annotate with the `care4alf` family.
Provide html (named <classname>.html) in the partials directory.

Your JS controller can be added to the main `care4alf.js` file.