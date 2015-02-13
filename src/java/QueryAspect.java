
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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.MinMaxPriorityQueue;

public class QueryAspect extends Aspect {
  private static final int NUM_SLOWEST_QUERIES = 20;

  public static Pattern QUERY = Pattern.compile(
      "^.*?[\\&\\{]q\\=(.*?)(?:&|}).*?hits\\=(\\d+).*?QTime\\=(\\d+).*$", Pattern.DOTALL);
  
  private MinMaxPriorityQueue<Query> queryQueue;
  
  public static class Query implements Comparable<Query> {
    String timestamp;
    String query;
    Integer qtime;
    Integer results;
    public String headLine;
    
    @Override
    public int compareTo(Query o) {
      return o.qtime.compareTo(this.qtime);
    }
    
    @Override
    public String toString() {
      return "Query [timestamp=" + timestamp + ", query=" + query + ", qtime="
          + qtime + ", results=" + results + "]";
    }
    
  }
  
  public QueryAspect() {
    queryQueue = MinMaxPriorityQueue.maximumSize(NUM_SLOWEST_QUERIES)
        .create();
  }
  
  @Override
  public boolean process(String filename, String timestamp, Date dateTs, String headLine, String entry) {
    Matcher m = QUERY.matcher(headLine);
    if (m.matches()) {
      String query = m.group(1);
      Integer results = Integer.parseInt(m.group(2));
      Integer qtime = Integer.parseInt(m.group(3));
      // System.out.println("add match");
      Query q = new Query();
      q.timestamp = timestamp;
      q.query = query;
      q.results = results;
      q.qtime = qtime;
      q.headLine = headLine;
  
      synchronized (queryQueue) {
        queryQueue.add(q);
      }
      return false;
     
    } 
    return false;
  }
  
  @Override
  public void printReport() {
    System.out.println("Query Report");
    System.out.println("-----------------");
    
    System.out.println(NUM_SLOWEST_QUERIES + " slowest queries:");
    Query q;
    while ((q = queryQueue.poll()) != null) {
      System.out.println(q);
      System.out.println("     " + q.headLine);
    }
  }
  
}
