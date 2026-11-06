# Starts a local LiveKit server in dev mode (API key "devkey", secret "secret") on ws://localhost:7880.
# Downloads the Windows build on first run.
# It announces 127.0.0.1 for media, so a headset reaches it through adb reverse (see app/README.md), no firewall rule needed.
$version = "1.13.11"
$exe = Join-Path $PSScriptRoot "bin\livekit-server.exe"
if (-not (Test-Path $exe)) {
    $zip = Join-Path $env:TEMP "livekit_$version.zip"
    Invoke-WebRequest "https://github.com/livekit/livekit/releases/download/v$version/livekit_${version}_windows_amd64.zip" -OutFile $zip
    Expand-Archive $zip (Join-Path $PSScriptRoot "bin") -Force
    Remove-Item $zip
}
& $exe --dev --bind 0.0.0.0 --node-ip 127.0.0.1
