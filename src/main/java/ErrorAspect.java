
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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;


public class ErrorAspect extends Aspect {
  private static final String CHARTS_FILE_NAME = "charts.html";


  public static Pattern TIMESTAMP = Pattern.compile(
      "(.*?\\s(?:ERROR|WARN|INFO|DEBUG|TRACE))(.*)", Pattern.DOTALL);
  
  
  private Set<LogError> errors = Collections.synchronizedSet(new HashSet<LogError>());


  private String outputDir;
  
  static class LogError implements Comparable<LogError> {
    List<String> headLines = Collections.synchronizedList(new ArrayList<String>());
    String entry;
    Date timestamp;
    String rawTimestamp;
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((entry == null) ? 0 : entry.hashCode());
      return result;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      LogError other = (LogError) obj;
      if (entry == null) {
        if (other.entry != null) return false;
      } else if (!entry.equals(other.entry)) return false;
      return true;
    }

    public LogError(String headLine, String entry) {
      this.headLines.add(headLine);
      this.entry = entry;
    }
    
    @Override
    public int compareTo(LogError o) {
      if (this.timestamp == null) {
        return -1;
      } else if (o.timestamp == null) {
        return 1;
      }
      
      return this.timestamp.compareTo(o.timestamp);
    }
  }
  
  public ErrorAspect(String outputDir) {
    errors = new HashSet<LogError>();
    this.outputDir = outputDir;
  }
  
  @Override
  public boolean process(String filename, String timestamp, Date dateTs, String headLine, String entry) {
    // System.out.println("headline:" + headLine);
    // System.out.println("entry:" + entry);
    if (headLine.contains("Exception") || headLine.contains(" ERROR ")) {
      synchronized (errors) {
        // System.out.println("Exception:" + headLine);
        // System.out.println("Entry:" + entry);
        LogError e;
        Matcher m = TIMESTAMP.matcher(headLine);
        String ts = "";
        if (m.matches()) {
          ts = m.group(1);
          e = new LogError(m.group(1) + " : " + filename, m.group(2) + "\n" + entry);
          e.timestamp = dateTs;
          e.rawTimestamp = ts;
        } else {
          e = new LogError("[UNKNOWN TS] : " + filename, headLine + "\n" + entry);
        }
  
        boolean added = errors.add(e);
        if (!added) {
          e.headLines.add(ts + " : " + filename);
        }
        return true;
      }
    }
    return false;
  }
  
  @Override
  public void printReport(PrintStream out) {
    out.println("Errors Report");
    out.println("-----------------");
    int expCnt = 0;
    for (LogError e : errors) {
      expCnt += e.headLines.size();
    }
    
    out.println("Errors found:" + expCnt);
    out.println();

    List<LogError> errorList = new ArrayList<LogError>(errors);
    Collections.sort(errorList);
    
    for (LogError error : errorList) {
      for (String hl : error.headLines) {
        out.println("(" + hl + ") ");
      }
      out.println(error.entry);
      out.println();
    }
    
    if (outputDir != null) {
      StringBuilder data = new StringBuilder();
      for (LogError error : errorList) {


        if (data.length() > 0) {
          data.append(", ");
        }

        StringBuilder entry = new StringBuilder();
        for (String hl : error.headLines) {
          entry.append("(" + hl + ") ");
        }
        entry.append(error.entry);

        String tooltip = "\"<div style='font-size:14px;padding:5px 5px 5px 5px'><b>Date=</b>"
            + error.rawTimestamp
            + "<br/>" + Matcher.quoteReplacement(entry.toString()).replaceAll("\n", "<br/>")
            + "</div>\"";
        if (error.timestamp != null) {
          data.append("[ 'Error', new Date(" + error.timestamp.getTime() + ")," + "new Date(" + error.timestamp.getTime() + ")," + tooltip + "]");
        }
      }
      String html;
      try {
        html = new String(readAllBytes(get("chart_template.html")));
        
        html = html.replaceAll("DATA_REPLACE", data.toString());
        html = html.replaceAll("ABOUT_REPLACE", "Error Count:" + errorList.size());
        html = html.replaceAll("DESC_REPLACE", "Charts");
        html = html.replaceAll("HEIGHT_REPLACE", "150");
        Files.write(Paths.get(outputDir +"/" + CHARTS_FILE_NAME), html.getBytes("UTF-8"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public String getSummaryLine() {
    List<LogError> errorList = new ArrayList<LogError>(errors);
    Collections.sort(errorList);
    String first = "";
    if (errorList.get(0).rawTimestamp != null) {
      first = " First Error: " + errorList.get(0).rawTimestamp;
    }
    return "Errors: " + Integer.toString(errors.size()) + first;
  }
  
}
