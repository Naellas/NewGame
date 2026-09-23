package com.alderfall.game.map;
public final class CavernPopulationProbe {
    public static void main(String[] args) {
        int monsters=0, props=0, floors=0;
        for (String theme : new String[]{"cave","goblin_camp"}) for (int seed=0;seed<100;seed++)
            for (int floor=1;floor<=3;floor++) {
                var profile=DungeonGenerator.profile("population-"+seed,"Population",theme,"highwall",'n',seed);
                var plan=DungeonGenerator.generate(profile,floor,3);
                monsters+=plan.encounters().size(); props+=plan.props().size(); floors++;
            }
        System.out.println(floors+" floors: "+monsters+" monster slots, "+props+" props");
    }
}
