// supervisor.asl (Corrigido)

!iniciar.
+!iniciar <-
    .print("[supervisor] === SUPERVISOR INICIANDO ===");
    .print("[supervisor] Sistema de coleta sequencial ativado");
    .wait(1000);
    !!processar_proximo_lixo.

// Loop principal: processa um lixo por vez
+!processar_proximo_lixo <-
    // --- CORREÇÃO: FORÇA A SINCRONIZAÇÃO DAS PERCEPÇÕES ---
    // Esta ação força o agente a ler o estado mais recente do MarsEnv
    // ANTES de executar o .findall, limpando lixos "fantasmas".
    sync_percepts; 
    
    .findall([X,Y], garbage(X,Y), TodosLixos);
    !pegar_primeiro_disponivel(TodosLixos).

// Se há lixos, pega o primeiro que NÃO está sendo processado
+!pegar_primeiro_disponivel([]) <-
    .print("[supervisor] >>> Ambiente limpo! Aguardando novos lixos...");
    .wait(2000);
    !!processar_proximo_lixo.

+!pegar_primeiro_disponivel([[X,Y]|Resto]) : processando(X,Y) <-
    // Lixo (X,Y) está sendo processado, tenta o próximo da lista
    !pegar_primeiro_disponivel(Resto).

+!pegar_primeiro_disponivel([[X,Y]|_]) : not processando(X,Y) <-
    // Lixo (X,Y) está livre, processa ele
    !processar_lixo(X,Y).

// Processa um único lixo
+!processar_lixo(X,Y) : pos(r1,X1,Y1) & pos(r3,X3,Y3) & pos(r2,IX,IY) <-
    +processando(X,Y); // Marca lixo como "sendo processado"
    
    .print("-------------------------------------------");
    .print("[supervisor] PROCESSANDO: Lixo em (", X, ",", Y, ")");
    .print("[supervisor]   r1 em (", X1, ",", Y1, ")");
    .print("[supervisor]   r3 em (", X3, ",", Y3, ")");
    
    // Calcula distâncias
    !calcular_dist(X1,Y1,X,Y,IX,IY,D1);
    !calcular_dist(X3,Y3,X,Y,IX,IY,D3);
    .print("[supervisor]   Distância r1: ", D1, " | r3: ", D3);
    
    // Decide e envia
    !decidir_e_enviar(X,Y,IX,IY,D1,D3);
    
    // AGUARDA conclusão
    .print("[supervisor]   Aguardando confirmação...");
    .wait(concluido(X,Y), 60000);
    
    // Limpa estados
    -concluido(X,Y); // Limpa a mensagem de crença
    -processando(X,Y); // Libera o lixo para (caso reapareça)
    
    .print("[supervisor] ✓ Lixo (",X,",",Y,") concluído");
    .print("-------------------------------------------");
    
    // Pequeno delay para robôs finalizarem
    .wait(1000);
    
    // Processa próximo
    !!processar_proximo_lixo.

// Calcula distância Manhattan
+!calcular_dist(XR,YR,GX,GY,IX,IY,Dist) <-
    !abs(XR-GX, DX1);
    !abs(YR-GY, DY1);
    !abs(GX-IX, DX2);
    !abs(GY-IY, DY2);
    Dist = DX1 + DY1 + DX2 + DY2.

+!abs(N, R) : N >= 0 <- R = N.
+!abs(N, R) : N < 0 <- R = -N.

// Decide qual robô
+!decidir_e_enviar(X,Y,IX,IY,D1,D3) : D1 <= D3 <-
    .print("[supervisor] DECISÃO: r1");
    .send(r1, achieve, coletar_lixo(X,Y,IX,IY)).

+!decidir_e_enviar(X,Y,IX,IY,D1,D3) : D3 < D1 <-
    .print("[supervisor] DECISÃO: r3");
    .send(r3, achieve, coletar_lixo(X,Y,IX,IY)).

// Recebe confirmação (para satisfazer o .wait)
+concluido(X,Y)[source(Ag)] <-
    .print("[supervisor] ✓ Confirmação de ", Ag, " para (",X,",",Y,")").

// Erro: não encontrou posições (plano de fallback)
+!processar_lixo(X,Y) <-
    .print("[supervisor] ERRO: Não consegui processar (",X,",",Y,") - posições não encontradas");
    -processando(X,Y);
    .wait(1000);
    !!processar_proximo_lixo.