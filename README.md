# Care4Alf

## Build

Run `./gradlew :c4a-impl:care4alf-5x:assemble` to build a jar for Alfresco 5.0/5.1/5.2 and
`./gradlew :c4a-impl:care4alf-6x:assemble` for Alfresco 6.0/6.1.

These jars can be deployed on Alfresco with Dynamic Extensions 2.0.1 or later.

## Test

You can run the integration tests with `./gradlew :c4a-test:test-5x:integrationTest` for Alfresco 5 and
`./gradlew :c4a-test:test-6x:integrationTest` for Alfresco 6. The containers will automatically shut down after the
tests are completed. If you want to run the integration tests several times without always needing to restart the
containers, use `DOCKER_ALF_PORT=8080 ./gradlew:c4a-test:test-5x:composeUp`, followed by
`./gradlew :c4a-test:test-5x:integrationTestLocal -Pport=8080`.

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

### Acessing the Care4Alf Console

Acess the Care4Alf console by acessing http://server:port/alfresco/s/xenit/care4alf