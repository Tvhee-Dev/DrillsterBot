# Used Endpoints
## Repertoire
### /api/3/repertoire
Returns a reperoire object (not to be confused with the [v2 repertoire object](https://www.drillster.com/info/developers/api/2.1.1/objects/drillables) (response given by /api/2.1.1/repertoire))
```jsonc
{
    "playableRenditions":
    [
        /* list of "drillable" objects */
    ],
    "pagination":
    {
        "total": /* (int) total */,
        "lastOnPage": /* (id) last drill id on page */,
        "moreAvailable": /* (bool) */
    }
}
```
For more explanaition on the drillable object, see the [official api documentation](https://www.drillster.com/info/developers/api/2.1.1/objects/drillable).

---
## Playable results
### /api/3/results?playable={playable}
Returns all Drills and their proficiencies in specific course.
```jsonc
{
    "playable": /* playable object */,
    "proficiency": /* proficiency object */,
    "highestProficency": /* proficiency object (highest proficiency reached) */,
    "eventualProficiency": /* proficiency object (bottom value for proficiency after a while)*/,
    "timeSpent": /* (int) spent time in (presumably) seconds */,
    "expectedDuration": /* (int) expected time to reach 100% */,
    "lastPracticed": /* (str, ISO 8601 UTC) last time user practiced */,
    "added": /* (str, ISO 8601 UTC) time course got added to account*/,
    "progress": /* unknown, not related to proficiency */,
    "results":
    [
        /* List of "Drills" objects, shown in website as steps */
        {
            "type": "DRILLS",
            "id": /* (str) step id */,
            "drills":
            [
                /* List of drills in step */
                {
                    "playable": /* playable object */,
                    "proficiency": /* proficiency object */,
                    "highestProficency": /* proficiency object (highest proficiency reached) */,
                    "eventualProficiency": /* proficiency object (bottom value for proficiency after a while)*/,
                    "timeSpent": /* (int) spent time in (presumably) seconds */,
                    "expectedDuration": /* (int) expected time to reach 100% */,
                    "lastPracticed": /* (str, ISO 8601 UTC) last time user practiced */,
                    "added": /* (str, ISO 8601 UTC) time course got added to account*/
                } /* repeat for each drill in step*/
            ]
        }, /* repeat for each step */
    ]
}
```

---
## Playable information
### /api/2.1.1/playable/{playable_id}
Get information about a specific playable.

```jsonc
{
  "id": /* id of the playable object */,
  "type": /* DRILL, COURSE or TEST /*,
  "name": /* name of the playable object */,
  "icon":
  {
    "type": /* image/png, audo/mp, video/peg, application/pdf, etc */,
    "url": /* the icon's url */
  }
}
```
---
## Question information
### /api/2.1.1/question/{drill}
Generate a new question for a Drill.

```jsonc
{
  "question": {
    "reference": /* the question id */,
    "sequenceNr": /* the question's sequence */,
    "ask":
    {
      "name": /* the question's column */,
      "term":
      {
        "value": "The question itself"
      }
    }
  }
}
```
---

## Answer information
### /api/2.1.1/answer/{reference}

```jsonc
{
  "proficiency":
  {
    "overall": /* percentage after answering */
  },
  "evaluation":
   {
    "result": /* INCORRECT or CORRECT */
    "termEvaluations":
    /* list of correct answers */
    [
      {
        "value": /* the correct answer */,
        "result": /* INCORRECT or CORRECT */
      }
    ]
  }
}
```
---
All objects except "drills" are documented in the [api v2 documentation](https://www.drillster.com/info/developers/api/2.1.1/)
