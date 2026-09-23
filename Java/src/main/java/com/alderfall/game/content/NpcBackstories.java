package com.alderfall.game;

import java.util.ArrayList;
import java.util.List;

/** Public introductions and earned personal disclosures; never evidence for an unplayed quest. */
public final class NpcBackstories {
    private NpcBackstories() { }

    public record Profile(String introduction, String background, String reasonToAsk,
                          String privateHistory, String greeting) { }

    public static Profile profile(Npc npc) {
        if (npc == null) return null;
        String key = npc.recruitId() != null ? npc.recruitId() : npc.questId() == null ? "" : npc.questId();
        return switch (key) {
            case "cairnvale_ore_assay" -> new Profile(
                    "Miner Dorran. I work the Cairnspire seams and check the ore orders for Cairnvale's smiths.",
                    "I began as a lamp runner. Now I assay the stockpiles and sign off on tool orders. The lower shaft stays sealed until the crew can prove it safe.",
                    "I need someone to carry marked samples from the approach while I keep the assay furnace running. You can do that without entering the sealed workings.",
                    "I once approved a hurried delivery full of poor iron. A broken tool injured a friend. I check every batch now, even when the buyer complains about the wait.",
                    "Mind the sample trays. Similar-looking stone can produce very different iron.");
            case "briarbridge_forged_seal" -> new Profile(
                    "Magistrate Halven. I oversee Briarbridge's charter disputes and the accounts for its crossings.",
                    "I heard small ferry disputes before taking this office. A missing payment can cost a household its boat long before a court calls it a serious crime.",
                    "My seal appears on the disputed receipt. An independent witness should compare it with the register before I decide what the evidence means.",
                    "It would be convenient to call the ferryman mistaken. It would also make the next complaint harder for an honest person to bring.",
                    "Bring me what you can establish. We can leave the unanswered questions open.");
            case "aria" -> new Profile(
                    "I'm Aria Foxglove. I guide people along the western border paths. If you need directions, ask before you follow a ribbon tied to a branch.",
                    "I learned these routes carrying messages between villages. My sister learned them with me. "
                            + "When she went missing, I started following the signs she used to leave. Now I've found a trail that looks as though somebody wants me to notice it.",
                    "You don't know our trail signs yet. That may help: you can describe what is there while I keep trying to see my sister in every mark. "
                            + "I'm asking you to examine a trail with me, not to trust a stranger with your life on her word.",
                    "I used to correct her knots. Even when they held perfectly well. I keep thinking about the last ordinary thing I said to her, "
                            + "and how much of it was instruction. If we find her, I want to manage a greeting first.",
                    "Mind the snares along the western paths. Some have been set where a traveler would stop to read a sign.");
            case "seraphine" -> new Profile(
                    "Seraphine Vale. I used to arrange introductions for a Riverside household. These days I spend more time reading the terms attached to them.",
                    "My family served a court household whose invitations could bind a guest to service. That is why I read every clause, even when the host laughs at me. "
                            + "Riverside's collectors still trade on our name, and I want to know what they are collecting in it.",
                    "Someone from outside those households can ask questions without already owing a favour. You owe me none either. "
                            + "Read the record before you decide whether to help.",
                    "I was good at making frightened guests feel welcome. I told myself that was kindness. "
                            + "It is harder to remember the occasions when I helped someone feel comfortable enough to sign.",
                    "If anyone offers you a favour here, ask which household is offering it. Names belong in a bargain.");
            case "maera" -> new Profile(
                    "I'm Maera Quill, a surveyor trained at the Archive. You may step on the map's corner. Please avoid the ink.",
                    "I learned to copy the official roads before I was allowed to survey them. Out on the ground, travelers kept describing routes my copies omitted. "
                            + "Now I compare the charts with stars, old waymarks, and the people who actually walk between settlements.",
                    "I need someone to help check a route, not agree with my theory. You can tell me where my drawing fails. "
                            + "A stranger who asks an obvious question is often more useful than a colleague too polite to ask it.",
                    "My first survey came back covered in corrections. I was angry until I walked it again and found how many were right. "
                            + "I still dislike being corrected. I have learned to bring spare paper.",
                    "Are you looking for a road, or checking one? The second question is harder, but I can help with either.");
            case "cassia" -> new Profile(
                    "Cassia Flint. I served as a shield captain on Highwall's frontier. I'm here about the watch, if that is what brought you.",
                    "I grew up beyond the walls I later commanded. In winter, an open gate meant shelter; a closed one could mean a night on the exposed road. "
                            + "There are orders from my watch that I need to examine, and people beyond the gate whose account must be heard.",
                    "A soldier who served under me may still hear a command when I ask a question. You have no reason to obey me. "
                            + "That makes you useful as a witness, provided you are willing to challenge my account.",
                    "My family measured a good watch by who came home. I learned to measure it by whether the gate held. "
                            + "Those measures can disagree. I should have faced that before I held command.",
                    "If you're heading out through Highwall, check the watch warnings first. The pass has caught experienced people too.");
            case "lyra" -> new Profile(
                    "I'm Lyra Bell. I treat people in the Fenland villages and travel with the clinics. Is someone hurt, or have you come about the supplies?",
                    "I grew up carrying clean water for a traveling healer. We followed the landing bells from village to village. "
                            + "Now people along those same routes are becoming ill despite receiving medicine, and I need to understand what is reaching them.",
                    "I can examine a patient or follow a shipment, but I can't do both at once. You can help with the part that takes me away from the bedside. "
                            + "I'll explain what to look for before I ask you to touch anything used in treatment.",
                    "When I was young I thought a healer who stayed all night must save everyone. The woman who taught me stayed anyway, "
                            + "including on the nights when she couldn't. I understand that better now. I don't find it easier.",
                    "Give me a moment to clean my hands. Then tell me what you need.");
            case "samir" -> new Profile(
                    "Samir Dawn. My family tends a sacred fire vessel on the southern shrine roads. I was trained to keep its rites and maintain its seal.",
                    "Travelers warmed themselves at our family's vessel before I was old enough to tend it. We called its flame a guest and recited words of welcome. "
                            + "Lately I have been asking what that welcome means if the guest cannot leave. I need to examine the vessel before I claim to know the answer.",
                    "My relatives know every rite by heart. So do I. An outsider may notice the question we all learned to step around. "
                            + "You needn't share my faith to help me look carefully.",
                    "I like the morning rite. The cleaning, the first warmth, the sound of someone arriving cold and leaving comfortable. "
                            + "I am afraid of learning that something I loved was hurting the very presence I thanked for it.",
                    "You may warm your hands. An offering isn't the price of being cold.");
            case "rafiq" -> new Profile(
                    "Rafiq Glass. I perform with a blade and, when necessary, fight with one. I would rather you met me during a performance.",
                    "I earned my living around the southern glassmaking households, at celebrations and bouts arranged by their patrons. "
                            + "A man died in a duel I took part in. I left afterward. There are things about that duel I need to understand, and people I still owe an answer.",
                    "The people who knew me there have good reasons to doubt what I say. You have no reason to defend me. "
                            + "I am asking for company while I face them, not a witness willing to repeat my excuses.",
                    "I used to practise my bow longer than my apology. A graceful exit was a useful skill on a stage. "
                            + "I carried that habit somewhere it did terrible harm.",
                    "If you've come to hire a performer, I should warn you that my next engagement may be an apology.");
            case "vesper" -> new Profile(
                    "I'm Vesper Snowroot, one of Snowrest's grovekeepers. We tend the living shelter that holds warmth around the village through winter.",
                    "My family taught me when to prune, when to leave a root alone, and which bowls to set out at the spring waking. "
                            + "The grove is stirring too early now. Warmth is reaching places that should still be frozen, and I want to learn why before we cut healthy growth.",
                    "I know what the grove usually does. You can help me notice what I am dismissing because it looks familiar. "
                            + "Stay on the marked ground until we know which roots are sound.",
                    "I used to sleep through the first thaw because I knew someone older was listening for it. "
                            + "Now I lie awake trying to hear whether a branch has moved. I miss being allowed to be careless for one night.",
                    "Watch where you put your feet near the roots. Some of the thawed ground is softer than it looks.");
            case "calder" -> new Profile(
                    "Calder Reed. I'm a bridgewright from the Freeholds. I've worked mainland crossings since returning from the eastern harbor workshops.",
                    "In the Lantern Isles I learned to plan a crossing with its sanctuary keepers as well as its boat crews. "
                            + "A channel might carry a spirit procession on a night no cargo boat sailed. Here, a bridge I worked on failed. I need to examine both the workmanship and the passage it crossed.",
                    "I can read the joints, but I also signed off on work I now have to question. Another pair of eyes helps. "
                            + "You don't need to know bridgewrights' terms; point at what troubles you and I'll explain what it does.",
                    "I like the sound of an ordinary cart going over a sound deck. No applause, no speech, just wheels arriving on the other bank. "
                            + "Since the collapse I find myself counting every axle even after the cart has passed.",
                    "Keep clear of the loose boards. Tell me where you need to cross and I'll tell you what I know about it.");
            case "ms_wake_ashes" -> new Profile(
                    "I'm Maelis. I organise the shelter here at Oathstead. You have a place by our fire while we work out what happened at the road shrine.",
                    "People arrived here needing roofs before anyone had agreed whose camp this was. I started keeping a count of blankets and meals. "
                            + "Now I also have to ask how we keep the next attack from reaching those people.",
                    "You witnessed the shrine breach and survived it. That gives you something to tell us, not a duty to fight for us. "
                            + "If you choose to help, we begin with what you can show us at the burned altar.",
                    "I keep a little extra in the meal count. Someone always arrives after the pot is supposed to be empty.",
                    "There's room by the fire. Take a breath before we talk about the shrine.");
            case "ms_names_dust" -> new Profile(
                    "Selene, custodian of the ward records here in Archive City. Tell me what you brought and where you found it.",
                    "My father knew names for fields that the official maps left blank. I came to the Archive because I wanted those smaller accounts kept somewhere. "
                            + "I have since learned how much an institution can lose while insisting it has preserved everything.",
                    "I can compare a carving with the records. You can tell me about the place it came from. "
                            + "We need both accounts before I claim the drawing explains what happened to you.",
                    "I am responsible for these shelves. If the records are incomplete, I want to find that out before another person relies on them.",
                    "The reading station is open. Start with the place you came from; we'll find the right record together.");
            case "ms_watchtower_bells" -> new Profile(
                    "Odrick. I command Highwall's roadwatch. The city is behind those walls; my watch also answers for the people using the pass outside them.",
                    "I served on the outer roads before I held this post. Cairn keepers tend the burial mounds of former watchmen along those routes, "
                            + "and our living sentries still use the bell signals passed down through that watch. A broken signal puts supply carts and households at risk.",
                    "I have crews waiting for safe directions. I need someone to examine the damaged watch sites and report what is actually there. "
                            + "I will give you the names and signs to look for; I won't ask you to guess our customs.",
                    "A wall makes it easy to count the people inside. A commander has to remember the people it leaves out.",
                    "Are you coming from the pass? If you saw a damaged signal, tell me where.");
            case "ms_shrine_shadow" -> new Profile(
                    "I'm Solari, a priest of Sanctum's road shrines. We offer water to travelers and keep the vessels that warm their shelters.",
                    "I learned the guest rites from keepers who could recite a welcome while repairing its stone stand. "
                            + "At Sunken Guest Shrine, a fire vessel has begun burning its attendants. I know the rite we teach. I need to learn what has happened to that particular vessel.",
                    "You can examine the damaged shrine while I arrange the attendants' care. I will explain its fittings before I ask you to touch them. "
                            + "You need not offer worship to bring me a careful account.",
                    "If our welcome has become a prison, the right words will not make it hospitable again. We will have to change what we do.",
                    "Water first, if you need it. Then we can speak about the shrine.");
            case "ms_toll_ledger" -> new Profile(
                    "Mirella. I arrange Riverside's supply commitments and bridge tolls. A sealed promise is useful to me only if the cart eventually arrives.",
                    "My work began with disputes over small river shipments. The same arguments reach my desk now with grander seals on them. "
                            + "Oathstead needs grain along these roads, so I need to know what the toll stations are charging and what actually gets through.",
                    "A toll clerk will recite the answer they think I want. You can read the Briarbridge entries yourself and bring me the particulars. "
                            + "I am asking for an account of three entries, not an endorsement of how I run the river roads.",
                    "I am fond of a tidy agreement. It takes effort to remember that the people living under it may have very untidy needs.",
                    "If you've come about supplies, give me the destination before the quantity.");
            case "ms_bell_alone" -> new Profile(
                    "Ysra. I organise Belltower's navigation bells and ferry warnings. A bell tells a boat something only if its keeper knows what they are ringing for.",
                    "I learned the channels in boats small enough to turn between the reeds. In fog, a familiar bell could bring us home. "
                            + "Now the bell at Reedbank Landing sounds with nobody tending it, and I won't direct a crew toward a warning I cannot explain.",
                    "I need the fittings examined from the bank. You can look at the bell rope without having to steer a boat by its sound. "
                            + "I'll tell you which three fittings to check.",
                    "I know stories about the drowned calling boats off course. I also know loose iron can ring in a current. "
                            + "I don't want to mistake one for the other when someone's family is aboard.",
                    "Tell me which landing you're bound for. Some warnings need checking before anyone sails.");
            case "ms_orchard_ward" -> new Profile(
                    "I'm Elder Rowan. I tend Oakhaven's orchard and help settle how its harvest is shared.",
                    "We used to leave the first fallen apple for the stag that guarded this ground. Now the guardian called Rootmaw is attacking the workers. "
                            + "I know the custom we kept and the harm happening now. I do not yet know what changed the stag.",
                    "The orchard workers cannot tend the trees while Rootmaw attacks them. You can help make the ground safe enough for us to return. "
                            + "That won't by itself explain why the old protection failed.",
                    "I have spent years telling children that this orchard shelters us. I need to earn the right to say it again.",
                    "Take care near the orchard. Its guardian no longer lets our workers pass.");
            case "ms_names_cold_stone" -> new Profile(
                    "Hollis. I keep burial records in Archive City and help families find their dead at Stonegate.",
                    "The northern burial rites name the person whose duty has ended. At Stonegate, some of those names have been damaged, "
                            + "and the Warden is attacking visitors. I have the records; I cannot safely examine the crypt while it stands against us.",
                    "A person with a weapon can reach places a gravekeeper carrying a book cannot. I need the Warden stopped so the burial work can continue. "
                            + "I won't claim the records alone explain what it has become.",
                    "Families sometimes remember a nickname when the ledger preserves only a formal name. I write both down. One was used by someone who loved them.",
                    "Are you looking for a burial record? Tell me the name you know; it needn't be the name the ledger uses.");
            case "ms_cold_road" -> new Profile(
                    "Captain Elric. I organise Snowrest's road supplies. Most of that work is counting loads and finding people willing to haul them.",
                    "Snowrest can shelter people through winter only if its stores arrive before the pass becomes too dangerous. "
                            + "The Hailback Broodmother is blocking that approach now. A store list won't bring a cart through a mountain predator.",
                    "I need help clearing the supply approach. My drivers know their animals and loads; that doesn't make them hunters. "
                            + "I'll show you the pass where the broodmother has been sighted.",
                    "I count an extra blanket for a late traveler. I would rather explain an unused blanket in spring than a locked door in winter.",
                    "If you have news of Snowrest's winter pass, I'm listening.");
            case "ms_missing_bell_rope" -> new Profile(
                    "Nessa, Glimmerfen's bellwright. I repair the bells and the supports that keep them above floodwater.",
                    "I learned to cast a bell before anyone would let me tune one. The old warning line here rests on foundations I did not build. "
                            + "Before I hang new weight from them, I need their condition checked.",
                    "You can examine the two marked foundations while I prepare the repair work. Tell me what you find, even if it means I have to change my plan. "
                            + "The people following that bell will never see the footing beneath it.",
                    "I prefer a plain bell that lasts to a beautiful one nobody can safely ring.",
                    "Mind the tools. If you're here about the warning line, I need an account of its foundations.");
            case "ms_blackvault_mark" -> new Profile(
                    "Damar, an ash-scribe in Redcairn. I study how the old ward network stored the power it could no longer use safely.",
                    "Blackvault was built to hold those spent remnants. Its maintenance books describe a repository, not a source anyone should casually open. "
                            + "Sareth now blocks access there, and I need to learn what has been disturbed.",
                    "I can read the maintenance records. I can't reach the repository while Sareth controls its approach. "
                            + "If you help recover the Stone of Ash, I will tell you what the books support and where they stop being useful.",
                    "I mend cracked cups when I need to stop reading. You can test a cup with water. Old accounts are rarely that obliging.",
                    "Come away from the ash samples before we speak. I haven't finished identifying them.");
            default -> null;
        };
    }

    public static List<MainStoryContent.Topic> topics(Npc npc, Quest q, int relationship) {
        Profile p = profile(npc);
        if (p == null) return List.of();
        boolean workedTogether = q != null && (q.completed || npc.questId() != null && !q.id.equals(npc.questId()));
        List<MainStoryContent.Topic> topics = new ArrayList<>();
        topics.add(topic("How did you come to this work?", p.background()));
        topics.add(topic(workedTogether || relationship >= 20 ? "Why do you need my help with this?"
                : "We barely know each other. Why ask me?", p.reasonToAsk()));
        if (npc.recruitId() == null || relationship >= 20 || workedTogether)
            topics.add(topic("What has this work cost you personally?", p.privateHistory()));
        return List.copyOf(topics);
    }

    private static MainStoryContent.Topic topic(String question, String answer) {
        return new MainStoryContent.Topic(question, answer, "", List.of());
    }
}
