// supervisor.asl

!iniciar.
+!iniciar <-
    .print("[supervisor] === SUPERVISOR STARTING ===");
    .print("[supervisor] Sequential collection system activated");
    .wait(1000);
    !!processar_proximo_lixo.

// Main loop: process one garbage item at a time
+!processar_proximo_lixo <-
    sync_percepts; 
    
    .findall([X,Y], garbage(X,Y), TodosLixos);
    !pegar_primeiro_disponivel(TodosLixos).

// If garbage list is empty, wait and retry
+!pegar_primeiro_disponivel([]) <-
    .print("[supervisor] >>> Grid clean! Waiting for new garbage...");
    .wait(2000);
    !!processar_proximo_lixo.

// If garbage list is not empty, find first available item
+!pegar_primeiro_disponivel([[X,Y]|Resto]) : processando(X,Y) <-
    // Item (X,Y) is busy, try next in list
    !pegar_primeiro_disponivel(Resto).

+!pegar_primeiro_disponivel([[X,Y]|_]) : not processando(X,Y) <-
    // Item (X,Y) is free, process it
    !processar_lixo(X,Y).

// Plan: Process a single garbage item
+!processar_lixo(X,Y) : pos(r1,X1,Y1) & pos(r3,X3,Y3) & pos(r2,IX,IY) <-
    +processando(X,Y); // Mark item as "in progress"
    
    .print("-----------------------------------------------------------------------------------");
    .print("[supervisor] PROCESSING: Garbage at (", X, ",", Y, ")");
    .print("[supervisor] r1 at (", X1, ",", Y1, ")");
    .print("[supervisor] r3 at (", X3, ",", Y3, ")");
    
    // Calculate distances
    !calcular_dist(X1,Y1,X,Y,IX,IY,D1);
    !calcular_dist(X3,Y3,X,Y,IX,IY,D3);
    .print("[supervisor] Distance r1: ", D1, " | r3: ", D3);
    
    // Decide and dispatch
    !decidir_e_enviar(X,Y,IX,IY,D1,D3);
    
    // Wait for completion message
    .print("[supervisor] Waiting for completion...");
    .wait(concluido(X,Y), 60000);
    
    // Clean up states
    -concluido(X,Y); 
    -processando(X,Y);
    
    .print("[supervisor] Task (",X,",",Y,") complete");
    .print("-----------------------------------------------------------------------------------");
    
    .wait(1000);
    
    !!processar_proximo_lixo.

// Plan: Calculate Manhattan distance
+!calcular_dist(XR,YR,GX,GY,IX,IY,Dist) <-
    !abs(XR-GX, DX1);
    !abs(YR-GY, DY1);
    !abs(GX-IX, DX2);
    !abs(GY-IY, DY2);
    Dist = DX1 + DY1 + DX2 + DY2.

+!abs(N, R) : N >= 0 <- R = N.
+!abs(N, R) : N < 0 <- R = -N.

// Plan: Decide agent
+!decidir_e_enviar(X,Y,IX,IY,D1,D3) : D1 <= D3 <-
    .print("[supervisor] DECISION: Assigning to r1");
    .send(r1, achieve, coletar_lixo(X,Y,IX,IY)).

+!decidir_e_enviar(X,Y,IX,IY,D1,D3) : D3 < D1 <-
    .print("[supervisor] DECISION: Assigning to r3");
    .send(r3, achieve, coletar_lixo(X,Y,IX,IY)).

+concluido(X,Y)[source(Ag)] <-
    .print("[supervisor] Confirmation from ", Ag, " for (",X,",",Y,")");
    +concluido(X,Y). 

// Fallback plan: Error finding agent positions
+!processar_lixo(X,Y) <-
    .print("[supervisor] ERROR: Failed to process (",X,",",Y,") - positions not found");
    -processando(X,Y);
    .wait(1000);
    !!processar_proximo_lixo.