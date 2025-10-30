// r1.asl - Robô coletor

!iniciar.
+!iniciar <-
    .print("[r1] === ROBÔ COLETOR (r1) INICIANDO ===").

// Plano principal
+!coletar_lixo(X,Y,IX,IY) <-
    .print("[r1] Nova tarefa: lixo em (", X, ",", Y, ")");
    !ir_para(X, Y);
    !pegar_lixo(X,Y);
    !ir_para(IX, IY);
    drop(garb);
    .print("[r1] Lixo entregue ao incinerador");
    .send(r2, achieve, incinerar_lixo);
    .wait(800); // Aguarda incineração
    
    // --- MUDANÇA: SAIR DA POSIÇÃO DO INCINERADOR ---
    !sair_do_incinerador(IX, IY);
    // --- FIM DA MUDANÇA ---

    .send(supervisor, tell, concluido(X,Y));
    .print("[r1] Tarefa concluída!").

// --- NOVO PLANO ---
// Move para uma posição adjacente (ex: Y-1)
+!sair_do_incinerador(IX, IY) <-
    DropY = IY - 1; // (3,3) -> (3,2)
    DropX = IX;
    .print("[r1] Saindo da posição do incinerador para (",DropX,",",DropY,")");
    !ir_para(DropX, DropY).
// --- FIM DO NOVO PLANO ---

// Movimento (CORRIGIDO)
+!ir_para(X, Y) : pos(r1, X, Y).
+!ir_para(X, Y) <-
    move_towards(X, Y);
    .wait(300); // <-- Adiciona uma pausa para o movimento ocorrer
    !ir_para(X, Y). // Tenta novamente (agora recursão controlada)

// Coleta com verificação de sucesso
+!pegar_lixo(X,Y) : carrying(r1) <-
    .print("[r1] Lixo coletado!").
+!pegar_lixo(X,Y) : not garbage(X,Y) & pos(r1,X,Y) <-
    .print("[r1] AVISO: Lixo não encontrado em (",X,",",Y,")").
+!pegar_lixo(X,Y) <-
    pick(garb);
    .wait(400);
    !pegar_lixo(X,Y).