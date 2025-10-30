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
    public static final int GOLD = 32; // Novo recurso
    
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
                    model.pickItem(ag);
                    break;
                }
                case "drop": {
                    model.dropItem(ag);
                    break;
                }
                case "burn": {
                    model.burnGarb();
                    break;
                }
                case "store": {
                    model.storeGold();
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
        clearPercepts("coordinator"); 
        clearPercepts("r1");
        clearPercepts("r2");
        clearPercepts("r3");

        Location r2Loc = model.getAgPos(2); // Get incinerator location
        
        for(int i=0; i<model.getNbOfAgs(); i++){
            String agName = model.getAgName(i); 
            Location loc = model.getAgPos(i);
            Literal posLit = Literal.parseLiteral("pos(" + agName + "," + loc.x + "," + loc.y + ")");
            addPercept(agName, posLit);
            addPercept("coordinator", posLit);
        }

        // Add perceptions for existing items (garbage or gold)
        for(int x=0; x<GSize; x++){
            for(int y=0; y<GSize; y++){
                
                boolean notAtIncinerator = !(x == r2Loc.x && y == r2Loc.y);
                
                // PERCEPTION OF GARBAGE
                if(model.hasObject(GARB, x, y) && notAtIncinerator){
                    addPercept("coordinator", Literal.parseLiteral("garbage(" + x + "," + y + ")"));
                }
                // PERCEPTION OF GOLD
                if(model.hasObject(GOLD, x, y) && notAtIncinerator){
                    addPercept("coordinator", Literal.parseLiteral("gold(" + x + "," + y + ")"));
                }
            }
        }

        // Add item/carrying percepts
        for(int i=0; i<model.getNbOfAgs(); i++){
            String agName = model.getAgName(i);
            Location loc = model.getAgPos(i);

            // Perception Local of item (for r1/r3)
            if(model.hasObject(GARB, loc)){
                addPercept(agName, Literal.parseLiteral("item(garbage)"));
            }
            if(model.hasObject(GOLD, loc)){
                addPercept(agName, Literal.parseLiteral("item(gold)"));
            }

            // Perception of carrying (for r1/r3)
            if((i == 0 || i == 1) && model.carryingType[i] == GARB){ 
                addPercept(agName, Literal.parseLiteral("carrying(garbage)"));
            }
            if((i == 0 || i == 1) && model.carryingType[i] == GOLD){ 
                addPercept(agName, Literal.parseLiteral("carrying(gold)"));
            }

            // Perception of item at incinerator (for r2)
            if(i==2 && model.hasObject(GARB, loc)){
                addPercept(agName, Literal.parseLiteral("item_at_incinerator(garbage)"));
            }
            if(i==2 && model.hasObject(GOLD, loc)){
                addPercept(agName, Literal.parseLiteral("item_at_incinerator(gold)"));
            }
        }
    }

    class MarsModel extends GridWorldModel {
        public static final int MErr = 2;
        int nerr;
        // 0=empty, 16=GARB, 32=GOLD
        int[] carryingType = new int[3]; // 0=r1, 1=r3, 2=r2
        Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
            super(GSize, GSize, 3); 

            try {
                setAgPos(0, 0, 0);              // r1 -> ID 0
                setAgPos(1, GSize-1, GSize-1);  // r3 -> ID 1
                setAgPos(2, 3, 3);              // r2 incinerator -> ID 2
            } catch(Exception e){ e.printStackTrace(); }

            add(GARB, 3, 0);
            add(GARB, 1, 2);
            add(GARB, 5, 4);
            add(GOLD, 2, 6); 
            add(GOLD, 6, 3);
            add(GARB, 4, 6);
            add(GOLD, 6, 0);
        }

        void moveTowards(String ag, int x, int y) throws Exception {
            if(ag.equals("r2")) return; // r2 (incinerator) does not move

            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            if(loc.x < x) loc.x++; else if(loc.x > x) loc.x--;
            if(loc.y < y) loc.y++; else if(loc.y > y) loc.y--;
            setAgPos(id, loc);
        }

        void pickItem(String ag) {
            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            if(carryingType[id] != 0) return; 

            if(hasObject(GARB, loc)){
                if(random.nextBoolean() || nerr==MErr){
                    remove(GARB, loc);
                    nerr=0;
                    carryingType[id] = GARB; 
                    logger.info(ag + " picked garbage at ("+loc.x+","+loc.y+")");
                } else {
                    nerr++;
                    logger.info(ag + " failed to pick garbage (attempt "+nerr+"/"+MErr+")");
                }
            } else if (hasObject(GOLD, loc)) {
                if(random.nextBoolean() || nerr==MErr){
                    remove(GOLD, loc);
                    nerr=0;
                    carryingType[id] = GOLD;
                    logger.info(ag + " picked gold at ("+loc.x+","+loc.y+")");
                } else {
                    nerr++;
                    logger.info(ag + " failed to pick gold (attempt "+nerr+"/"+MErr+")");
                }
            }
        }

        void dropItem(String ag){
            int id = getAgentId(ag);
            Location loc = getAgPos(id); 
            if(carryingType[id] == GARB){
                carryingType[id] = 0;
                add(GARB, loc);
                logger.info(ag + " dropped garbage at ("+loc.x+","+loc.y+")");
            } else if (carryingType[id] == GOLD) {
                carryingType[id] = 0;
                add(GOLD, loc);
                logger.info(ag + " dropped gold at ("+loc.x+","+loc.y+")");
            }
        }

        // Burns GARBAGE (with safety lock)
        void burnGarb(){
            Location r2Loc = getAgPos(2); 
            // SAFETY LOCK: Only burns if there is garbage AND no gold
            if(hasObject(GARB, r2Loc) && !hasObject(GOLD, r2Loc)){
                remove(GARB, r2Loc);
                logger.info("r2 burned garbage at (" + r2Loc.x + "," + r2Loc.y + ")");
            } else if (hasObject(GOLD, r2Loc)) {
                logger.warning("r2: BURN FAILED! Cannot burn while gold is present!");
            } else {
                logger.info("r2: Nothing to burn.");
            }
        }
        
        void storeGold() {
            Location r2Loc = getAgPos(2); 
            if(hasObject(GOLD, r2Loc)){
                remove(GOLD, r2Loc);
                logger.info("r2 stored gold at (" + r2Loc.x + "," + r2Loc.y + ")");
            } else {
                logger.info("r2: Nothing to store.");
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
            if(object == MarsEnv.GOLD) drawGold(g, x, y); 
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
        
        public void drawGold(Graphics g, int x, int y){
            g.setColor(Color.yellow);             
            g.fillRect(x * cellSizeW, y * cellSizeH, cellSizeW, cellSizeH);
            
            g.setColor(Color.black); 
            drawString(g, x, y, defaultFont, "Au");
        }
    }
}