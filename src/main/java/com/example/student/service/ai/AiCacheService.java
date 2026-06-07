package com.example.student.service.ai;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCacheService {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public <T> T getFromCache(String cacheName, Object key, Class<T> clazz) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                T value = clazz.cast(wrapper.get());
                if (value != null) {
                    log.info("AI_CACHE | event=HIT | feature={} | key={}", cacheName, key);
                    meterRegistry.counter("claude.api.cache.hits", "feature", cacheName).increment();
                    return value;
                }
            }
        }
        log.info("AI_CACHE | event=MISS | feature={} | key={}", cacheName, key);
        meterRegistry.counter("claude.api.cache.misses", "feature", cacheName).increment();
        return null;
    }

    public void putInCache(String cacheName, Object key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    public void evictStudyPlan(Long studentId) {
        Cache cache = cacheManager.getCache("studyPlans");
        if (cache != null) {
            cache.evict(studentId);
            log.info("AI_CACHE | event=EVICT | feature=studyPlans | key={}", studentId);
        }
    }

    public void evictAnalytics() {
        Cache cache = cacheManager.getCache("analytics");
        if (cache != null) {
            cache.evict("global");
            log.info("AI_CACHE | event=EVICT | feature=analytics | key=global");
        }
    }

    public void evictAll() {
        Cache studyPlansCache = cacheManager.getCache("studyPlans");
        if (studyPlansCache != null) {
            studyPlansCache.clear();
        }
        Cache analyticsCache = cacheManager.getCache("analytics");
        if (analyticsCache != null) {
            analyticsCache.clear();
        }
        log.info("AI_CACHE | event=EVICT_ALL | message=Cleared all AI caches");
    }
}
