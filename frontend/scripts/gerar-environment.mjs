import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const nomeVariavel = 'API_BASE_URL';
const valorInformado = process.env[nomeVariavel]?.trim();

if (!valorInformado) {
  throw new Error(
    `Defina ${nomeVariavel} na Vercel com a URL pública do backend.`
  );
}

const semBarraFinal = valorInformado.replace(/\/+$/, '');
const ehLocal = /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i
  .test(semBarraFinal);

if (!ehLocal && !semBarraFinal.startsWith('https://')) {
  throw new Error(
    `${nomeVariavel} deve usar HTTPS fora do ambiente local.`
  );
}

const apiUrl = semBarraFinal.endsWith('/api/v1')
  ? semBarraFinal
  : `${semBarraFinal}/api/v1`;

const conteudo = `export const environment = {
  production: true,
  apiUrl: '${apiUrl}'
};
`;

const diretorio = path.resolve(
  process.cwd(),
  'src/environments'
);
const arquivo = path.join(
  diretorio,
  'environment.ts'
);

await mkdir(diretorio, { recursive: true });
await writeFile(arquivo, conteudo, 'utf8');

console.log(`Environment de produção gerado com API em ${apiUrl}`);
