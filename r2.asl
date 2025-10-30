// r2.asl - Incinerador de lixo (Versão com Ação Real)

!iniciar.
+!iniciar : true <-
    .print("[r2] === INCINERADOR INICIANDO ===").

// Plano para incinerar quando o r1 ou r3 avisar
+!incinerar_lixo : true <-
    .print("[r2] Recebido lixo para incineração...");
    .wait(500); // Simula o tempo de queima
    burn(garb); // Ação real de queimar
    .print("[r2] Lixo incinerado com sucesso!").

// Plano reativo (Bônus): Se o lixo for solto na posição dele, ele queima
+garbage(r2) <-
    .print("[r2] Lixo detectado na minha posição! Incinerando...");
    burn(garb);
    .print("[r2] Lixo incinerado com sucesso!").