$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$project = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
$source = [System.Drawing.Bitmap]::new((Join-Path $project 'assets/source/built-dungeons/terrain-atlas.png'))
try {
    $themes = @('prison', 'crypt', 'gothic', 'arcane')
    $roles = @('floor', 'gravel', 'face', 'roof')
    for ($row = 0; $row -lt 4; $row++) {
        for ($col = 0; $col -lt 4; $col++) {
            # Fractional boundaries support the actual generated sheet dimensions.
            # Remove the generated atlas gutters before repeating the textures in world space.
            $left = [int][Math]::Round($col * $source.Width / 4.0) + 8
            $top = [int][Math]::Round($row * $source.Height / 4.0) + 8
            $right = [int][Math]::Round(($col + 1) * $source.Width / 4.0) - 8
            $bottom = [int][Math]::Round(($row + 1) * $source.Height / 4.0) - 8
            # Wall-top mass excludes the decorative cap at the top of the generated roof cell.
            if ($col -eq 3) { $top += [int][Math]::Round(($bottom - $top) * 0.25) }
            $cell = $source.Clone([System.Drawing.Rectangle]::new($left, $top, $right-$left, $bottom-$top), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                $cell.Save((Join-Path $project ('assets/environments/terrain/common/masonry_' + $themes[$row] + '_' + $roles[$col] + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
            } finally { $cell.Dispose() }
        }
    }
} finally { $source.Dispose() }

