import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

public class MarsEnv extends Environment {

    public static final int     GSize = 7; // grid size
    public static final int GARB  = 16; // garbage code in grid model

    public static final Term    ns = Literal.parseLiteral("next(slot)");
    public static final Term    pg = Literal.parseLiteral("pick(garb)");
    public static final Term    dg = Literal.parseLiteral("drop(garb)");
    public static final Term    bg = Literal.parseLiteral("burn(garb)");
    public static final Literal g1 = Literal.parseLiteral("garbage(r1)");
    public static final Literal g2 = Literal.parseLiteral("garbage(r2)");
    public static final Literal g3 = Literal.parseLiteral("garbage(r3)");

    static Logger logger = Logger.getLogger(MarsEnv.class.getName());

    private MarsModel model;
    private MarsView  view;

    @Override
    public void init(String[] args) {
        model = new MarsModel();
        view  = new MarsView(model);
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
                int x = (int)((NumberTerm)action.getTerm(0)).solve();
                int y = (int)((NumberTerm)action.getTerm(1)).solve();
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
        }

        updatePercepts();

        try {
            Thread.sleep(200);
        } catch (Exception e) {}
        informAgsEnvironmentChanged();
        return true;
    }

    /** creates the agents perception based on the MarsModel */
    void updatePercepts() {
        clearPercepts();

        // add positions
        for (int i = 0; i < model.getNbOfAgs(); i++) {
            String agName = "r" + (i+1);
            Location loc = model.getAgPos(i);
            addPercept(Literal.parseLiteral("pos(" + agName + "," + loc.x + "," + loc.y + ")"));
        }

        // add global garbage positions for supervisor
        for (int x = 0; x < GSize; x++) {
            for (int y = 0; y < GSize; y++) {
                if (model.hasObject(GARB, x, y)) {
                    addPercept("supervisor", Literal.parseLiteral("garbage(" + x + "," + y + ")"));
                }
            }
        }

        // check if any robot is on garbage
        for (int i = 0; i < model.getNbOfAgs(); i++) {
            Location loc = model.getAgPos(i);
            if (model.hasObject(GARB, loc)) {
                addPercept(Literal.parseLiteral("garbage(r" + (i+1) + ")"));
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
                setAgPos(0, 0, 0);            // r1
                setAgPos(1, 3, 3);            // r2 (incinerador)
                setAgPos(2, GSize - 1, GSize - 1);    // r3
            } catch (Exception e) {
                e.printStackTrace();
            }

            // inicializa lixo
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
            if (hasObject(GARB, getAgPos(id))) {
                if (random.nextBoolean() || nerr == MErr) {
                    remove(GARB, getAgPos(id));
                    nerr = 0;
                    hasGarb[id] = true;
                    logger.info(ag + " picked garbage.");
                } else {
                    nerr++;
                }
            }
        }

        void dropGarb(String ag) {
            int id = getAgentId(ag);
            if (hasGarb[id]) {
                hasGarb[id] = false;
                add(GARB, getAgPos(id));
                logger.info(ag + " dropped garbage.");
            }
        }

        void burnGarb() {
            if (hasObject(GARB, getAgPos(1))) {
                remove(GARB, getAgPos(1));
                logger.info("r2 burned garbage!");
            }
        }

        private int getAgentId(String agName) {
            if (agName.equals("r1")) return 0;
            if (agName.equals("r2")) return 1;
            if (agName.equals("r3")) return 2;
            return 0;
        }
    }

    class MarsView extends GridWorldView {
        public MarsView(MarsModel model) {
            super(model, "Mars World", 600);
            defaultFont = new Font("Arial", Font.BOLD, 18);
            setVisible(true);
            repaint();
        }

        @Override
        public void draw(Graphics g, int x, int y, int object) {
            if (object == MarsEnv.GARB) drawGarb(g, x, y);
        }

        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id) {
            String label = "R" + (id + 1);
            if (id == 1) c = Color.red; // incinerador
            else if (id == 0) c = Color.yellow;
            else if (id == 2) c = Color.green;
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
