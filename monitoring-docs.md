## Care4Alf Monitoring configuration

There are metrics and shippers. The metrics are the objects that are activated on a cron schedule and decide what properties are monitored, whereas the shippers collect this information and send it off to a third-party monitoring system such as Graphite.

Each monitored source is linked to a particular shipper, which is done by the `shipperName` property of the metric object. Both metrics and shippers can be enabled and disabled individually in `alfresco-global.properties`, through the `c4a.monitoring.…` options. For instance, `c4a.monitoring.graphite.enabled` for the Graphite shipper, and `c4a.monitoring.activesessions.enabled`  for the ActiveSessionsMetric class.

If a *metric* is not mentioned explicitly in `alfresco-global.properties`, it defaults to **enabled**. If a *shipper* is not mentioned explicitly, it defaults to **disabled**.

Additionally, monitoring on the whole can be enabled or disabled, through the `c4a.monitoring.enabled` setting. The exact behaviour of this is due to change in the next release of care4alf. In 1.2.0, explicitly setting this to `true` or `false` will set all shippers and metrics to that value. In other words, it will override all granular control. In later versions, however, it will work the opposite way: if `c4a.monitoring.enabled` is set explicitly, it will function as a fallback for all properties that have not been set explicitly.

As an example of the above rules, we will look at the resolution chain (according to the later versions) of the AbstractMonitoredSource class, linked to the Graphite shipper:

1. Check if Graphite is enabled:

   1. Check if the `c4a.monitoring.graphite.enabled` property exists

      - If it exists and is `true`, Graphite is enabled
      - If it exists and is `false`, Graphite is disabled

   2. If it does not exist, check if the `c4a.monitoring.enabled` property exists

      - If it exists and is `true`, Graphite is enabled
      - If it exists and is `false`, Graphite is disabled

   3. If it does not exist, Graphite is disabled
2. Check if ActiveSessionsMetric is enabled:

   1. Check if the `c4a.monitoring.activesessions.enabled` property exists

      - If it exists and is `true`, ActiveSessionsMetric is enabled
      - If it exists and is `false`, ActiveSessionsMetric is disabled

   2. If it does not exist, check if the `c4a.monitoring.enabled` property exists

      - If it exists and is `true`, ActiveSessionsMetric is enabled
      - If it exists and is `false`, ActiveSessionsMetric is disabled

   3. If it does not exist, ActiveSessionsMetric is enabled
3. If both Graphite and ActiveSessionsMetric are enabled, the active sessions will be monitored.

### Metrics

Below is a list of all the metrics in Care4Alf at the time of writing. This document contains the cron expression and key for each of them. The key is the name of the metric on the [care4alf/#/monitoring](http://localhost:8080/alfresco/s/xenit/care4alf/#/monitoring) page. The cron expression determines how often each metric runs (note that they use the [Quartz syntax](http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger), which contains a seconds field, as opposed to the Unix syntax).

In addition to the keys mentioned below, the monitoring page also contains a number of `metric.<metricname>.timing` keys, which display the performance of each metric.

####ActiveSessionsMetric

**Cron:** 0 0/1 * * * ? **Key:** `users.tickets`

**Description:** Counts the number of active users. That is, the number of users with active non-expired tickets.

#### CachesMetrics

**Cron:** 0 0/10 * * * ? **Key:** `cache.<cacheName>.<property>`

**Description**: Enumerates all the different caches and provides information and statistics about each. This information includes the size, type, and maximum number of items. If statistics are enabled, it also counts the numbers of gets, puts, hits, misses and evictions.

#### ClusteringMetric

**Cron:** 0/15 * * * * ? **Key:** `cluster.nodes`

**Description**: Counts the number of nodes in the current cluster. Will be 0 if not in a clustered environment.

#### ContentMetric

**Cron:** * 0/5 * * * ? **Key:** `operations.read`

**Description:** Also named OperationsMetric in certain parts of the code. Measures how long a read operation takes, the value of `operations.read` is the length of time of a read operation in milliseconds.

#### DbMetrics

**Cron:** * 0/5 * * * ? **Key:** `db.connectionpool.…`, `db.healthy`, `db.ping`

**Description:** Retrieve information on the database connections, as well as whether the database repository descriptor can be obtained (`db.healthy`) and what the latency to the database is in milliseconds (`db.ping`). The information about the connections is the following:

| db.connectionpool property | description                              |
| -------------------------- | ---------------------------------------- |
| max                        | Max number of database connections that can be active at the same time |
| active                     | Number of database connections active at this time |
| initial                    | Number of database connections created on startup |
| idle.min                   | Minimum number of idle database connections |
| idle.max                   | Maximum number of database connections that can remain idle |
| wait.max                   | In milliseconds, how long the pool will wait for a connection to be returned |

#### DocumentTypesMetrics

**Cron:** 0 0 0/1 * * ? **Key:** `documenttypes.<prefix>.<doctype>`

**Description:**  Counts the number of NodeRefs for each document type.

#### GCMonitoring

**Cron:** 0 0/5 * * * ? **Key:** `jvm.gc.…`

**Description:**  Get information on the JVM garbage collector. For each of the GC beans, this metric lists the number of collections that have occurred, as well as how long these collections took (in both seconds and milliseconds).

#### LDAPSyncMetric

**Cron:** 0 0 0/2 * * ? **Key:** `sync.ldap.status`

**Description:**  Provides the status of the LDAP synchronisation status. The status is displayed as a number between -1 and 3, with the following meanings:

- 1: WAITING: A sync has been requested but has not yet started
- 2: IN_PROGRESS: A sync is in progress
- 3: COMPLETE: A sync is finished with no errors
- -1: COMPLETE_ERROR: A sync finished with at least 1 error

#### LicenseMetric

**Cron:** 0 0 2 * * ? **Key:** `license.valid`

**Description:**  Retrieves license information. If the license is still valid, `license.valid` displays the number of remaining days on the license. If the license is expired or absent, it will display a negative number.

#### MemoryMetric

**Cron:** 0 0/5 * * * ? **Key:** `jvm.memory.heap.…`, `jvm.memory.nonheap.…`, `jvm.memory.…`

**Description:**  Measures the JVM's memory usage, in unit of bytes. For the *heap* and *nonheap*, it retrieves the following four values:

- **init:** Amount of memory that the JVM initially requested from the OS;
- **used:** Amount of memory used;
- **committed:** Amount of memory that is committed for the JVM to use;
- **max:** Maximum amount of memory that can be used for memory management.

Additionally, it also displays the *used* and *max* measurements for any other memory pools that it finds, such as *eden*, *old* and *survivor*.

#### RepositoryMetric

**Cron:** 0 0 1 * * ? **Key:** `repository.…`

**Description:**  Counts the number of items in several categories of the repository.

| repository.\<property\> | description                              |
| ----------------------- | ---------------------------------------- |
| workspace.nodes         | Number of nodes in `workspace://SpacesStore` |
| archive.nodes           | Number of nodes in `archive://SpacesStore` |
| version.nodes           | Number of nodes in `workspace://version2Store` |
| transactions            | Number of transactions                   |
| acl                     | Number of Access Control Lists           |
| acltransactions         | Number of ACL transactions               |

#### ResidualPropertiesMetric

**Cron:** 0 0 1 * * ? **Key:** `properties.residual`

**Description:**  Counts the number of residual properties.

This excludes the default system residual properties (i.e. the ones in the `{http://www.alfresco.org/…}` namespace).

#### SolrSummaryMetrics

**Cron:** 0 0/5 * * * ? **Key:** `solr.…`

**Description:**  Retrieves various metrics and statistics from solr.

| solr.\<property\> | description                              |
| ----------------- | ---------------------------------------- |
| errors            | Number of errors Solr has encountered    |
| lag.nodes         | Number of NodeRefs that still need to be indexed |
| lag.time          | Transaction lag in seconds               |
| model.errors      | Number of errors due to failed model changes with an incompatible data model |

In addition to the above metrics, all the items from Solr's own summary are included as well, under the `solr.summary.…` key.

#### SystemMetrics

**Cron:** 0 0/5 * * * ? **Key:** `jvm.memory.…`, `jvm.threads.…`, `system.loadavg`, `system.processors`

**Description:** Displays information on a variety of system properties.

Concerning memory usage:

- **jvm.memory.runtime.max** is the max amount of memory that the JVM will attempt to use. This should be equal to **jvm.memory.heap.max**.
- **jvm.memory.runtime.total** is the total amount of memory in the JVM. This should be equal to **jvm.memory.heap.committed**.
- **jvm.memory.runtime.free** is the amount of free memory in the JVM.

Concerning Java threads:

- **jvm.threads.count** is the number of Java threads.
- **jvm.threads.\<state\>** is the number of threads in \<state\>.

Concerning the CPU:

- **system.loadavg** is the [system load average](https://en.wikipedia.org/wiki/Load_(computing)) for the last minute, multiplied by 100.
- **system.processors** is the number of CPU cores.

#### TestMetric

**Key:** `test`

**Description**: TestMetric has no purposes beyond testing. It sets the value for `test` to 123.