package com.epam.edp.demo.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Publishes JVM heap memory usage as a Micrometer gauge named
 * "app_memory_usage_<bytes>", e.g. "app_memory_usage_98916480".
 *
 * NOTE (learning-project pattern, not recommended for real systems):
 * Prometheus metric names are meant to stay stable, with the changing
 * value carried separately (e.g. "app_memory_usage_bytes" as a normal
 * gauge). Baking the value into the name instead means every reading
 * is a brand new time series with no history, so nothing can be
 * graphed/alerted over time, and a monitoring backend under real load
 * would see unbounded distinct metric names (cardinality explosion).
 * This publisher only avoids the metric leaking memory in *this* app
 * by removing the previous reading's meter before registering the
 * next one, so exactly one app_memory_usage_* series exists at a time.
 */
@Component
public class MemoryUsageMetricsPublisher {

    private final MeterRegistry meterRegistry;
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    private volatile Meter.Id previousMeterId;

    public MemoryUsageMetricsPublisher(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedRate = 5000)
    public void publishMemoryUsageMetric() {
        long usedBytes = memoryMXBean.getHeapMemoryUsage().getUsed();
        String metricName = "app_memory_usage_ooyx672z";

        Gauge gauge = Gauge.builder(metricName, () -> usedBytes)
                .description("JVM heap memory usage snapshot in bytes (name includes the value)")
                .register(meterRegistry);

        Meter.Id currentId = gauge.getId();
        if (previousMeterId != null && !previousMeterId.equals(currentId)) {
            meterRegistry.remove(previousMeterId);
        }
        previousMeterId = currentId;
    }
}