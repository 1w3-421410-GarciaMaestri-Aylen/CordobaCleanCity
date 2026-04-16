param(
    [string]$MongoContainer = "ecoruta-mongodb",
    [string]$MongoDatabase = "test",
    [string]$RedisContainer = "ecoruta-redis",
    [string]$RabbitContainer = "ecoruta-rabbitmq",
    [string]$UploadsDir = "C:\Proyectos\Tp1Datos\Tp1Datos\uploads"
)

$ErrorActionPreference = "Stop"

Write-Host "Resetting development report state..."

if (-not (Test-Path $UploadsDir)) {
    throw "Uploads directory not found: $UploadsDir"
}

$reportImages = docker exec $MongoContainer mongosh $MongoDatabase --quiet --eval "db.reports.distinct('imageUrl')"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read reports from MongoDB container $MongoContainer"
}

$imagePaths = @()
if ($reportImages -and $reportImages.Trim() -ne "") {
    $parsed = $reportImages | ConvertFrom-Json
    foreach ($item in $parsed) {
        if ([string]::IsNullOrWhiteSpace($item)) {
            continue
        }

        $normalized = $item.ToString().Replace("/", "\")
        if ($normalized.StartsWith("\uploads\")) {
            $normalized = Join-Path (Split-Path $UploadsDir -Parent) $normalized.TrimStart("\")
        }
        $imagePaths += $normalized
    }
}

docker exec $MongoContainer mongosh $MongoDatabase --quiet --eval "db.reports.deleteMany({})"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to delete reports from MongoDB database $MongoDatabase"
}

docker exec $RedisContainer redis-cli DEL "reports:today::SimpleKey []" "routes:optimization::v2:TODAY" "routes:optimization::v2:ACTIVE" | Out-Null
docker exec $RedisContainer redis-cli --scan --pattern "reports:*" | ForEach-Object {
    docker exec $RedisContainer redis-cli DEL $_ | Out-Null
}
docker exec $RedisContainer redis-cli --scan --pattern "routes:*" | ForEach-Object {
    docker exec $RedisContainer redis-cli DEL $_ | Out-Null
}

docker exec $RabbitContainer rabbitmqctl purge_queue report.processing.queue | Out-Null
docker exec $RabbitContainer rabbitmqctl purge_queue report.processing.dlq | Out-Null

Get-ChildItem -Path $UploadsDir -File | Remove-Item -Force
foreach ($path in $imagePaths) {
    if (Test-Path $path) {
        Remove-Item -Path $path -Force
    }
}

Write-Host "Development report state reset complete."
