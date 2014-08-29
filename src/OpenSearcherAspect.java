
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: not finished
public class OpenSearcherAspect extends Aspect {
  public static Pattern INIT_SEARCHER = Pattern.compile(".*?SolrIndexSearcher <init>.*");
  public static Pattern OPEN_SEARCHER_ID = Pattern.compile(".*?Opening Searcher@(\\S+?)(?:\\[.*?\\])? (realtime|main).*?");
  public static Pattern REGISTER_SEARCHER_ID = Pattern.compile(".*?Registered new searcher Searcher@(\\S+?)(?:\\[.*?\\])? .*?");
  
  private Set<String> mainSearchers = Collections.synchronizedSet(new HashSet<String>());
  private Set<String> realtimeSearchers = Collections.synchronizedSet(new HashSet<String>());
  private Set<String> registerSearchers = Collections.synchronizedSet(new HashSet<String>());
  private AtomicLong mainOpens = new AtomicLong(0);
  private AtomicLong realtimeOpens = new AtomicLong(0);
  private AtomicLong registers = new AtomicLong(0);
  
  @Override
  public boolean process(String timestamp, String headLine, String entry) {
    // start tracking an opening searcher
    Matcher m = OPEN_SEARCHER_ID.matcher(headLine);
    if (m.matches()) {
      String id = m.group(1);
      String type = m.group(2);
      if (type.equals("realtime")) {
        realtimeOpens.incrementAndGet();
        realtimeSearchers.add(id);
      } else {
        mainOpens.incrementAndGet();
        mainSearchers.add(id);
      }
      // System.out.println("add id " + id);
      return true;
    } else {
       // System.out.println("did not match " + headLine);
    }
    
    // System.out.println("found reg searcher");
    m = REGISTER_SEARCHER_ID.matcher(headLine);
    if (m.matches()) {
      String id = m.group(1);
      registerSearchers.add(id);
      registers.incrementAndGet();
      return true;
    }
    
    return false;
  }

  @Override
  public void printReport() {
    System.out.println("Searcher Report");
    System.out.println("-----------------");
    System.out.println("SolrIndexSearcher main open events: " + mainOpens.get());
    System.out.println("SolrIndexSearcher realtime open events: " + realtimeOpens.get());
    System.out.println("SolrIndexSearcher register events: " + registers.get());
    mainSearchers.removeAll(registerSearchers);
    if (mainSearchers.size() > 0) {
      System.out.println("Found " + mainSearchers.size() + " searchers that were not registered: " + mainSearchers);
    }
    
  }
  
}
