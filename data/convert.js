// db.perfstats.aggregate([{$unwind: 'stats'}])
db.perfstats.aggregate([
  {$unwind: '$stats'},
  {$project: {
      _id:0,
      city:1, 
      postCode: 1, 
      countryCode: 1, 
      countryName: 1, 
      region: 1, 
      timestamp: 1, 
      location: {'type': {$literal: "Point"}, coordinates: [ '$location.longitude', '$location.latitude' ] }
      ,statName: '$stats.name',
      timeTakenMillis: '$stats.timeTakenMillis',
      cdn: '$stats.cdn'
  }}
  ,{$out:'PerfStats2'}
])
