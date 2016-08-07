# This is a reactive application built using Scala, play and Akka actors
## Todo: currently it is using old Play Iteratee and Enumerations, we need to migrate this to akka streams.

the unique thing about this application is that the actor reads the tweets only 
when somone is consuming them. If there is no one consuming the tweets then the 
server won't poll or fetch from twitter API.

