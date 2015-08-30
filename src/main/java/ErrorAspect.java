
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

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ErrorAspect extends Aspect {
  private static final String CHARTS_FILE_NAME = "error-chart.html";
  
  public static Pattern EXCEPTION_CLASS = Pattern.compile("\\s((?:[a-zA-Z_$][a-zA-Z\\d_$]*\\.)+[a-zA-Z_$][a-zA-Z\\d_$]*)");
  
  private final AtomicInteger ooms = new AtomicInteger();
  private final Set<LogEntry> errors = Collections.synchronizedSet(new HashSet<LogEntry>());
  
  private final Map<String,Integer> uniqueExceptions = new HashMap<String,Integer>();


  private String outputDir;
  
  private boolean sawUnknownTimestamp = false;
  
  public ErrorAspect(String outputDir) {
    this.outputDir = outputDir;
  }
  
  @Override
  public boolean process(String filename, String timestamp, Date dateTs, String headLine, String entry) {
    // System.out.println("headline:" + headLine);
    // System.out.println("entry:" + entry);
    if (headLine.contains("Exception") || headLine.contains(" ERROR ")) {
      if (dateTs == null) {
        sawUnknownTimestamp = true;
      }
      
      if (headLine.contains("OutOfMemoryError")) {
        ooms.incrementAndGet();
      }
      // System.out.println("Exception:" + headLine);
      // System.out.println("Entry:" + entry);
      LogEntry e;
      
      e = new LogEntry(timestamp + " : " + filename, headLine + "\n" + entry);
      e.timestamp = dateTs;
      e.rawTimestamp = timestamp;

      errors.add(e);
      
      // unique errors
      if (outputDir != null) {
        Matcher m = EXCEPTION_CLASS.matcher(headLine);
        while (m.find()) {
          collect(m);
        }
        m = EXCEPTION_CLASS.matcher(entry);
        while (m.find()) {
          collect(m);
        }
      }
      
      return false;
      
    }
    return false;
  }

  private void collect(Matcher m) {
    String match = m.group(1);
    if (match.contains("Exception")) {
      synchronized (uniqueExceptions) {
        Integer count = uniqueExceptions.get(match);
        if (count == null) {
          uniqueExceptions.put(match, 1);
        } else {
          uniqueExceptions.put(match, ++count);
        }
      }
    }
  }
  
  @Override
  public void printReport(PrintStream out) {
    out.println("Errors Report");
    out.println("-----------------");
    
    out.println("Errors found:" + errors.size() + " OOMS:" + ooms.get());
    out.println();

    List<LogEntry> errorList = new ArrayList<LogEntry>(errors.size());
    synchronized (errors) {
      errorList.addAll(errors);
    }
    Collections.sort(errorList);
    
    for (LogEntry error : errorList) {
      out.println("(" + error.headLine + ") ");
      out.println(error.entry);
      out.println();
    }
    
    if (outputDir != null) {
      writeHtmlErrorChart(errorList);
      writeUniqueExceptionReport();
    }
  }

  private void writeUniqueExceptionReport() {
    
    try {
      PrintWriter output = new PrintWriter(
          new BufferedWriter(new FileWriter(outputDir + File.separator + "unique-error-report.txt"), 2 ^ 20));
      synchronized (uniqueExceptions) {
        Set<Entry<String,Integer>> entries = uniqueExceptions.entrySet();
        for (Entry<String,Integer> error : entries) {
          output.println(error.getKey() + " " + error.getValue());
        }
      }
      output.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private void writeHtmlErrorChart(List<LogEntry> errorList) {
    StringBuilder data = new StringBuilder()
    
    ;
    for (LogEntry error : errorList) {


      if (data.length() > 0) {
        data.append(", ");
      }

      StringBuilder entry = new StringBuilder();
      
      entry.append("(" + error.headLine + ") ");
      
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
      Files.write(Paths.get(outputDir + "/" +
      
      CHARTS_FILE_NAME), html.getBytes("UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getSummaryLine() {
    List<LogEntry> errorList = new ArrayList<LogEntry>(errors.size());
    synchronized (errors) {
      errorList.addAll(errors);
    }
    Collections.sort(errorList);
    String first = "";
    if (errorList.size() > 0 && errorList.get(0).rawTimestamp != null) {
      first = " First Error: " + errorList.get(0).rawTimestamp;
    }
    return "Errors: " + Integer.toString(errors.size()) + " OOMS: " + ooms.get() + first;
  }
  
  /**
   * @return the ooms for tests
   */
  public AtomicInteger getOoms() {
    
    return ooms;
  }
  
  /**
   * @return the errors for tests
   */
  public Set<LogEntry> getErrors() {
    return errors;
  }
  
  /**
   * @return the outputDir for tests
   */
  public String getOutputDir() {
    return outputDir;
  }
  
  /**
   * @return the sawUnknownTimestamp for tests
   */
  public boolean getSawUnknownTimestamp() {
    return sawUnknownTimestamp;
  }
  
}
