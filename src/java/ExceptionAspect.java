
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class ExceptionAspect extends Aspect {
  public static Pattern TIMESTAMPE = Pattern.compile(
      "(.*?\\s(?:ERROR|WARN|INFO|DEBUG|TRACE))(.*)", Pattern.DOTALL);
  
  
  private Set<Exp> exceptions = Collections.synchronizedSet(new HashSet<Exp>());
  
  static class Exp {
    List<String> headLines = Collections.synchronizedList(new ArrayList<String>());
    String entry;
    
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
      Exp other = (Exp) obj;
      if (entry == null) {
        if (other.entry != null) return false;
      } else if (!entry.equals(other.entry)) return false;
      return true;
    }

    public Exp(String headLine, String entry) {
      this.headLines.add(headLine);
      this.entry = entry;
    }
  }
  
  public ExceptionAspect() {
    exceptions = new HashSet<Exp>();
  }
  
  @Override
  public boolean process(String timestamp, Date dateTs, String headLine, String entry) {
     //System.out.println("headline:" + headLine);
     //System.out.println("entry:" + entry);
    if (headLine.contains("Exception")) {
      synchronized (exceptions) {
        // System.out.println("Exception:" + headLine);
        // System.out.println("Entry:" + entry);
        Exp e;
        Matcher m = TIMESTAMPE.matcher(headLine);
        String ts = "";
        if (m.matches()) {
          ts = m.group(1);
          e = new Exp(m.group(1), m.group(2) + entry);
        } else {
          throw new RuntimeException();
        }
  
        boolean added = exceptions.add(e);
        if (!added) {
          e.headLines.add(ts);
        }
        return true;
      }
    }
    return false;
  }
  
  @Override
  public void printReport() {
    System.out.println("Exceptions Report");
    System.out.println("-----------------");
    int expCnt = 0;
    for (Exp e : exceptions) {
      expCnt += e.headLines.size();
    }
    
    System.out.println("Exceptions found:" + expCnt);
    System.out.println();

    for (Exp exp : exceptions) {
      for (String hl : exp.headLines) {
        System.out.println("(" + hl + ") ");
      }
      System.out.println(exp.entry);
      System.out.println();
    }
    
  }
  
}
