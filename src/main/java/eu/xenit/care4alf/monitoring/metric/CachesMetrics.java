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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by yregaieg on 19.05.17.
 */
public class CachesMetrics extends AbstractMonitoredSource implements ApplicationContextAware {
    private final Logger logger = LoggerFactory.getLogger(CachesMetrics.class);
    private ApplicationContext ctx;
    private Map<String, SimpleCache> monitoredCaches;

    @Autowired()
    @Qualifier("global-properties")
    private Properties properties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
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
        StringBuilder metricKey = new StringBuilder("cache.");
        return metricKey.append(cacheName).append(".").append(key).toString();
    }

    public Map<String, Long> getCacheProperties(String cacheName){
        Map<String, Long> cacheProps = new HashMap<>();
        cacheProps.put(buildKey(cacheName, "maxItems"), Long.valueOf(properties.getProperty(buildKey(cacheName, "maxItems"))));
        cacheProps.put(buildKey(cacheName, "tx.maxItems"), Long.valueOf(properties.getProperty(buildKey(cacheName, "tx.maxItems"))));
        cacheProps.put(buildKey(cacheName, "statsEnabled"), Long.valueOf(properties.getProperty(buildKey(cacheName, "tx.statsEnabled")).equals("true")?1:0));
        return cacheProps;
    }

    public Map<String, Long> getMonitoringMetrics(String cacheName, SimpleCache cache) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map<String, Long> metrics = new HashMap<>();
        metrics.putAll(getCacheProperties(cacheName));
        String cacheType = cache.getClass().getName();
        metrics.put(buildKey(cacheName, "size"), Long.valueOf(cache.getKeys().size()));
        if ("org.alfresco.repo.cache.DefaultSimpleCache".equals(cacheType)){
            metrics.putAll(getDefaultSimpleCacheStats(cacheName, (DefaultSimpleCache) cache));
        }else if("org.alfresco.enterprise.repo.cluster.cache.InvalidatingCache".equals(cacheType)){
            metrics.putAll(getInvalidatingCacheStats(cacheName, (InvalidatingCache) cache));
        }else if("org.alfresco.enterprise.repo.cluster.cache.HazelcastSimpleCache".equals(cacheType)){
            // These metrics can be a bit less reliable than former cache stats
            metrics.putAll(getHazelcastSimpleCacheStats(cacheName, (HazelcastSimpleCache) cache));
        }else{
            logger.debug("Ignoring cache " + cacheName + " of type " + cacheType);
        }
        return metrics;
    }

    private Map<String, Long> getHazelcastSimpleCacheStats(String cacheName, HazelcastSimpleCache cache) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
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

    private Map<String, Long> getInvalidatingCacheStats(String cacheName, InvalidatingCache cache) throws NoSuchFieldException, IllegalAccessException {
        final Field cacheField = cache.getClass().getDeclaredField("cache");
        cacheField.setAccessible(true);
        DefaultSimpleCache realCache = (DefaultSimpleCache) cacheField.get(cache);

        return getDefaultSimpleCacheStats(cacheName, realCache);
    }

    private Map<String, Long> getDefaultSimpleCacheStats(String cacheName, DefaultSimpleCache cache) throws NoSuchFieldException, IllegalAccessException {
        Map<String, Long> metrics = new HashMap<>();

        final Field cacheField = cache.getClass().getDeclaredField("cache");
        cacheField.setAccessible(true);
        Cache realCache = (Cache) cacheField.get(cache);
        final CacheStats stats = realCache.stats();

        metrics.put(buildKey(cacheName, "nbGets"), Long.valueOf(stats.requestCount()));
        metrics.put(buildKey(cacheName, "nbPuts"), Long.valueOf(stats.loadCount()));
        metrics.put(buildKey(cacheName, "nbHits"), Long.valueOf(stats.hitCount()));
        metrics.put(buildKey(cacheName, "nbMiss"), Long.valueOf(stats.missCount()));
        metrics.put(buildKey(cacheName, "nbEvictions"), Long.valueOf(stats.evictionCount()));

        return metrics;
    }

    protected void initMonitoredCaches(){
        String[] allCacheBeanNames = ctx.getBeanNamesForType(SimpleCache.class, false, false);
        for (int i=0; i < allCacheBeanNames.length; i++){
            SimpleCache cache = ctx.getBean(allCacheBeanNames[i], SimpleCache.class);
            if (!(cache instanceof TransactionalCache)){
                monitoredCaches.put(allCacheBeanNames[i], cache);
            }
        }
    }
}
