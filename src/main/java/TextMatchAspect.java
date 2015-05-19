
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
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

public class TextMatchAspect extends Aspect {
  private SortedSet<Text> texts = Collections.synchronizedSortedSet(new TreeSet<Text>());
  private final String text;
  private String outputDir;
  private String filename;
  
  static class Text implements Comparable<Text> {
    String text;
    Date date;
    String filename;
    String timestamp;
    
    @Override
    public int compareTo(Text o) {
      int rslt;
      try {
        rslt = this.date.compareTo(o.date);
      } catch (Exception e) {
        throw new RuntimeException("Problem with data:" + text, e);
      }
      
      return rslt;
    }
  }
  
  public TextMatchAspect(String text, String outputDir) {
    this.text = text;
    this.outputDir = outputDir;
    
    if (this.outputDir != null) {
      filename = text.replaceAll("[^a-zA-Z0-9.-]", "_") + ".txt";
      StringBuilder sb = new StringBuilder();
      sb.append("TextMatch Report: " + text + "\n");
      sb.append("-----------------\n\n");
      
      try {
        Files.write(Paths.get(outputDir, filename), sb.toString().getBytes("UTF-8"), StandardOpenOption.CREATE);
      } catch (UnsupportedEncodingException e) {
        // UTF-8
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  @Override
  public boolean process(String filename, String timestamp, Date dateTs, String headLine, String entry) {
    if (dateTs == null) {
      dateTs = new Date(0);
    }
    if (headLine.contains(text) || entry.contains(text)){
      Text text = new Text();
      text.text = headLine + (entry != null && entry.length() > 0 ? ":" + entry : "");
      text.date = dateTs;
      text.timestamp = timestamp;
      text.filename = filename;
      texts.add(text);
    }
    return false;
  }
  
  @Override
  public void endOfFile() {
    if (outputDir != null) {
      flushSS();
    }
  }
  
  private void flushSS() {
    synchronized (texts) {
      StringBuilder sb = new StringBuilder();
      for (Text t : texts) {
        sb.append("(" + t.timestamp + " : " + t.filename + ")\n");
        sb.append("  " + t.text + "\n\n");
        
        try {
          Files.write(Paths.get(outputDir, filename), sb.toString().getBytes("UTF-8"), StandardOpenOption.APPEND);
        } catch (UnsupportedEncodingException e) {
          // UTF-8
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        sb.setLength(0);
      }
      texts.clear();
    }
  }
  
  @Override
  public void printReport(PrintStream out) {
  
  }
  
  @Override
  public void close() {
  
  }
  
}
