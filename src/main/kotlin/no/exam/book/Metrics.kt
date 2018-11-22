package no.exam.book

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
                .build(graphite)
        reporter.start(1, TimeUnit.SECONDS)
        return reporter
    }
}