// r2.asl - Incinerador de lixo (Versão Real)

!iniciar.
+!iniciar : true <-
    .print("[r2] === INCINERADOR INICIANDO ===").

// Queima ao receber comando
+!incinerar_lixo : true <-
    .print("[r2] Recebido lixo para incineração...");
    .wait(500);
    burn(garb);
    .print("[r2] Lixo incinerado com sucesso!").

// Queima lixo solto na posição dele
+garbage(r2) <-
    .print("[r2] Lixo detectado na minha posição! Incinerando...");
    burn(garb);
    .print("[r2] Lixo incinerado com sucesso!").
