$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$project = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
$source = [System.Drawing.Bitmap]::new((Join-Path $project 'assets/source/regional-caverns/terrain-atlas.png'))
try {
    $themes = @('stone', 'ice', 'moss', 'sand')
    $roles = @('floor', 'gravel', 'face', 'roof')
    for ($row = 0; $row -lt 4; $row++) {
        for ($col = 0; $col -lt 4; $col++) {
            # Fractional boundaries support the actual generated sheet dimensions.
            # Trim two pixels inside each cell to exclude adjacent biome colors.
            $left = [int][Math]::Round($col * $source.Width / 4.0) + 2
            $top = [int][Math]::Round($row * $source.Height / 4.0) + 2
            $right = [int][Math]::Round(($col + 1) * $source.Width / 4.0) - 2
            $bottom = [int][Math]::Round(($row + 1) * $source.Height / 4.0) - 2
            $cell = $source.Clone([System.Drawing.Rectangle]::new($left, $top, $right-$left, $bottom-$top), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                $cell.Save((Join-Path $project ('assets/environments/terrain/common/cavern_' + $themes[$row] + '_' + $roles[$col] + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
            } finally { $cell.Dispose() }
        }
    }
} finally { $source.Dispose() }
