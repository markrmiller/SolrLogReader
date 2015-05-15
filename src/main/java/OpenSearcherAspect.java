
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.MinMaxPriorityQueue;

// TODO: not finished
public class OpenSearcherAspect extends Aspect {
  private static final int NUM_SLOWEST_LOAD_TIMES = 5;
  public static Pattern INIT_SEARCHER = Pattern.compile(".*?SolrIndexSearcher <init>.*");
  public static Pattern OPEN_SEARCHER_ID = Pattern.compile(".*?Opening Searcher@(\\S+?)(?:\\[.*?\\])? (realtime|main).*?");
  public static Pattern REGISTER_SEARCHER_ID = Pattern.compile(".*?Registered new searcher Searcher@(\\S+?)(?:\\[.*?\\])? .*?");
  
  private Map<String,OpenSearcherEvent> mainSearchers = Collections.synchronizedMap(new HashMap<String,OpenSearcherEvent>());
  private Map<String,OpenSearcherEvent> realtimeSearchers = Collections.synchronizedMap(new HashMap<String,OpenSearcherEvent>());
  private Map<String,RegisterSearcherEvent> registerSearchers = Collections.synchronizedMap(new HashMap<String,RegisterSearcherEvent>());
  private AtomicLong mainOpens = new AtomicLong(0);
  private AtomicLong realtimeOpens = new AtomicLong(0);
  private AtomicLong registers = new AtomicLong(0);
  
  private MinMaxPriorityQueue<Long> loadTimes;
  
  public OpenSearcherAspect() {
    loadTimes = MinMaxPriorityQueue.orderedBy(new Comparator<Long>() {

      @Override
      public int compare(Long o1, Long o2) {
        return -o1.compareTo(o2);
      }}).maximumSize(NUM_SLOWEST_LOAD_TIMES)
        .create();
  }
  
  @Override
  public boolean process(String filename, String timestamp, Date dateTs, String headLine, String entry) {
    // start tracking an opening searcher
    Matcher m = OPEN_SEARCHER_ID.matcher(headLine);
    if (m.matches()) {
      String id = m.group(1);
      String type = m.group(2);
      
      OpenSearcherEvent ose = new OpenSearcherEvent();
      ose.id = id;
      ose.ts = dateTs;
      
      if (type.equals("realtime")) {
        realtimeOpens.incrementAndGet();
        realtimeSearchers.put(id, ose);
      } else {
        mainOpens.incrementAndGet();
        mainSearchers.put(id, ose);
      }
      // out.println("add id " + id);
      return false;
    } else {
       // out.println("did not match " + headLine);
    }
    
    // out.println("found reg searcher");
    m = REGISTER_SEARCHER_ID.matcher(headLine);
    if (m.matches()) {
      String id = m.group(1);
      RegisterSearcherEvent rse = new RegisterSearcherEvent();
      rse.id = id;
      rse.ts = dateTs;
      registerSearchers.put(id, rse);
      registers.incrementAndGet();
      return false;
    }
    
    return false;
  }

  @Override
  public void printReport(PrintStream out) {
    long loadCnt = 0;
    long loadTotal = 0;
    Set<Entry<String,OpenSearcherEvent>> entries = mainSearchers.entrySet();
    synchronized (mainSearchers) {
      for (Entry<String,OpenSearcherEvent> entry : entries) {
        RegisterSearcherEvent rse = registerSearchers.get(entry.getKey());
        if (rse != null) {
          Date d1 = entry.getValue().ts;
          Date d2 = rse.ts;
          if (rse != null && d1 != null && d2 != null) {
            long diff = d2.getTime() - d1.getTime();
            loadTimes.add(diff);
            loadCnt++;
            loadTotal += diff;
          }
        } else {
          // System.err.println("Could not find searcher main search being
          // registered: " + entry.getKey());
        }
      }
    }
    
    out.println("Searcher Report");
    out.println("-----------------");
    out.println("SolrIndexSearcher main open events: " + mainOpens.get());
    out.println("SolrIndexSearcher realtime open events: " + realtimeOpens.get());
    out.println("SolrIndexSearcher register events: " + registers.get());

    if (loadCnt > 0) {
      out.println("Avg Searcher Load Time: " + new DecimalFormat("##.#").format(loadTotal / (double) loadCnt / 1000) + " seconds");
      out.println(NUM_SLOWEST_LOAD_TIMES + " Slowest Load Times:");
      Long l;
      while ((l = loadTimes.poll()) != null) {
        out.println(new DecimalFormat("##.#").format((double)l / 1000.0) + " seconds");
      }
    }
    mainSearchers.keySet().removeAll(registerSearchers.keySet());
    if (mainSearchers.size() > 0) {
      out.println("Found " + mainSearchers.size() + " searchers that were not registered: " + mainSearchers);
    }
    
  }
  
  static class OpenSearcherEvent {
    String id;
    Date ts;
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      OpenSearcherEvent other = (OpenSearcherEvent) obj;
      if (id == null) {
        if (other.id != null) return false;
      } else if (!id.equals(other.id)) return false;
      return true;
    }
  }
  
  static class RegisterSearcherEvent {
    String id;
    Date ts;
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      RegisterSearcherEvent other = (RegisterSearcherEvent) obj;
      if (id == null) {
        if (other.id != null) return false;
      } else if (!id.equals(other.id)) return false;
      return true;
    }
  }
  
}
