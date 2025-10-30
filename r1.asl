// r1.asl - Robô coletor de lixo (Versão com Ações Reais)

!iniciar. // Objetivo inicial (pode ser definido no .mas2j)
+!iniciar : true <-
    .print("[r1] === ROBÔ COLETOR (r1) INICIANDO ===").

// Plano principal para coletar o lixo
+!coletar_lixo(X,Y,IX,IY) : true <-
    .print("[r1] Nova tarefa recebida! Lixo em (", X, ",", Y, "), incinerador em (", IX, ",", IY, ")");
    
    // 1. Vai até o lixo
    !ir_para(X, Y);
    .print("[r1] Chegou no lixo em (", X, ",", Y, ")");
    
    // 2. Pega o lixo (com retentativas)
    !pegar_lixo;
    
    // 3. Vai até o incinerador
    !ir_para(IX, IY);
    .print("[r1] Chegou no incinerador (", IX, ",", IY, ")");
    
    // 4. Solta o lixo para o r2
    drop(garb);
    .print("[r1] Lixo entregue ao incinerador!");
    .send(r2, achieve, incinerar_lixo); // Manda o r2 queimar
    
    // 5. Retorna ao local original do lixo (como solicitado)
    !ir_para(X, Y);
    .print("[r1] Retornou à posição original do lixo (", X, ",", Y, ")");
    
    // 6. Notifica o supervisor
    .print("[r1] Notificando supervisor sobre conclusão...");
    .send(supervisor, tell, concluido(X,Y)).

// --- PLANOS AUXILIARES ---

// Plano para se mover até um local
+!ir_para(X, Y) : pos(r1, X, Y). // Já estou no local
+!ir_para(X, Y) <-
    move_towards(X, Y);
    .wait(300); // Pausa para o ambiente atualizar
    !ir_para(X, Y).

// Plano para pegar o lixo (tenta até conseguir)
+!pegar_lixo : carrying(r1). // Já estou carregando
+!pegar_lixo <-
    .print("[r1] Tentando pegar o lixo...");
    pick(garb);
    .wait(300); // Pausa
    !pegar_lixo.