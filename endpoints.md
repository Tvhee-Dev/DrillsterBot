# Used Endpoints
## Repertoire
### /api/3/repertoire
Returns reperoire object (not to be confused with [v2 repertoire object](https://www.drillster.com/info/developers/api/2.1.1/objects/drillables) (response given by /api/2.1.1/repertoire))
```json
{
    "playableRenditions": [
        /* list of "drillable" objects */
    ],
    "pagination":{
        "total": /* (int) total */,
        "lastOnPage": /* (id) last drill id on page */,
        "moreAvailable": /* (bool) */
    }
}
```
For more explanaition on drillable object, see [official api documentation](https://www.drillster.com/info/developers/api/2.1.1/objects/drillable).

---
## Playable results
### /api/3/results?playable={playable}
Returns all drills and their proficiencies in specific course
```json
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
    "results": [
        /* List of "Drills" objects, shown in website as steps */
        {
            "type": "DRILLS",
            "id": /* (str) step id */,
            "drills": [
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
All objects except "drills" are documented in api v2 documentation

---
## [/api/2.1.1/playable/{id}](https://www.drillster.com/info/developers/api/2.1.1/endpoints/get-playable)
---
## [/api/2.1.1/question/{id}](https://www.drillster.com/info/developers/api/2.1.1/endpoints/get-question)
---
## [/api/2.1.1/answer/{refid}](https://www.drillster.com/info/developers/api/2.1.1/endpoints/put-answer)