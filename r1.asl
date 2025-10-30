// r1.asl - Collector Robot 1

!iniciar.
+!iniciar <-
    .print("[r1] ============ COLLECTOR (r1) STARTING ============").

// Main plan
+!collect_item(X,Y,IX,IY,Type) <-
    .print("[r1] New task: ", Type, " at (", X, ",", Y, ")");
    !go_to(X, Y);
    !pick_up_item(X,Y);
    !go_to(IX, IY);
    drop; 
    .print("[r1] Item delivered to incinerator/vault");
    
    !dispatch_to_incinerator(Type);
    
    .wait(800); // Wait for r2
    
    !move_from_incinerator(IX, IY);

    .send(coordinator, tell, task_complete(X,Y));
    .print("[r1] Task complete!").

// Dispatch plan
+!dispatch_to_incinerator("garbage") <-
    .print("[r1] Telling r2 to BURN garbage");
    .send(r2, achieve, incinerate_garbage).
+!dispatch_to_incinerator("gold") <-
    .print("[r1] Telling r2 to STORE gold");
    .send(r2, achieve, store_gold).

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

// Pick up item
+!pick_up_item(X,Y) : carrying(garbage) | carrying(gold) <-
    .print("[r1] Item collected!").
+!pick_up_item(X,Y) : not item(garbage) & not item(gold) & pos(r1,X,Y) <-
    .print("[r1] WARNING: Item not found at (",X,",",Y,")").
+!pick_up_item(X,Y) <-
    pick;
    .wait(400);
    !pick_up_item(X,Y).