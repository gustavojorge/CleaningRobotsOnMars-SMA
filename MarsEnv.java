import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

// Imports for Unifier
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import jason.asSyntax.NumberTermImpl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

public class MarsEnv extends Environment {

    public static final int GSize = 7;       // grid size
    public static final int GARB = 16;       // garbage code in grid model
    
    // --- CORREÇÃO: Códigos ANSI removidos ---
    // public static final String ANSI_RESET = "\u001B[0m";
    // public static final String ANSI_GREEN = "\u001B[32m";

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
        // --- CORREÇÃO: Log limpo, sem cor e sem duplicata ---
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
                case "compute_distances": {
                    // Extract Positions
                    int x1 = (int) ((NumberTerm) action.getTerm(0)).solve();
                    int y1 = (int) ((NumberTerm) action.getTerm(1)).solve();
                    int x3 = (int) ((NumberTerm) action.getTerm(2)).solve();
                    int y3 = (int) ((NumberTerm) action.getTerm(3)).solve();
                    int gx = (int) ((NumberTerm) action.getTerm(4)).solve();
                    int gy = (int) ((NumberTerm) action.getTerm(5)).solve();
                    int ix = (int) ((NumberTerm) action.getTerm(6)).solve();
                    int iy = (int) ((NumberTerm) action.getTerm(7)).solve();

                    // Extract agent's variables (D1, D3)
                    Term d1Var = action.getTerm(8);
                    Term d3Var = action.getTerm(9);

                    // Calculate
                    int d1 = Math.abs(x1 - gx) + Math.abs(y1 - gy) +
                             Math.abs(gx - ix) + Math.abs(gy - iy);
                    int d3 = Math.abs(x3 - gx) + Math.abs(y3 - gy) +
                             Math.abs(gx - ix) + Math.abs(gy - iy);

                    // Unify (fill in) the agent's variables
                    Unifier unifier = new Unifier();
                    unifier.unifies(d1Var, new NumberTermImpl(d1));
                    unifier.unifies(d3Var, new NumberTermImpl(d3));
                    
                    // --- CORREÇÃO: Log limpo ---
                    logger.info("Distances calculated and unified: D1=" + d1 + ", D3=" + d3);
                    break;
                }
                
                // --- SYNC ACTION ---
                case "sync_percepts": {
                    // This action does nothing.
                    break;
                }
                
                default: return false;
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        // --- R2 VISUAL FIX ---
        Location r2Loc = model.getAgPos(1); // r2's location (3,3)
        if (model.isFree(r2Loc)) { // If r1/r3 just moved off
            try {
                model.setAgPos(1, r2Loc); 
            } catch (Exception e) {}
        }

        // Update percepts and notify agents
        updatePercepts();
        informAgsEnvironmentChanged();

        try { Thread.sleep(200); } catch(Exception e) {}
        return true;
    }

    /**
     * This method is crucial for synchronizing the agents' beliefs with the model.
     * It clears all old percepts and adds only the current ones.
     */
    void updatePercepts() {
        // --- PERCEPTION CLEANUP ---
        clearPercepts("supervisor"); // Correct line: Clears supervisor's old percepts
        clearPercepts("r1");
        clearPercepts("r2");
        clearPercepts("r3");

        // Add agent positions
        Location r2Loc = model.getAgPos(1); // Get incinerator (r2) location
        
        for(int i=0; i<model.getNbOfAgs(); i++){
            String agName = "r"+(i+1);
            Location loc = model.getAgPos(i);
            Literal posLit = Literal.parseLiteral("pos(" + agName + "," + loc.x + "," + loc.y + ")");
            addPercept(agName, posLit);
            addPercept("supervisor", posLit);
        }

        // Add perceptions for existing garbage
        for(int x=0; x<GSize; x++){
            for(int y=0; y<GSize; y++){
                
                boolean notAtIncinerator = !(x == r2Loc.x && y == r2Loc.y);
                
                if(model.hasObject(GARB, x, y) && notAtIncinerator){
                    addPercept("supervisor", Literal.parseLiteral("garbage(" + x + "," + y + ")"));
                }
            }
        }

        // Add garbage/carrying percepts for r1 and r3
        for(int i=0; i<model.getNbOfAgs(); i++){
            String agName = "r"+(i+1);
            Location loc = model.getAgPos(i);
            
            if(model.hasObject(GARB, loc)){
                addPercept(agName, Literal.parseLiteral("garbage(" + loc.x + "," + loc.y + ")"));
            }
            
            if((i == 0 || i == 2) && model.hasGarb[i]){
                addPercept(agName, Literal.parseLiteral("carrying(" + agName + ")"));
            }

            if(i==1 && model.hasObject(GARB, loc)){
                addPercept(agName, Literal.parseLiteral("garbage(r2)"));
            }
        }
    }

    class MarsModel extends GridWorldModel {
        public static final int MErr = 2;
        int nerr;
        boolean[] hasGarb = new boolean[3]; // 0=r1, 1=r2, 2=r3
        Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
            super(GSize, GSize, 3); // 3 agents

            try {
                setAgPos(0, 0, 0);        // r1
                setAgPos(1, 3, 3);        // r2 incinerator
                setAgPos(2, GSize-1, GSize-1);  // r3
            } catch(Exception e){ e.printStackTrace(); }

            // Add initial garbage
            add(GARB, 3, 0);
            add(GARB, 1, 2);
            add(GARB, 5, 4);
            add(GARB, 2, 6);
            add(GARB, 6, 3);
        }

        void moveTowards(String ag, int x, int y) throws Exception {
            if(ag.equals("r2")) return;  // r2 is stationary

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
                    // --- CORREÇÃO: Log limpo ---
                    logger.info(ag + " picked garbage at ("+loc.x+","+loc.y+")");
                } else {
                    nerr++;
                    // --- CORREÇÃO: Log limpo ---
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
                // --- CORREÇÃO: Log limpo ---
                logger.info(ag + " dropped garbage at ("+loc.x+","+loc.y+")");
            }
        }

        void burnGarb(){
            Location r2Loc = getAgPos(1); // r2's location
            if(hasObject(GARB, r2Loc)){
                remove(GARB, r2Loc);
                // --- CORREÇÃO: Log limpo ---
                logger.info("r2 burned garbage at (" + r2Loc.x + "," + r2Loc.y + ")");
            }
        }

        private int getAgentId(String agName){
            switch(agName){
                case "r1": return 0;
                case "r2": return 1;
                case "r3": return 2;
            }
            return 0; // Default
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
            
            // --- CORREÇÃO DAS CORES DO GRID ---
            // Assegura que as cores pedidas sejam usadas
            if(id==1){ c=Color.red; label="R2"; } // R2 Vermelho
            else if(id==0){ c=Color.yellow; label="R1"; } // R1 Amarelo
            else if(id==2){ c=Color.magenta; label="R3"; } // R3 Roxo/Magenta
            // --- FIM DA CORREÇÃO ---
            
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