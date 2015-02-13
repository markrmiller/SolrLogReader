
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fast Solr log reader.
 */
public class SolrLogReader {
  public static Pattern END_DIGITS = Pattern.compile(".*?(\\d+)$", Pattern.DOTALL);
  public static Pattern DIGITS = Pattern.compile("(\\d+)", Pattern.DOTALL);

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.out.println("Usage: SolrLogReader [file or folder path] {TextMatchAspect} {TextMatchAspect} ...");
      System.out.println("Example: SolrLogReader /solr/logs org.apache.solr.cloud");
      System.exit(1);
    }
    
    Properties props = new Properties();
    FileInputStream fis = new FileInputStream(new File("config.txt"));
    try {
      props.load(fis);
    } finally {
      fis.close();
    }
    
    List<String> tsPatterns = new ArrayList<String>();
    List<String> dfPatterns = new ArrayList<String>();
    List<String> propKeys = new ArrayList<String>();
    Enumeration<String> keys = (Enumeration<String>) props.propertyNames();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      propKeys.add(key);
    }
    
    Collections.sort(propKeys, new DigitComparator(END_DIGITS, false));
    
    for (String key : propKeys) {
      if (key.startsWith("timestamp") && !key.endsWith("-dateformat")) {
        tsPatterns.add(props.getProperty(key));
        String df = props.getProperty(key + "-dateformat");
        dfPatterns.add(df);
      }
    }
    
    System.out.println("Configured timestamp patterns: " + tsPatterns);
    System.out.println("Configured date format patterns:" +  dfPatterns);
    System.out.println();
    
    Pattern[] patterns = new Pattern[tsPatterns.size()];
    for (int i = 0; i < patterns.length; i++) {
      patterns[i] = Pattern.compile(tsPatterns.get(i), Pattern.DOTALL);
    }
    
    for (int i = 1; i < args.length; i++) {
      System.out.println("Using Text Aspect: " + args[i]);
    }
    
    long timeStart = new Date().getTime();
    List<File> files = new ArrayList<File>();
    File file = new File(args[0]);
    
    if (file.isDirectory()) {
      files.addAll(Arrays.asList(file.listFiles()));
    } else {
      files.add(file);
    }
    
    Pattern pattern = DIGITS;
    for (File f : files) {
      Matcher m = END_DIGITS.matcher(f.getName());
      if (m.matches()) {
        pattern = END_DIGITS;
        break;
      }
    }
    
    final Pattern digitPattern = pattern;
    Collections.sort(files, new DigitComparator(digitPattern, true));
    List<Aspect> aspects = new ArrayList<Aspect>();
    

    aspects.add(new OpenSearcherAspect());
    aspects.add(new CommitAspect());
    aspects.add(new QueryAspect());
    aspects.add(new ErrorAspect());
    
    for (int i = 1; i < args.length; i++) {
      aspects.add(new TextMatchAspect(args[i]));
    }
    
    for (File f : files) {
      processFile(f, aspects, patterns, dfPatterns.toArray(new String[0]));
    }
    long timeEnd = new Date().getTime();
    
    System.out.println("Took " + (timeEnd - timeStart) + "ms");

    for (Aspect aspect : aspects) {
      System.out.println();
      aspect.printReport();
    }

  }

  private static void processFile(File file, List<Aspect> aspects, Pattern[] patterns, String[] dfPatterns)
      throws IOException {
    System.out.println("Processing file: " + file.getName());
    int threads = Runtime.getRuntime().availableProcessors();
    RandomAccessFile raf = new RandomAccessFile(file, "r");
    long length = raf.length();
    raf.close();
    long chunkSize = length / threads;
    long start = 0;
    long end = chunkSize;
    List<ReaderThread> threadReaders = new ArrayList<ReaderThread>();
    for (int i = 0; i < threads; i++) {
      ReaderThread rt = new ReaderThread(file, start, end, length, i == threads - 1, aspects, patterns, dfPatterns);
      threadReaders.add(rt);
      rt.start();
      start = start + chunkSize;
      end = end + chunkSize;
      if (i == threads - 1) {
        end = length;
      }
    }
    
    for (Thread thread : threadReaders) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.interrupted();
        e.printStackTrace();
      }
    }
  }
  
  private static final class DigitComparator implements Comparator<Object> {
    private final Pattern digitPattern;
    private final boolean dec;
    
    private DigitComparator(Pattern digitPattern, boolean dec) {
      this.digitPattern = digitPattern;
      this.dec = dec;
    }
    
    @Override
    public int compare(Object obj1, Object obj2) {
      Long f1 = 0L;
      Long f2 = 0L;
      String obj1String = obj1.toString();
      String obj2String = obj2.toString();
      Matcher m = digitPattern.matcher(obj1String);
      if (digitPattern == END_DIGITS && m.matches()) {
        f1 = Long.parseLong(m.group(1));
      } else {
        m = digitPattern.matcher(obj1String);
        while (m.find()) {
          f1 = Long.parseLong(m.group(1));
        }
      }
      Matcher m2 = digitPattern.matcher(obj2String);
      if (digitPattern == END_DIGITS && m2.matches()) {
        f2 = Long.parseLong(m2.group(1));
      } else {
        m2 = digitPattern.matcher(obj2String);
        while (m2.find()) {
          f2 = Long.parseLong(m2.group(1));
        }
      }
      int result = f2.compareTo(f1);
      
      if (!dec) {
        result *= -1;
      }
      return result;
    }
  }
}
