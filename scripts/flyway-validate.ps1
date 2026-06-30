param(
    [string] $Url = $env:DB_URL,
    [string] $User = $env:DB_USERNAME,
    [string] $Password = $env:DB_PASSWORD
)

if (-not $Url) {
    $Url = 'jdbc:postgresql://localhost:5432/caisora'
}

if (-not $User) {
    $User = 'postgres'
}

if (-not $Password) {
    throw 'Defina DB_PASSWORD ou informe -Password para executar o Flyway.'
}

& .\mvnw.cmd `
    "-Dflyway.url=$Url" `
    "-Dflyway.user=$User" `
    "-Dflyway.password=$Password" `
    '-Dflyway.locations=filesystem:src/main/resources/db/migration' `
    '-Dflyway.ignoreMigrationPatterns=*:pending' `
    'org.flywaydb:flyway-maven-plugin:12.4.0:validate'

exit $LASTEXITCODE
