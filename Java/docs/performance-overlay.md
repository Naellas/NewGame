# Live performance overlay

Press **F4** anywhere, or open **Settings > Debug > Performance (F4)**.
Cycle through Off, Stats, and Heatmap. This is a session-only debug option and
starts off; F3 retains the existing battle VFX diagnostic.

Stats show paint cadence (FPS, mean and worst interval over half-second windows),
the latest sampled paint duration, simulation update and selected renderer stage
times, and JVM heap used/committed/maximum. A slow paint or EDT stall affects FPS.
This is Swing paint cadence, not a monitor presentation or GPU FPS counter.
Paint timing includes the back-buffer copy but excludes drawing the diagnostics;
the next paint interval includes their overhead. Update time sums updates since
the previous paint; other stage timings describe the sampled paint, not averages.
Heap is process-wide Java heap, not native image memory or per-region allocation.

Heatmap cells estimate elapsed rendering cost. Depth-sorted scenery and characters
are individually timed, including foreground cutaways, and their time is spread
over their clipped visible drawing bounds. Sidebar, battle, dialogue, inventory,
chest, shop, party and world-map passes use coarser panel bounds. Shared world work
(terrain caching, lighting, atmosphere, sorting, ground overlays) is not spatially
attributed. World stages remain available in the statistics / console profiler.
Overlapping sampled passes accumulate; the world beneath a UI panel still costs
time to draw. Blank cells mean unmeasured, not zero resource usage.

Green means lower cost and red means the highest cell cost in the current paint;
labels give estimated milliseconds per cell. Colors rescale each paint, so compare
the numbers across scenes. Samples reset each paint to avoid stale heat when moving,
changing maps, resizing, or opening menus. Measurements use Java2D wall-clock elapsed
time, including cache misses or pauses; they are not per-pixel CPU/GPU utilization.
Heatmap instrumentation itself adds overhead. Stats mode skips regional timing.

`RenderMetrics` supplies timings to the overlay without enabling console output.
The existing `scripts/run-profile.ps1` console profiler remains independent.
Validate with `scripts/test.ps1 -Test com.alderfall.game.PerformanceOverlayTest`.
