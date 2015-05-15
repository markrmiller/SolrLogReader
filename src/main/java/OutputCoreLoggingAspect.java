
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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutputCoreLoggingAspect extends Aspect {
  
  public static Pattern CORE_LOGGING = Pattern
.compile("\\spath=\\S+\\s", Pattern.DOTALL);
      
  private PrintWriter fullOutput;
  
  private SortedSet<LogEntry> ss = Collections.synchronizedSortedSet(new TreeSet<LogEntry>());
  
  public OutputCoreLoggingAspect(String outputDir) {
    if (outputDir != null) {
      try {
        fullOutput = new PrintWriter(
            new BufferedWriter(new FileWriter(outputDir + File.separator + "core-logging.txt"), 2 ^ 20));
        StringBuilder sb = new StringBuilder();
        sb.append("Core Logging (No updates or queries)" + "\n");
        sb.append("-----------------" + "\n");
        fullOutput.write(sb.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  @Override
  public boolean process(String filename, String timestamp, Date dateTs, String headLine, String entry) {
    Matcher m = CORE_LOGGING.matcher(headLine);
    if (!m.find()) {
      if (fullOutput != null) {
        LogEntry e = new LogEntry(timestamp, headLine + "\n" + entry);
        e.timestamp = dateTs;
        e.rawTimestamp = timestamp;
        
        // TODO: something more RAM efficient ??
        synchronized (ss) {
          ss.add(e);
        }
        
      }
    }
    return false;
  }

  private void flushSS() {
    synchronized (ss) {
      Iterator<LogEntry> it = ss.iterator();
      while (it.hasNext()) {
        LogEntry t = it.next();
        it.remove();
        fullOutput.write(t.headLine);
        fullOutput.write(t.entry);
      }
    }
  }
  
  @Override
  public void printReport(PrintStream out) {
  
  }
  
  @Override
  public void newFile() {
    flushSS();
    ss.clear();
  }
  
  @Override
  public void close() {
    if (fullOutput != null) {
      flushSS();
      fullOutput.close();
    }
  }
  
}
