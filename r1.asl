// r1.asl - Collector Robot 1

!iniciar.
+!iniciar <-
    .print("[r1] ============ COLLECTOR (r1) STARTING ============").

// Main plan
+!collect_garbage(X,Y,IX,IY) <-
    .print("[r1] New task: garbage at (", X, ",", Y, ")");
    !go_to(X, Y);
    !pick_up_garbage(X,Y);
    !go_to(IX, IY);
    drop(garb);
    .print("[r1] Garbage delivered to incinerator");
    .send(r2, achieve, incinerate_garbage);
    .wait(800); // Wait for incineration
    
    !move_from_incinerator(IX, IY);

    .send(coordinator, tell, task_complete(X,Y));
    .print("[r1] Task complete!").

// Plan: move to parking spot
+!move_from_incinerator(IX, IY) <-
    DropY = IY - 1; 
    DropX = IX;
    .print("[r1] Moving off incinerator to (",DropX,",",DropY,")");
    !go_to(DropX, DropY).

// --- Helpers ---

// Move
+!go_to(X, Y) : pos(r1, X, Y).
+!go_to(X, Y) <-
    move_towards(X, Y);
    .wait(300); 
    !go_to(X, Y).

// Pick up garbage
+!pick_up_garbage(X,Y) : carrying(r1) <-
    .print("[r1] Garbage collected!").
+!pick_up_garbage(X,Y) : not garbage(X,Y) & pos(r1,X,Y) <-
    .print("[r1] WARNING: Garbage not found at (",X,",",Y,")").
+!pick_up_garbage(X,Y) <-
    pick(garb);
    .wait(400);
    !pick_up_garbage(X,Y).