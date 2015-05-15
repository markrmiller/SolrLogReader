
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
import java.util.List;

public class LogEntry implements Comparable<LogEntry> {
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
    LogEntry other = (LogEntry) obj;
    if (entry == null) {
      if (other.entry != null) return false;
    } else if (!entry.equals(other.entry)) return false;
    return true;
  }

  public LogEntry(String headLine, String entry) {
    this.headLines.add(headLine);
    this.entry = entry;
  }
  
  @Override
  public int compareTo(LogEntry o) {
    if (this.timestamp == null) {
      return -1;
    } else if (o.timestamp == null) {
      return 1;
    }
    
    return this.timestamp.compareTo(o.timestamp);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "LogError [headLines=" + headLines + ", entry=" + entry + "]";
  }
}