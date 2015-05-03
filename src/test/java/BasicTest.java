
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class BasicTest extends Assert {

  @Test
  public void basicTest() throws Exception {
    String exampleLog = "logs/example.log";
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
    assertEquals("Should be no errors", 0, errorAspect.getErrors().size());
    assertEquals("Should be no OOMs", 0, errorAspect.getOoms().get());
    assertNull("outputdir not set", errorAspect.getOutputDir());
    
    assertNotNull(commitAspect);
    assertEquals("Wrong number of hard commits found", 2, commitAspect.getCommits().get());
    assertEquals("Wrong number of soft commits found", 1, commitAspect.getSoftCommit().get());
    assertEquals("Wrong number of commits with openSearcher found", 1, commitAspect.getOpenSearcher().get());
  }
  
  @Test
  public void exceptionTest() throws Exception {
    String exampleLog = "logs/exceptions/simple.log";
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
    
    assertNotNull(commitAspect);
    assertEquals("Wrong number of hard commits found", 0, commitAspect.getCommits().get());
    assertEquals("Wrong number of soft commits found", 0, commitAspect.getSoftCommit().get());
    assertEquals("Wrong number of commits with openSearcher found", 0, commitAspect.getOpenSearcher().get());
    
    assertNotNull(errorAspect);
    
    assertEquals("Wrong number of errors found", 2, errorAspect.getErrors().size());
    assertEquals("Should be no OOMs", 0, errorAspect.getOoms().get());
    assertNull("outputdir not set", errorAspect.getOutputDir());
    
    Set<ErrorAspect.LogError> errors = errorAspect.getErrors();
    for (ErrorAspect.LogError error : errors) {
      assertTrue(error.headLines.get(0).contains("simple.log"));
      assertTrue("Could not find expected text on first line of exception",
          error.entry.contains("org.apache.solr.common.SolrException"));
      assertTrue("Could not find expected text on last line of exception",
          error.entry.contains("java.lang.Thread.run"));
    }
    
  }
  
  @Test
  public void cutOffExceptionTest() throws Exception {
    String exampleLog = "logs/exceptions/cutoff.log";
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
    
    assertNotNull(commitAspect);
    assertEquals("Wrong number of hard commits found", 1, commitAspect.getCommits().get());
    assertEquals("Wrong number of soft commits found", 1, commitAspect.getSoftCommit().get());
    assertEquals("Wrong number of commits with openSearcher found", 1, commitAspect.getOpenSearcher().get());
    
    assertEquals("Wrong number of errors found: " + errorAspect.getErrors(), 0, errorAspect.getErrors().size());
    assertEquals("Should be no OOMs", 0, errorAspect.getOoms().get());
    assertNull("outputdir not set", errorAspect.getOutputDir());
  }
  
  @Test
  public void fileParseOrderTest() throws Exception {
    String exampleLog = "logs/multiple-logs";
    Map<String,LogInstance> hostToLogInstance = SolrLogReader.summarize(new String[] {exampleLog});
    assertEquals("Only dealing with one server", 1, hostToLogInstance.size());
    
    List<File> files = hostToLogInstance.values().iterator().next().getFiles();
    assertEquals(new File(exampleLog + "/example.log.3"), files.get(0));
    assertEquals(new File(exampleLog + "/example.log.1"), files.get(1));
    assertEquals(new File(exampleLog + "/example.log.0"), files.get(2));
  }
  
  @Test
  public void multipleHostTest() throws Exception {
    String exampleLog = "logs/multiple-hosts";
    Map<String,LogInstance> hostToLogInstance = SolrLogReader.summarize(new String[] {exampleLog});
    assertEquals("Should be 2 hosts", 2, hostToLogInstance.size());
  }
}
