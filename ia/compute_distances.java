package ia;

import jason.asSemantics.*;
import jason.asSyntax.*;

public class compute_distances extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        
        try {
            // Extrai argumentos de entrada (posições)
            int x1 = (int) ((NumberTerm) args[0]).solve();
            int y1 = (int) ((NumberTerm) args[1]).solve();
            int x3 = (int) ((NumberTerm) args[2]).solve();
            int y3 = (int) ((NumberTerm) args[3]).solve();
            int gx = (int) ((NumberTerm) args[4]).solve();
            int gy = (int) ((NumberTerm) args[5]).solve();
            int ix = (int) ((NumberTerm) args[6]).solve();
            int iy = (int) ((NumberTerm) args[7]).solve();

            // Calcula distâncias Manhattan
            int d1 = Math.abs(x1 - gx) + Math.abs(y1 - gy) + 
                     Math.abs(gx - ix) + Math.abs(gy - iy);
            
            int d3 = Math.abs(x3 - gx) + Math.abs(y3 - gy) + 
                     Math.abs(gx - ix) + Math.abs(gy - iy);

            // Unifica os resultados
            boolean result = un.unifies(args[8], new NumberTermImpl(d1));
            result = result && un.unifies(args[9], new NumberTermImpl(d3));
            
            return result;

        } catch (Exception e) {
            System.err.println("Erro em compute_distances: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}