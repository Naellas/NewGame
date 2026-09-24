# Roaming event world objects

Status: visually reviewed in the actual game renderer.

- [Road objects](road-objects.png): stationary bush on the road and a leather purse on nearby ground; no discovery popup.
- Shrine palettes: [blue](shrine-0.png), [amber](shrine-1.png), [violet](shrine-2.png). One existing shrine is activated; neighboring world art is retained.
- [Deliberate interaction](deliberate-interaction.png): the compact choices card used only after interacting, without an NPC portrait.

Reproduce with WorldGenerationPreview's `roaming` mode and the commands in
[roaming-events.md](../../../../Java/docs/roaming-events.md). The preview explicitly
places events for reproducible captures; the behavioral test exercises movement
and interaction. Preview placement is not evidence of random spawn frequency.
World seed 0, high render quality, 100% zoom; shrine animation frame 75.
Runtime tinting and a code-drawn purse reuse the game pipeline; no new external
art sources or runtime PNG imports were required. Bulk captures remain in ignored
Java/temp/roaming-events/captures.
