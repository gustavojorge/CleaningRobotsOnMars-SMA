// supervisor.asl
// Gerencia a fila de lixos, atribuindo um de cada vez e aguardando a conclusão.
// Versão corrigida: Sem ".if", sem "is", usando "compute_distances" e com ";".

!iniciar.

+!iniciar <-
    .print("[supervisor] === SUPERVISOR INICIANDO ===");
    .print("[supervisor] Sistema de coleta sequencial ativado");
    !!processar_proximo_lixo.

// --- LOOP PRINCIPAL ---

// 1. Encontra lixos e chama o verificador
+!processar_proximo_lixo <-
    .findall([X,Y], garbage(X,Y), Lixos); // Encontra lixos não atribuídos
    .length(Lixos, N);
    !verificar_lista(Lixos, N). // Chama planos auxiliares para decisão

// 2.A. Verificador: Se houver lixos (N > 0), processa o primeiro
+!verificar_lista(Lixos, N) : N > 0 <-
    Lixos = [[GX, GY] | _]; // Pega o primeiro lixo da lista
    !atribuir_tarefa(GX, GY).

// 2.B. Verificador: Se não houver lixos (N = 0), espera e reinicia
+!verificar_lista(Lixos, 0) <-
    .print("[supervisor] >>> Ambiente limpo! Aguardando novos lixos...");
    .wait(5000);
    !!processar_proximo_lixo. // Reinicia o loop

// 3. Atribui a tarefa: calcula distâncias e chama o plano de decisão
+!atribuir_tarefa(GX, GY) : pos(r1, X1, Y1) & pos(r3, X3, Y3) & pos(r2, IX, IY) <-
    .print("-------------------------------------------");
    .print("[supervisor] PROCESSANDO: Lixo em (", GX, ",", GY, ")");
    .print("[supervisor]   r1 em (", X1, ",", Y1, ")");
    .print("[supervisor]   r3 em (", X3, ",", Y3, ")");
    .print("[supervisor]   Incinerador em (", IX, ",", IY, ")");
    
    // 1. Chama a ação do ambiente
    compute_distances(X1, Y1, X3, Y3, GX, GY, IX, IY, D1, D3);
    .wait(500);
    // 2. Marca o lixo como atribuído NO AMBIENTE
    task_assigned(GX, GY);
    
    // 3. Chama planos de decisão (que agora imprimirão os valores)
    !decidir_e_enviar(GX, GY, IX, IY, D1, D3);
    
    .print("[supervisor]   Aguardando confirmação de conclusão da tarefa...");
    +aguardando_conclusao(GX, GY).

// 4.A. Decisão: r1 é mais próximo ou igual
+!decidir_e_enviar(GX, GY, IX, IY, D1, D3) : D1 <= D3 <-
    // --- CORREÇÃO: .print movido para cá ---
    .print("[supervisor] DECISÃO: Atribuindo para r1");
    .send(r1, achieve, coletar_lixo(GX, GY, IX, IY)).

// 4.B. Decisão: r3 é mais próximo
+!decidir_e_enviar(GX, GY, IX, IY, D1, D3) : D3 < D1 <-
    // --- CORREÇÃO: .print movido para cá ---
    .print("[supervisor]   Distância r1: ", D1, " | Distância r3: ", D3);
    .print("[supervisor] DECISÃO: Atribuindo para r3");
    .send(r3, achieve, coletar_lixo(GX, GY, IX, IY)).

// 5. Recebe a confirmação e reinicia o loop para o *próximo* lixo
+concluido(X, Y)[source(Ag)] : aguardando_conclusao(X, Y) <-
    .print("[supervisor]   ✓ Conclusão recebida de ", Ag, " para o lixo (", X, ",", Y, ")");
    .print("-------------------------------------------");
    -aguardando_conclusao(X, Y); // Remove a crença
    .wait(1000); // Pausa antes de buscar o próximo
    !!processar_proximo_lixo. // Reinicia o loop principal