
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
  private final List<String> texts = Collections.synchronizedList(new ArrayList<String>());
  private final String text;
  
  public TextMatchAspect(String text) {
    this.text = text;
  }
  
  @Override
  public boolean process(String timestamp, Date dateTs, String headLine, String entry) {
    if (headLine.contains(text)){
      texts.add(headLine + (entry != null && entry.length() > 0 ? ":" + entry : ""));
    }
    return false;
  }
  
  @Override
  public void printReport() {
    System.out.println("TextMatch Report: " + text);
    System.out.println("-----------------");
    for (String t : texts) {
      System.out.println(t);
    }
  }
  
}
