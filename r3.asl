// r3.asl - Collector Robot 3

!iniciar.
+!iniciar <-
    .print("[r3] === COLLECTOR (r3) STARTING ===").

// Main plan
+!coletar_lixo(X,Y,IX,IY) <-
    .print("[r3] New task: garbage at (", X, ",", Y, ")");
    !ir_para(X, Y);
    !pegar_lixo(X,Y);
    !ir_para(IX, IY);
    drop(garb);
    .print("[r3] Garbage delivered to incinerator");
    .send(r2, achieve, incinerar_lixo);
    .wait(800); 
    
    !sair_do_incinerador(IX, IY);
    
    .send(supervisor, tell, concluido(X,Y));
    .print("[r3] Task complete!").

// Plan: move to parking spot
+!sair_do_incinerador(IX, IY) <-
    DropY = IY + 1; 
    DropX = IX;
    .print("[r3] Moving off incinerator to (",DropX,",",DropY,")");
    !ir_para(DropX, DropY).

// --- Helpers ---

// Move
+!ir_para(X, Y) : pos(r3, X, Y).
+!ir_para(X, Y) <-
    move_towards(X, Y);
    .wait(300);
    !ir_para(X, Y).

// Pick up garbage
+!pegar_lixo(X,Y) : carrying(r3) <-
    .print("[r3] Garbage collected!").
+!pegar_lixo(X,Y) : not garbage(X,Y) & pos(r3,X,Y) <-
    .print("[r3] WARNING: Garbage not found at (",X,",",Y,")").
+!pegar_lixo(X,Y) <-
    pick(garb);
    .wait(400);
    !pegar_lixo(X,Y).