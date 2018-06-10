
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fast Solr log reader.
 */
public class SolrLogReader {
  private static final String REPORT_FILENAME = "logs-report.txt";
  public static Pattern END_DIGITS = Pattern.compile("(.*?)(\\d+)$", Pattern.DOTALL);
  public static Pattern END_DIGITS2 = Pattern.compile("(.*?)(\\.\\d+)$", Pattern.DOTALL);
  public static Pattern DIGITS = Pattern.compile("(\\d+)", Pattern.DOTALL);
  private static String outputDir;
  private static Range range;
  private static int nSlowQueries = 10;
  private static int nSlowLoadTimes = 5;

  public static class Range {
    Date start;
    Date end;
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.out.println("Usage: SolrLogReader [file or folder path] {TextMatchAspect} {TextMatchAspect} ...");
      System.out.println("Example: SolrLogReader /solr/logs org.apache.solr.cloud");
      System.exit(1);
    }
    
    summarize(args);

  }

  public static Map<String,LogInstance> summarize(String[] args) throws FileNotFoundException, IOException {
    PrintStream out = System.out;
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
    
    out.println("# Configured timestamp patterns: " + tsPatterns);
    out.println("# Configured date format patterns:" +  dfPatterns);
    out.println();
    
    Pattern[] patterns = new Pattern[tsPatterns.size()];
    for (int i = 0; i < patterns.length; i++) {
      patterns[i] = Pattern.compile(tsPatterns.get(i), Pattern.DOTALL);
    }
    
    List<String> textAspects = new ArrayList<String>();
    
    for (int i = 1; i < args.length; i++) {
      if (args[i].equals("-o")) {
        outputDir = args[++i];
        out.println("# Writing file reports to:" + outputDir);
      } else if (args[i].equals("-r")) {
        String startDate = args[++i];
        String endDate = args[++i];
        
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date start = null;
        try {
          start = format.parse(startDate);
        } catch (ParseException e) {
          e.printStackTrace();
        }
        
        Date end = null;
        try {
          end = format.parse(endDate);
        } catch (ParseException e) {
          e.printStackTrace();
        }
        
        if (start != null && end != null) {
          range = new Range();
          range.start = start;
          range.end = end;
        }
        
        out.println("# Range:" + startDate + ", " + endDate);
      } else if (args[i].equals("-nSlowQueries")) {
        String count = args[++i];
        if(count != null && !count.isEmpty()) {
          nSlowQueries = Integer.parseInt(count);
        }
      } else if (args[i].equals("-nSlowLoadTimes")) {
        String count = args[++i];
        if(count != null && !count.isEmpty()) {
          nSlowLoadTimes = Integer.parseInt(count);
        }
      } else {
        out.println("# Using Text Aspect: " + args[i]);
        textAspects.add(args[i]);
      }
    }
    
    if (textAspects.size() > 0 && outputDir == null) {
      System.out.println();
      System.out.println("TextAspects only work with the -o output directory option.");
      System.exit(1);
    }
    
    long timeStart = new Date().getTime();
    List<File> files = new ArrayList<File>();
    File file = new File(args[0]);
    String matchText = null;
    File srcDir;
    if (file.getName().contains("*")) {
      // file matching
      matchText = file.getName().replaceAll("\\*", ".*");
      srcDir = file.getParentFile();
    } else {
      srcDir = file;
    }
    
    out.println("# Scanning Directory: " + srcDir);
    if (matchText != null) {
      out.println("# File Match Text: " + matchText);
    }
    
    out.println();
    
    getFiles(files, srcDir, matchText);
    
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
    
    Map<String,LogInstance> logInstances = new HashMap<String,LogInstance>();
    
    
    if (outputDir != null) {
      createDir(outputDir);
    }
    
    long totalBytes = 0;
    
    Map<String,LogInstance> hostToLogInstance = new LinkedHashMap<>();
    
    for (File f : files) {
      String k;
      Matcher m = END_DIGITS2.matcher(f.getName());
      if (m.matches()) {
        k = m.group(1);
      } else {
        k = f.getName();
      }
      LogInstance logInstance = logInstances.get(k);
      if (logInstance == null) {
        String intanceOutputDir = null;
        if (outputDir != null) {
          intanceOutputDir = outputDir + File.separator + k;
          createDir(intanceOutputDir);
        }
        
        List<Aspect> aspects = new ArrayList<Aspect>();
        aspects.add(new OpenSearcherAspect(nSlowLoadTimes));
        aspects.add(new CommitAspect());
        aspects.add(new QueryAspect(intanceOutputDir, nSlowQueries));
        aspects.add(new ErrorAspect(intanceOutputDir));
        aspects.add(new OutputCoreLoggingAspect(intanceOutputDir));
        for (String aspect : textAspects) {
          aspects.add(new TextMatchAspect(aspect, intanceOutputDir));
        }
        logInstance = new LogInstance(aspects);
        hostToLogInstance.put(k, logInstance);
        logInstances.put(k, logInstance);
      }
      logInstance.track(f);
      totalBytes += f.length();
      processFile(f, logInstance.getAspects(), patterns, dfPatterns.toArray(new String[0]), out);
    }
    
    long timeEnd = new Date().getTime();
    
    DecimalFormat df = new DecimalFormat("#.00");
    
    out.println();
    out.println("Took " + df.format((timeEnd - timeStart) / 1000.0 / 60.0) + "min to crunch " + df.format(totalBytes / 1024.0 / 1024.0) + "MB  AVG(" + df.format(totalBytes / (float) files.size() / 1024.0 / 1024.0) + ")");
    out.println();
    
    StringBuilder summary = new StringBuilder();
    summary.append("- Summary Report -\n\n");
    for (Entry<String,LogInstance> liEntry : logInstances.entrySet()) {
      summary.append("Instance: " + liEntry.getKey() + "\n");
      for (Aspect aspect : liEntry.getValue().getAspects()) {
        summary.append("  " + aspect.getSummaryLine());
      }
      summary.append("\n");
    }
    out.print(summary + "\n\n");
    if (outputDir != null) {
      PrintStream summaryOut = new PrintStream(
          new BufferedOutputStream(new FileOutputStream(outputDir + File.separator + "summary.txt")));
      summaryOut.print(summary);
      summaryOut.close();
    }
    
    for (Entry<String,LogInstance> liEntry : logInstances.entrySet()) {
      PrintStream entryOut = out;
      if (outputDir != null) {
        entryOut = new PrintStream(new BufferedOutputStream(
            new FileOutputStream(outputDir + File.separator + liEntry.getKey() + File.separator + REPORT_FILENAME)));
      }
      entryOut.println("* Instance Report: " + liEntry.getKey());
      for (Aspect aspect : liEntry.getValue().getAspects()) {
        entryOut.print("  " + aspect.getSummaryLine());
      }
      
      liEntry.getValue().printResults(entryOut);
      if (outputDir != null) {
        entryOut.close();
      }
      liEntry.getValue().close();
    }
    
    return hostToLogInstance;
  }

  private static void createDir(String dir) throws IOException {
    Path path = FileSystems.getDefault().getPath(dir);
    try {
      Files.createDirectory(path);
    } catch (FileAlreadyExistsException e) {
      // fine
    }
  }

  private static void getFiles(List<File> files, File file, String matchText) {
    if (file.isDirectory()) {
      File[] listFiles = file.listFiles();
      for (File f : listFiles) {
        if (f.isFile()) {
          if (matchText == null || f.getName().matches(matchText)) {
            files.add(f);
          }
        } else {
          getFiles(files, f, matchText);
        }
      }
    } else {
      if (matchText == null || file.getName().matches(matchText)) {
        files.add(file);
      }
    }
  }

  private static void processFile(File file, List<Aspect> aspects, Pattern[] patterns, String[] dfPatterns, PrintStream out)
      throws IOException {
    out.println("Processing file: " + file.getName());
    
    int threads = Runtime.getRuntime().availableProcessors();
    RandomAccessFile raf = new RandomAccessFile(file, "r");
    long length = raf.length();
    raf.close();
    long chunkSize = length / threads;
    long start = 0;
    long end = chunkSize;
    List<ReaderThread> threadReaders = new ArrayList<ReaderThread>();
    for (int i = 0; i < threads; i++) {
      ReaderThread rt = new ReaderThread(file, start, end, length, i == threads - 1, aspects, patterns, dfPatterns,
          range);
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
    
    for (Aspect aspect : aspects) {
      aspect.endOfFile();
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
        f1 = Long.parseLong(m.group(2));
      } else {
        m = digitPattern.matcher(obj1String);
        while (m.find()) {
          f1 = Long.parseLong(m.group(1));
        }
      }
      Matcher m2 = digitPattern.matcher(obj2String);
      if (digitPattern == END_DIGITS && m2.matches()) {
        f2 = Long.parseLong(m2.group(2));
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
