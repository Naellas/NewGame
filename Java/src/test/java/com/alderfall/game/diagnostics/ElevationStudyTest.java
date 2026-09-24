package com.alderfall.game;

/** Movement and projection contract for the isolated elevation experiment. */
public final class ElevationStudyTest {
    public static void main(String[] args) {
        var f=new ElevationStudy();
        require(!f.canStep(new TilePoint(7,12),new TilePoint(7,11)),"Cannot climb a cliff");
        require(!f.canStep(new TilePoint(7,12),new TilePoint(8,12)),"Cannot enter ramp through retaining side");
        require(!f.canStep(new TilePoint(8,13),new TilePoint(9,12)),"No diagonal bypass");
        var start=new TilePoint(8,14);var end=new TilePoint(14,5);
        var path=f.path(start,end);
        require(path.contains(new TilePoint(8,12)) && path.contains(new TilePoint(13,8)),"Both ramps required");
        require(path.getLast().equals(end),"Reach upper terrace");
        var stepPath=f.path(new TilePoint(13,14),new TilePoint(13,10));
        require(stepPath.size()==4,"Small steps provide a direct alternative to the ramp");
        require(!stepPath.contains(new TilePoint(8,12)),"Step route does not detour through ramp");
        var shelves=new java.util.HashSet<Double>();
        for(double y=10;y<15;y+=.01)shelves.add(f.height(13.5,y));
        require(shelves.containsAll(java.util.List.of(0.0,.25,.5,.75,1.0)),"Quarter-height shelves exist");
        var stepPrevious=new TilePoint(13,14);
        for(var p:stepPath) {
            require(f.canStep(stepPrevious,p)&&f.canStep(p,stepPrevious),"Low steps support ascent and descent");
            stepPrevious=p;
        }
        double previousFoot=f.actorScreenY(13.5,14.5);
        for(double y=14.49;y>=10.5;y-=.01) {
            double foot=f.actorScreenY(13.5,y);
            require(Math.abs(foot-previousFoot)<1.2,"Short steps lift feet smoothly");previousFoot=foot;
        }
        var previous=start;
        for(var p:path) {require(f.canStep(previous,p) && f.canStep(p,previous),"Bidirectional path");previous=p;}
        for(var p:java.util.List.of(start,end,new TilePoint(8,12),new TilePoint(13,8),new TilePoint(10,10)))
            require(p.equals(f.pick((p.x()+.5)*48,f.screenY(p.x()+.5,p.y()+.5))),"Visible centers pick correct elevation: "+p);
        require(new TilePoint(13,4).equals(f.pick(13.5*48,4.5*48-60)),"Raised surface takes priority over hidden ground");
        require(f.pick(7.5*48,12*48-15)==null,"Cliff faces reject clicks");
        require(Math.abs(f.height(8.5,12-1e-6)-f.height(8.5,12+1e-6))<.00001,"Ramp top continuous");
        require(Math.abs(f.height(8.5,13-1e-6)-f.height(8.5,13+1e-6))<.00001,"Ramp bottom continuous");
        require(f.pick(-1,100)==null && f.pick(9999,100)==null,"Off-map picking");
        // The silhouette crosses tile interiors; it must no longer be a set of square cells.
        boolean curved=false;
        for(int y=3;y<12;y++)for(int x=4;x<18;x++)
            if(f.height(x+.15,y+.2)!=f.height(x+.85,y+.8)) curved=true;
        require(curved,"Contour has sub-tile geometry");
        // Every available navigation edge must remain safe along its entire length.
        for(int y=0;y<16;y++)for(int x=0;x<22;x++)for(var d:new int[][]{{1,0},{0,1}}) {
            var a=new TilePoint(x,y);var b=new TilePoint(x+d[0],y+d[1]);
            if(!f.canStep(a,b))continue;
            double previousHeight=f.height(x+.5,y+.5);
            for(int i=1;i<=256;i++) {
                double h=f.height(x+.5+d[0]*i/256.0,y+.5+d[1]*i/256.0);
                require(Math.abs(h-previousHeight)<=ElevationStudy.STEP_HEIGHT+1e-8,"Navigation cannot cross a tall curved cliff");previousHeight=h;
            }
        }
        System.out.println("Elevation study passed: low steps, smooth foot lift, blocked tall cliffs, ramps, reverse travel and picking.");
    }
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
