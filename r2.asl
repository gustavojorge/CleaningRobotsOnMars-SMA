// r2.asl - incinerador fixo

+garbage(r2) <-
    .print("r2 detectou lixo - incinerando...");
    burn(garb);
    .print("r2 concluiu incineração.").
