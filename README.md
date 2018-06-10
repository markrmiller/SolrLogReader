## A high performance Solr log reader / parser.

Crunches and summarizes Solr log files. 

**java -jar slr.jar [file or folder path] {TextMatchAspect} {TextMatchAspect} {-o outputdir} {-r '2015-05-12 14:23:00' '2015-05-12 15:11:56'}**

Example: **java -jar slr.jar /solr/logs**  

An optional TextMatchAspect will pull out any logs with matching text and you can specify as many as you want, for example: **java -jar slr.jar /solr/logs org.apache.solr.cloud -o /results**  
**Note:** TextMatchAspect will only work when using -o to specify an output directory.  

**-o** If you specify an outputdir, more verbose summaries are dumped to files in that folder as well as an html error chart.  

**-r** You can filter processed log entries by timestamp range using the format yyyy-MM-dd HH:mm:ss.

If a folder is given, logs are parsed in reverse order if they end with digits.  
**solr.log.2, solr.log.1, solr.log.0, etc**

Logs that look like they come from different servers will be summarized separately.  
**solr-host1.log.1, solr-host1.log.0, solr-host2.log.0, etc**

#### Optional Params
**-nSlowQueries** No. of Slow Queries to output, default value is 10.

**-nSlowLoadTimes** No. of Slow Load Times to output, default value is 5.

### Getting Started

Download SolrLogReader: https://github.com/markrmiller/SolrLogReader/releases/download/v1.0.0/solr-log-reader-1.0.0-dist.zip

Extract it.

Run it.

**java -jar slr.jar /path/to/logs**

Often you want all the extra output and separation you can get by using the -o output folder option.

**java -jar slr.jar /path/to/logs /path/to/output**

### Timestamp Patterns

Timestamp patterns can be added or modified in config.txt.

Example:  
**timestamp1=^(\\d\\d\\d\\d\\-\\d\\d\\-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d)(.*)$**  
**timestamp1-dateformat=yyyy-MM-dd HH:mm:ss,SSS**  

The first entry is a Java regex pattern for matching the timestamp. http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
The first entry should capture two groups using parenthesis. The first group should capture the timestamp and the second group should capture the rest of the line.

The second entry is for parsing that match into a Java Date. http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html  

Entries will be tried until one matches. If none match, summary will be done without comparing timestamps.


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