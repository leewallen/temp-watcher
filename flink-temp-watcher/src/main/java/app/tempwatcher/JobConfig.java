/** Provides a wrapper around the configuration for the job. */
package app.tempwatcher;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Properties;

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
    final String flinkEnv = "FLINK_ENV";

    final var env = System.getenv(flinkEnv) == null ? "local" : System.getenv(flinkEnv);

    final Config envConfig = ConfigFactory.load("application." + env + ".conf");

    return new JobConfig(appConfig.withFallback(envConfig));
  }

  public String brokers() {
    final var endpoints = config.getString("kafka.endpoints");
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
