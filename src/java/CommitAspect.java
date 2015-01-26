
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

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: commit times, params, etc
public class CommitAspect extends Aspect {
  public static Pattern OPTIMIZE = Pattern.compile("optimize=(true|false)");
  public static Pattern SOFTCOMMIT = Pattern.compile("softCommit=(true|false)");
  public static Pattern OPENSEARCHER = Pattern.compile("openSearcher=(true|false)");
  private AtomicLong commits = new AtomicLong(0);
  private AtomicLong optimize = new AtomicLong(0);
  private AtomicLong softCommit = new AtomicLong(0);
  private AtomicLong openSearcher = new AtomicLong(0);
  
  @Override
  public boolean process(String filename, String timestamp, Date dateTs, String headLine, String entry) {
    // System.out.println("ts:" + timestamp + " headline:" + headLine + " entry:" + entry);
    
    // TODO: track end_commit_flush and line up commit times
    
    if (headLine.contains("start commit{")) {
      commits.incrementAndGet();
      
      Matcher m = OPTIMIZE.matcher(headLine);
      if (m.find()) {
        boolean val = Boolean.parseBoolean(m.group(1));
        if (val) {
          optimize.incrementAndGet();
        }
      }
      
      m = SOFTCOMMIT.matcher(headLine);
      if (m.find()) {
        boolean val = Boolean.parseBoolean(m.group(1));
        if (val) {
          softCommit.incrementAndGet();
        }
      }
      
      m = OPENSEARCHER.matcher(headLine);
      if (m.find()) {
        boolean val = Boolean.parseBoolean(m.group(1));
        if (val) {
          openSearcher.incrementAndGet();
        }
      }
      
    } else if (headLine.contains("end_commit")) {

    }
    
    return false;
  }
  
  @Override
  public void printReport() {
    System.out.println("Commit Report");
    System.out.println("-----------------");
    System.out.println("Commits Found: " + commits.get());
    System.out.println("Contained Optimize: " + optimize.get());
    System.out.println("Hard Commits: " + (commits.get() - softCommit.get()));
    System.out.println("Soft Commits: " + softCommit.get());
    System.out.println("With openSearcher: " + openSearcher.get());
    System.out.println("Without openSearcher: " + (commits.get() - openSearcher.get()));
    
  }
  
}
