param([string]$Sheet = (Join-Path $PSScriptRoot 'boundaries-clean.png'), [string]$Recipe = '')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$output = Join-Path $repo 'Java/assets/environments/settlements/city/props/garden'
New-Item -ItemType Directory -Force -Path $output | Out-Null
# The generated sheet uses unequal object widths. Explicit empty-gutter cuts preserve every silhouette.
$cuts = @(0, 470, 720, 1140, 1536)
$names = @('town_hedge_h', 'town_hedge_v', 'town_hedge_corner', 'town_hedge_end',
           'town_fence_h', 'town_fence_v', 'town_fence_corner', 'town_fence_gate')
$entries = if ($Recipe) { Get-Content -LiteralPath $Recipe -Raw | ConvertFrom-Json } else {
    for ($i = 0; $i -lt $names.Count; $i++) {
        $column = $i % 4
        [PSCustomObject]@{name=$names[$i]; x=$cuts[$column]; y=[int][Math]::Floor($i / 4) * 512;
            width=$cuts[$column + 1] - $cuts[$column]; height=512}
    }
}
$source = [Drawing.Bitmap]::FromFile((Resolve-Path -LiteralPath $Sheet))
try {
    foreach ($entry in $entries) {
        if ($entry.name -notmatch '^town_[a-z0-9_]+$') { throw 'Invalid asset ID' }
        $rect = [Drawing.Rectangle]::new($entry.x, $entry.y, $entry.width, $entry.height)
        if ($rect.X -lt 0 -or $rect.Y -lt 0 -or $rect.Right -gt $source.Width -or $rect.Bottom -gt $source.Height) { throw 'Crop outside source' }
        $tile = $source.Clone($rect, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try { $tile.Save((Join-Path $output ($entry.name + '.png')), [Drawing.Imaging.ImageFormat]::Png) }
        finally { $tile.Dispose() }
    }
} finally { $source.Dispose() }
