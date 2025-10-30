// r3.asl - Segundo robô coletor

!iniciar.
+!iniciar <-
    .print("[r3] === ROBÔ COLETOR (r3) INICIANDO ===").

// Plano principal
+!coletar_lixo(X,Y,IX,IY) <-
    .print("[r3] Nova tarefa: lixo em (", X, ",", Y, ")");
    !ir_para(X, Y);
    !pegar_lixo(X,Y);
    !ir_para(IX, IY);
    drop(garb);
    .print("[r3] Lixo entregue ao incinerador");
    .send(r2, achieve, incinerar_lixo);
    .wait(800); // Aguarda incineração
    
    // --- MUDANÇA: SAIR DA POSIÇÃO DO INCINERADOR ---
    !sair_do_incinerador(IX, IY);
    // --- FIM DA MUDANÇA ---
    
    .send(supervisor, tell, concluido(X,Y));
    .print("[r3] Tarefa concluída!").

// --- NOVO PLANO (MODIFICADO) ---
// Move para uma posição adjacente (ex: Y+1)
+!sair_do_incinerador(IX, IY) <-
    // --- CORREÇÃO DO BUG ---
    // r1 estaciona em (3,2). r3 deve estacionar em outro local.
    DropY = IY + 1; // (3,3) -> (3,4)
    // --- FIM DA CORREÇÃO ---
    DropX = IX;
    .print("[r3] Saindo da posição do incinerador para (",DropX,",",DropY,")");
    !ir_para(DropX, DropY).
// --- FIM DO NOVO PLANO ---

// Movimento (CORRIGIDO)
+!ir_para(X, Y) : pos(r3, X, Y).
+!ir_para(X, Y) <-
    move_towards(X, Y);
    .wait(300); // <-- Adiciona uma pausa para o movimento ocorrer
    !ir_para(X, Y). // Tenta novamente (agora recursão controlada)

// Coleta com verificação de sucesso
+!pegar_lixo(X,Y) : carrying(r3).
+!pegar_lixo(X,Y) : not garbage(X,Y) & pos(r3,X,Y) <-
    .print("[r3] AVISO: Lixo não encontrado em (",X,",",Y,")").
+!pegar_lixo(X,Y) <-
    pick(garb);
    .wait(400);
    !pegar_lixo(X,Y).