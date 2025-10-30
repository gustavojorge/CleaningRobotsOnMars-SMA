// r2.asl - Incinerator/Vault Agent

!iniciar.
+!iniciar : true <-
    .print("[r2] ============ INCINERATOR/VAULT STARTING ============").

// Burn upon request
+!incinerate_garbage : true <-
    .print("[r2] Incineration task received...");
    .wait(500);
    burn;
    .print("[r2] Burn attempt finished.").

// Store upon request (Novo)
+!store_gold : true <-
    .print("[r2] Store gold task received...");
    .wait(500);
    store; 
    .print("[r2] Gold stored successfully!").

// Automatic reaction to items
+item_at_incinerator(garbage) <-
    .print("[r2] Garbage detected at location! Incinerating...");
    burn;
    .print("[r2] Burn attempt finished.").

+item_at_incinerator(gold) <-
    .print("[r2] Gold detected at location! Storing...");
    store;
    .print("[r2] Gold stored successfully!").