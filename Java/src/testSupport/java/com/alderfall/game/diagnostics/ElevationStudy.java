package com.alderfall.game;

import java.util.*;
import java.awt.geom.Path2D;

/** Development-only elevation experiment; deliberately not applied to saved/generated maps. */
final class ElevationStudy {
    static final int WIDTH = 22, HEIGHT = 16, TILE = 48, RISE = 30;
    static final double STEP_HEIGHT = TerrainElevation.STEP;
    private final Path2D lower = new Path2D.Double();
    private final Path2D upper = new Path2D.Double();

    ElevationStudy() {
        lower.moveTo(5.1,6.1);
        lower.curveTo(4.9,4.6,7.5,3.1,10.5,3.65);
        lower.curveTo(12,3.5,14.4,3.2,16.3,4.2);
        lower.curveTo(18.2,5.1,18.5,7.8,17.5,9.9);
        lower.curveTo(16.6,11.9,14.7,11.65,12.4,11.5);
        lower.curveTo(11.1,11.3,10.1,11.9,9,12);
        lower.lineTo(8,12);
        lower.curveTo(6.5,12.1,4.6,11.0,4.7,9.8);
        lower.curveTo(4.4,8.6,5.0,7.5,5.1,6.1); lower.closePath();
        upper.moveTo(10.7,5.8);
        upper.curveTo(10.8,4.6,12.0,4.0,13.2,4.15);
        upper.curveTo(14.8,3.95,16.2,4.7,16.0,6.0);
        upper.curveTo(16.0,7.3,15.2,7.9,14,8);
        upper.lineTo(13,8);
        upper.curveTo(11.7,8.2,10.2,7.1,10.7,5.8); upper.closePath();
    }

    boolean contains(int x, int y) { return x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT; }
    double height(double x, double y) {
        int tx = (int)Math.floor(x), ty = (int)Math.floor(y);
        if (!contains(tx, ty)) return 0;
        if (tx == 8 && ty == 12) return 13-y;
        if (tx == 13 && ty == 8) return 10-y;
        if(upper.contains(x,y)) return 2;
        if(lower.contains(x,y)) return 1;
        // A fan of shallow natural shelves along the southern bank; the ramps remain.
        if(y>10.8 && x>10 && x<17) {
            double spread=Math.sin(Math.PI*(x-10)/7);
            for(int step=1;step<=3;step++)
                if(lower.contains(x,y-step*.72*spread)) return 1-step*STEP_HEIGHT;
        }
        return 0;
    }
    boolean canStep(TilePoint a, TilePoint b) {
        if (!contains(a.x(), a.y()) || !contains(b.x(), b.y())) return false;
        int dx = b.x() - a.x(), dy = b.y() - a.y();
        if (Math.abs(dx) + Math.abs(dy) != 1) return false;
        // Sample the actual curved contour, rather than the old square tile boundaries.
        double previous=height(a.x()+.5,a.y()+.5);
        for(int i=1;i<=64;i++) {
            double h=height(a.x()+.5+dx*i/64.0,a.y()+.5+dy*i/64.0);
            if(Math.abs(h-previous)>STEP_HEIGHT+1e-8) return false;
            previous=h;
        }
        return true;
    }
    List<TilePoint> path(TilePoint start, TilePoint target) {
        if (!contains(start.x(), start.y()) || !contains(target.x(), target.y())) return List.of();
        var previous = new HashMap<TilePoint, TilePoint>();
        var queue = new ArrayDeque<TilePoint>();
        previous.put(start, start); queue.add(start);
        int[][] dirs = {{0,-1},{1,0},{0,1},{-1,0}};
        while (!queue.isEmpty()) {
            var a = queue.remove();
            if (a.equals(target)) break;
            for (var d : dirs) {
                var b = new TilePoint(a.x()+d[0], a.y()+d[1]);
                if (!previous.containsKey(b) && canStep(a,b)) { previous.put(b,a); queue.add(b); }
            }
        }
        if (!previous.containsKey(target)) return List.of();
        var result = new ArrayList<TilePoint>();
        for (var at = target; !at.equals(start); at = previous.get(at)) result.add(at);
        Collections.reverse(result); return result;
    }
    double screenY(double x, double y) { return y*TILE - height(x,y)*RISE; }
    double actorScreenY(double x,double y) {
        // Short foot lift over a small riser; terrain and collision keep their exact heights.
        double north=height(x,y-.12),south=height(x,y+.12);
        if(Math.abs(north-south)>1e-8 && Math.abs(north-south)<=STEP_HEIGHT+1e-8) {
            double a=y-.12,b=y+.12;
            for(int i=0;i<16;i++) {
                double mid=(a+b)/2;
                if(Math.abs(height(x,mid)-north)<1e-8)a=mid;else b=mid;
            }
            double t=Math.max(0,Math.min(1,(y-(a+b)/2+.12)/.24));
            t=t*t*(3-2*t);
            return y*TILE-(north+(south-north)*t)*RISE;
        }
        return screenY(x,y);
    }
    TilePoint pick(double px, double py) {
        int x = (int)Math.floor(px/TILE);
        if (x < 0 || x >= WIDTH) return null;
        // Intersect the view ray from front to back. No intersection means an exposed face.
        for(double wy=(py+2*RISE+1)/TILE;wy>=py/TILE;wy-=.25/TILE) {
            if(wy<0 || wy>=HEIGHT) continue;
            double projected=screenY(px/TILE,wy);
            if(projected>=py && projected<py+.5) return new TilePoint(x,(int)wy);
        }
        return null;
    }
}
