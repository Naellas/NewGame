package com.alderfall.game.ui;

import java.awt.*;

/** Shared settlement card, metric and progression styling. */
public final class SettlementPanelStyle {
    static final Color INK = new Color(234, 233, 219);
    static final Color MUTED = new Color(164, 183, 181);
    static final Color GOLD = new Color(220, 185, 115);
    static final Color TEAL = new Color(106, 184, 160);
    private SettlementPanelStyle() { }

    public static void card(Graphics2D g, int x, int y, int w, int h) {
        Paint old = g.getPaint();
        g.setPaint(new GradientPaint(x,y,new Color(31,48,53,245),x,y+h,new Color(19,29,37,245)));
        g.fillRoundRect(x,y,w,h,14,14); g.setPaint(old);
        g.setColor(new Color(65,88,91)); g.drawRoundRect(x,y,w-1,h-1,14,14);
    }

    static void tiers(Graphics2D g, int x, int y, int w, int level) {
        int gap=4, cell=(w-gap*5)/6;
        for(int i=0;i<6;i++) {
            g.setColor(i<level?TEAL:new Color(49,66,72));
            g.fillRoundRect(x+i*(cell+gap),y,cell,5,4,4);
        }
    }

    static void metric(Graphics2D g, String label, String value, int x, int y) {
        g.setFont(new Font("SansSerif",Font.BOLD,17));g.setColor(INK);g.drawString(value,x,y);
        g.setFont(new Font("SansSerif",Font.PLAIN,10));g.setColor(MUTED);g.drawString(label.toUpperCase(),x,y+16);
    }

    static void progress(Graphics2D g, String label, int value, int target, int x, int y, int w) {
        boolean done=value>=target;
        g.setFont(new Font("SansSerif",Font.PLAIN,10));g.setColor(done?TEAL:MUTED);
        g.drawString(label+"  "+value+"/"+target,x,y);
        g.setColor(new Color(43,60,65));g.fillRoundRect(x,y+6,w,4,4,4);
        g.setColor(done?TEAL:GOLD);
        g.fillRoundRect(x,y+6,(int)(w*Math.min(1,target==0?1:value/(double)target)),4,4,4);
    }
}
