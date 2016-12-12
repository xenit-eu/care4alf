package eu.xenit.care4alf.monitoring;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by mhgam on 23/09/2016.
 * compile 'com.codahale.metrics:metrics-core:3.0.2'
 * compile 'com.codahale.metrics:metrics-graphite:3.0.2'
 * compile 'com.basistech:metrics-statsd:3.0.0'
 */
@Service
public class MonitoringService {
    private Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    private final MetricRegistry metrics;

    @Value("${monitoring.graphite.host:localhost}")
    private String graphiteHost;
    @Value("${monitoring.graphite.port:2003}")
    private int graphitePort;
    public MonitoringService() {
        metrics = new MetricRegistry();
    }
    public MonitoringService(String graphiteHost,int graphitePort) {
        this.graphiteHost = graphiteHost;
        this.graphitePort = graphitePort;
        metrics = new MetricRegistry();
    }



    @PostConstruct
    public void setupReportToGraphite() {
        logger.info("Logging to graphite at {}:{}", graphiteHost, graphitePort);
        final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
        final GraphiteReporter reporter2 = GraphiteReporter.forRegistry(metrics)
                .prefixedWith("Care4Alf Monitoring")
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
        reporter2.start(5, TimeUnit.SECONDS);
        registerMetrics();

    }
    
    private Histogram histoStoreContentLoad;

    private Histogram histoStoreNodesLoad;
    private Histogram histoOutputDocs;
    private Histogram histoInputKbs;
    private Histogram backendNumProcessing;
    private Histogram histoCapacity;
    private Histogram histoReportsReady;
    private Histogram histoReadyToUploadCount;
    private Histogram histoReporterLoad;


    private void registerMetrics()
    {
        int windowSize = 20;

//        histoStoreContentLoad = metrics.register("StoreContentLoad", new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));
//        histoStoreNodesLoad = metrics.register("StoreNodesLoad", new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));
//        histoInputKbs = metrics.register("BackendInputKbSec", new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));
//        histoOutputDocs = metrics.register("BackendOutputDocSec", new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));
//        backendNumProcessing = metrics.register("BackendNumProcessing", new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));
//        histoCapacity = metrics.register("BackendCapacity",new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));
//        histoReportsReady = metrics.register("BackendReportsReady",new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));
//
//        histoReadyToUploadCount = metrics.register("ReadyToUploadCount",new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));
//        histoReporterLoad = metrics.register("ReporterLoad",new Histogram(new SlidingTimeWindowReservoir(windowSize, TimeUnit.SECONDS)));



    }

    public Histogram getHistoReporterLoad() {
        return histoReporterLoad;
    }

    public Histogram getHistoReadyToUploadCount() {
        return histoReadyToUploadCount;
    }


    public Histogram getHistoStoreContentLoad() {
        return histoStoreContentLoad;
    }

    public Histogram getHistoStoreNodesLoad() {
        return histoStoreNodesLoad;
    }

    public Histogram getHistoOutputDocs() {
        return histoOutputDocs;
    }

    public Histogram getHistoInputKbs() {
        return histoInputKbs;
    }

    public Histogram getBackendNumProcessing() {
        return backendNumProcessing;
    }

    public Histogram getHistoCapacity() {
        return histoCapacity;
    }

    public Histogram getHistoReportsReady() {
        return histoReportsReady;
    }
}
