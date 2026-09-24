# Spell and movement audio

Settings > Audio has Master, Music, SFX and Footsteps sliders. Footsteps defaults
to 55%, persists in private settings, and has its own mix under Master. The v2 mix
softens the samples themselves, so existing saved preferences benefit too.

Footsteps use eight variations per surface, randomized without immediate repeats.
Heel pitch, scuff timing, decay and weight differ. Transients are rounded and the
first-variant RMS is 10-15 dB below the initial batch. Cadence still follows world
travel but contacts are capped at one every 150 ms per walker. Ambient contacts
are spaced at least 100 ms apart, fade over five tiles, and have lower gain than
the player. At most four footstep clips overlap. Idle actors, blocked movement,
map changes and large teleports stay silent. Timber interiors/bridges sound wooden;
rugs follow the world's rug coverage. Ground, grass, stone, sand, snow and water
retain their own textures. The player, companions and nearby humanoid NPCs use it.

Combat audio uses actual class/monster visual profiles before falling back to
legacy gameplay keys. Named healing, wards, holy magic, poison, roots, shadow,
water and wind now have distinct samples. Weapons use blade/blunt/arrow releases
and impacts. Creatures gain bite/claw/heavy/bone impacts plus synthesized growls,
roars, hisses or rattles for matching monster types. Impact and release families
have three variants with no immediate repeats; elemental legacy samples remain.
These are synthesized effects, not recorded voices. There are 144 generated WAVs.

Release cues fire once when the cast ends. Supported projectile flights loop only
during travel; melee and self effects do not loop. Impacts follow each target's
collision frame, including chains and staggered volleys. Simultaneous area hits
share one cue. Pausing preserves the event cursor and stops flight loops. Combat
speed changes event timing without changing sample pitch or gameplay randomness.
Unknown/custom effects retain the old fallback sound mapping.

SoundManager caches PCM, bounds voices, supports live category volume and exact
mute, closes finished clips, and retries unavailable devices after a cooldown.
There are no new runtime dependencies.

Listen: [sample review](../tools/reviews/audio/index.html).
Recipe/provenance: [v2 batch](../../art-source/audio/2026-09-24-expanded-v2/README.md).

Validation: EffectAudioTest covers collision/release timing, all class profiles,
regular/elite/boss monster profiles, asset decoding, variation, contact rate,
surfaces, settings and mute. Python test_sfxgen checks all 144 signals, quiet
footstep bounds, distinct variants and reproducibility. Listening acceptance is
pending. Saved volume values are preserved.
