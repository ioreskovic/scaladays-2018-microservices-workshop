# ScalaDays 2018 Notes
## Domain Driven Design and Microservices
### Day 1
Events first. What happens in our case?  
What commands trigger those events?  
Aggregates emerge by themselves.  
Look for dynamic properties of our system.  
Aggregate (persistent entity) validates command. It takes command and produces either an error or event(s).  
Akka (Erlang?) blog: Let it crash!!!  
