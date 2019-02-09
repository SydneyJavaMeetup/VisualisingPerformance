# Visualising Performance
Talk on visualisation of performance statistics.

[![CircleCI](https://circleci.com/gh/SydneyJavaMeetup/VisualisingPerformance.svg?style=svg)](https://circleci.com/gh/SydneyJavaMeetup/VisualisingPerformance) [![Coverage Status](https://coveralls.io/repos/github/SydneyJavaMeetup/VisualisingPerformance/badge.svg?branch=master)](https://coveralls.io/github/SydneyJavaMeetup/VisualisingPerformance?branch=master)

Uses MongoDB collections of statistics and HighCharts in the browser to collect statistics.

### Preso

[https://slides.com/lthompson/visualisingperformance](https://slides.com/lthompson/visualisingperformance)

### Running Site

[https://visualisingperformance.mycodefu.com/ui](https://visualisingperformance.mycodefu.com/ui)


#### Data
If you want to run locally, you'll need a MongoDB instance and to import the data.

The data set we're using is a set of download metrics from CDNs.

The data collected is in perfstats.zip, and after extracting you can load this into MongoDB like this:

```$text
mongoimport -d SydneyJavaMeetup -c perfstats perfstats.json
```
