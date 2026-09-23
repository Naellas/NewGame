param([switch]$Import)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$names = @('oak', 'round', 'pine', 'blue_pine', 'young')
$preview = New-Object System.Drawing.Bitmap 1000, 540
$g = [System.Drawing.Graphics]::FromImage($preview)
$g.Clear([System.Drawing.Color]::FromArgb(53, 62, 45))
$font = New-Object System.Drawing.Font 'Arial', 12
foreach ($name in $names) {
    $column = [array]::IndexOf($names, $name)
    $asset = Join-Path $root "Java/assets/environments/terrain/biomes/forest/deco_tree_$name.png"
    if ($Import) {
        $source = [System.Drawing.Bitmap]::FromFile((Join-Path $PSScriptRoot "generated/$name.png"))
        $left = $source.Width; $top = $source.Height; $right = -1; $bottom = -1
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                if ($source.GetPixel($x, $y).A -gt 8) {
                    $left = [Math]::Min($left, $x); $right = [Math]::Max($right, $x)
                    $top = [Math]::Min($top, $y); $bottom = [Math]::Max($bottom, $y)
                }
            }
        }
        if ($right -lt $left -or $source.GetPixel(0, 0).A -ne 0) { throw "Missing transparent background: $name" }
        $h = 248; $w = [int][Math]::Round(($right - $left + 1) * $h / ($bottom - $top + 1))
        $output = New-Object System.Drawing.Bitmap ($w + 8), 256
        $og = [System.Drawing.Graphics]::FromImage($output)
        $og.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $og.DrawImage($source, [System.Drawing.Rectangle]::new(4, 4, $w, $h), $left, $top, ($right - $left + 1), ($bottom - $top + 1), [System.Drawing.GraphicsUnit]::Pixel)
        $og.Dispose(); $source.Dispose()
        $output.Save($asset, [System.Drawing.Imaging.ImageFormat]::Png); $output.Dispose()
    }
    $g.DrawString($name, $font, [System.Drawing.Brushes]::White, ($column * 200 + 12), 6)
    foreach ($row in @(0, 1)) {
        $path = if ($row -eq 0) { Join-Path $PSScriptRoot "originals/deco_tree_$name.png" } else { $asset }
        $im = [System.Drawing.Bitmap]::FromFile($path)
        $scale = [Math]::Min(184.0 / $im.Width, 224.0 / $im.Height)
        $dw = [int]($im.Width * $scale); $dh = [int]($im.Height * $scale)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.DrawImage($im, ($column * 200 + (200 - $dw) / 2), (38 + $row * 250 + 224 - $dh), $dw, $dh)
        Write-Output "$name row=$row $($im.Width)x$($im.Height) alpha=$($im.GetPixel(0,0).A)"
        $im.Dispose()
    }
}
$g.Dispose(); $font.Dispose()
$preview.Save((Join-Path $PSScriptRoot 'comparison.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$preview.Dispose()
