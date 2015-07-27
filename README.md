# Care4Alf

Using Gradle, run gradle installBundle to deploy on your local Alfresco. (requires Dynamic Extensions 1.1)

Add new module webscripts to the `xenit.care4alf.module` package. Make sure to annotate with the `care4alf` family.
Provide html (named <classname>.html) in the partials directory.

Your JS controller file reference can be added to the main `care4alf.js` file.

Note that node, tsc, lessc are required for development.

sudo npm install -g typescript
sudo npm install -g less

For Windows( make sure both npm and node are in PATH of environment variables):
npm install -g typescript
npm install -g less


./gradlew installBundle -Phost=usg-nl