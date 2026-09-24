param(
    [Parameter(Mandatory = $true)][string]$Source,
    [Parameter(Mandatory = $true)][string]$Asset
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetsRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../assets'))
$placements = Get-Content -LiteralPath (Join-Path $PSScriptRoot '../../../config/asset-placements.json') -Raw | ConvertFrom-Json
$route = $placements.paths.PSObject.Properties[$Asset.Replace('\', '/')]
if ($null -ne $route) { $Asset = $route.Value }
$destination = [IO.Path]::GetFullPath((Join-Path $assetsRoot $Asset))
if (-not $destination.StartsWith($assetsRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Asset path must stay inside Java/assets.'
}
if ([IO.Path]::GetExtension($destination) -ne '.png') { throw 'Expected a PNG destination.' }

# Export the approved painted sprites at game dimensions while preserving alpha.
$size = if ($Asset.StartsWith('items/')) { 96 } elseif ($Asset.Contains('tree_')) { 256 } else { 192 }
$sourceImage = [Drawing.Bitmap]::new($Source)
$canvas = [Drawing.Bitmap]::new($size, $size, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [Drawing.Graphics]::FromImage($canvas)
try {
    if (-not [Drawing.Image]::IsAlphaPixelFormat($sourceImage.PixelFormat)) {
        throw "Generated sprite has no alpha channel: $Source"
    }
    $graphics.Clear([Drawing.Color]::Transparent)
    $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
    $scale = [Math]::Min($size / $sourceImage.Width, $size / $sourceImage.Height)
    $width = [Math]::Max(1, [int][Math]::Round($sourceImage.Width * $scale))
    $height = [Math]::Max(1, [int][Math]::Round($sourceImage.Height * $scale))
    $rectangle = [Drawing.Rectangle]::new([int](($size - $width) / 2), [int](($size - $height) / 2), $width, $height)
    $graphics.DrawImage($sourceImage, $rectangle, 0, 0, $sourceImage.Width, $sourceImage.Height, [Drawing.GraphicsUnit]::Pixel)
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
    $canvas.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $canvas.Dispose()
    $sourceImage.Dispose()
}
Write-Output "Imported $Asset ($size x $size)"
