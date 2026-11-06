# Starts a local LiveKit server in dev mode (API key "devkey", secret "secret") on ws://localhost:7880.
# Downloads the Windows build on first run.
# Media goes to the PC's LAN IP, so allow it through the Windows firewall; a headset in the same network reaches it directly.
$version = "1.13.11"
$exe = Join-Path $PSScriptRoot "bin\livekit-server.exe"
if (-not (Test-Path $exe)) {
    $zip = Join-Path $env:TEMP "livekit_$version.zip"
    Invoke-WebRequest "https://github.com/livekit/livekit/releases/download/v$version/livekit_${version}_windows_amd64.zip" -OutFile $zip
    Expand-Archive $zip (Join-Path $PSScriptRoot "bin") -Force
    Remove-Item $zip
}
& $exe --dev --bind 0.0.0.0
