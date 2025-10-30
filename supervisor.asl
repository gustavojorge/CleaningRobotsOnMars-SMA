// supervisor.asl - Coordenador de coleta de lixo (100% AgentSpeak)

!iniciar.

+!iniciar <-
    .print("=== SUPERVISOR INICIANDO ===");
    .print("Sistema de coleta inteligente ativado");
    !!monitorar.

// Ciclo principal de monitoramento
+!monitorar <-
    .wait(1000);
    .findall([X,Y], garbage(X,Y), Lixos);
    !verificar_lixos(Lixos);
    !!monitorar.

// Verifica se há lixos e imprime informação
+!verificar_lixos(Lixos) : .length(Lixos, N) & N > 0 <-
    .print(">>> Varredura: ", N, " item(ns) de lixo detectado(s)");
    !processar_lista_lixos(Lixos).

+!verificar_lixos(Lixos) : .length(Lixos, 0) <-
    .print(">>> Varredura: ambiente limpo, nenhum lixo detectado").

// Caso base: lista vazia
+!processar_lista_lixos([]).

// Caso recursivo: processa um lixo por vez
+!processar_lista_lixos([L | Resto]) <-
    L = [GX, GY];
    !atribuir_coleta(GX, GY);
    .wait(500);
    !processar_lista_lixos(Resto).

// Atribuição inteligente baseada em distância Manhattan
+!atribuir_coleta(X, Y) : pos(r1, X1, Y1) & pos(r3, X3, Y3) & pos(r2, IX, IY) <-
    .print("-------------------------------------------");
    .print("ANÁLISE: Lixo em (", X, ",", Y, ")");
    .print("  r1 em (", X1, ",", Y1, ")");
    .print("  r3 em (", X3, ",", Y3, ")");
    .print("  Incinerador em (", IX, ",", IY, ")");
    
    // Calcula distâncias Manhattan em AgentSpeak puro
    !calcular_distancia(X1, Y1, X, Y, IX, IY, D1);
    !calcular_distancia(X3, Y3, X, Y, IX, IY, D3);
    
    .print("  Distância total r1: ", D1, " passos");
    .print("  Distância total r3: ", D3, " passos");
    
    // Decisão: chama plano apropriado baseado na menor distância
    !decidir_coletor(X, Y, IX, IY, D1, D3).

// Calcula distância Manhattan: (XR, YR) -> (GX, GY) -> (IX, IY)
+!calcular_distancia(XR, YR, GX, GY, IX, IY, Dist) <-
    // Distância robô -> lixo
    !abs(XR - GX, DX1);
    !abs(YR - GY, DY1);
    D1 = DX1 + DY1;
    
    // Distância lixo -> incinerador
    !abs(GX - IX, DX2);
    !abs(GY - IY, DY2);
    D2 = DX2 + DY2;
    
    // Distância total
    Dist = D1 + D2.

// Calcula valor absoluto
+!abs(N, Result) : N >= 0 <- Result = N.
+!abs(N, Result) : N < 0 <- Result = -N.

// Se r1 tem menor ou igual distância
+!decidir_coletor(X, Y, IX, IY, D1, D3) : D1 <= D3 <-
    .print("DECISÃO: r1 selecionado (distância: ", D1, ")");
    .send(r1, tell, coleta(X, Y, IX, IY));
    .print("-------------------------------------------").

// Se r3 tem menor distância
+!decidir_coletor(X, Y, IX, IY, D1, D3) : D3 < D1 <-
    .print("DECISÃO: r3 selecionado (distância: ", D3, ")");
    .send(r3, tell, coleta(X, Y, IX, IY));
    .print("-------------------------------------------").

// Tratamento de erro caso não encontre posições
+!atribuir_coleta(X, Y) <-
    .print("ERRO: Não foi possível determinar posições para atribuição");
    .print("  Lixo em (", X, ",", Y, ")").