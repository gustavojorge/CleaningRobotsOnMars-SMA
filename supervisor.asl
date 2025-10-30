// supervisor.asl

!iniciar.

+!iniciar <-
    .print("=== Supervisor iniciando varredura ===");
    !!monitorar.

+!monitorar <-
    .wait(1000);
    .findall([X,Y], garbage(X,Y), Lixos);
    .print("SUPERVISOR: lixos detectados = ", Lixos);
    !processar_lista_lixos(Lixos);
    !!monitorar.

+!processar_lista_lixos([]). 
+!processar_lista_lixos([L | Resto]) <-
    L = [GX, GY];
    !atribuir_coleta(GX, GY);
    .wait(500);
    !processar_lista_lixos(Resto).

// --- CORREÇÃO NESTE PLANO ---
// Movemos a aritmética 'is' para dentro da condição do .if

+!atribuir_coleta(X,Y) : pos(r1,X1,Y1) & pos(r3,X3,Y3) & pos(r2,X2,Y2) <-
    .print("SUPERVISOR: atribuindo coleta para lixo em (", X, ",", Y, ")");
    .print("SUPERVISOR: posições -> r1=(", X1, ",", Y1, "), r3=(", X3, ",", Y3, "), incinerador=(", X2, ",", Y2, ")");

    // CORREÇÃO:
    // Em vez de calcular D1 e D3 separadamente, fazemos isso
    // dentro da condição do .if, onde 'is' é 100% garantido de funcionar.
    .if ( D1 is abs(X1 - X) + abs(Y1 - Y) + abs(X - X2) + abs(Y - Y2) &
          D3 is abs(X3 - X) + abs(Y3 - Y) + abs(X - X2) + abs(Y - Y2) ) 
    {
        // D1 e D3 agora existem dentro deste bloco
        .print("SUPERVISOR: total -> D1=", D1, " | D3=", D3);

        .if (D1 =< D3) {
            .print("SUPERVISOR -> Atribuindo coleta (", X, ",", Y, ") para r1 (D1=", D1, ").");
            .send(r1, tell, coleta(X,Y,X2,Y2));
        } .else {
            .print("SUPERVISOR -> Atribuindo coleta (", X, ",", Y, ") para r3 (D3=", D3, ").");
            .send(r3, tell, coleta(X,Y,X2,Y2));
        }.
    } .else {
        // Este 'else' só aconteceria se a aritmética falhasse (o que não deve)
        .print("SUPERVISOR: ERRO no calculo de distancia.");
    }.