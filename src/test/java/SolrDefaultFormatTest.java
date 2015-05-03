
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

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class SolrDefaultFormatTest extends Assert {

  @Test
  public void default51FormatTest() throws Exception {
    String exampleLog = "logs/solr-default/sample.log";
    Map<String,LogInstance> hostToLogInstance = SolrLogReader.summarize(new String[] {exampleLog});
    assertEquals("We are only pointing to one file", 1, hostToLogInstance.size());
    List<Aspect> aspects = hostToLogInstance.values().iterator().next().getAspects();
    
    ErrorAspect errorAspect = null;
    CommitAspect commitAspect = null;
    for (Aspect aspect : aspects) {
      if (aspect instanceof ErrorAspect) {
        errorAspect = (ErrorAspect) aspect;
      } else if (aspect instanceof CommitAspect) {
        commitAspect = (CommitAspect) aspect;
      }
    }
    
    assertNotNull(errorAspect);
    assertEquals("Found wrong number of errors", 1, errorAspect.getErrors().size());
    assertEquals("Should be no OOMs", 0, errorAspect.getOoms().get());
    assertNull("outputdir not set", errorAspect.getOutputDir());
    assertFalse("We should have been able to parse all the timestamps", errorAspect.getSawUnknownTimestamp());
    
    assertNotNull(commitAspect);
    assertEquals("Wrong number of hard commits found", 0, commitAspect.getCommits().get());
    assertEquals("Wrong number of soft commits found", 0, commitAspect.getSoftCommit().get());
    assertEquals("Wrong number of commits with openSearcher found", 0, commitAspect.getOpenSearcher().get());
  }
}
