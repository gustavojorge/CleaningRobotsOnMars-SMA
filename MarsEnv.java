import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

// --- CORREÇÃO 1: Imports necessários para o Unifier ---
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import jason.asSyntax.NumberTermImpl;
// --- Fim da Correção 1 ---

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

public class MarsEnv extends Environment {

    public static final int GSize = 7;       // grid size
    public static final int GARB = 16;       // garbage code in grid model

    static Logger logger = Logger.getLogger(MarsEnv.class.getName());

    private MarsModel model;
    private MarsView view;

    @Override
    public void init(String[] args) {
        model = new MarsModel();
        view = new MarsView(model);
        model.setView(view);
        updatePercepts();
    }

    @Override
    public boolean executeAction(String ag, Structure action) {
        logger.info(ag + " doing: " + action);
        try {
            switch(action.getFunctor()) {
                case "move_towards": {
                    int x = (int) ((NumberTerm) action.getTerm(0)).solve();
                    int y = (int) ((NumberTerm) action.getTerm(1)).solve();
                    model.moveTowards(ag, x, y);
                    break;
                }
                case "pick": {
                    if(action.getTerm(0).toString().equals("garb"))
                        model.pickGarb(ag);
                    break;
                }
                case "drop": {
                    if(action.getTerm(0).toString().equals("garb"))
                        model.dropGarb(ag);
                    break;
                }
                case "burn": {
                    if(action.getTerm(0).toString().equals("garb"))
                        model.burnGarb();
                    break;
                }
                // --- CORREÇÃO 1: Lógica do compute_distances ---
                case "compute_distances": {
                    // Extrai Posições
                    int x1 = (int) ((NumberTerm) action.getTerm(0)).solve();
                    int y1 = (int) ((NumberTerm) action.getTerm(1)).solve();
                    int x3 = (int) ((NumberTerm) action.getTerm(2)).solve();
                    int y3 = (int) ((NumberTerm) action.getTerm(3)).solve();
                    int gx = (int) ((NumberTerm) action.getTerm(4)).solve();
                    int gy = (int) ((NumberTerm) action.getTerm(5)).solve();
                    int ix = (int) ((NumberTerm) action.getTerm(6)).solve();
                    int iy = (int) ((NumberTerm) action.getTerm(7)).solve();

                    // Extrai as *Variáveis* (D1, D3) que o agente enviou
                    Term d1Var = action.getTerm(8);
                    Term d3Var = action.getTerm(9);

                    // Calcula
                    int d1 = Math.abs(x1 - gx) + Math.abs(y1 - gy) +
                             Math.abs(gx - ix) + Math.abs(gy - iy);
                    int d3 = Math.abs(x3 - gx) + Math.abs(y3 - gy) +
                             Math.abs(gx - ix) + Math.abs(gy - iy);

                    // Unifica (preenche) as variáveis D1 e D3 no agente
                    Unifier unifier = new Unifier();
                    unifier.unifies(d1Var, new NumberTermImpl(d1));
                    unifier.unifies(d3Var, new NumberTermImpl(d3));
                    
                    // Removemos a lógica de addPercept daqui
                    logger.info("Distances calculated and unified: D1=" + d1 + ", D3=" + d3);
                    break;
                }
                // --- Fim da Correção 1 ---
                case "task_assigned": {
                    // marca lixo como atribuído para o supervisor
                    int gx = (int) ((NumberTerm) action.getTerm(0)).solve();
                    int gy = (int) ((NumberTerm) action.getTerm(1)).solve();
                    model.lixoAtribuido[gx][gy] = true;
                    break;
                }
                default: return false;
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        // Atualiza percepções e notifica agentes
        updatePercepts();
        informAgsEnvironmentChanged();

        try { Thread.sleep(200); } catch(Exception e) {}
        return true;
    }

    void updatePercepts() {
        // --- CORREÇÃO 2: Limpa percepções "fantasmas" dos agentes ---
        clearPercepts(); // Limpa "default" (supervisor)
        clearPercepts("r1");
        clearPercepts("r2");
        clearPercepts("r3");
        // --- Fim da Correção 2 ---

        // Adiciona posições de todos os agentes
        for(int i=0; i<model.getNbOfAgs(); i++){
            String agName = "r"+(i+1);
            Location loc = model.getAgPos(i);
            Literal posLit = Literal.parseLiteral("pos(" + agName + "," + loc.x + "," + loc.y + ")");
            addPercept(agName, posLit);
            addPercept("supervisor", posLit);
        }

        // Adiciona apenas lixo que ainda existe no grid e não foi atribuído
        for(int x=0; x<GSize; x++){
            for(int y=0; y<GSize; y++){
                if(model.hasObject(GARB, x, y) && !model.lixoAtribuido[x][y]){
                    addPercept("supervisor", Literal.parseLiteral("garbage(" + x + "," + y + ")"));
                }
            }
        }

        // Adiciona percepção de lixo e carrying para r1 e r3
        for(int i=0; i<model.getNbOfAgs(); i++){
            String agName = "r"+(i+1);
            Location loc = model.getAgPos(i);
            if(model.hasObject(GARB, loc)){
                addPercept(agName, Literal.parseLiteral("garbage(" + loc.x + "," + loc.y + ")"));
            }
            // Apenas r1 (id 0) e r3 (id 2) podem carregar lixo
            if((i == 0 || i == 2) && model.hasGarb[i]){
                addPercept(agName, Literal.parseLiteral("carrying(" + agName + ")"));
            }

            // r2 incinerador
            if(i==1 && model.hasObject(GARB, loc)){
                addPercept(agName, Literal.parseLiteral("garbage(r2)"));
            }
        }
    }

    class MarsModel extends GridWorldModel {
        public static final int MErr = 2;
        int nerr;
        boolean[] hasGarb = new boolean[3];
        boolean[][] lixoAtribuido = new boolean[GSize][GSize]; // nova matriz
        Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
            super(GSize, GSize, 3);

            try {
                setAgPos(0, 0, 0);        // r1
                setAgPos(1, 3, 3);        // r2 incinerador
                setAgPos(2, GSize-1, GSize-1);  // r3
            } catch(Exception e){ e.printStackTrace(); }

            // Inicializa lixo
            add(GARB, 3, 0);
            add(GARB, 1, 2);
            add(GARB, 5, 4);
            add(GARB, 2, 6);
            add(GARB, 6, 3);
        }

        void moveTowards(String ag, int x, int y) throws Exception {
            if(ag.equals("r2")) return;  // r2 fixo

            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            if(loc.x < x) loc.x++; else if(loc.x > x) loc.x--;
            if(loc.y < y) loc.y++; else if(loc.y > y) loc.y--;
            setAgPos(id, loc);
        }

        void pickGarb(String ag) {
            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            if(hasObject(GARB, loc)){
                if(random.nextBoolean() || nerr==MErr){
                    remove(GARB, loc);
                    nerr=0;
                    hasGarb[id] = true;
                    logger.info(ag + " picked garbage at ("+loc.x+","+loc.y+")");
                } else {
                    nerr++;
                    logger.info(ag + " failed to pick garbage (attempt "+nerr+"/"+MErr+")");
                }
            }
        }

        void dropGarb(String ag){
            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            if(hasGarb[id]){
                hasGarb[id] = false;
                add(GARB, loc);
                logger.info(ag + " dropped garbage at ("+loc.x+","+loc.y+")");
            }
        }

        void burnGarb(){
            Location r2Loc = getAgPos(1);
            if(hasObject(GARB, r2Loc)){
                remove(GARB, r2Loc);
                logger.info("r2 burned garbage at (" + r2Loc.x + "," + r2Loc.y + ")");
            }
        }

        private int getAgentId(String agName){
            switch(agName){
                case "r1": return 0;
                case "r2": return 1;
                case "r3": return 2;
            }
            return 0;
        }
    }

    class MarsView extends GridWorldView {
        public MarsView(MarsModel model){
            super(model, "Mars World - Garbage Collection System", 600);
            defaultFont = new Font("Arial", Font.BOLD, 18);
            setVisible(true);
            repaint();
        }

        @Override
        public void draw(Graphics g, int x, int y, int object){
            if(object == MarsEnv.GARB) drawGarb(g, x, y);
        }

        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id){
            String label = "R" + (id+1);
            if(id==1){ c=Color.red; label="R2"; }
            else if(id==0){ c=Color.yellow; label="R1"; }
            else if(id==2){ c=Color.green; label="R3"; }
            super.drawAgent(g, x, y, c, -1);
            g.setColor(Color.black);
            super.drawString(g, x, y, defaultFont, label);
        }

        public void drawGarb(Graphics g, int x, int y){
            super.drawObstacle(g, x, y);
            g.setColor(Color.white);
            drawString(g, x, y, defaultFont, "G");
        }
    }
}