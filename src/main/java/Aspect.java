
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

import java.io.PrintStream;
import java.util.Date;

public abstract class Aspect {
  
  /**
   * @param filename file being processed
   * @param timestamp raw timestamp string
   * @param dateTs timestamp in Date form or null if not available
   * @param headLine the first line of a log entry
   * @param entry the rest of a log entry
   * @return true if aspect handled the entry and doesn't think another aspects needs to
   */
  public abstract boolean process(String filename, String timestamp, Date dateTs, String headLine, String entry);
  
  /**
   * Prints a summary report for the Aspect to standard out.
   */
  public abstract void printReport(PrintStream out);
  
  public String getSummaryLine() {
    return "";
  };
  
  public void close() {};
  
}
