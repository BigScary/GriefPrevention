# GriefPreventionPlus
While I was developing GriefPrevention-Cities, I found that GriefPrevention has lots of performance issues (expecially with MySQL database) and some limitation. Currently, most of MC servers are version 1.7.10, so I found very bad to not apply new fixes to Grief Prevention for MC1.7.10. This became GriefPreventionPlus!

Feedback are needed! If you found an issue, please report it!

###Installation
- If you've installed GriefPrevention, remove GriefPrevention jar from plugins folder
- Put GriefPreventionPlus jar into plugins folder

If an existing GriefPrevention database is found, a copy will migrate to GriefPreventionPlus.
Your GriefPrevention database won't be removed: you can rollback to GriefPrevention if you need!

###Major features
- GriefPreventionPlus's MC1.7.10 version contains last fixes from GriefPrevention's MC1.8 version!
- MySQL database is a requirement. Removed file based storage.
- Drastically improved database performances and reduced size: bigger servers will notice it!
- Overall speed improvements
- API improvements: all claims and subdivisions have an unique id
- Less waste of resources (RAM)
- Javadoc for extension developers! (planned)

####Notice
- GriefPrevention's extensions don't work with GriefPreventionPlus without some little change on the code. If needed, I will fork most important GriefPrevention's extension to make it work with GriefPreventionPlus! You can ask for it!
- GriefPrevention 10.6.2 commit is not applied. Untrust in top level claims won't untrust in subdivisions. You can remove a player from all your claims if you're not on a claim.