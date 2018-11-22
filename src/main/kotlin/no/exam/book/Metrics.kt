package no.exam.book

import com.codahale.metrics.MetricAttribute
import com.codahale.metrics.MetricFilter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

@Configuration
class Metrics {

    @Bean
    fun getRegistry(): MetricRegistry {
        return MetricRegistry()
    }

    @Bean
    fun getReporter(registry: MetricRegistry): GraphiteReporter {
        val host = if (System.getenv("GRAPHITE_HOST") != null) System.getenv("GRAPHITE_HOST") else ""
        val key = if (System.getenv("HOSTEDGRAPHITE_APIKEY") != null) System.getenv("HOSTEDGRAPHITE_APIKEY") else ""

        val graphite = Graphite(InetSocketAddress(host, 2003))
        val reporter = GraphiteReporter.forRegistry(registry)
                .prefixedWith(key)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .disabledMetricAttributes(getDisabledMetrics())
                .build(graphite)
        reporter.start(1, TimeUnit.SECONDS)
        return reporter
    }

    fun getDisabledMetrics(): Set<MetricAttribute> {
        return setOf(
//                MetricAttribute.COUNT,
//                MetricAttribute.MAX,
//                MetricAttribute.MIN,
//                MetricAttribute.MEAN,
                MetricAttribute.M1_RATE,
                MetricAttribute.M15_RATE,
                MetricAttribute.M5_RATE,
                MetricAttribute.MEAN_RATE,
                MetricAttribute.P50,
                MetricAttribute.P75,
                MetricAttribute.P95,
                MetricAttribute.P98,
                MetricAttribute.P99,
                MetricAttribute.P999,
                MetricAttribute.STDDEV
        )
    }
}