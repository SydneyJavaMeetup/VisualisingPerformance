db.PerfStats.aggregate([
     {$match: {'statName': 'img-large'}}
    ,{$bucket: {
      groupBy: "$timeTakenMillis",
      boundaries: [ 0, 200, 400, 600, 800, 1000, 1200 ],
      default: "Other",
      output: {
        "count": { $sum: 1 }
      }
    }}
])