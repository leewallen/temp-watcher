/**
 * Provides a wrapper around the configuration for the job.
 */
package app.tempwatcher;

import java.util.Properties;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


final class JobConfig {
    private final Config config;

    private JobConfig(final Config config) {
        this.config = config;
        // This verifies that the Config is sane and has our
        // reference config. Importantly, we specify the "simple-lib"
        // path so we only validate settings that belong to this
        // library. Otherwise, we might throw mistaken errors about
        // settings we know nothing about.
        config.checkValid(ConfigFactory.defaultReference());
    }

    public static JobConfig create() {
        final Config appConfig = ConfigFactory.load();

        var env = System.getenv("FLINK_ENV") == null ? "local" : System.getenv("FLINK_ENV");

        final Config envConfig = ConfigFactory.load("application." + env + ".conf");

        return new JobConfig(appConfig.withFallback(envConfig));
    }

    public String brokers() {
        var endpoints = config.getString("kafka.endpoints");
        return endpoints;
    }

    public String schemaRegistryUrl() {
        return config.getString("schemaRegistry.url");
    }

    public Properties consumer() {
        final Properties props = new Properties();

        props.setProperty("group.id", config.getString("kafka.consumer.groupId"));

        return props;
    }

    public Properties producer() {
        final Properties props = new Properties();

        props.setProperty("transaction.timeout.ms", config.getString("kafka.producer.transTimeout"));

        return props;
    }
}
