from __future__ import annotations

from dataclasses import dataclass
from random import randint, random

from .data import MONSTERS, item_name
from .effects import EffectStack, STATUS_EFFECTS, status_hints_for_ability
from .entities import Actor
from .monsters import MonsterAbility, monster_abilities_for


@dataclass
class FloatingText:
    text: str
    x: int
    y: int
    life: int
    color: str


@dataclass
class MonsterEffect:
    kind: str
    source_index: int
    target_party_index: int
    timer: int
    name: str


class Battle:
    def __init__(self, player: Actor, monster_keys: str | list[str], terrain: str = "g", allies: list[Actor] | None = None) -> None:
        keys = [monster_keys] if isinstance(monster_keys, str) else list(monster_keys)
        self.enemies = [self._make_monster(key) for key in keys]
        self.enemy_keys = keys
        self.selected_enemy_index = 0
        self.xp_reward = sum(int(MONSTERS[key]["xp"]) for key in keys)
        base_gold = sum(int(MONSTERS[key]["gold"]) for key in keys)
        self.player = player
        treasure_bonus = 1.0 + 0.10 * self.player.skill_rank("scavenger")
        self.gold_reward = int(base_gold * treasure_bonus)
        self.allies = allies or []
        self.loot: dict[str, int] = {}
        self._roll_loot()
        self.terrain = terrain
        intro = self.monster.name if len(self.enemies) == 1 else f"{len(self.enemies)} enemies"
        self.log = [f"{intro} appear!"]
        self.floaters: list[FloatingText] = []
        self.shake = 0
        self.monster_offset = 0
        self.player_offset = 0
        self.finished = False
        self.victory = False
        self.guards: dict[int, int] = {}
        self.player_lunge = 0
        self.monster_lunge = 0
        self.flash_timer = 0
        self.monster_fade = 255
        self.effect_kind = ""
        self.effect_timer = 0
        self.effect_source: Actor | None = None
        self.effect_target: Actor | None = None
        self.turn_order = self.party_members() + self.enemies
        self.turn_index = 0
        self.turn_started = False
        self.enemy_action_delay = 0
        self.enemy_lunges: dict[int, int] = {}
        self.monster_effect: MonsterEffect | None = None
        self._enemy_keys_by_id: dict[int, str] = {id(enemy): key for enemy, key in zip(self.enemies, keys)}
        self._monster_cooldowns: dict[int, dict[str, int]] = {}
        self._statuses: dict[int, dict[str, EffectStack]] = {}

    @property
    def party(self) -> list[Actor]:
        return self.party_members()

    @property
    def monster(self) -> Actor:
        return self.current_enemy()

    def party_members(self) -> list[Actor]:
        return [self.player] + self.allies

    def _make_monster(self, key: str) -> Actor:
        data = MONSTERS[key]
        monster = Actor(
            data["name"],
            data.get("sprite", key),
            "Monster",
            data["hp"],
            data["hp"],
            0,
            0,
            data["attack"],
            data["defense"],
        )
        monster.inventory = dict(data.get("loot", {}))
        return monster

    def _roll_loot(self) -> None:
        for key in self.enemy_keys:
            for item_key, chance in MONSTERS[key].get("loot", {}).items():
                if random() * 100 < int(chance):
                    self.loot[item_key] = self.loot.get(item_key, 0) + 1
        if self.player.skill_rank("forager") and random() < 0.12 * self.player.skill_rank("forager"):
            self.loot["potion_small"] = self.loot.get("potion_small", 0) + 1
        if self.player.has_perk("fieldcraft") and self.loot:
            first = next(iter(self.loot))
            self.loot[first] += 1

    def living_enemies(self) -> list[Actor]:
        return [enemy for enemy in self.enemies if enemy.alive]

    def living_party(self) -> list[Actor]:
        return [actor for actor in self.party_members() if actor.alive]

    def is_party_actor(self, actor: Actor | None) -> bool:
        return actor is not None and any(actor is member for member in self.party_members())

    def is_enemy_actor(self, actor: Actor | None) -> bool:
        return actor is not None and any(actor is enemy for enemy in self.enemies)

    def active_actor(self) -> Actor | None:
        if self.finished or not self.turn_order:
            return None
        self._skip_dead_turns()
        if not self.turn_order:
            return None
        return self.turn_order[self.turn_index]

    def is_party_turn(self) -> bool:
        actor = self.active_actor()
        return self.is_party_actor(actor) and bool(actor and actor.alive)

    def can_player_act(self) -> bool:
        self._ensure_turn_ready()
        return self.is_party_turn()

    def select_enemy(self, index: int) -> None:
        if self.finished or index < 0 or index >= len(self.enemies):
            return
        if not self.enemies[index].alive:
            return
        self.selected_enemy_index = index
        if len(self.living_enemies()) > 1:
            self.log.insert(0, f"Targeting {self.enemies[index].name}.")

    def cycle_target(self, direction: int = 1) -> None:
        living_indexes = [idx for idx, enemy in enumerate(self.enemies) if enemy.alive]
        if self.finished or len(living_indexes) <= 1:
            return
        if self.selected_enemy_index not in living_indexes:
            self.selected_enemy_index = living_indexes[0]
            return
        current_pos = living_indexes.index(self.selected_enemy_index)
        self.select_enemy(living_indexes[(current_pos + direction) % len(living_indexes)])

    def current_enemy(self) -> Actor:
        living = self.living_enemies()
        if not living:
            return self.enemies[-1]
        if 0 <= self.selected_enemy_index < len(self.enemies) and self.enemies[self.selected_enemy_index].alive:
            return self.enemies[self.selected_enemy_index]
        self.selected_enemy_index = self.enemies.index(living[0])
        return living[0]

    def get_statuses(self, actor: Actor) -> list[EffectStack]:
        bucket = self._statuses.get(id(actor), {})
        return sorted(bucket.values(), key=lambda stack: stack.effect.name)

    def tick(self) -> None:
        self.shake = max(0, self.shake - 1)
        self.monster_offset = int(self.shake * (-1 if self.shake % 2 else 1))
        self.player_offset = int((self.shake // 2) * (1 if self.shake % 2 else -1))
        self.player_lunge = max(0, self.player_lunge - 2)
        self.monster_lunge = max(0, self.monster_lunge - 2)
        self.flash_timer = max(0, self.flash_timer - 1)
        self.effect_timer = max(0, self.effect_timer - 1)
        for index in list(self.enemy_lunges):
            self.enemy_lunges[index] = max(0, self.enemy_lunges[index] - 2)
            if self.enemy_lunges[index] <= 0:
                del self.enemy_lunges[index]
        if self.monster_effect:
            self.monster_effect.timer -= 1
            if self.monster_effect.timer <= 0:
                self.monster_effect = None
        if not self.current_enemy().alive:
            self.monster_fade = max(0, self.monster_fade - 16)
        for floater in list(self.floaters):
            floater.life -= 1
            floater.y -= 1
            if floater.life <= 0:
                self.floaters.remove(floater)
        if not self.finished:
            self._ensure_turn_ready()
            self._tick_enemy_turn()

    def player_attack(self) -> bool:
        actor = self._active_party_actor()
        if not actor:
            return False
        self._set_guard(actor, False)
        target = self.current_enemy()
        raw = self._basic_attack_damage(actor)
        damage = self._deal_damage(actor, target, raw)
        self.floaters.append(FloatingText(f"-{damage}", self._floater_x(target), 295, 34, "#ffdddd"))
        self.log.insert(0, self._attack_message(actor, target, damage))
        self.shake = 10
        self.player_lunge = 18
        self.flash_timer = 4
        self.effect_kind = "strike"
        self.effect_timer = 12
        self.effect_source = actor
        self.effect_target = target
        if not target.alive:
            self.log.insert(0, f"{target.name} falls.")
            self._clear_statuses(target)
        self._finish_actor_turn(actor)
        return True

    def use_ability(self, index: int) -> bool:
        actor = self._active_party_actor()
        if not actor or index < 0 or index >= len(actor.abilities):
            return False
        ability = actor.abilities[index]
        if actor.mp < ability.cost:
            self.log.insert(0, f"{actor.name} does not have enough MP.")
            return False
        actor.mp -= ability.cost
        if ability.kind == "heal":
            self._set_guard(actor, False)
            amount = randint(max(4, ability.power - 4), ability.power + 4)
            amount += actor.skill_rank("channeling") * 4
            target = self.lowest_party_member() if ability.target == "ally" else actor
            before = target.hp
            target.hp = min(target.max_hp, target.hp + amount)
            healed = target.hp - before
            self._apply_ability_statuses(ability.name, ability.kind, actor, target, self.current_enemy())
            self.floaters.append(FloatingText(f"+{healed}", self._floater_x(target), 385, 34, "#b9f7c3"))
            self.log.insert(0, self._cast_message(actor, ability.name, f"{target.name} recovers {healed}."))
            self.effect_kind = "heal"
            self.effect_timer = 18
            self.effect_source = actor
            self.effect_target = target
        elif ability.kind == "defend":
            self._defend_actor(actor, ability.name, 2)
            self._apply_ability_statuses(ability.name, ability.kind, actor, actor, self.current_enemy())
        else:
            self._set_guard(actor, False)
            target = self.current_enemy()
            power = ability.power
            if actor is self.player and self.player.has_perk("spark_mastery"):
                power += 3
            power += actor.skill_rank("spellcraft") * 2
            raw = self._modify_outgoing_damage(actor, randint(power - 3, power + 5))
            damage = self._deal_damage(actor, target, raw)
            self.floaters.append(FloatingText(f"-{damage}", self._floater_x(target), 295, 34, "#ffe0a6"))
            self.log.insert(0, self._cast_message(actor, ability.name, f"deals {damage} to {target.name}."))
            self.shake = 14
            self.player_lunge = 22
            self.flash_timer = 5
            self.effect_kind = self._effect_for_ability(ability.name)
            self.effect_timer = 18
            self.effect_source = actor
            self.effect_target = target
            self._apply_ability_statuses(ability.name, ability.kind, actor, actor, target)
            if not target.alive:
                self.log.insert(0, f"{target.name} falls.")
                self._clear_statuses(target)
        if actor.has_perk("clarity"):
            actor.mp = min(actor.max_mp, actor.mp + 1)
        if actor.skill_rank("ether_flow"):
            actor.mp = min(actor.max_mp, actor.mp + actor.skill_rank("ether_flow"))
        self._finish_actor_turn(actor)
        return True

    def player_defend(self) -> bool:
        actor = self._active_party_actor()
        if not actor:
            return False
        mp_gain = 5 if actor.has_perk("shield_training") else 3
        mp_gain += actor.skill_rank("stalwart_guard")
        self._defend_actor(actor, "Defend", mp_gain)
        self._finish_actor_turn(actor)
        return True

    def player_use_item(self, item_name: str, heal: int = 0, mp: int = 0) -> bool:
        actor = self._active_party_actor()
        if not actor:
            return False
        if heal > 0:
            if self.player.has_perk("triage"):
                heal += 8
            heal += self.player.skill_rank("merchant_sense") * 3
            if self.player.skill_rank("battle_medic"):
                heal += 8
            before = actor.hp
            actor.hp = min(actor.max_hp, actor.hp + heal)
            healed = actor.hp - before
            self.floaters.append(FloatingText(f"+{healed}", self._floater_x(actor), 365, 32, "#b9f7c3"))
        if mp > 0:
            mp += self.player.skill_rank("merchant_sense") * 2
            before_mp = actor.mp
            actor.mp = min(actor.max_mp, actor.mp + mp)
            gained = actor.mp - before_mp
            self.floaters.append(FloatingText(f"+{gained} MP", self._floater_x(actor), 337, 30, "#bdd2ff"))
        self._set_guard(actor, False)
        self.effect_kind = "item"
        self.effect_timer = 14
        self.effect_source = actor
        self.effect_target = actor
        self.log.insert(0, f"{actor.name} used {item_name}.")
        self._finish_actor_turn(actor)
        return True

    def allies_turn(self) -> None:
        self.log.insert(0, "Allies now act on their own turns.")

    def monster_turn(self) -> None:
        actor = self.active_actor()
        if not self.is_enemy_actor(actor):
            actor = next(iter(self.living_enemies()), None)
        if actor:
            self._enemy_take_turn(actor)

    def lowest_party_member(self) -> Actor:
        living = self.living_party()
        return min(living, key=lambda actor: actor.hp / max(1, actor.max_hp)) if living else self.player

    def _win(self) -> None:
        if self.finished:
            return
        self.finished = True
        self.victory = True
        self.player.gold += self.gold_reward
        for item_key, amount in self.loot.items():
            self.player.add_item(item_key, amount)
        level_notes = self.player.gain_xp(self.xp_reward)
        recovered = [actor.name for actor in self.party_members() if not actor.alive]
        for actor in self.party_members():
            if not actor.alive:
                actor.hp = max(1, actor.max_hp // 4)
        self.log.insert(0, f"Victory! +{self.xp_reward} XP, +{self.gold_reward} gold.")
        if recovered:
            self.log.insert(0, f"{', '.join(recovered)} recover after the fight.")
        for item_key, amount in self.loot.items():
            suffix = f" x{amount}" if amount > 1 else ""
            self.log.insert(0, f"Loot found: {item_name(item_key)}{suffix}.")
        for note in reversed(level_notes):
            if note.startswith("Level "):
                self.log.insert(0, f"Level up! {note}")
            else:
                self.log.insert(0, note)

    def _lose(self) -> None:
        if self.finished:
            return
        self.finished = True
        self.victory = False
        self.log.insert(0, "Your party falls. Choose Revive to return to town.")

    def _check_finished(self) -> bool:
        if self.finished:
            return True
        if not self.living_enemies():
            self._win()
            return True
        if not self.living_party():
            self._lose()
            return True
        return False

    def _skip_dead_turns(self) -> None:
        if not self.turn_order:
            return
        for _ in range(len(self.turn_order)):
            if self.turn_order[self.turn_index].alive:
                return
            self.turn_index = (self.turn_index + 1) % len(self.turn_order)
            self.turn_started = False
            self.enemy_action_delay = 0

    def _ensure_turn_ready(self) -> None:
        if self._check_finished():
            return
        self._skip_dead_turns()
        if self._check_finished():
            return
        actor = self.turn_order[self.turn_index]
        if self.turn_started:
            return

        self.turn_started = True
        self._trigger_turn_statuses(actor)
        if self._check_finished():
            return
        if not actor.alive:
            self.log.insert(0, f"{actor.name} falls before acting.")
            self._clear_statuses(actor)
            self._advance_turn(actor)
            self._check_finished()
            return

        self.log.insert(0, f"{actor.name}'s turn.")
        if self.is_enemy_actor(actor):
            self.enemy_action_delay = 12

    def _tick_enemy_turn(self) -> None:
        actor = self.active_actor()
        if not self.turn_started or not self.is_enemy_actor(actor) or not actor:
            return
        if self.enemy_action_delay > 0:
            self.enemy_action_delay -= 1
            return
        self._enemy_take_turn(actor)

    def _active_party_actor(self) -> Actor | None:
        if self.finished:
            return None
        self._ensure_turn_ready()
        actor = self.active_actor()
        if self.is_party_actor(actor) and actor and actor.alive:
            return actor
        return None

    def _finish_actor_turn(self, actor: Actor) -> None:
        self._advance_status_turn(actor)
        if self._check_finished():
            return
        self._advance_turn(actor)

    def _advance_turn(self, actor: Actor) -> None:
        if not self.turn_order:
            return
        for index, candidate in enumerate(self.turn_order):
            if candidate is actor:
                self.turn_index = (index + 1) % len(self.turn_order)
                break
        else:
            self.turn_index = (self.turn_index + 1) % len(self.turn_order)
        self.turn_started = False
        self.enemy_action_delay = 0
        self._skip_dead_turns()

    def _set_guard(self, actor: Actor, enabled: bool) -> None:
        if enabled:
            self.guards[id(actor)] = 1
        else:
            self.guards.pop(id(actor), None)

    def _is_guarding(self, actor: Actor) -> bool:
        return self.guards.get(id(actor), 0) > 0

    def _basic_attack_damage(self, actor: Actor) -> int:
        raw = actor.basic_damage()
        if actor is self.player:
            if self.player.has_perk("quickdraw") or self.player.has_perk("riposte"):
                raw += 4
            if self.player.has_perk("soulflare"):
                raw += 3
            raw += self.player.skill_rank("blade_flurry") * 3
            if self.player.skill_rank("keen_edge") and random() < self.player.skill_rank("keen_edge") * 0.08:
                raw += 8
                self.log.insert(0, "Keen Edge finds an opening.")
        elif self.is_party_actor(actor):
            if self.player.alive and self.player.has_perk("pack_tactics"):
                raw += 2
            raw += self.player.skill_rank("shared_training")
            raw += self.player.skill_rank("pack_coordination")
        return self._modify_outgoing_damage(actor, raw)

    def _defend_actor(self, actor: Actor, label: str, mp_gain: int) -> None:
        self._set_guard(actor, True)
        actor.mp = min(actor.max_mp, actor.mp + mp_gain)
        self._apply_status(actor, "shield")
        self._apply_status(actor, "fortified")
        self.floaters.append(FloatingText("Guard", self._floater_x(actor), 355, 32, "#d6e8ff"))
        if label == "Defend":
            if actor is self.player:
                self.log.insert(0, f"You defend and steady your breath (+{mp_gain} MP).")
            else:
                self.log.insert(0, f"{actor.name} defends and regains {mp_gain} MP.")
        else:
            self.log.insert(0, f"{actor.name} uses {label}: guard stance up, +{mp_gain} MP.")
        self.effect_kind = "shield"
        self.effect_timer = 16
        self.effect_source = actor
        self.effect_target = actor

    def _enemy_take_turn(self, enemy: Actor) -> None:
        if self.finished or not enemy.alive:
            return
        targets = self.living_party()
        if not targets:
            self._lose()
            return
        target = targets[randint(0, len(targets) - 1)]
        self._tick_monster_cooldowns(enemy)
        ability = self._choose_monster_ability(enemy)
        if ability:
            self._use_monster_ability(enemy, target, ability)
            self._finish_actor_turn(enemy)
            return

        raw = self._guard_reduced_raw(target, self._modify_outgoing_damage(enemy, enemy.basic_damage()))
        damage = self._deal_damage(enemy, target, raw)
        damage = self._apply_guard_mastery(target, damage)
        self.floaters.append(FloatingText(f"-{damage}", self._floater_x(target), 385, 34, "#ffbbbb"))
        self.log.insert(0, f"{enemy.name} strikes {target.name} for {damage}.")
        self.shake = max(self.shake, 8)
        self.monster_lunge = 20
        self.enemy_lunges[self._enemy_index(enemy)] = 20
        self.effect_source = enemy
        self.effect_target = target
        if not target.alive:
            self.log.insert(0, f"{target.name} falls.")
            self._clear_statuses(target)
        self._finish_actor_turn(enemy)

    def _use_monster_ability(self, enemy: Actor, target: Actor, ability: MonsterAbility) -> None:
        enemy_index = self._enemy_index(enemy)
        self._monster_cooldowns.setdefault(id(enemy), {})[ability.name] = max(1, ability.cooldown)
        self.monster_effect = MonsterEffect(
            ability.effect,
            enemy_index,
            -1 if ability.kind == "buff" else self._party_index(target),
            20,
            ability.name,
        )
        self.effect_source = enemy
        self.effect_target = target
        self.enemy_lunges[enemy_index] = 16 if ability.kind == "damage" else 8

        if ability.kind == "buff":
            applied = self._apply_monster_ability_statuses(enemy, target, ability)
            self.floaters.append(FloatingText("Focus", self._floater_x(enemy), 315, 30, "#d7e6ff"))
            detail = "draws inward." if not applied else "power gathers."
            self.log.insert(0, f"{enemy.name} uses {ability.name}; {detail}")
            return

        raw = randint(max(1, ability.power - 3), ability.power + 5)
        raw = self._guard_reduced_raw(target, self._modify_outgoing_damage(enemy, raw))
        damage = self._deal_damage(enemy, target, raw)
        damage = self._apply_guard_mastery(target, damage)
        self.floaters.append(FloatingText(f"-{damage}", self._floater_x(target), 385, 34, "#ffbbbb"))
        self.log.insert(0, f"{enemy.name} uses {ability.name}; {target.name} takes {damage}.")
        self.shake = max(self.shake, 10)
        self.flash_timer = max(self.flash_timer, 2)
        self._apply_monster_ability_statuses(enemy, target, ability)
        if not target.alive:
            self.log.insert(0, f"{target.name} falls.")
            self._clear_statuses(target)

    def _choose_monster_ability(self, enemy: Actor) -> MonsterAbility | None:
        abilities = monster_abilities_for(self._monster_key(enemy))
        if not abilities:
            return None
        cooldowns = self._monster_cooldowns.setdefault(id(enemy), {})
        hp_ratio = enemy.hp / max(1, enemy.max_hp)
        bucket = self._status_bucket(enemy)
        for ability in abilities:
            if cooldowns.get(ability.name, 0) > 0:
                continue
            if ability.hp_below is not None and hp_ratio > ability.hp_below:
                continue
            if ability.kind == "buff":
                self_statuses = [status.key for status in ability.statuses if status.target == "self"]
                if self_statuses and all(key in bucket for key in self_statuses):
                    continue
            chance = ability.chance + (0.14 if ability.hp_below is not None and hp_ratio <= ability.hp_below else 0.0)
            if random() < min(0.92, chance):
                return ability
        return None

    def _apply_monster_ability_statuses(self, enemy: Actor, target: Actor, ability: MonsterAbility) -> bool:
        applied = False
        for status in ability.statuses:
            if status.chance < 1.0 and random() > status.chance:
                continue
            status_target = enemy if status.target == "self" else target
            if self._apply_status(status_target, status.key, status.power):
                applied = True
                self.log.insert(0, f"{status_target.name} is affected by {STATUS_EFFECTS[status.key].name}.")
        return applied

    def _guard_reduced_raw(self, target: Actor, raw: int) -> int:
        if self._is_guarding(target):
            self.log.insert(0, f"{target.name}'s guard absorbs part of the strike.")
            self._set_guard(target, False)
            return max(1, raw - 8)
        if target is not self.player and self.player.alive and self.player.has_perk("guardian"):
            return max(1, raw - 2)
        return raw

    def _apply_guard_mastery(self, target: Actor, damage: int) -> int:
        if target is not self.player or not self.player.skill_rank("guard_mastery"):
            return damage
        reduction = min(damage - 1, self.player.skill_rank("guard_mastery"))
        if reduction <= 0:
            return damage
        target.hp = min(target.max_hp, target.hp + reduction)
        return damage - reduction

    def _tick_monster_cooldowns(self, enemy: Actor) -> None:
        cooldowns = self._monster_cooldowns.setdefault(id(enemy), {})
        for name in list(cooldowns):
            cooldowns[name] -= 1
            if cooldowns[name] <= 0:
                del cooldowns[name]

    def _enemy_index(self, enemy: Actor) -> int:
        try:
            return self.enemies.index(enemy)
        except ValueError:
            return 0

    def _party_index(self, actor: Actor) -> int:
        try:
            return self.party_members().index(actor)
        except ValueError:
            return 0

    def _monster_key(self, enemy: Actor) -> str:
        return self._enemy_keys_by_id.get(id(enemy), enemy.sprite)

    def _attack_message(self, actor: Actor, target: Actor, damage: int) -> str:
        if actor is self.player:
            return f"You hit {target.name} for {damage}."
        return f"{actor.name} hits {target.name} for {damage}."

    def _cast_message(self, actor: Actor, ability_name: str, detail: str) -> str:
        if actor is self.player:
            return f"You cast {ability_name}; {detail}"
        return f"{actor.name} casts {ability_name}; {detail}"

    def _floater_x(self, actor: Actor) -> int:
        return 330 if self.is_party_actor(actor) else 760

    def _effect_for_ability(self, ability_name: str) -> str:
        lowered = ability_name.lower()
        if "fire" in lowered or "arc" in lowered:
            return "fire"
        if "frost" in lowered or "ice" in lowered:
            return "frost"
        if "poison" in lowered or "venom" in lowered:
            return "poison"
        if "volley" in lowered or "shot" in lowered or "arrow" in lowered:
            return "volley"
        if "slash" in lowered or "rush" in lowered:
            return "slash"
        return "strike"

    def _status_bucket(self, actor: Actor) -> dict[str, EffectStack]:
        key = id(actor)
        bucket = self._statuses.get(key)
        if bucket is None:
            bucket = {}
            self._statuses[key] = bucket
        return bucket

    def _clear_statuses(self, actor: Actor) -> None:
        self._statuses.pop(id(actor), None)

    def _apply_status(self, actor: Actor, key: str, power: int | None = None) -> bool:
        if key not in STATUS_EFFECTS:
            return False
        effect = STATUS_EFFECTS[key]
        bucket = self._status_bucket(actor)
        stack = bucket.get(key)
        if stack:
            stack.refresh(power)
            return True
        bucket[key] = EffectStack(effect=effect, turns_remaining=effect.duration, power=power or effect.power)
        return True

    def _apply_ability_statuses(self, ability_name: str, kind: str, caster: Actor, allied_target: Actor, enemy_target: Actor) -> None:
        for hint in status_hints_for_ability(ability_name, kind):
            if hint.chance < 1.0 and random() > hint.chance:
                continue
            target = enemy_target
            if hint.target == "self":
                target = caster
            elif hint.target == "target":
                target = allied_target if kind == "heal" else enemy_target
            elif hint.target == "ally":
                target = allied_target
            if self._apply_status(target, hint.key):
                self.log.insert(0, f"{target.name} is affected by {STATUS_EFFECTS[hint.key].name}.")

    def _trigger_turn_statuses(self, actor: Actor) -> None:
        bucket = self._status_bucket(actor)
        if not bucket:
            return
        if "poison" in bucket and actor.alive:
            dmg = max(1, bucket["poison"].power)
            dealt = self._apply_direct_damage(actor, dmg)
            self.log.insert(0, f"{actor.name} suffers {dealt} poison damage.")
            self.floaters.append(FloatingText(f"-{dealt}", self._floater_x(actor), 355, 30, "#9ce084"))
        if "burn" in bucket and actor.alive:
            dmg = max(1, bucket["burn"].power + 2)
            dealt = self._apply_direct_damage(actor, dmg)
            self.log.insert(0, f"{actor.name} burns for {dealt} damage.")
            self.floaters.append(FloatingText(f"-{dealt}", self._floater_x(actor), 335, 30, "#ffc494"))
        if "regeneration" in bucket and actor.alive:
            heal = max(1, bucket["regeneration"].power)
            before = actor.hp
            actor.hp = min(actor.max_hp, actor.hp + heal)
            gained = actor.hp - before
            if gained > 0:
                self.log.insert(0, f"{actor.name} regenerates {gained} HP.")
                self.floaters.append(FloatingText(f"+{gained}", self._floater_x(actor), 315, 30, "#b9f7c3"))

    def _advance_status_turn(self, actor: Actor) -> None:
        bucket = self._status_bucket(actor)
        for key in list(bucket):
            if not bucket[key].tick():
                self.log.insert(0, f"{actor.name} is no longer {bucket[key].effect.name}.")
                del bucket[key]
        if not bucket:
            self._clear_statuses(actor)

    def _modify_outgoing_damage(self, actor: Actor, raw: int) -> int:
        bucket = self._status_bucket(actor)
        modified = raw
        if "weak" in bucket:
            modified = int(modified * 0.70)
        if "haste" in bucket:
            modified = int(modified * 1.55)
        return max(1, modified)

    def _deal_damage(self, attacker: Actor, target: Actor, raw: int) -> int:
        bucket = self._status_bucket(target)
        modified = raw
        if "vulnerable" in bucket:
            modified = int(modified * 1.25)
        if "fortified" in bucket:
            modified = int(modified * 0.75)
        if "shield" in bucket:
            modified = max(1, modified - bucket["shield"].power)
        return target.take_damage(modified)

    def _apply_direct_damage(self, actor: Actor, amount: int) -> int:
        before = actor.hp
        actor.hp = max(0, actor.hp - max(0, amount))
        return max(0, before - actor.hp)
