// coordinator.asl (was supervisor.asl)

!iniciar.
+!iniciar <-
    .print("[coordinator] === COORDINATOR STARTING ===");
    .print("[coordinator] Sequential collection system activated");
    .wait(1000);
    !!process_next_garbage.

// Main loop: process one garbage item at a time
+!process_next_garbage <-
    // This action forces the agent to read the latest state from MarsEnv
    sync_percepts; 
    
    .findall([X,Y], garbage(X,Y), AllGarbage);
    !find_first_available(AllGarbage).

// If garbage list is empty, wait and retry
+!find_first_available([]) <-
    .print("[coordinator] >>> Grid clean! Waiting for new garbage...");
    .wait(2000);
    !!process_next_garbage.

// If garbage list is not empty, find first available item
+!find_first_available([[X,Y]|Rest]) : processando(X,Y) <-
    // Item (X,Y) is busy, try next in list
    !find_first_available(Rest).

+!find_first_available([[X,Y]|_]) : not processando(X,Y) <-
    // Item (X,Y) is free, process it
    !process_garbage(X,Y).

// Plan: Process a single garbage item
+!process_garbage(X,Y) : pos(r1,X1,Y1) & pos(r3,X3,Y3) & pos(r2,IX,IY) <-
    +processando(X,Y); // Mark item as "in progress"
    
    .print("-----------------------------------------------------------------------------------");
    .print("[coordinator] PROCESSING: Garbage at (", X, ",", Y, ")");
    .print("[coordinator] r1 at (", X1, ",", Y1, ")");
    .print("[coordinator] r3 at (", X3, ",", Y3, ")");
    
    // Calculate distances
    !calculate_distance(X1,Y1,X,Y,IX,IY,D1);
    !calculate_distance(X3,Y3,X,Y,IX,IY,D3);
    .print("[coordinator] Distance r1: ", D1, " | r3: ", D3);
    
    // Decide and dispatch
    !decide_and_dispatch(X,Y,IX,IY,D1,D3);
    
    // Wait for completion message
    .print("[coordinator] Waiting for completion...");
    .wait(task_complete(X,Y), 60000);
    
    // Clean up states
    -task_complete(X,Y); 
    -processando(X,Y); // Free up this garbage ID
    
    .print("[coordinator] Task (",X,",",Y,") complete");
    .print("-----------------------------------------------------------------------------------");
    
    .wait(1000);
    
    // Process next
    !!process_next_garbage.

// Plan: Calculate Manhattan distance
+!calculate_distance(XR,YR,GX,GY,IX,IY,Dist) <-
    !abs(XR-GX, DX1);
    !abs(YR-GY, DY1);
    !abs(GX-IX, DX2);
    !abs(GY-IY, DY2);
    Dist = DX1 + DY1 + DX2 + DY2.

+!abs(N, R) : N >= 0 <- R = N.
+!abs(N, R) : N < 0 <- R = -N.

// Plan: Decide agent
+!decide_and_dispatch(X,Y,IX,IY,D1,D3) : D1 <= D3 <-
    .print("[coordinator] DECISION: Assigning to r1");
    .send(r1, achieve, collect_garbage(X,Y,IX,IY)).

+!decide_and_dispatch(X,Y,IX,IY,D1,D3) : D3 < D1 <-
    .print("[coordinator] DECISION: Assigning to r3");
    .send(r3, achieve, collect_garbage(X,Y,IX,IY)).

// Receives the MESSAGE from the robot
// and ADDS the BELIEF that .wait is waiting for.
+task_complete(X,Y)[source(Ag)] <-
    .print("[coordinator] Confirmation from ", Ag, " for (",X,",",Y,")");
    +task_complete(X,Y).

// Fallback plan: Error finding agent positions
+!process_garbage(X,Y) <-
    .print("[coordinator] ERROR: Failed to process (",X,",",Y,") - positions not found");
    -processando(X,Y);
    .wait(1000);
    !!process_next_garbage.