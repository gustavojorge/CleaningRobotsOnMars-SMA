// coordinator.asl

!iniciar.
+!iniciar <-
    .print("[coordinator] ============ COORDINATOR STARTING ============");
    .print("[coordinator] Sequential collection system activated");
    .wait(1000);
    !!process_next_item.

// Main loop: process one item at a time
+!process_next_item <-
    sync_percepts; 
    
    .findall([X,Y,"garbage"], garbage(X,Y), AllGarbage);
    .findall([X,Y,"gold"], gold(X,Y), AllGold);
    .concat(AllGarbage, AllGold, AllItems); // Combina as listas
    
    !find_first_available(AllItems).

// If item list is empty, wait and retry
+!find_first_available([]) <-
    .print("[coordinator] >>> Grid clean! Waiting for new items...");
    .wait(2000);
    !!process_next_item.

// If item list is not empty, find first available item
+!find_first_available([[X,Y,Type]|Rest]) : processando(X,Y) <-
    // Item (X,Y) is busy, try next in list
    !find_first_available(Rest).

+!find_first_available([[X,Y,Type]|_]) : not processando(X,Y) <-
    // Item (X,Y) is free, process it
    !process_item(X,Y,Type). //"Type" is a flag to define if item is a garbage or a gold

// Plan: Process a single item
+!process_item(X,Y,Type) : pos(r1,X1,Y1) & pos(r3,X3,Y3) & pos(r2,IX,IY) <-
    +processando(X,Y); // Mark item as "in progress"
    
    .print("-----------------------------------------------------------------------------------");
    .print("[coordinator] PROCESSING: ", Type, " at (", X, ",", Y, ")");
    .print("[coordinator]   r1 at (", X1, ",", Y1, ")");
    .print("[coordinator]   r3 at (", X3, ",", Y3, ")");
    
    // Calculate distances
    !calculate_distance(X1,Y1,X,Y,IX,IY,D1);
    !calculate_distance(X3,Y3,X,Y,IX,IY,D3);
    .print("[coordinator]   Distance r1: ", D1, " | r3: ", D3);
    
    // Decide and dispatch
    !decide_and_dispatch(X,Y,IX,IY,D1,D3,Type);
    
    // Wait for completion message
    .print("[coordinator]   Waiting for completion...");
    .wait(task_complete(X,Y), 60000);
    
    -task_complete(X,Y); 
    -processando(X,Y);
    
    .print("[coordinator] Task (",X,",",Y,") complete");
    .print("-----------------------------------------------------------------------------------");
    
    .wait(1000);
    
    !!process_next_item.

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
+!decide_and_dispatch(X,Y,IX,IY,D1,D3,Type) : D1 <= D3 <-
    .print("[coordinator] DECISION: Assigning to r1");
    .send(r1, achieve, collect_item(X,Y,IX,IY,Type)).

+!decide_and_dispatch(X,Y,IX,IY,D1,D3,Type) : D3 < D1 <-
    .print("[coordinator] DECISION: Assigning to r3");
    .send(r3, achieve, collect_item(X,Y,IX,IY,Type)).

// Receives the MESSAGE from the robot
+task_complete(X,Y)[source(Ag)] <-
    .print("[coordinator] Confirmation from ", Ag, " for (",X,",",Y,")");
    +task_complete(X,Y). // Add belief to satisfy .wait

// Fallback plan: Error finding agent positions
+!process_item(X,Y,Type) <-
    .print("[coordinator] ERROR: Failed to process (",X,",",Y,") - positions not found");
    -processando(X,Y);
    .wait(1000);
    !!process_next_item.