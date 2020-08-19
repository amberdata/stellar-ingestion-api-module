package io.amberdata.inbound.stellar.client;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V> extends LinkedHashMap<K, V> {
  private int capacity;

  public LruCache(int capacity) {
    this.capacity = capacity;
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return this.size() > this.capacity;
  }
}