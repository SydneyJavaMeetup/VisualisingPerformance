# Visualising Performance
Talk on visualisation of performance statistics.

Uses MongoDB collections of statistics and HighCharts in the browser to collect statistics.


#### Data
The data set we're using is a set of download metrics from CDNs.

The data collected is in perfstats.zip, and after extracting you can load this into MongoDB like this:

```$text
mongoimport -d SydneyJavaMeetup -c perfstats perfstats.json
```

The two files which have been downloaded are also included for reference:
data/json-small.json
data/img-large.jpeg

:).

### Go Serverless! 
We'll use a serverless Lambda to back the API and host in AWS cloud.

Serverless.com Docs:
https://docs.serverless.com/

Logging:
https://docs.aws.amazon.com/lambda/latest/dg/java-logging.html#java-logging-log4j2

Sample Java Lambda:
https://github.com/serverless/examples/tree/master/aws-java-simple-http-endpoint

Mongo connection pooling in Lambda:

https://dzone.com/articles/how-to-use-mongodb-connection-pooling-on-aws-lambd

https://stackoverflow.com/questions/37415469/how-we-can-use-jdbc-connection-pooling-with-aws-lambda

### Preso

[https://slides.com/lthompson/visualisingperformance](https://slides.com/lthompson/visualisingperformance)

### TODO: 
Update to use the $bucket aggregation framework feature instead of calculating the buckets in code (if poss!).