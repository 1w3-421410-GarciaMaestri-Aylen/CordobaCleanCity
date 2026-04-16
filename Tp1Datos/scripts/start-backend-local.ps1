param(
    [string]$MongoUri = "mongodb://localhost:27017/test",
    [string]$RedisHost = "localhost",
    [string]$RabbitHost = "localhost",
    [string]$ServerPort = "8080",
    [string]$TrashCatalogDir = "storage/catalog/basura",
    [string]$NonTrashCatalogDir = "storage/catalog/no-basura"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot

$env:SERVER_PORT = $ServerPort
$env:MONGODB_URI = $MongoUri
$env:REDIS_HOST = $RedisHost
$env:RABBITMQ_HOST = $RabbitHost
$env:TRASH_CATALOG_DIR = $TrashCatalogDir
$env:NON_TRASH_CATALOG_DIR = $NonTrashCatalogDir

Write-Host "Starting backend on http://localhost:$ServerPort"
Write-Host "MongoDB URI: $MongoUri"
Write-Host "Redis host: $RedisHost"
Write-Host "RabbitMQ host: $RabbitHost"
Write-Host "Trash catalog dir: $TrashCatalogDir"
Write-Host "Non-trash catalog dir: $NonTrashCatalogDir"

.\mvnw.cmd spring-boot:run
