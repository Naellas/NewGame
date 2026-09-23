$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
$manifest = Get-Content (Join-Path $projectRoot 'assets/source/modular-dungeons/prompts.json') -Raw | ConvertFrom-Json
$destination = Join-Path $projectRoot 'assets/deco/dungeon_details'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
foreach ($sheet in $manifest) {
    $source = [System.Drawing.Bitmap]::new((Join-Path $projectRoot $sheet.source))
    try {
        for ($index = 0; $index -lt 4; $index++) {
            $col = $index % 2
            $row = [int][Math]::Floor($index / 2)
            $x0 = [int][Math]::Floor($source.Width * $col / 2)
            $y0 = [int][Math]::Floor($source.Height * $row / 2)
            $x1 = [int][Math]::Floor($source.Width * ($col + 1) / 2)
            $y1 = [int][Math]::Floor($source.Height * ($row + 1) / 2)
            $left = $x1; $top = $y1; $right = -1; $bottom = -1
            for ($y = $y0; $y -lt $y1; $y++) {
                for ($x = $x0; $x -lt $x1; $x++) {
                    # Ignore nearly invisible alpha noise when finding the sprite bounds.
                    if ($source.GetPixel($x, $y).A -gt 16) {
                        $left = [Math]::Min($left, $x); $right = [Math]::Max($right, $x)
                        $top = [Math]::Min($top, $y); $bottom = [Math]::Max($bottom, $y)
                    }
                }
            }
            if ($right -lt 0) { throw "Empty sprite: $($sheet.names[$index])" }
            if ($left -eq $x0 -or $top -eq $y0 -or $right -eq ($x1 - 1) -or $bottom -eq ($y1 - 1)) {
                throw "Sprite touches sheet cell boundary: $($sheet.names[$index])"
            }
            $width = $right - $left + 1; $height = $bottom - $top + 1
            $scale = 112.0 / [Math]::Max($width, $height)
            $drawWidth = [int][Math]::Round($width * $scale)
            $drawHeight = [int][Math]::Round($height * $scale)
            $canvas = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            $graphics = [System.Drawing.Graphics]::FromImage($canvas)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $targetRect = [System.Drawing.Rectangle]::new([int][Math]::Floor((128 - $drawWidth) / 2), 120 - $drawHeight, $drawWidth, $drawHeight)
                $graphics.DrawImage($source, $targetRect, $left, $top, $width, $height, [System.Drawing.GraphicsUnit]::Pixel)
                $filename = 'dungeon_detail_' + $sheet.names[$index] + '.png'
                $canvas.Save((Join-Path $destination $filename), [System.Drawing.Imaging.ImageFormat]::Png)
                Write-Output "$filename 128x128 RGBA"
            } finally { $graphics.Dispose(); $canvas.Dispose() }
        }
    } finally { $source.Dispose() }
}
