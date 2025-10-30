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
    .send(supervisor, tell, concluido(X,Y));
    .print("[r3] Tarefa concluída!").

// Movimento
+!ir_para(X, Y) : pos(r3, X, Y).
+!ir_para(X, Y) <-
    move_towards(X, Y);
    !ir_para(X, Y).

// Coleta com verificação de sucesso
+!pegar_lixo(X,Y) : carrying(r3) <-
    .print("[r3] Lixo coletado!").

+!pegar_lixo(X,Y) : not garbage(X,Y) & pos(r3,X,Y) <-
    .print("[r3] AVISO: Lixo não encontrado em (",X,",",Y,")").

+!pegar_lixo(X,Y) <-
    pick(garb);
    .wait(400);
    !pegar_lixo(X,Y).