# Download ProjectRed 4.23.0 for 1.21.1 NeoForge and the library versions it declares, into libs\projectred\
# Dev and GameTest runtime only. The values are Modrinth version ids.
$versions = @(
    'RueNBgM9', # ProjectRed core 4.23.0
    'iM7enYpN', # ProjectRed integration 4.23.0
    'YCCf1wi9', # ProjectRed transmission 4.23.0
    'mhteXGK9', # CB Multipart 3.5.0.155
    'vLDGhjNJ'  # CodeChicken Lib 4.6.1.529
)
$dir = 'libs\projectred'
New-Item -ItemType Directory -Force $dir | Out-Null
foreach ($id in $versions) {
    $version = Invoke-RestMethod "https://api.modrinth.com/v2/version/$id"
    $file = $version.files | Where-Object primary | Select-Object -First 1
    if (-not $file) { $file = $version.files[0] }
    $path = Join-Path $dir $file.filename
    Invoke-WebRequest -UseBasicParsing $file.url -OutFile $path
    $actual = (Get-FileHash $path -Algorithm SHA1).Hash.ToLower()
    if ($actual -ne $file.hashes.sha1) { throw "Checksum mismatch for $($file.filename)" }
    Write-Host $file.filename
}
