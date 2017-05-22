package eu.xenit.care4alf.monitoring.metric;

import com.google.common.cache.CacheStats;
import com.hazelcast.monitor.LocalMapStats;
import com.google.common.cache.Cache;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import org.alfresco.enterprise.repo.cluster.cache.HazelcastSimpleCache;
import org.alfresco.enterprise.repo.cluster.cache.InvalidatingCache;
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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by yregaieg on 19.05.17.
 */
@Component
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
                logger.error("An error has occured when trying to fetch cache metrics: ", e);
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
        }else if("org.alfresco.enterprise.repo.cluster.cache.InvalidatingCache".equals(cacheType)){
            metrics.putAll(getInvalidatingCacheStats(cacheName, cache));
            metrics.put(buildKey(cacheName, "type"), 2L);
        }else if("org.alfresco.enterprise.repo.cluster.cache.HazelcastSimpleCache".equals(cacheType)){
            // These metrics can be a bit less reliable than former cache stats
            metrics.putAll(getHazelcastSimpleCacheStats(cacheName, cache));
            metrics.put(buildKey(cacheName, "type"), 3L);
        }else{
            logger.debug("Ignoring cache " + cacheName + " of type " + cacheType);
            metrics.put(buildKey(cacheName, "type"), -1L);
        }
        return metrics;
    }

    private Map<String, Long> getHazelcastSimpleCacheStats(String cacheName, SimpleCache simpleCache) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final HazelcastSimpleCache cache = (HazelcastSimpleCache) simpleCache;
        final Field mapField = cache.getClass().getDeclaredField("map");
        mapField.setAccessible(true);
        final Object map = mapField.get(cache);
        final Method mapStatMethod = map.getClass().getMethod("getLocalMapStats");
        final LocalMapStats stats = (LocalMapStats) mapStatMethod.invoke(map);
        Map<String, Long> metrics = new HashMap<>();

        metrics.put(buildKey(cacheName, "nbGets"), Long.valueOf(stats.getOperationStats().getNumberOfGets()));
        metrics.put(buildKey(cacheName, "nbPuts"), Long.valueOf(stats.getOperationStats().getNumberOfPuts()));
        metrics.put(buildKey(cacheName, "nbHits"), Long.valueOf(stats.getHits()));
        metrics.put(buildKey(cacheName, "nbMiss"), -1L);
        metrics.put(buildKey(cacheName, "nbEvictions"), Long.valueOf(stats.getOperationStats().getNumberOfRemoves()));

        return metrics;
    }

    private Map<String, Long> getInvalidatingCacheStats(String cacheName, SimpleCache simpleCache) throws NoSuchFieldException, IllegalAccessException {
        final InvalidatingCache cache = (InvalidatingCache) simpleCache;
        final Field cacheField = cache.getClass().getDeclaredField("cache");
        cacheField.setAccessible(true);
        DefaultSimpleCache realCache = (DefaultSimpleCache) cacheField.get(cache);

        return getDefaultSimpleCacheStats(cacheName, realCache);
    }

    private Map<String, Long> getDefaultSimpleCacheStats(String cacheName, DefaultSimpleCache cache) throws NoSuchFieldException, IllegalAccessException {
        Map<String, Long> metrics = new HashMap<>();

        final Field cacheField = cache.getClass().getDeclaredField("cache");
        cacheField.setAccessible(true);
        final Object realCacheObject = cacheField.get(cache);
        if ("com.google.common.cache.LocalCache$LocalManualCache".equals(realCacheObject.getClass().getName())){
            return metrics;
        }
        final Cache realCache = (Cache) realCacheObject;
        final CacheStats stats = realCache.stats();

        metrics.put(buildKey(cacheName, "nbGets"), Long.valueOf(stats.requestCount()));
        metrics.put(buildKey(cacheName, "nbPuts"), Long.valueOf(stats.loadCount()));
        metrics.put(buildKey(cacheName, "nbHits"), Long.valueOf(stats.hitCount()));
        metrics.put(buildKey(cacheName, "nbMiss"), Long.valueOf(stats.missCount()));
        metrics.put(buildKey(cacheName, "nbEvictions"), Long.valueOf(stats.evictionCount()));

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
