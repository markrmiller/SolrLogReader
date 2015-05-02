
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderThread extends Thread {
  private List<Aspect> aspects;
  private File file;
  private long start;
  private long end;
  private long length;
  private boolean last;
  private Pattern[] patterns;
  private Pattern pattern;
  private String[] dfPatterns;
  private int patternIndex;

  public ReaderThread(File file, long start, long end, long length, boolean endOfFileThread, List<Aspect> aspects, Pattern[] patterns, String[] dfPatterns)
      throws IOException {
    this.aspects = aspects;
    this.start = start;
    this.end = end;
    // System.out.println("create thread start:" + start + " end:" + end);
    this.file = file;
    this.last = endOfFileThread;
    this.length = length;
    this.patterns = patterns;
    this.dfPatterns = dfPatterns;
  }
  
  public void run() {
    RandomAccessFile raf = null;
    FileChannel channel = null;
    // System.out.println("map size:" + (end - start) + " start:" + start + " length:" + length);
    long readEnd = end;
    if (!last) {
      // we over read so that we don't miss a line
      readEnd += 10000;
      readEnd = Math.min(readEnd, length);
    }
    
    String dfString = null;
    try {
      raf = new RandomAccessFile(file, "r");
      channel = raf.getChannel();
      MappedByteBuffer map = channel.map(
          FileChannel.MapMode.READ_ONLY, start, readEnd - start);

      if (start > end) {
        return;
      }
      
      String timestamp = null;
      String pline = null;
      boolean foundAtLeastOneTimeStamp = false;
      while (map.position() < readEnd) {

        pline = readLine(map);
        if (pline == null) {
          break;
        }
        // System.out.println(Thread.currentThread().getId() + " lineis:" + pline);
        
        int cnt = 0;
        
        do {
          patternIndex = cnt;
          pattern = patterns[cnt++];
          Matcher tm = pattern.matcher(pline);
          // System.out.println(start + " " + "Try timestamp pattern: " +
          // pattern + " on line: " + pline);
          if (tm.matches()) {
            // found start of line
            foundAtLeastOneTimeStamp = true;
            timestamp = tm.group(1);
            dfString = dfPatterns[patternIndex];
            break;
          } else {
            // System.out.println("Failed");
          }
        } while (cnt < patterns.length);

        if (foundAtLeastOneTimeStamp) {
          break;
        }
      }
      if (!foundAtLeastOneTimeStamp) {
        System.out.println("WARNING: no log entries found, could not match on timestamp.");
        return;
      }
      // System.out.println("Using timestamp pattern:" + pattern);
      StringBuilder entry = new StringBuilder();
      boolean done = false;
      String headline = null;
      Date dateTs = null;
      do {
        // System.out.println(Thread.currentThread().getId() +  " readlineis:" + pline);

        // System.out.println("look at line" + cnt++);
        Matcher tm = pattern.matcher(pline);
        if (tm.matches()) {
          timestamp = tm.group(1);
          if (timestamp != null) {
            // System.out.println("Check df:" + dfString + " for " +
            // patternIndex + " in " + Arrays.asList(dfPatterns));
            if (dfString != null) {
              SimpleDateFormat format = new SimpleDateFormat(dfString);
              try {
                dateTs = format.parse(timestamp);
              } catch (ParseException e) {
                e.printStackTrace();
              }
            }
            if (headline != null) {
              process(file.getName(), timestamp, entry, headline, dateTs);
              headline = null;
            }
            if (done) {
              break;
            }
            headline = pline;
          }
          entry.setLength(0);
          if (map.position() >= end - start) {
            done = true;
          }
        } else {
          // building an entry
          // System.out.println("building:" + pline);
          entry.append(pline + "\n");
        }
        
      } while ((pline = readLine(map)) != null);
      
      // process any final entry
      if (headline != null) {
        process(file.getName(), timestamp, entry, headline, dateTs);
        headline = null;
      }
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      try {
        if (channel != null) {
          channel.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        raf.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void process(String filename, String timestamp, StringBuilder entry, String headline, Date dateTs) {
    for (Aspect aspect : aspects) {
      // System.out.println("process entry:" + pline + "\n" + entry.toString());
      boolean result = aspect.process(filename, timestamp, dateTs, headline, entry.toString());
      if (result) {
        break;
      }
    }
  }
  
  public final String readLine(MappedByteBuffer map) throws IOException {
    StringBuilder input = new StringBuilder();
    int c = -1;
    boolean eol = false;
    try {
      while (!eol) {
        switch (c = map.get()) {
          case -1:
          case '\n':
            eol = true;
            break;
          case '\r':
            eol = true;
            long cur = map.position();
            if ((map.get()) != '\n') {
              map.position((int) cur);
            }
            break;
          default:
            input.append((char) c);
            break;
        }
      }
    } catch (BufferUnderflowException e) {
      return null;
    }
    
    if ((c == -1) && (input.length() == 0)) {
      return null;
    }
    return input.toString();
  }
}
