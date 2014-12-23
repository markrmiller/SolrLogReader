
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

public class TextMatchAspect extends Aspect {
  private final List<Text> texts = Collections.synchronizedList(new ArrayList<Text>());
  private final String text;
  
  static class Text implements Comparable<Text> {
    String text;
    Date date;
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
  
  public TextMatchAspect(String text) {
    this.text = text;
  }
  
  @Override
  public boolean process(String timestamp, Date dateTs, String headLine, String entry) {
    if (dateTs == null) {
      dateTs = new Date(0);
    }
    if (headLine.contains(text) || entry.contains(text)){
      Text text = new Text();
      text.text = headLine + (entry != null && entry.length() > 0 ? ":" + entry : "");
      text.date = dateTs;
      texts.add(text);
    }
    return false;
  }
  
  @Override
  public void printReport() {
    Collections.sort(texts);
    
    System.out.println("TextMatch Report: " + text);
    System.out.println("-----------------");
    for (Text t : texts) {
      System.out.println(t.text);
    }
  }
  
}
