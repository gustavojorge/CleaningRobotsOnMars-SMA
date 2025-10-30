// r2.asl - Incinerator Agent

!iniciar.
+!iniciar : true <-
    .print("[r2] ============ INCINERATOR STARTING ============").

// Burn upon request
+!incinerate_garbage : true <-
    .print("[r2] Incineration task received...");
    .wait(500);
    burn(garb);
    .print("[r2] Garbage incinerated successfully!").

// Burn garbage dropped at location
+garbage(r2) <-
    .print("[r2] Garbage detected at incinerator! Let's incinerate...");
    burn(garb);
    .print("[r2] Garbage incinerated successfully!").