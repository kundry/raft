Project5: Raft Implementation
My implementation of raft will be divided in the two following parts:

Part I: Log Replication
-	Designing the structure of the log of the operations
-	Designing the structure of the state machine
-	Defining the append entries RPCs (end points) needed for receiving entries to be added to the logs and receiving committed entries to be applied to the state machines.
-	Coding all the logic of receiving and processing the append entries defined above. This includes:
    1) The leader receives a request and the request it is logged in its log.
    2) The leader sends append entries to all the followers and it waits for the response of at least half of the followers in the cluster.  (Under the follower perspective the safety check has to be implemented and the proper response has to be returned to the leader)
    3) Once the leader gets the responses needed from the followers, it will update its state machine and returns to the client.
    4) RPCs of committed entry will be sent to the followers so they can proceed to update their own state machines. (Be aware of: If a leader wants to commit changes to the state machines, he has to wait for at least one new entry from its term to be stored on the majority of the servers).

Part II: Leader Election
-	Defining the append entry RPC (end point) needed to implement the heartbeat message sent by the leader to the followers to indicate that he is still alive.
-	Defining the request vote RPC (end point) needed to implement the message sent by the followers asking for votes to the rest of the active nodes.
-	Coding the election process. This means that the follower that initiates the election process should:
    1) Increment its term.
    2) Change its state to candidate.
    3) Vote for it self.
    4) Send request vote RPCs to all the nodes (if no response retry).
	    4.1) When votes from the majority are received the candidate becomes leader and send heartbeats to all nodes.
	    4.2) When a heartbeat from a valid leader is received the candidate has to go back to the follower state.
	    4.3) When a request vote RPC is received, the candidate has to check if it has voted already for another candidate and if it has not, it has to compare its log with the log of the candidate and decide if it will give the vote or not.
-	Reconciling Data. The leader tries to keep all logs consistent with its own. The next_index variable is introduced. There is one of those variables per follower to keep track of what has been sent to the followers and reach to the point at which the log is consistent with the leader’s one and all the log after that point has to be restored.


I am going to start with Part I and my goal is to complete all Part I for the first CheckPoint.


