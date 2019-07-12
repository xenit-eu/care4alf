package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.jobs.ScheduledQuartzJob;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.alfresco.repo.cache.DefaultSimpleCache;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.cache.TransactionalCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by yregaieg on 19.05.17.
 */
@Component
@ScheduledQuartzJob(name = "CachesMetrics", group = Monitoring.SCHEDULE_GROUP, cron = "0 0/10 * * * ?", cronProp = "c4a.monitoring.caches.cron")
public class CachesMetrics extends AbstractMonitoredSource implements ApplicationContextAware {
    private final Logger logger = LoggerFactory.getLogger(CachesMetrics.class);
    private ApplicationContext ctx;
    private Map<String, SimpleCache> monitoredCaches;

    @Autowired()
    @Qualifier("global-properties")
    private Properties properties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext.getParent().getParent();
    }

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        if (monitoredCaches==null){
            initMonitoredCaches();
        }

        Map<String, Long> metrics = new HashMap<>();
        for (Map.Entry<String, SimpleCache> cacheEntry:
             monitoredCaches.entrySet()) {
            try {
                metrics.putAll(getMonitoringMetrics(cacheEntry.getKey(), cacheEntry.getValue()));
            } catch (Exception e) {
                logger.error("An error has occured when trying to fetch cache metrics for '"+cacheEntry.getKey()+"': ", e);
            }
        }
        return metrics;
    }

    private String buildKey(String cacheName, String key){
        return buildKey(cacheName, key, true);
    }

    private String buildKey(String cacheName, String key, boolean sanitizeCacheName){
        StringBuilder metricKey = new StringBuilder("cache.");
        return metricKey.append(sanitizeCacheName?cacheName.replace('.', '-'):cacheName).append(".").append(key).toString();
    }

    public Map<String, Long> getCacheProperties(String cacheName){
        Map<String, Long> cacheProps = new HashMap<>();
        String maxItems = properties.getProperty(buildKey(cacheName, "maxItems", false));
        String txMaxItems = properties.getProperty(buildKey(cacheName, "tx.maxItems", false));
        String statsEnabled = properties.getProperty(buildKey(cacheName, "tx.statsEnabled", false));
        cacheProps.put(buildKey(cacheName, "maxItems"), Long.valueOf(maxItems == null?"-1":maxItems));
        cacheProps.put(buildKey(cacheName, "tx.maxItems"), Long.valueOf(txMaxItems == null?"-1":txMaxItems));
        cacheProps.put(buildKey(cacheName, "statsEnabled"), Long.valueOf("true".equals(statsEnabled)?1:0));
        return cacheProps;
    }

    public Map<String, Long> getMonitoringMetrics(String cacheName, SimpleCache cache) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map<String, Long> metrics = new HashMap<>();
        metrics.putAll(getCacheProperties(cacheName));
        String cacheType = cache.getClass().getName();
        metrics.put(buildKey(cacheName, "size"), Long.valueOf(cache.getKeys().size()));
        if ("org.alfresco.repo.cache.DefaultSimpleCache".equals(cacheType)){
            metrics.putAll(getDefaultSimpleCacheStats(cacheName, (DefaultSimpleCache) cache));
            metrics.put(buildKey(cacheName, "type"), 1L);
        }else{
            logger.debug("Ignoring cache " + cacheName + " of type " + cacheType);
            metrics.put(buildKey(cacheName, "type"), -1L);
        }
        return metrics;
    }


    private Map<String, Long> getDefaultSimpleCacheStats(String cacheName, DefaultSimpleCache cache) throws NoSuchFieldException, IllegalAccessException {
        Map<String, Long> metrics = new HashMap<>();

        try {
            final Field cacheField = cache.getClass().getDeclaredField("cache");
            cacheField.setAccessible(true);
            final Object realCacheObject = cacheField.get(cache);
            try {
                final Cache realCache = (Cache) realCacheObject;
                final CacheStats stats = realCache.stats();

                metrics.put(buildKey(cacheName, "nbGets"), Long.valueOf(stats.requestCount()));
                metrics.put(buildKey(cacheName, "nbPuts"), Long.valueOf(stats.loadCount()));
                metrics.put(buildKey(cacheName, "nbHits"), Long.valueOf(stats.hitCount()));
                metrics.put(buildKey(cacheName, "nbMiss"), Long.valueOf(stats.missCount()));
                metrics.put(buildKey(cacheName, "nbEvictions"), Long.valueOf(stats.evictionCount()));
            } catch (ClassCastException cce) {
                logger.warn("Exception while trying to cast cache , issue might be related to guava version :", cce);
            }
        }catch (NoSuchFieldException|NoClassDefFoundError e){
            // This field got introduced in Alfresco 5.x, ignore this exception
            logger.debug("Skipping cache statistics collection: unsopported Alfresco version");
        }

        return metrics;
    }

    protected void initMonitoredCaches(){
        monitoredCaches = new HashMap<>();
        String[] allCacheBeanNames = ctx.getBeanNamesForType(SimpleCache.class, false, false);
        for (int i=0; i < allCacheBeanNames.length; i++){
            SimpleCache cache = ctx.getBean(allCacheBeanNames[i], SimpleCache.class);
            if (!(cache instanceof TransactionalCache)){
                monitoredCaches.put(allCacheBeanNames[i], cache);
            }
        }
    }
}
