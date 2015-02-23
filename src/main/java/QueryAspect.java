
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.MinMaxPriorityQueue;

public class QueryAspect extends Aspect {
  private static final int NUM_SLOWEST_QUERIES = 20;

  public static Pattern QUERY = Pattern.compile(
      "^.*?[\\&\\{]q\\=(.*?)(?:&|}).*?hits\\=(\\d+).*?QTime\\=(\\d+).*$", Pattern.DOTALL);
  
  private final MinMaxPriorityQueue<Query> queryQueue;
  
  private AtomicInteger queryCount = new AtomicInteger();
  
  private Date oldestDate;
  
  private Date latestDate;

  private PrintWriter out;
  
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
  
  public QueryAspect(String outputDir) {
    queryQueue = MinMaxPriorityQueue.maximumSize(NUM_SLOWEST_QUERIES).create();
    if (outputDir != null) {
      try {
        out  = new PrintWriter(new BufferedWriter(new FileWriter(outputDir + File.separator + "query_report.txt"), 2^20));
        StringBuilder sb = new StringBuilder();
        sb.append("Query Report" + "\n");
        sb.append("-----------------" + "\n");
        out.write(sb.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
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
        trackOldestLatestTimestamp(dateTs);
        
        queryQueue.add(q);
        queryCount.incrementAndGet();
        if (out != null) {
          out.write(q.toString() + "\n");
          out.write("     " + q.headLine + "\n");
        }
      }
      return false;
     
    } 
    return false;
  }

  private void trackOldestLatestTimestamp(Date dateTs) {
    if (oldestDate == null) {
      oldestDate = dateTs;
    } else if (dateTs != null && dateTs.before(oldestDate)) {
      oldestDate = dateTs;
    }
    
    if (latestDate == null) {
      latestDate = dateTs;
    } else if (dateTs != null && dateTs.after(latestDate)) {
      latestDate = dateTs;
    }
  }
  
  @Override
  public void printReport() {
    System.out.println("Query Report");
    System.out.println("-----------------");
    System.out.println();
    if (oldestDate != null && latestDate != null) {
      long diff = latestDate.getTime() - oldestDate.getTime();
      long seconds = TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS);
      
      float qps = queryCount.get() / (float) seconds;
      System.out.println("Approx QPS:" + qps + " (careful - across all logs and nodes)");
    }
    System.out.println();
    System.out.println(NUM_SLOWEST_QUERIES + " slowest queries:");
    Query q;
    while ((q = queryQueue.poll()) != null) {
      System.out.println(q);
      System.out.println("     " + q.headLine);
    }
  }
  
  @Override
  public void close() {
    if (out != null) out.close();
  }
  
}
