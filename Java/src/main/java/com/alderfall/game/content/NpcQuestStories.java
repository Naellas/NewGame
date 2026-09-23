package com.alderfall.game;

import java.util.List;
import com.alderfall.game.map.WorldMap;

/** Local requests are incidents in a resident's life, not descriptions of quest mechanics. */
public final class NpcQuestStories {
    private NpcQuestStories() { }

    public record Story(String opening, String question, String background, String concern,
                        String accept, String waiting, String ready, String aftermath) { }

    public static Story story(Quest q) {
        if (q.id.equals("hay_for_horses")) return new Story(
                "The watch came back on foot. Their horses had started chewing the leather traces, and the riders finally thought to ask me about feed. "
                        + "I've put aside three dry bales for them. Now my own cart has a split axle, so the hay is still out in the field.",
                "Why are you feeding the watch's horses?",
                "My daughter takes our eggs along the Oakhaven road. A mounted patrol can reach her before a man on foot even hears the shouting. "
                        + "I promised the feed myself. The captain can argue about payment after the horses have eaten.",
                "My father left the last forkful for the field's keeper, the spirit he believed sheltered our crops. "
                        + "I still do it. Keeping faith with a field ought to include feeding the creatures that work around it.",
                "I'll bring your three bales in.",
                "I've kept a space dry beside the stable. Have you reached the hay yet?",
                "That's dry hay. Good. Set it where I can check for mould before it goes in the racks.",
                "Three sound bales. I'll feed the watch mounts from these and keep the damp hay away from them. "
                        + "You've spared me another evening trying to mend an axle with one hand and hold a horse with the other.");
        if (!q.id.startsWith("weekly_")) return null;
        return switch (q.title) {
            case "Roadside Trouble" -> new Story(
                    "A carrier turned back with a cut harness. He told me two goblin scouts waited until he stopped to retie the load. "
                            + "The next cart carries supplies we have already paid for. Its driver wants the scouts dealt with before leaving.",
                    "Did the carrier actually see the scouts?",
                    "He described two goblins crouched beside his wheels, then showed me the severed strap. I saw the strap; the account of the attack is his. "
                            + "I've marked the place he gave me so you can find them.",
                    "The driver is willing to work, but he won't take his youngest along that road now. I can't ask him to risk both their lives for my order.",
                    "I'll look for those two scouts.", "The carrier is still waiting for an answer about the scouts.",
                    "You've dealt with both? Tell me where you found them.", "I'll pass your report to the carrier. He can decide when to take his cart out again.");
            case "Market Errand" -> new Story(
                    "The grain seller has put aside three sheaves for me. I meant to collect them myself, but the work here has run over again. "
                            + "That grain was promised to the shared oven; I don't want the baker opening another household's winter sack because I was late.",
                    "Whose grain would I be taking?",
                    "The three marked sheaves are my order. The grower bundled them separately for collection. Leave the standing crop and anyone else's bundles alone.",
                    "I hate owing a neighbour an apology over something I could have arranged properly. Help me keep this promise before it becomes one.",
                    "I'll collect your three sheaves.", "The grower is keeping my bundles apart from the rest of the crop.",
                    "Those are the sheaves. Let me take the weight.", "I'll take this grain to the shared oven. The baker can leave that winter sack closed a little longer.");
            case "Old Marks" -> new Story(
                    "A family asked me to help find a grave. They remember the burial, but the lettering is wearing away, and two stones could fit their account. "
                            + "I'd rather send them with a clear description than let them stand there guessing where to leave their flowers.",
                    "What can the stones tell us?",
                    "The family described a cut border and a small cup carved below the name. Compare the two marked stones and note what survives. "
                            + "We may still need the family's help to identify the right grave.",
                    "People still bring food and flowers to their dead here. Whatever answers them, a family deserves to know whose grave it is tending.",
                    "I'll examine the two grave marks.", "A drawing of the damaged marks would help more than a guess at the name.",
                    "You checked both stones? Show me what you could make out.", "I'll take your descriptions back to the family. They can compare them with what they remember.");
            case "Camp Smoke" -> new Story(
                    "I watched smoke rise from two raider fires just before a laden wagon passed. One fire stopped when the wagon turned back. "
                            + "I think the fires are a signal between their lookouts. I'd like that warning line broken before another wagon tries the road.",
                    "Why do you think the fires are signals?",
                    "Their timing is what worries me. I saw the smoke change with the wagon's movement; I didn't hear any order. "
                            + "The two fires are marked. Put them out if you can reach them safely.",
                    "A wagon driver can't watch the hillside and mind the horses at the same time. Someone is using that against them.",
                    "I'll put out the two marked fires.", "Watch the approaches to those fires. Their keepers may still be nearby.",
                    "Both fires are out? Tell me whether anyone was tending them.", "That interrupts the signals I saw. I'll warn the drivers that the raiders may try another method.");
            case "Boundary Stones" -> new Story(
                    "Two neighbours have asked me which path belongs to the common. They both remember following the same pair of old stones. "
                            + "Now each insists the stones pointed a different way. I want their marks checked before someone starts pulling up a hedge.",
                    "Are these boundary stones or shrine stones?",
                    "They carry ward marks as well as the old boundary cuts. Those are different things: a mark meant to protect a path doesn't tell us who owns it. "
                            + "Look at both marked stones and bring back what is actually carved there.",
                    "I know both households. Neither can spare a strip of planting ground, which is why neither is listening very well.",
                    "I'll check both stones before anyone moves them.", "Leave the stones where they stand. I need their marks read, not the boundary changed.",
                    "You examined both? Tell me which marks survived.", "I'll bring your account to the neighbours. Reading the stones is a beginning; agreeing on the boundary is still their work.");
            case "Herbs Before Rain" -> new Story(
                    "The last jar of salve is nearly empty. I checked it after a labourer came in with a split palm, and found barely enough left to cover the wound. "
                            + "There are three good herb clumps at the gathering place I've marked. I need them before I can prepare another batch.",
                    "Is somebody waiting for treatment?",
                    "The cut hand is already dressed. This is for the next injury, and I'd rather have salve ready than send a hurt person out to gather its ingredients.",
                    "Useful herbs need to be gathered cleanly. Leave the roots of the unmarked plants so there will be another crop.",
                    "I'll gather the three herb bundles.", "Keep dirt out of the bundles. I can dry clean leaves; I can't make grit safe for a wound.",
                    "You've brought the herbs? Let me check them before they go in the pot.", "I'll sort and prepare these. Bringing ingredients was the first part; the salve still needs making.");
            case "Missing by the Tree Line" -> new Story(
                    "A charcoal carrier came to me shaking. She had seen a traveler climb onto a fallen trunk with a wolf snapping below. "
                            + "She dropped her load and ran for help. The traveler was still there when she left. I've marked the place she described.",
                    "Who saw the traveler, and why did she come to you?",
                    "A carrier who brings charcoal into the settlement. She found me at work and asked for somebody who could face the wolf. "
                            + "I haven't seen the traveler myself, and she didn't know their name. That's why I'm asking you to check her account while there may still be time.",
                    "She keeps apologising for running. I'd rather she had reached us than climbed onto that trunk beside the traveler. "
                            + "Please deal with the wolf at the rescue marker so the traveler has a way out.",
                    "I'll find the traveler and deal with the wolf.", "The carrier described a fallen trunk. Use the rescue marker to reach the place she meant.",
                    "The wolf is dealt with? Tell me what happened at the rescue site.", "I'll tell the carrier you dealt with the wolf at the place she marked. She deserves to know that coming for help mattered.");
            case "Hold the Storehouse" -> new Story(
                    "The supply keeper found a cut rope on the outside cache, then spotted goblins watching from cover. "
                            + "We've moved what we can, but somebody needs to stand at the two marked approaches while the remaining stores are secured.",
                    "Why leave supplies outside the settlement?",
                    "Carters use the cache to split loads before bringing them in. Hauling everything back at once would leave the slowest carts stranded. "
                            + "The keeper's warning gives us a chance to protect the approaches.",
                    "Those supplies were bought a little at a time by people who can't easily replace them. I don't want their winter plans decided by two raiders.",
                    "I'll hold the two marked approaches.", "Keep the raiders away from the marked supply points.",
                    "You held both approaches? Tell me what came against you.", "I'll give the keeper your report. We'll need a better storage arrangement before another load arrives.");
            case "Hidden Clue" -> new Story(
                    "A delivery has gone missing. I found a torn wrapper where the carrier usually rests, and it still had our binding cord tied around it. "
                            + "I've marked two places where more of the wrapping may have caught. I'd like them searched before I accuse the carrier of selling the load.",
                    "What makes you think it was your delivery?",
                    "I tied that cord myself and used a double knot around the label. That identifies the wrapping. "
                            + "It doesn't tell me whether the carrier dropped the load, was robbed, or took it elsewhere.",
                    "An accusation spreads faster than a correction. I want something better than my own temper before I put a neighbour's livelihood at risk.",
                    "I'll search the two marked places.", "Look for the binding cord and pieces of wrapping at both search marks.",
                    "You searched both places? Show me what you found.", "I'll keep your findings with the torn wrapper. We can ask the carrier about these instead of starting with an accusation.");
            case "Ask the Neighbors" -> new Story(
                    "A store sack split while it was being moved. By the time the spilled grain was swept up, people were saying the carrier had been stealing from it. "
                            + "Two neighbours were close enough to see what happened. Will you ask them before that story costs someone their work?",
                    "Did you see the sack split?",
                    "No. I saw the grain afterward and heard the accusation. I want the two witnesses' accounts kept separate so we can tell what each actually saw.",
                    "I have worked beside people who lost a week's pay over a rumour. Once the story reaches the market, nobody remembers who first guessed.",
                    "I'll ask the two witnesses what they saw.", "Ask the witnesses about the sack itself, not what other people said afterward.",
                    "You've heard both accounts? Tell me where they agree and where they don't.", "I'll use those accounts when the accusation comes up again. I won't pretend they prove more than the witnesses saw.");
            case "Plain Report" -> new Story(
                    "I've written down the supply keeper's warning: a cart left late, and its load has not been counted back in. "
                            + "People keep repeating that as 'the cart was stolen'. I want to state the warning plainly to somebody before I pass it on.",
                    "What do you actually want me to confirm?",
                    "That you've heard the distinction: late and uncounted, not proven stolen. This is a conversation about the warning I have, not a report of a journey you haven't made.",
                    "An alarm sends people away from their work. I'll raise one when it is needed, but I want the reason to survive being repeated.",
                    "I'll hear the warning and repeat it back clearly.", "Say what the keeper reported, without turning a missing count into a theft.",
                    "Yes. That is the warning I meant to give.", "Thank you for hearing it through. I'll pass on those words and leave the cause open.");
            case "Safe Passage" -> new Story(
                    "A carter asked whether the old path would take a loaded handcart. I was about to say yes, then realised I haven't walked its outer stretch since the weather turned. "
                            + "Will you check the two marked waypoints before I send someone slower than us along it?",
                    "Am I escorting the carter?",
                    "First I need the route checked. The carter is waiting for my answer; you won't be leading them on this walk. "
                            + "Reach both waypoints and report back so I can give advice based on a recent visit.",
                    "It's easy to call a path safe when you can climb out of a washout. A person pulling a loaded cart has fewer choices.",
                    "I'll walk both waypoints before you advise them.", "Both waypoints matter. A clear first turn tells us nothing about the second.",
                    "You've reached both waypoints? Tell me about the route.", "I'll pass your account to the carter. They can judge it against the weight they're carrying.");
            case "Sealed Packet" -> new Story(
                    "I've sealed a statement about the supplies I received. I want someone outside the household to hear me acknowledge it, "
                            + "so nobody can later say I slipped extra terms into the arrangement.",
                    "Where am I taking the packet?",
                    "Nowhere on this errand. We make the acknowledgment here, together. There is no separate parcel for you to carry across the country.",
                    "My neighbours trust me with their shares. That is a reason to make the terms clear while everyone is calm.",
                    "I'll witness your acknowledgment here.", "Hear the acknowledgment and confirm it in this conversation.",
                    "You heard the terms. Shall we finish the acknowledgment?", "The acknowledgment is made. I can keep the statement without claiming you agreed to anything beyond witnessing it.");
            case "Hard Truth" -> new Story(
                    "An apprentice admitted taking grain from the shared stores for a hungry relative. I counted the missing share myself. "
                            + "The apprentice says it will be repaid, but I haven't told the other households yet. I need another person's judgment before I act.",
                    "What is certain, and what has only been promised?",
                    "The missing grain and the admission are certain. Repayment is a promise. I haven't received any, and I haven't investigated the relative's circumstances myself.",
                    "If I conceal it, everyone else carries a loss they never agreed to. If I name the apprentice publicly, that household may lose its only paid work. "
                            + "Help me choose what to recommend; saying it here won't carry out the decision for us.",
                    "Let's decide what you should do about the grain.", "We still need to agree how the missing grain and the apprentice's admission should be handled.",
                    "That is the recommendation we agreed on. I want to hear it once more before I take it to anyone.", "I've heard your advice. Speaking to the households and arranging repayment are still ahead of me.");
            case "Missing off the Road" -> new Story(
                    q.target + " and I argued about who should finish the day's work. I said something unkind, and they walked out. "
                            + "I know the place they headed toward, but they haven't come back. Will you speak to them? They may listen more readily to you.",
                    "Are you asking me to bring them back by force?",
                    "No. Find " + q.target + ", hear them out, and let me know they're safe. I owe them an apology; that doesn't give me a claim on where they stand.",
                    "I'd like to be right about the argument. I'd like them home safely more. I should have remembered that before opening my mouth.",
                    "I'll find " + q.target + " and hear them out.", "Use the marked meeting place. Ask what they need before repeating my side of the argument.",
                    "You spoke to " + q.target + "? How are they?", "Thank you for speaking to them. I still need to make my own apology; I won't count your visit as forgiveness.");
            default -> new Story(
                    "Three mountain goats have been breaking the roadside rails. I watched one push the same loose post until it came free, "
                            + "then start on the next. A loaded cart uses that edge; the repairs won't last while the goats keep returning.",
                    "Could something be driving the goats down?",
                    "Possibly, but I haven't been up to their grazing ground. I saw the goats at the rails. I can identify the immediate danger without pretending to know its cause.",
                    "I like a stubborn creature until I'm the one paying for its entertainment. Clear the three marked goats before another cart reaches that damaged edge.",
                    "I'll deal with the three goats at the damaged road.", "The three encounter marks show where the goats have been returning.",
                    "All three dealt with? Tell me where the rail is worst.", "I'll arrange another repair. At least this time the workmen won't have horns at their backs.");
        };
    }

    public static String occupation(Npc npc) {
        if (npc == null) return "";
        if (npc.name().equals("Farmer Joss")) return "I'm a farmer. I know feed and weather better than I know captains, which is why I ask about the horses first.";
        if (npc.job() != null) return switch (npc.job().kind()) {
            case FARMER -> "I work the fields here. I like the rows straight and my promises kept; lately only the rows have been cooperating.";
            case WOODCUTTER -> "I cut timber here. I can judge a bad branch at a glance; judging when a neighbour needs help takes longer.";
            case HERBALIST -> "I gather and prepare herbs here. I label every bundle twice. People laugh until two similar leaves do very different things.";
        };
        if (npc.shopId() != null) return "I trade here. I remember who paid late, but I also remember who came back to pay at all.";
        return "I live and work here. I have to meet these same neighbours after the argument is over, so I try to leave room for that.";
    }

    public static MainStoryContent.Topic belief(Npc npc) {
        var region = npc == null ? RegionalSettlementIdentity.Region.NONE : RegionalSettlementIdentity.region(npc.mapId());
        String answer = switch (region) {
            case NORTH -> "At the winter cairns we speak the names of people who kept others alive, not only warriors. "
                    + "My family says those dead remember whether we leave room for a stranger. I can't prove they hear us. I can make sure the stranger isn't left outside.";
            case SUN -> "The well-road shrines offer a traveler water before asking their business. "
                    + "The old stories include fire spirits among those guests. I was taught that offering shelter gives you a duty, not ownership of whoever accepts it.";
            case FEN -> "We teach children to look at the flood marks before following a bell. There are stories of drowned voices ringing from below the water. "
                    + "I don't know which sounds belong to the dead. I do know why our bellkeepers insist that a warning must have a living person answer for it.";
            case FREEHOLDS -> "At the crew cairns we name the deckhands along with the captains. A shipowner still owes help when another crew is stranded. "
                    + "People argue about whether the sea remembers broken promises. The families left ashore certainly do.";
            case RIVER -> "Around these river roads, people tell of woodland hosts who offer fruit and ask for years of service afterward. "
                    + "I haven't sat at their tables. The useful part of the tale is to ask what a gift requires before accepting it. I owe you the same plain dealing.";
            default -> "In the orchard country, families leave the first sound fruit for the local grove spirit and name the households sharing the rest. "
                    + "Some neighbours believe the trees listen. Others come for the company. Either way, you hear who has an empty basket before you go home.";
        };
        return new MainStoryContent.Topic("What do people here owe one another?", answer, "", List.of());
    }

    public static List<MainStoryContent.Topic> topics(Quest q, Npc npc) {
        Story s = story(q);
        if (s == null) return List.of();
        return List.of(new MainStoryContent.Topic(s.question(), s.background(), "", List.of(
                        new MainStoryContent.Topic("And what worries you most?", s.concern(), "", List.of()))),
                new MainStoryContent.Topic("What is your work like here?", occupation(npc), "", List.of(belief(npc))));
    }

    public static String subject(Quest q) {
        Story s = story(q);
        if (s == null) return QuestNarrative.subject(q);
        return q.completed ? s.aftermath() : q.ready() ? s.ready() : q.accepted ? s.waiting() : q.activeStartDialog();
    }

    public static String grainDecision(String outcome, boolean response) {
        return switch (outcome) {
            case "truth" -> response ? "You advise telling the store households about both the missing grain and the apprentice's admission. I'll need to hear them out; this conversation hasn't informed them yet."
                    : "Tell the households what the apprentice admitted.";
            case "protect" -> response ? "You advise reporting the missing grain without naming the apprentice while I check the relative's circumstances. That keeps a name private, not the loss itself."
                    : "Report the loss; withhold the name while you check the story.";
            case "mercy" -> response ? "You advise asking the apprentice for a workable repayment plan before seeking punishment. No grain has been repaid yet; I will have to ask what they can manage."
                    : "Ask for repayment before you seek punishment.";
            default -> response ? "You advise taking the counted loss and the admission to the store keeper. I'll ask for a hearing, not pretend a sentence has already been passed."
                    : "Take the missing share and the admission to the store keeper.";
        };
    }

    /** Text-only replacement: preserve objective identity, progress, cargo, and rewards. */
    public static Quest refine(Quest q) {
        return refine(q, null, null);
    }

    public static Quest refine(Quest q, Npc giver, WorldMap world) {
        Story s = story(q);
        if (s == null) return q;
        String opening = s.opening();
        // Weekly offers become available only after this giver's authored side request is complete.
        if (giver != null && giver.questId() != null) {
            Quest previous = GameData.QUESTS.get(giver.questId());
            if (previous != null && previous.type == Quest.QuestType.SIDE)
                opening = QuestNarrative.clean(previous.stages.getLast().completeDialog()) + " There's something else I need to tell you. " + opening;
        }
        String instruction = q.progressDialog;
        if (world != null) {
            String kind = q.objectiveLocationKind;
            String destination = "";
            if (kind != null && !kind.isBlank()) {
                TilePoint point = world.objectivePoint(kind, q.objectiveLocationIndex, 0, q.objectiveAsset);
                var closest = world.settlementSites().stream().min(java.util.Comparator.comparingInt(site ->
                        Math.abs(site.x() - point.x()) + Math.abs(site.y() - point.y()))).orElseThrow();
                String vertical = point.y() < closest.y() - 2 ? "north" : point.y() > closest.y() + 2 ? "south" : "";
                String horizontal = point.x() < closest.x() - 2 ? "west" : point.x() > closest.x() + 2 ? "east" : "";
                String direction = vertical + horizontal;
                String label = world.locationSites(kind).stream().min(java.util.Comparator.comparingInt(site ->
                        Math.abs(site.x() - point.x()) + Math.abs(site.y() - point.y()))).map(WorldMap.LocationSite::label).orElse("marked site");
                destination = " Look for " + label + (direction.isEmpty() ? " beside " : " " + direction + " of ")
                        + closest.label() + "; I've marked the exact spot on your map.";
            }
            instruction = switch (q.title) {
                case "Plain Report" -> "Repeat the supply keeper's warning to me here: late and uncounted does not mean proven stolen.";
                case "Sealed Packet" -> "Witness my acknowledgment here. There is no parcel to collect elsewhere.";
                case "Hard Truth" -> "Choose what to recommend about the missing grain. We are agreeing on advice, not carrying it out.";
                case "Ask the Neighbors" -> "Ask two neighbours about the split grain sack, then return with their accounts.";
                case "Missing off the Road" -> "Speak to " + q.target + " and ask whether they are safe." + destination;
                case "Missing by the Tree Line" -> "Deal with the wolf at the rescue marker so the trapped traveler has a way out." + destination;
                case "Safe Passage" -> "Visit both route waypoints before returning with your report." + destination;
                default -> s.accept().replaceFirst("^I'll ", "Please ").replaceFirst("^Let's ", "Please ") + destination;
            };
        }
        var old = q.stages.getFirst();
        var stage = new Quest.QuestStage(old.id(), old.title(), old.target(), old.needed(), old.objectiveKind(),
                old.objectiveMapId(), old.objectiveLocationKind(), old.objectiveLocationIndex(), old.objectiveAsset(),
                old.monsterKey(), old.targetNpcId(), old.branchOutcomeKey(), opening, instruction, s.ready(), s.aftermath());
        Quest changed = new Quest(q.id, q.title, opening, q.target, q.needed, q.rewardGold, q.rewardXp,
                q.objectiveKind, q.objectiveMapId, q.objectiveLocationKind, q.objectiveLocationIndex, q.objectiveAsset,
                q.monsterKey, opening, instruction, s.ready(), s.aftermath(), q.type, q.chainOwnerId, q.nextQuestId,
                q.targetNpcId, q.branchOutcomeKey, List.of(stage));
        changed.accepted = q.accepted;
        changed.completed = q.completed;
        changed.progress = q.progress;
        changed.contentRevision = q.contentRevision;
        changed.observedStages.addAll(q.observedStages);
        changed.cargo.putAll(q.cargo);
        return changed;
    }
}
