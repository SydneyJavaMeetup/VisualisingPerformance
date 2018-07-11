db.PerfStats.aggregate([
     {$match: {'statName': 'img-large'}}
    ,{$bucket: {
      groupBy: "$timeTakenMillis",
      boundaries: [ 0, 200, 400, 600, 800, 1000 ],
      default: "Other",
      output: {
        "count": { $sum: 1 }
      }
    }}
])