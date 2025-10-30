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
    .send(supervisor, tell, concluido(X,Y));
    .print("[r1] Tarefa concluída!").

// Movimento
+!ir_para(X, Y) : pos(r1, X, Y).
+!ir_para(X, Y) <-
    move_towards(X, Y);
    !ir_para(X, Y).

// Coleta com verificação de sucesso
+!pegar_lixo(X,Y) : carrying(r1) <-
    .print("[r1] Lixo coletado!").

+!pegar_lixo(X,Y) : not garbage(X,Y) & pos(r1,X,Y) <-
    .print("[r1] AVISO: Lixo não encontrado em (",X,",",Y,")").

+!pegar_lixo(X,Y) <-
    pick(garb);
    .wait(400);
    !pegar_lixo(X,Y).