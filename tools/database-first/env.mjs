import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
// Carrega somente o .env da demo, independentemente do diretorio do terminal.
const file = fileURLToPath(new URL('./.env', import.meta.url));
if (existsSync(file)) process.loadEnvFile(file);
