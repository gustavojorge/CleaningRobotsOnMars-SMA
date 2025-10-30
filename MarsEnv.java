import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import jason.asSyntax.NumberTermImpl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

public class MarsEnv extends Environment {

    public static final int GSize = 7;       
    public static final int GARB = 16;       
    
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
                case "compute_distances": {
                    int x1 = (int) ((NumberTerm) action.getTerm(0)).solve();
                    int y1 = (int) ((NumberTerm) action.getTerm(1)).solve();
                    int x3 = (int) ((NumberTerm) action.getTerm(2)).solve();
                    int y3 = (int) ((NumberTerm) action.getTerm(3)).solve();
                    int gx = (int) ((NumberTerm) action.getTerm(4)).solve();
                    int gy = (int) ((NumberTerm) action.getTerm(5)).solve();
                    int ix = (int) ((NumberTerm) action.getTerm(6)).solve();
                    int iy = (int) ((NumberTerm) action.getTerm(7)).solve();

                    Term d1Var = action.getTerm(8);
                    Term d3Var = action.getTerm(9);

                    int d1 = Math.abs(x1 - gx) + Math.abs(y1 - gy) +
                             Math.abs(gx - ix) + Math.abs(gy - iy);
                    int d3 = Math.abs(x3 - gx) + Math.abs(y3 - gy) +
                             Math.abs(gx - ix) + Math.abs(gy - iy);

                    Unifier unifier = new Unifier();
                    unifier.unifies(d1Var, new NumberTermImpl(d1));
                    unifier.unifies(d3Var, new NumberTermImpl(d3));
                    
                    logger.info("Distances calculated and unified: D1=" + d1 + ", D3=" + d3);
                    break;
                }
                
                case "sync_percepts": {
                    break;
                }
                
                default: return false;
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        Location r2Loc = model.getAgPos(2); 
        if (model.isFree(r2Loc)) {
            try {
                model.setAgPos(2, r2Loc); 
            } catch (Exception e) {}
        }

        if (action.getFunctor().equals("move_towards")) {
            try {
                int id = model.getAgentId(ag);
                if (id != 2) { 
                    model.setAgPos(id, model.getAgPos(id)); 
                }
            } catch (Exception e) {}
        }

        // Update percepts and notify agents
        updatePercepts();
        informAgsEnvironmentChanged();

        try { Thread.sleep(200); } catch(Exception e) {}
        return true;
    }

    void updatePercepts() {
        clearPercepts("supervisor"); 
        clearPercepts("r1");
        clearPercepts("r2");
        clearPercepts("r3");

        Location r2Loc = model.getAgPos(2); // Get incinerator location
        
        for(int i=0; i<model.getNbOfAgs(); i++){
            String agName = model.getAgName(i); 
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

        // Add garbage/carrying percepts
        for(int i=0; i<model.getNbOfAgs(); i++){
            String agName = model.getAgName(i);
            Location loc = model.getAgPos(i);
            
            if(model.hasObject(GARB, loc)){
                addPercept(agName, Literal.parseLiteral("garbage(" + loc.x + "," + loc.y + ")"));
            }
            
            if((i == 0 || i == 1) && model.hasGarb[i]){ 
                addPercept(agName, Literal.parseLiteral("carrying(" + agName + ")"));
            }

            if(i==2 && model.hasObject(GARB, loc)){
                addPercept(agName, Literal.parseLiteral("garbage(r2)"));
            }
        }
    }

    class MarsModel extends GridWorldModel {
        public static final int MErr = 2;
        int nerr;
        boolean[] hasGarb = new boolean[3]; // 0=r1, 1=r3, 2=r2
        Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
            super(GSize, GSize, 3); 

            try {
                setAgPos(0, 0, 0);        
                setAgPos(1, GSize-1, GSize-1);  
                setAgPos(2, 3, 3);        
            } catch(Exception e){ e.printStackTrace(); }

            add(GARB, 3, 0);
            add(GARB, 1, 2);
            add(GARB, 5, 4);
            add(GARB, 2, 6);
            add(GARB, 6, 3);
        }

        void moveTowards(String ag, int x, int y) throws Exception {
            if(ag.equals("r2")) return; // r2 (incinerator) does not move

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
            Location r2Loc = getAgPos(2); 
            if(hasObject(GARB, r2Loc)){
                remove(GARB, r2Loc);
                logger.info("r2 burned garbage at (" + r2Loc.x + "," + r2Loc.y + ")");
            }
        }

        private int getAgentId(String agName){
            switch(agName){
                case "r1": return 0;
                case "r3": return 1; 
                case "r2": return 2;
            }
            return 0;
        }

        private String getAgName(int id) {
            switch(id) {
                case 0: return "r1";
                case 1: return "r3";
                case 2: return "r2";
            }
            return "";
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
            String label = "R?";

            if(id==2){ c=Color.red; label="R2"; }
            else if(id==0){ c=Color.yellow; label="R1"; }
            else if(id==1){ c=Color.magenta; label="R3"; }

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