// r3.asl - coletor

+coleta(X,Y,IX,IY) <-
    .print("r3 recebeu tarefa: coletar lixo em (" , X , "," , Y , ") -> incinerador (" , IX , "," , IY , ")");
    !ir_para(X,Y);
    pick(garb);
    !ir_para(IX,IY);
    drop(garb);
    .print("r3 concluiu entrega ao incinerador.").

+!ir_para(X,Y) : pos(r3,X,Y).
+!ir_para(X,Y) <-
    move_towards(X,Y);
    !ir_para(X,Y).
