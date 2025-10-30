// r3.asl - Segundo robô coletor de lixo (Versão com Ações Reais)

!iniciar.
+!iniciar : true <-
    .print("[r3] === ROBÔ COLETOR (r3) INICIANDO ===").

// Plano principal para coletar o lixo
+!coletar_lixo(X,Y,IX,IY) : true <-
    .print("[r3] Nova tarefa recebida! Lixo em (", X, ",", Y, "), incinerador em (", IX, ",", IY, ")");
    
    // 1. Vai até o lixo
    !ir_para(X, Y);
    .print("[r3] Chegou no lixo em (", X, ",", Y, ")");
    
    // 2. Pega o lixo (com retentativas)
    !pegar_lixo;
    
    // 3. Vai até o incinerador
    !ir_para(IX, IY);
    .print("[r3] Chegou no incinerador (", IX, ",", IY, ")");
    
    // 4. Solta o lixo para o r2
    drop(garb);
    .print("[r3] Lixo entregue ao incinerador!");
    .send(r2, achieve, incinerar_lixo); // Manda o r2 queimar
    
    // 5. Retorna ao local original do lixo (como solicitado)
    !ir_para(X, Y);
    .print("[r3] Retornou à posição original do lixo (", X, ",", Y, ")");
    
    // 6. Notifica o supervisor
    .print("[r3] Notificando supervisor sobre conclusão...");
    .send(supervisor, tell, concluido(X,Y)).

// --- PLANOS AUXILIARES ---

// Plano para se mover até um local
+!ir_para(X, Y) : pos(r3, X, Y). // Já estou no local
+!ir_para(X, Y) <-
    move_towards(X, Y);
    .wait(300); // Pausa para o ambiente atualizar
    !ir_para(X, Y).

// Plano para pegar o lixo (tenta até conseguir)
+!pegar_lixo : carrying(r3). // Já estou carregando
+!pegar_lixo <-
    .print("[r3] Tentando pegar o lixo...");
    pick(garb);
    .wait(300); // Pausa
    !pegar_lixo.