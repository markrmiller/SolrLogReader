
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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.MinMaxPriorityQueue;

public class QueryAspect extends Aspect {
  private static int NUM_SLOWEST_QUERIES = 10;//default value

  public static Pattern QUERY = Pattern.compile(
      "^.*?[\\&\\{]q\\=(.*?)(?:&|}).*?hits\\=(\\d+).*?QTime\\=(\\d+).*$", Pattern.DOTALL);
  
  private MinMaxPriorityQueue<Query> queryQueue;
  
  private final AtomicInteger queryCount = new AtomicInteger();
  
  private Date oldestDate;
  
  private Date latestDate;

  private PrintWriter fullOutput;
  
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
      return "Query: " + query + "\nInfo: [timestamp=" + timestamp + ", qtime="
          + qtime + ", results=" + results + "]";
    }
    
  }

  public QueryAspect(String outputDir, int numSlowQueries) {
    NUM_SLOWEST_QUERIES = numSlowQueries;
    prepare(outputDir);
  }

  public QueryAspect(String outputDir) {
    prepare(outputDir);
  }

  private void prepare(String outputDir) {
    queryQueue = MinMaxPriorityQueue.maximumSize(NUM_SLOWEST_QUERIES).create();
    if (outputDir != null) {
      try {
        fullOutput = new PrintWriter(
                new BufferedWriter(new FileWriter(outputDir + File.separator + "query-report.txt"), 2 ^ 20));
        StringBuilder sb = new StringBuilder();
        sb.append("Query Report" + "\n");
        sb.append("-----------------" + "\n\n");
        fullOutput.write(sb.toString());
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
      // out.println("add match");
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
        if (fullOutput != null) {
          fullOutput.write(q.toString() + "\n");
          fullOutput.write("Log: " + q.headLine + "\n\n");
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
  public void printReport(PrintStream out) {
    out.println("Query Report");
    out.println("-----------------");
    out.println();
    if (oldestDate != null && latestDate != null) {
      float qps = getQPS();
      out.println("Approx QPS:" + qps);
    }
    out.println();
    out.println(NUM_SLOWEST_QUERIES + " slowest queries:");
    out.println();
    Query q;
    
    synchronized (queryQueue) {
      while ((q = queryQueue.poll()) != null) {
        out.println(q);
        out.println("Log: " + q.headLine);
        out.println();
      }
    }
  }

  private float getQPS() {
    if (latestDate == null || oldestDate == null) {
      return -1;
    }
    long diff = latestDate.getTime() - oldestDate.getTime();
    long seconds = TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS);
    
    float qps = queryCount.get() / (float) seconds;
    return qps;
  }
  
  @Override
  public String getSummaryLine() {
    return "QPS: " + getQPS();
  }
  
  @Override
  public void close() {
    if (fullOutput != null) fullOutput.close();
  }
  
}
