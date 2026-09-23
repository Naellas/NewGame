param(
    [string]$SourceDirectory = "assets/source/dungeon-floor-replacements",
    [string]$DestinationDirectory = "assets/terrain"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$pairs = [ordered]@{
    "dungeon_crypt_floor_source.png"      = "dungeon_crypt_floor.png"
    "dungeon_moss_floor_source.png"       = "dungeon_moss_floor.png"
    "dungeon_rubble_floor_source.png"     = "dungeon_rubble_floor.png"
    "dungeon_torch_floor_source.png"      = "dungeon_torch_floor.png"
    "dungeon_boss_sigil_floor_source.png" = "dungeon_boss_sigil_floor.png"
}

function Average-Color([System.Drawing.Color]$a, [System.Drawing.Color]$b) {
    return [System.Drawing.Color]::FromArgb(
        255,
        [int](($a.R + $b.R) / 2),
        [int](($a.G + $b.G) / 2),
        [int](($a.B + $b.B) / 2)
    )
}

function Make-Seamless([System.Drawing.Bitmap]$image) {
    $last = $image.Width - 1
    for ($offset = 0; $offset -lt 4; $offset++) {
        for ($y = 0; $y -lt $image.Height; $y++) {
            $color = Average-Color $image.GetPixel($offset, $y) $image.GetPixel($last - $offset, $y)
            $image.SetPixel($offset, $y, $color)
            $image.SetPixel($last - $offset, $y, $color)
        }
        for ($x = 0; $x -lt $image.Width; $x++) {
            $color = Average-Color $image.GetPixel($x, $offset) $image.GetPixel($x, $last - $offset)
            $image.SetPixel($x, $offset, $color)
            $image.SetPixel($x, $last - $offset, $color)
        }
    }
}

$sourceRoot = (Resolve-Path -LiteralPath $SourceDirectory).Path
$destinationRoot = (Resolve-Path -LiteralPath $DestinationDirectory).Path

foreach ($entry in $pairs.GetEnumerator()) {
    $sourcePath = Join-Path $sourceRoot $entry.Key
    $destinationPath = Join-Path $destinationRoot $entry.Value
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "Missing generated floor source: $sourcePath"
    }
    $source = [System.Drawing.Bitmap]::FromFile($sourcePath)
    try {
        $side = [Math]::Min($source.Width, $source.Height)
        $cropX = [int](($source.Width - $side) / 2)
        $cropY = [int](($source.Height - $side) / 2)
        $target = New-Object System.Drawing.Bitmap 48, 48, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($target)
            try {
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                $graphics.DrawImage($source,
                    (New-Object System.Drawing.Rectangle 0, 0, 48, 48),
                    (New-Object System.Drawing.Rectangle $cropX, $cropY, $side, $side),
                    [System.Drawing.GraphicsUnit]::Pixel)
            } finally {
                $graphics.Dispose()
            }
            Make-Seamless $target
            for ($y = 0; $y -lt 48; $y++) {
                for ($x = 0; $x -lt 48; $x++) {
                    if ($target.GetPixel($x, $y).A -ne 255) {
                        throw "Non-opaque result in $($entry.Value) at $x,$y"
                    }
                }
            }
            $target.Save($destinationPath, [System.Drawing.Imaging.ImageFormat]::Png)
            Write-Output "Imported $destinationPath"
        } finally {
            $target.Dispose()
        }
    } finally {
        $source.Dispose()
    }
}
