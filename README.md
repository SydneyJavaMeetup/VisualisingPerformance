# Visualising Performance
Talk on visualisation of performance statistics.

Uses MongoDB collections of statistics and HighCharts in the browser to collect statistics.

We're using Vert.x as the web server, just because I fancied trying it!

Ref: https://vertx.io/docs/vertx-web/java/

Ref: https://vertx.io/docs/vertx-mongo-client/java/

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

### Preso

[](https://slides.com/lthompson/visualisingperformance#/)
