## A high performance Solr log reader / parser.

Early days. Crunches and summarizes Solr log files.  

**java -jar slr.jar [file or folder path] {TextMatchAspect} {TextMatchAspect} {-o outputdir}...**

Example: **java -jar slr.jar /solr/logs**  

An optional TextMatchAspect will pull out any logs with matching text, for example: **java -jar slr.jar /solr/logs org.apache.solr.cloud** 

If you specify an outputdir, more verbose summaries are dumped to files in that folder as well as an html error chart.  

If a folder is given, logs are parsed in reverse order if they end with digits.  
**solr.log.2, solr.log.1, solr.log.0, etc**

Logs that look like they come from different servers will be summarized separately.  
**solr-host1.log.1, solr-host1.log.0, solr-host2.log.0, etc**


### Getting Started

Download SolrLogReader: https://github.com/markrmiller/SolrLogReader/releases/download/v0.0.1/solr-log-reader-0.0.1-SNAPSHOT-dist.zip

Extract it.

Run it.

**java -jar slr.jar /path/to/logs**


### Timestamp Patterns

Timestamp patterns can be added or modified in config.txt.

Example:  
**timestamp1=^(\\d\\d\\d\\d\\-\\d\\d\\-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d).*$**  
**timestamp1-dateformat=yyyy-MM-dd HH:mm:ss,SSS**  

The first entry is a Java regex pattern for matching the timestamp. The second entry is for parsing that match into a Java Date.
Entries will be tried until one matches.


### FAQ

**Q**: Can I just process the Solr logs in a deep directory hierarchy with lots of log files?  
**A**: SolrLogReader /solr/logs/solr* A filename with a glob pattern will be used to match against file names for all files under the /solr/logs directory hierarchy.


#### Developer Help

I use eclipse to develop and run SolrLogReader. Here is how you might run it command line.

**wget https://github.com/markrmiller/SolrLogReader/archive/master.zip**  
  
**unzip master.zip**  
**cd SolrLogReader-master**  
**mvn install**  
**javac -cp target/lib/* src/main/java/*.java**  
**java -cp target/lib/*:src/main/java SolrLogReader /path/to/logs**    