// r1.asl - Collector Robot 1

!iniciar.
+!iniciar <-
    .print("[r1] === COLLECTOR (r1) STARTING ===").

// Main plan
+!coletar_lixo(X,Y,IX,IY) <-
    .print("[r1] New task: garbage at (", X, ",", Y, ")");
    !ir_para(X, Y);
    !pegar_lixo(X,Y);
    !ir_para(IX, IY);
    drop(garb);
    .print("[r1] Garbage delivered to incinerator");
    .send(r2, achieve, incinerar_lixo);
    .wait(800); // Wait for incineration
    
    // Move to parking spot
    !sair_do_incinerador(IX, IY);

    .send(supervisor, tell, concluido(X,Y));
    .print("[r1] Task complete!").

// Plan: move to parking spot
+!sair_do_incinerador(IX, IY) <-
    DropY = IY - 1; // (3,3) -> (3,2)
    DropX = IX;
    .print("[r1] Moving off incinerator to (",DropX,",",DropY,")");
    !ir_para(DropX, DropY).

// --- Helpers ---

// Move
+!ir_para(X, Y) : pos(r1, X, Y).
+!ir_para(X, Y) <-
    move_towards(X, Y);
    .wait(300); // Pause for movement to occur
    !ir_para(X, Y). // Recursive move call

// Pick up garbage
+!pegar_lixo(X,Y) : carrying(r1) <-
    .print("[r1] Garbage collected!").
+!pegar_lixo(X,Y) : not garbage(X,Y) & pos(r1,X,Y) <-
    .print("[r1] WARNING: Garbage not found at (",X,",",Y,")").
+!pegar_lixo(X,Y) <-
    pick(garb);
    .wait(400);
    !pegar_lixo(X,Y).