import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;
import jason.asSemantics.Unifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

public class MarsEnv extends Environment {

    public static final int GSize = 7; // grid size
    public static final int GARB = 16; // garbage code in grid model

    public static final Term ns = Literal.parseLiteral("next(slot)");
    public static final Term pg = Literal.parseLiteral("pick(garb)");
    public static final Term dg = Literal.parseLiteral("drop(garb)");
    public static final Term bg = Literal.parseLiteral("burn(garb)");

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
            if (action.equals(ns)) {
                model.nextSlot(ag);
            } else if (action.getFunctor().equals("move_towards")) {
                int x = (int) ((NumberTerm) action.getTerm(0)).solve();
                int y = (int) ((NumberTerm) action.getTerm(1)).solve();
                model.moveTowards(ag, x, y);
            } else if (action.equals(pg)) {
                model.pickGarb(ag);
            } else if (action.equals(dg)) {
                model.dropGarb(ag);
            } else if (action.equals(bg)) {
                model.burnGarb();
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        updatePercepts();

        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }
        informAgsEnvironmentChanged();
        return true;
    }

    /** creates the agents perception based on the MarsModel */
    void updatePercepts() {
        clearPercepts();

        // Adiciona posições de todos os agentes
        for (int i = 0; i < model.getNbOfAgs(); i++) {
            String agName = "r" + (i + 1);
            Location loc = model.getAgPos(i);
            Literal posLit = Literal.parseLiteral("pos(" + agName + "," + loc.x + "," + loc.y + ")");
            
            // Adiciona para o próprio agente
            addPercept(agName, posLit);
            // Adiciona para o supervisor
            addPercept("supervisor", posLit);
        }

        // Adiciona posições globais de lixo para o supervisor
        for (int x = 0; x < GSize; x++) {
            for (int y = 0; y < GSize; y++) {
                if (model.hasObject(GARB, x, y)) {
                    addPercept("supervisor", Literal.parseLiteral("garbage(" + x + "," + y + ")"));
                }
            }
        }

        // Verifica se algum robô está sobre lixo (para r2 incinerar)
        for (int i = 0; i < model.getNbOfAgs(); i++) {
            Location loc = model.getAgPos(i);
            if (model.hasObject(GARB, loc)) {
                String agName = "r" + (i + 1);
                addPercept(agName, Literal.parseLiteral("garbage(" + agName + ")"));
            }
        }
    }

    class MarsModel extends GridWorldModel {

        public static final int MErr = 2;
        int nerr;
        boolean[] hasGarb = new boolean[3];
        Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
            super(GSize, GSize, 3);

            try {
                setAgPos(0, 0, 0);                    // r1 inicia em (0,0)
                setAgPos(1, 3, 3);                    // r2 incinerador no centro
                setAgPos(2, GSize - 1, GSize - 1);    // r3 inicia em (6,6)
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Inicializa lixo
            add(GARB, 3, 0);
            add(GARB, 1, 2);
            add(GARB, 5, 4);
            add(GARB, 2, 6);
            add(GARB, 6, 3);
        }

        void nextSlot(String ag) throws Exception {
            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            loc.x++;
            if (loc.x == getWidth()) {
                loc.x = 0;
                loc.y++;
            }
            if (loc.y == getHeight()) {
                return;
            }
            setAgPos(id, loc);
        }

        void moveTowards(String ag, int x, int y) throws Exception {
            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            if (loc.x < x)
                loc.x++;
            else if (loc.x > x)
                loc.x--;
            if (loc.y < y)
                loc.y++;
            else if (loc.y > y)
                loc.y--;
            setAgPos(id, loc);
        }

        void pickGarb(String ag) {
            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            if (hasObject(GARB, loc)) {
                if (random.nextBoolean() || nerr == MErr) {
                    remove(GARB, loc);
                    nerr = 0;
                    hasGarb[id] = true;
                    logger.info(ag + " picked garbage at (" + loc.x + "," + loc.y + ")");
                } else {
                    nerr++;
                    logger.info(ag + " failed to pick garbage (attempt " + nerr + "/" + MErr + ")");
                }
            } else {
                logger.warning(ag + " tried to pick garbage but none found at position");
            }
        }

        void dropGarb(String ag) {
            int id = getAgentId(ag);
            Location loc = getAgPos(id);
            if (hasGarb[id]) {
                hasGarb[id] = false;
                add(GARB, loc);
                logger.info(ag + " dropped garbage at (" + loc.x + "," + loc.y + ")");
            } else {
                logger.warning(ag + " tried to drop garbage but is not carrying any");
            }
        }

        void burnGarb() {
            Location r2Loc = getAgPos(1);
            if (hasObject(GARB, r2Loc)) {
                remove(GARB, r2Loc);
                logger.info("r2 burned garbage at (" + r2Loc.x + "," + r2Loc.y + ")");
            } else {
                logger.warning("r2 tried to burn garbage but none found at position");
            }
        }

        private int getAgentId(String agName) {
            if (agName.equals("r1"))
                return 0;
            if (agName.equals("r2"))
                return 1;
            if (agName.equals("r3"))
                return 2;
            return 0;
        }
    }

    class MarsView extends GridWorldView {
        public MarsView(MarsModel model) {
            super(model, "Mars World - Garbage Collection System", 600);
            defaultFont = new Font("Arial", Font.BOLD, 18);
            setVisible(true);
            repaint();
        }

        @Override
        public void draw(Graphics g, int x, int y, int object) {
            if (object == MarsEnv.GARB)
                drawGarb(g, x, y);
        }

        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id) {
            String label = "R" + (id + 1);
            if (id == 1) {
                c = Color.red; // incinerador
                label = "R2";
            } else if (id == 0) {
                c = Color.yellow; // coletor r1
                label = "R1";
            } else if (id == 2) {
                c = Color.green; // coletor r3
                label = "R3";
            }
            super.drawAgent(g, x, y, c, -1);
            g.setColor(Color.black);
            super.drawString(g, x, y, defaultFont, label);
        }

        public void drawGarb(Graphics g, int x, int y) {
            super.drawObstacle(g, x, y);
            g.setColor(Color.white);
            drawString(g, x, y, defaultFont, "G");
        }
    }
}