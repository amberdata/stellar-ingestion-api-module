package io.amberdata.inbound.stellar.client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metrics {
  private static final Logger LOG = LoggerFactory.getLogger(Metrics.class);
  private static final Map<String, Double> metrics = new TreeMap<>();
  private static final Map<String, LinkedList<Double>> samples = new HashMap<>();
  private static long lastPrint = System.currentTimeMillis();

  private static final long PRINT_DELAY_MILLIS = 30 * 1000;
  private static final int MAX_SAMPLE_COUNT = 16;

  /**
   * Adds the value to the metric identified by the metric key.
   *
   * @param metric The metric key.
   * @param value  The value to add to it (or initially set to if non-existent)
   */
  public static void count(String metric, double value) {
    final double newValue = metrics.getOrDefault(metric, 0.) + 1;
    metrics.put(metric, newValue);
    maybeLog();
  }

  /**
   * Sets the value of the metric identified by the metric key.
   *
   * @param metric The metric key.
   * @param value  The value to set that metric to.
   */
  public static void gauge(String metric, double value) {
    metrics.put(metric, value);
    maybeLog();
  }

  /**
   * Adds the value into a running list of samples for that metric key,
   * sets the metric to the mean over all samples.
   *
   * @param metric The metric key.
   * @param value  The value to add as a sample for mean calculation.
   */
  public static void mean(String metric, double value) {
    LinkedList<Double> metricSamples;

    if (samples.containsKey(metric)) {
      metricSamples = samples.get(metric);
    } else {
      metricSamples = new LinkedList<>();
    }

    // Keep a lookback of only MAX_SAMPLE_COUNT samples.
    metricSamples.add(value);
    if (metricSamples.size() > MAX_SAMPLE_COUNT) {
      metricSamples.removeFirst();
    }
    samples.put(metric, metricSamples);

    // Calculate mean, min, max over all samples.
    double sum = 0;
    Double max = null;
    Double min = null;

    for (double i : metricSamples) {
      sum += i;
      if (max == null || i > max) {
        max = i;
      }
      if (min == null || i < min) {
        min = i;
      }
    }

    gauge(metric + ".mean", sum / metricSamples.size());
    gauge(metric + ".min", min);
    gauge(metric + ".max", max);
  }

  private static void maybeLog() {
    final long now = System.currentTimeMillis();
    if (now - lastPrint > PRINT_DELAY_MILLIS) {
      LOG.info("[METRICS] " + metrics);
      lastPrint = now;
    }
  }
}