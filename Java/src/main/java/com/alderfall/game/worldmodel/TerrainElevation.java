package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.util.*;

/** Seeded landform field shared by travel and terrain relief. Heights are in terrace units. */
public final class TerrainElevation {
    public static final double STEP = .25;
    public static final Field FLAT = new Field();
    private final WorldMap world;
    private final long seed;
    private final BitSet generatedMountains=new BitSet();
    private final Map<String, Field> fields = new HashMap<>();
    private List<List<TilePoint>> mountainTrails;

    public TerrainElevation(WorldMap world,long seed) {
        this.world=world;this.seed=seed;
        String map=WorldMap.OVERWORLD_ID;int w=world.width(map);
        for(int y=0;y<world.height(map);y++)for(int x=0;x<w;x++)
            if(world.tileAt(map,x,y)=='m')generatedMountains.set(y*w+x);
    }
    public Field field(String id) {
        if(Boolean.getBoolean("alderfall.disableElevation"))return FLAT;
        MapArea area=world.area(id);
        if(area==null || id.startsWith("editor_") || !Set.of("overworld","village","city").contains(area.kind)) return FLAT;
        Field previous=fields.get(id);
        if(previous!=null && previous.revision==area.visualRevision()) return previous;
        boolean overworld=id.equals(WorldMap.OVERWORLD_ID);
        Field result=new Field(world,area,seed,overworld?generatedMountains:new BitSet(),overworld?mountainTrails:null);
        if (id.equals(WorldMap.PLAYER_VILLAGE_ID)) result.applyVillageHeights(world);
        if(overworld && mountainTrails==null)mountainTrails=result.trails();
        fields.put(id,result);return result;
    }
    public static final class Field {
        private final int width,height;
        private final float[] heights;
        private final byte[] mountainDepth;
        private final char[] terrain;
        private final ElevationMaterial[] materials;
        private final BitSet generatedMountains;
        private final boolean[] ramps;
        private final List<List<TilePoint>> trails=new ArrayList<>();
        private final String region;
        private final long revision;
        private Field() {width=height=0;heights=new float[0];mountainDepth=new byte[0];terrain=new char[0];materials=new ElevationMaterial[0];ramps=new boolean[0];generatedMountains=new BitSet();region="crownlands";revision=0;}
        private Field(WorldMap world,MapArea area,long seed,BitSet generatedMountains,List<List<TilePoint>> savedTrails) {
            this.generatedMountains=generatedMountains;
            width=area.width();height=area.height();revision=area.visualRevision();
            heights=new float[width*height];terrain=new char[width*height];
            ramps=new boolean[width*height];
            materials=new ElevationMaterial[width*height];
            boolean[] wet=new boolean[heights.length],fixed=new boolean[heights.length],nonMountain=new boolean[heights.length],paved=new boolean[heights.length];
            region=switch(RegionalSettlementIdentity.region(area.id)) {
                case NORTH -> "highwall";case FREEHOLDS -> "northroad";case SUN -> "sanctum";
                case FEN -> "belltower";case RIVER -> "riverside";default -> "crownlands";
            };
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                int i=y*width+x;char t=area.tileAt(x,y);terrain[i]=t;
                wet[i]="w~B".indexOf(t)>=0;nonMountain[i]=t!='m';
                paved[i]="KpCG".indexOf(t)>=0;
                fixed[i]="gfnsbvPmA".indexOf(t)<0 || x<2 || y<2 || x>=width-2 || y>=height-2;
            }
            // Foundations, doors and authored interaction areas keep a level apron.
            for(var building:world.cityBuildings(area.id))
                for(int y=Math.max(0,building.y1()-2);y<=Math.min(height-1,building.y2()+2);y++)
                    for(int x=Math.max(0,building.x1()-2);x<=Math.min(width-1,building.x2()+2);x++)fixed[y*width+x]=true;
            for(var prop:area.props) if(!prop.asset().startsWith("deco_") && !prop.asset().startsWith("weather_"))
                for(int y=Math.max(0,prop.y()-1);y<=Math.min(height-1,prop.y()+2);y++)
                    for(int x=Math.max(0,prop.x()-1);x<=Math.min(width-1,prop.x()+2);x++)fixed[y*width+x]=true;
            byte[] waterDistance=distance(wet,width,height),apronDistance=distance(fixed,width,height);
            byte[] pavedDistance=area.kind.equals("city")?distance(paved,width,height):null;
            mountainDepth=distance(nonMountain,width,height);
            double phase=(seed ^ area.id.hashCode())*.00013;
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                int i=y*width+x;char t=terrain[i];
                String culture=area.kind.equals("overworld")?world.kingdomAt(x,y).id():region;
                materials[i]=ElevationMaterial.forTerrain(t,culture);
                if(pavedDistance!=null && pavedDistance[i]<=4 && "gfn".indexOf(t)>=0)materials[i]=ElevationMaterial.MASONRY;
                double amplitude=switch(t) {
                    case 'm' -> 4.0;case 'q','n' -> 1.65;case 'b' -> 2.1;
                    case 'f' -> 1.15;case 's' -> .95;case 'v' -> .65;case 'P' -> .3;default -> .85;
                };
                double regional=switch(culture) {case "highwall","northroad" -> 1.15;case "belltower" -> .65;case "sanctum" -> 1.1;default -> 1;};
                if(!area.kind.equals("overworld")) amplitude*=.65;
                double wave=.5+.25*Math.sin(x*.19+Math.sin(y*.1)+phase)+.25*Math.cos(y*.16-x*.06-phase);
                // Mountain mass follows its geography, not independent wave peaks per tile.
                double shape=t=='m' ? .62+.38*Math.min(1,mountainDepth[i]/5.0) : Math.max(0,wave-.12);
                double apron=Math.min(1,Math.max(0,(apronDistance[i]-1)/3.0));
                double coast=Math.min(1,Math.max(0,(waterDistance[i]-1)/4.0));
                heights[i]=(float)Math.min(4,amplitude*regional*shape*apron*coast);
            }
            // Smooth amplitudes across biome boundaries; fixed foundations remain exact zero.
            for(int pass=0;pass<8;pass++) {
                float[] copy=heights.clone();
                for(int y=1;y<height-1;y++)for(int x=1;x<width-1;x++) {
                    int i=y*width+x;
                    if(apronDistance[i]<=1 || waterDistance[i]<=1) {heights[i]=0;continue;}
                    heights[i]=(copy[i]*4+copy[i-1]+copy[i+1]+copy[i-width]+copy[i+width])/8;
                }
            }
            if(area.kind.equals("overworld")) {
                if(savedTrails==null)planMountainTrails(world,area);
                else {
                    trails.addAll(savedTrails);
                    for(var trail:trails)for(var p:trail)
                        if("w~B".indexOf(terrain[p.y()*width+p.x()])<0)ramps[p.y()*width+p.x()]=true;
                }
            }
        }
        private void applyVillageHeights(WorldMap world) {
            world.playerVillageHeights().forEach((p,h) -> {
                if (world.canEditVillageHeight(p.x(),p.y())) heights[p.y()*width+p.x()]=h.floatValue();
            });
        }
        public boolean active() {return width>0;}
        public boolean mountainWalkable(int x,int y) {
            return active() && x>=0 && y>=0 && x<width && y<height && generatedMountains.get(y*width+x)
                && terrain[y*width+x]=='m' && (mountainDepth[y*width+x]<=2 || heights[y*width+x]<2.2 || ramps[y*width+x]);
        }
        public boolean summit(int x,int y) {
            return active() && x>=0 && y>=0 && x<width && y<height && terrain[y*width+x]=='m' && !mountainWalkable(x,y);
        }
        private double sample(int x,int y) {
            return heights[Math.max(0,Math.min(height-1,y))*width+Math.max(0,Math.min(width-1,x))];
        }
        long surfaceSignature(int x,int y) {
            if(!active())return 0;
            int i=Math.max(0,Math.min(height-1,y))*width+Math.max(0,Math.min(width-1,x));
            return (Float.floatToIntBits(heights[i]) & 0xffffffffL)
                    ^ ((long)materials[i].hashCode()<<32) ^ ((long)terrain[i]<<48) ^ (ramps[i]?1L<<63:0);
        }
        public double heightAt(double x,double y) {
            if(!active() || x<0 || y<0 || x>=width || y>=height) return 0;
            double px=x-.5,py=y-.5;int ix=(int)Math.floor(px),iy=(int)Math.floor(py);
            double fx=px-ix,fy=py-iy;
            double raw=(sample(ix,iy)*(1-fx)+sample(ix+1,iy)*fx)*(1-fy)
                +(sample(ix,iy+1)*(1-fx)+sample(ix+1,iy+1)*fx)*fy;
            // Broad summit scarps are confined to the already blocked mountain core.
            // Keep fractional slopes below the upper massif. A continuous threshold avoids
            // exposing square one-tile cliff fragments at the mountain-core boundary.
            double quantum=terrain[Math.min(height-1,(int)y)*width+Math.min(width-1,(int)x)]=='m' && raw>=2?1:STEP;
            double terraced=Math.floor((raw+1e-8)/quantum)*quantum;
            double blend=Math.max(0,Math.min(1,(rampAt(x,y)-.2)/.6));
            blend=blend*blend*(3-2*blend);
            return terraced+(raw-terraced)*blend;
        }
        public List<List<TilePoint>> trails(){return List.copyOf(trails);}
        private double rampSample(int x,int y){return x>=0&&y>=0&&x<width&&y<height&&ramps[y*width+x]?1:0;}
        public double rampAt(double x,double y) {
            if(!active())return 0;
            int ix=(int)Math.floor(x-.5),iy=(int)Math.floor(y-.5);double fx=x-.5-ix,fy=y-.5-iy;
            return (rampSample(ix,iy)*(1-fx)+rampSample(ix+1,iy)*fx)*(1-fy)
                +(rampSample(ix,iy+1)*(1-fx)+rampSample(ix+1,iy+1)*fx)*fy;
        }
        private void planMountainTrails(WorldMap world,MapArea area) {
            boolean[] blocked=new boolean[heights.length];
            for(int i=generatedMountains.nextSetBit(0);i>=0;i=generatedMountains.nextSetBit(i+1))
                if(world.hasBlockingGroundProp(area.id,i%width,i/width))blocked[i]=true;
            for(var prop:area.props) {
                var footprint=PropCollision.footprint(world,area.id,prop);
                if(footprint==null)continue;
                double pad=PropCollision.PLAYER_RADIUS;
                for(int y=Math.max(0,(int)Math.floor(footprint.y-pad));y<=Math.min(height-1,(int)Math.floor(footprint.getMaxY()+pad));y++)
                    for(int x=Math.max(0,(int)Math.floor(footprint.x-pad));x<=Math.min(width-1,(int)Math.floor(footprint.getMaxX()+pad));x++)blocked[y*width+x]=true;
            }
            var candidates=new ArrayList<Integer>();
            for(int y=4;y<height-4;y+=2)for(int x=4;x<width-4;x+=2) {
                int i=y*width+x;
                if(!blocked[i]&&generatedMountains.get(i)&&mountainDepth[i]>=2&&heights[i]>=1.25&&heights[i]<2.2)candidates.add(i);
            }
            candidates.sort(Comparator.<Integer>comparingDouble(i->heights[i]).reversed().thenComparingInt(i->i));
            var selected=new ArrayList<TilePoint>();
            for(int goal:candidates) {
                var top=new TilePoint(goal%width,goal/width);
                if(selected.stream().anyMatch(p->Math.hypot(p.x()-top.x(),p.y()-top.y())<18))continue;
                var route=routeToPass(goal,blocked,world,area.id);
                if(route.size()<5)continue;
                selected.add(top);trails.add(List.copyOf(route));
                for(var point:route)ramps[point.y()*width+point.x()]=true;
            }
        }
        private List<TilePoint> routeToPass(int start,boolean[] blocked,WorldMap world,String map) {
            record Node(int index,double cost) { }
            var queue=new PriorityQueue<Node>(Comparator.comparingDouble(Node::cost));
            int[] previous=new int[heights.length];Arrays.fill(previous,-1);
            double[] cost=new double[heights.length];Arrays.fill(cost,Double.POSITIVE_INFINITY);
            previous[start]=start;cost[start]=0;queue.add(new Node(start,0));int target=-1;
            int sx=start%width,sy=start/width;
            while(!queue.isEmpty()) {
                var node=queue.remove();int i=node.index,x=i%width,y=i/width;
                if(node.cost>cost[i])continue;
                if(Terrain.ROAD_LIKE.contains(terrain[i]) && Math.abs(x-sx)+Math.abs(y-sy)>4){target=i;break;}
                if(Math.hypot(x-sx,y-sy)>22)continue;
                int[] next={x>0?i-1:-1,x+1<width?i+1:-1,y>0?i-width:-1,y+1<height?i+width:-1};
                for(int n:next) {
                    if(n<0 || blocked[n] || !(Terrain.passable(terrain[n])||generatedMountains.get(n)) || "w~B".indexOf(terrain[n])>=0)continue;
                    if(terrain[n]!='m' && !world.isGroundPassable(map,n%width,n/width))continue;
                    double nextCost=cost[i]+1+Math.abs(heights[i]-heights[n])*8+(heights[n]>2.2?8:0);
                    if(nextCost<cost[n]){cost[n]=nextCost;previous[n]=i;queue.add(new Node(n,nextCost));}
                }
            }
            if(target<0)return List.of();
            var route=new ArrayList<TilePoint>();
            for(int at=target;at!=start;at=previous[at])route.add(new TilePoint(at%width,at/width));
            route.add(new TilePoint(sx,sy));return route;
        }
        public boolean canTravel(double x1,double y1,double x2,double y2) {
            if(!active())return true;
            int samples=Math.max(1,(int)Math.ceil(Math.hypot(x2-x1,y2-y1)*64));
            double previous=heightAt(x1,y1);
            for(int i=1;i<=samples;i++) {
                double h=heightAt(x1+(x2-x1)*i/samples,y1+(y2-y1)*i/samples);
                if(Math.abs(h-previous)>STEP+1e-8 && !resolvesStep(
                    x1+(x2-x1)*(i-1)/samples,y1+(y2-y1)*(i-1)/samples,previous,
                    x1+(x2-x1)*i/samples,y1+(y2-y1)*i/samples,h,12))return false;
                previous=h;
            }
            return true;
        }
        private boolean resolvesStep(double ax,double ay,double ah,double bx,double by,double bh,int remaining) {
            if(Math.abs(bh-ah)<=STEP+1e-7)return true;
            if(remaining==0)return false;
            // Separate a legal quarter-step from the tiny continuous ramp grade next to it.
            double mx=(ax+bx)/2,my=(ay+by)/2,mh=heightAt(mx,my);
            return resolvesStep(ax,ay,ah,mx,my,mh,remaining-1)
                &&resolvesStep(mx,my,mh,bx,by,bh,remaining-1);
        }
        public ElevationMaterial materialAt(double x,double y) {
            if(!active())return ElevationMaterial.EARTH;
            return materials[Math.max(0,Math.min(height-1,(int)y))*width+Math.max(0,Math.min(width-1,(int)x))];
        }
    }
    private static byte[] distance(boolean[] sources,int w,int h) {
        byte[] d=new byte[sources.length];Arrays.fill(d,(byte)100);
        int[] queue=new int[d.length];int head=0,tail=0;
        for(int i=0;i<d.length;i++)if(sources[i]){d[i]=0;queue[tail++]=i;}
        while(head<tail) {
            int i=queue[head++],x=i%w,y=i/w;
            if(d[i]>=20)continue;
            int[] next={x>0?i-1:-1,x+1<w?i+1:-1,y>0?i-w:-1,y+1<h?i+w:-1};
            for(int n:next)if(n>=0 && d[n]>d[i]+1){d[n]=(byte)(d[i]+1);queue[tail++]=n;}
        }
        return d;
    }
}
