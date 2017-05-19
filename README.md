# Care4Alf

## Build

Using Gradle, run gradle installBundle to deploy on your local Alfresco. (requires Dynamic Extensions 1.1+)

Your JS controller file reference can be added to the main `care4alf.js` file.

Note that node, tsc, lessc are required for development.

sudo npm install -g typescript
sudo npm install -g less

For Windows( make sure both npm and node are in PATH of environment variables):

npm install -g typescript
npm install -g less

./gradlew installBundle -Pprotocol=http -Phost=localhost -Pport=8080 -Pusername=admin -Ppassword=admin

## Extend

Add new module webscripts to the `xenit.care4alf.module` package. Make sure to annotate with the `care4alf` family.
Provide html (named <classname>.html) in the resources/eu/xenit/care4alf/web/partials directory.

## Monitoring

There are 2 ways to integrate monitoring:

1) Collect metrics using json endpoint alfresco/s/xenit/care4alf/monitoring/vars

2) Configure c4a to write to a graphite endpoint, see Graphite integration


### Graphite integration

Enable writing monitoring metrics to Graphite endpoint:

    c4a.monitoring.graphite.enabled=true
    c4a.monitoring.graphite.prefix=c4a.<HOSTNAME>
    c4a.monitoring.graphite.host=<CARBON HOST|IP>
    c4a.monitoring.graphite.port=2003
    
Optionally you can enable/disable specific metrics using:

    c4a.monitoring.metric.<metric>.enabled=true
    c4a.monitoring.metric.documenttypes.enabled=true
    c4a.monitoring.metric.repository.enabled=false
    c4a.monitoring.metric.residualproperties.enabled=false
    