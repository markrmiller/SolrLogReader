
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TextMatchAspect extends Aspect {
  private final List<Text> texts = Collections.synchronizedList(new ArrayList<Text>());
  private final String text;
  private String outputDir;
  
  static class Text implements Comparable<Text> {
    String text;
    Date date;
    String filename;
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
      text.filename = filename;
      texts.add(text);
    }
    return false;
  }
  
  @Override
  public void printReport(PrintStream out) {
    synchronized (texts) {
      
      Collections.sort(texts);
      
      out.println("TextMatch Report: " + text);
      out.println("-----------------");
      for (Text t : texts) {
        out.println("(" + t.filename + ")");
        out.println("  " + t.text);
      }
    }
  }
  
  @Override
  public void close() {
    if (outputDir != null) {
      fileReport(outputDir);
    }
  }

  public void fileReport(String outputDir) {
    String filename = text.replaceAll("[^a-zA-Z0-9.-]", "_");
    StringBuilder sb = new StringBuilder();
    sb.append("TextMatch Report: " + text);
    sb.append("-----------------");
    
    try {
      Files.write(Paths.get(outputDir, filename), sb.toString().getBytes("UTF-8"), StandardOpenOption.APPEND);
    } catch (UnsupportedEncodingException e) {
      // UTF-8
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    synchronized (texts) {
      for (Text t : texts) {
        sb = new StringBuilder();
        sb.append("(" + t.filename + ")");
        sb.append("  " + t.text);
        
        try {
          Files.write(Paths.get(outputDir, filename), sb.toString().getBytes("UTF-8"), StandardOpenOption.APPEND);
        } catch (UnsupportedEncodingException e) {
          // UTF-8
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
  
}
