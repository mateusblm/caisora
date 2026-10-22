// Executado apenas pelos testes opt-in AIE; proxy local da build de produção.
import assert from "node:assert/strict";
import http from "node:http";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { resolve, extname } from "node:path";
import { createRequire } from "node:module";
const require = createRequire(
  resolve(
    process.env.CAISORA_BROWSER_TOOLS || "/tmp/caisora-browser-tools",
    "package.json",
  ),
);
const { chromium } = require("playwright");
const backend = new URL(process.argv[2]),
  mode = process.argv[3];
assert.equal(backend.hostname, "127.0.0.1");

const dist = resolve("frontend/dist/caisora-web/browser");
const out = resolve(
  process.env.CAISORA_APF_OUTPUT || "docs/evidencias/apf",
);
await mkdir(out, { recursive: true });
const mime = {
  ".js": "application/javascript",
  ".css": "text/css",
  ".html": "text/html",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".woff2": "font/woff2",
  ".ico": "image/x-icon",
};
const respostas = new Map();
const server = http.createServer(async (req, res) => {
  if (req.url.startsWith("/api/")) {
    const headers = { ...req.headers, host: backend.host };
    delete headers.origin;
    const upstream = http.request(
      new URL(req.url, backend),
      { method: req.method, headers },
      (incoming) => {
        const partes = [];
        incoming.on("data", (parte) => partes.push(parte));
        incoming.on("end", () => {
          const grupo = new URL(req.url, backend).pathname.split("/").at(-1);
          try {
            respostas.set(grupo, {
              status: incoming.statusCode,
              data: JSON.parse(Buffer.concat(partes).toString("utf8")),
            });
          } catch {
            respostas.set(grupo, { status: incoming.statusCode, data: null });
          }
        });
        res.writeHead(incoming.statusCode, incoming.headers);
        incoming.pipe(res);
      },
    );
    upstream.on("error", () => {
      res.writeHead(502);
      res.end();
    });
    req.pipe(upstream);
    return;
  }
  try {
    let file = resolve(
      dist,
      "." + new URL(req.url, "http://localhost").pathname,
    );
    if (!file.startsWith(dist + "/")) file = resolve(dist, "index.html");
    let body;
    try {
      body = await readFile(file);
    } catch {
      file = resolve(dist, "index.html");
      body = await readFile(file);
    }
    res.writeHead(200, {
      "content-type": mime[extname(file)] || "application/octet-stream",
    });
    res.end(body);
  } catch {
    res.writeHead(500);
    res.end();
  }
});
await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
const url = `http://127.0.0.1:${server.address().port}`;
const browser = await chromium.launch({
  headless: true,
  args: ["--no-sandbox"],
});

const snapshotUrl = new URL(process.argv[3]);
assert.equal(snapshotUrl.hostname, '127.0.0.1');
const { execFileSync } = await import('node:child_process');
const sha = execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim();
const complementar=process.env.CAISORA_APF_COMPLEMENTAR==='true';
const evidence=complementar?JSON.parse(await readFile(resolve(out,'execucao-browser.json'),'utf8')):{sha, data:new Date().toISOString(), navegador:browser.version(), cenarios:[], efeitos:[], falhas:[], perfis:[], rede:[]};
evidence.execucoes??=[];evidence.execucoes.push({data:new Date().toISOString(),complementar,sha});
let page, profile='ADMINISTRADOR_MARINA', viewportName='desktop';
const snap=async()=>await (await fetch(snapshotUrl)).json();
const flush=async()=>writeFile(resolve(out,'execucao-browser.json'),JSON.stringify(evidence,null,2)+'\n');
const settle=async()=>{await page.waitForTimeout(250);await page.waitForLoadState('networkidle');await page.evaluate(()=>document.fonts.ready);};
async function capture(id,state,mirror=true){
 await settle();
 const root=page.locator('mat-sidenav-content');
 const old=page.viewportSize();
 // O layout possui scroll próprio; altura ampliada preserva a largura e torna o formulário legível.
 let height=old.height;
 if(await root.count()) {height=Math.min(6000,Math.max(height,await root.evaluate(e=>e.scrollHeight+100)));await page.setViewportSize({width:old.width,height});await root.evaluate(e=>e.scrollTop=0);}
 const file=`capturas/${id}/${state}-${viewportName}.png`;
 await mkdir(resolve(out,'capturas',id),{recursive:true});
 const dom=await page.evaluate(()=>{
  const clean=s=>(s||'').replace(/\s+/g,' ').trim();
  const scope=document.querySelector('mat-sidenav-content')||document.body;
  const visible=e=>!!e.getClientRects().length;
  return {titulo:clean(scope.querySelector('h1')?.textContent),
   entradas:[...scope.querySelectorAll('mat-form-field')].filter(visible).map(e=>({rotulo:clean(e.querySelector('mat-label')?.textContent),controle:e.querySelector('[formcontrolname]')?.getAttribute('formcontrolname')||e.querySelector('[name]')?.getAttribute('name'),valor:e.querySelector('input:not([type=password]),textarea')?.value||clean(e.querySelector('mat-select')?.textContent)})),
   botoes:[...scope.querySelectorAll('button,a[mat-button],a[mat-flat-button],a[mat-stroked-button]')].filter(visible).map(e=>({texto:clean(e.textContent),aria:e.getAttribute('aria-label'),desabilitado:!!e.disabled})),
   erros:[...document.querySelectorAll('mat-error,[role=alert],.alerta-erro,.mensagem--erro')].filter(visible).map(e=>clean(e.textContent)).filter(Boolean),
   dialogos:[...document.querySelectorAll('mat-dialog-container')].map(e=>clean(e.textContent)),
   colunas:[...scope.querySelectorAll('th')].filter(visible).map(e=>clean(e.textContent)),
   texto:clean(scope.textContent),overflowPagina:document.documentElement.scrollWidth>innerWidth,
   overflowConteudo:scope.scrollWidth>scope.clientWidth};
 });
 await page.screenshot({path:resolve(out,file),fullPage:true});
 evidence.cenarios.push({capturadoEm:new Date().toISOString(),id,estado:state,perfil:profile,viewport:viewportName,largura:old.width,alturaBase:old.height,alturaCaptura:height,captura:file,rota:new URL(page.url()).pathname,origem:'Dados sintéticos exclusivamente no PostgreSQL descartável; Open-Meteo controlado local; WorldTides desabilitada',...dom});
 await page.setViewportSize(old);await flush();
 if(mirror && viewportName==='desktop' && ['entrada','saida','confirmacao','motivo-obrigatorio','saida-preenchida'].includes(state)){viewportName='mobile';await page.setViewportSize({width:390,height:844});await capture(id,state,false);viewportName='desktop';await page.setViewportSize(old);}
}
async function effect(id,fn){const antes=await snap();const result=await fn();await page.waitForTimeout(650);await settle();const depois=await snap();evidence.efeitos.push({id,execucao:complementar?'complementar':'principal',antes,depois,tabelasAlteradas:Object.keys(antes).filter(t=>JSON.stringify(antes[t])!==JSON.stringify(depois[t]))});await flush();return result;}
async function scenario(id,fn){console.log('AUDITORIA',viewportName,id);try{await fn();}catch(e){evidence.falhas.push({id,viewport:viewportName,motivo:String(e.message).split('\n').slice(0,5).join(' ')});console.log('CENARIO NAO CONCLUIDO',id,String(e.message).split('\n')[0]);try{await capture(id,'falhou');}catch{}await page.keyboard.press('Escape').catch(()=>{});await flush();}}
async function go(path){await page.goto(url+path);await settle();}
async function fill(name,value){const el=page.locator(`[formcontrolname="${name}"]`);if(await el.count())await el.fill(value);else await page.locator(`[name="${name}"]`).fill(value);}
async function select(name,text){let el=page.locator(`mat-select[formcontrolname="${name}"],mat-select[name="${name}"]`);await el.click();await page.getByRole('option',{name:text,exact:true}).click();await settle();}
async function firstOption(name){await page.locator(`mat-select[formcontrolname="${name}"]`).click();await page.getByRole('option').filter({hasNotText:/Nenhum|Selecione/}).first().click();await settle();}
const btn=name=>page.getByRole('button',{name,exact:true});
async function login(perfil){
 profile=perfil;await go('/login');
 await fill('codigoOrganizacao','se06-a');await fill('email',perfil.toLowerCase()+'@example.com');await fill('senha','Auditoria-Teste-2026');await btn('Entrar').click();await page.waitForURL('**/dashboard');await settle();
}
let token;
async function auth(){return page.evaluate(()=>JSON.parse(localStorage.getItem('caisora.sessao')||'null'));}
async function api(method,path,data){
 // Sessão somente em memória; nunca escrita nas evidências.
 if(!token){token=await page.evaluate(()=>localStorage.getItem('caisora.token'));}
 const r=await page.context().request.fetch(url+'/api/v1'+path,{method,data,headers:{Authorization:'Bearer '+token}});
 let body;try{body=await r.json();}catch{body=null;}
 if(r.status()>=400)throw new Error(`API ${method} ${path}: ${r.status()} ${body?.mensagem||''}`);
 return body;
}
const list=async name=>(await api('GET','/'+name+'?size=100')).content;
async function submit(id,name){await capture(id,'entrada');await effect(id,async()=>{await btn(name).click();await settle();});await capture(id,'saida');}
async function dialog(id,open,confirm){if(id.startsWith('movimentacoes-')){await page.locator('tr').filter({hasText:'Barco Auditoria'}).first().getByRole('button').click();await page.getByRole('menuitem').filter({hasText:open}).click();}else await btn(open).click();await page.getByRole('dialog').waitFor();await capture(id,'confirmacao');const descricao=page.locator('[formcontrolname=descricao]');if(await descricao.count())await descricao.fill('Auditoria controlada de teste');await effect(id,async()=>{await page.getByRole('dialog').getByRole('button',{name:confirm,exact:true}).click();await settle();});await capture(id,'saida');}
try {
 if(process.env.CAISORA_APF_CONTROLES==='true'){
  evidence.acoes??=[];
  const acao=async(modulo,expressao,fn)=>{await fn();await settle();evidence.acoes.push({modulo,expressao,executada:true});};
  const cx=await browser.newContext({viewport:{width:1440,height:1100},locale:'pt-BR'});page=await cx.newPage();page.setDefaultTimeout(5000);token=null;viewportName='desktop';await login('ADMINISTRADOR_PLATAFORMA');
  const org=await page.evaluate(()=>JSON.parse(localStorage.getItem('caisora.usuario')).organizacaoId);for(const m of ['AMBIENTE','RELATORIOS','REFERENCIAS'])await api('PUT',`/organizacoes/${org}/modulos/${m}`,{ativo:true});await cx.close();
  const ad=await browser.newContext({viewport:{width:1440,height:1100},locale:'pt-BR'});page=await ad.newPage();page.setDefaultTimeout(5000);token=null;await login('ADMINISTRADOR_MARINA');
  const expressoes={clientes:['alterarFiltroStatus()'],embarcacoes:['alterarFiltroTipo()','alterarFiltroStatus()'],vagas:['alterarModoBusca()','alterarFiltroTipo()','alterarFiltroStatus()'],ocupacoes:['alterarFiltroEmbarcacao()','alterarFiltroVaga()','alterarFiltroStatus()'],movimentacoes:['alterarFiltroStatus()','alterarFiltroTipo()','alterarFiltroEmbarcacao()'],contratos:['aplicarFiltros()','aplicarFiltros()','aplicarFiltros()']};
  for(const [mod,exp] of Object.entries(expressoes))await scenario(mod+'-controles',async()=>{await go('/'+mod);await effect(mod+'-controles',async()=>{for(let i=0;i<await page.locator('mat-select').count();i++){const el=page.locator('mat-select').nth(i);if(await el.getAttribute('aria-disabled')==='true')continue;await el.focus();await el.press('Space');const options=page.getByRole('option').filter({hasNot:page.locator('[aria-disabled=true]')});if(await options.count()){await options.last().click();await settle();evidence.acoes.push({modulo:mod,expressao:exp[i],executada:true});}else await page.keyboard.press('Escape');}
   await capture(mod+'-consultar','filtros-selecao');
   const limpar=page.getByRole('button',{name:/Limpar (filtros|busca)/}).first();if(await limpar.count())await acao(mod,'limparFiltros()',()=>limpar.click());
  });await capture(mod+'-consultar','apos-limpar');});
  for(const [mod,newpath] of [['clientes','novo'],['embarcacoes','nova'],['vagas','nova'],['ocupacoes','nova'],['movimentacoes','nova'],['contratos','novo']])await scenario(mod+'-retornar',async()=>{await go(`/${mod}/${newpath}`);const cancelar=page.getByRole('button',{name:'Cancelar',exact:true});const link=page.getByRole('link',{name:'Cancelar',exact:true});await acao(mod,'cancelar()',()=>cancelar.count().then(n=>n?cancelar.click():link.click()));assert.equal(new URL(page.url()).pathname,'/'+mod);});
  await scenario('dashboard-controles',async()=>{await go('/dashboard');await acao('dashboard','atualizarAgora()',()=>btn('Atualizar').click());for(const el of await page.locator('button.indicador').all())if(await el.isEnabled())await el.click();await capture('dashboard','apos-atualizar');});
  await scenario('painel-tv-controles',async()=>{await go('/painel-tv');await acao('painel-tv','atualizarAgora()',()=>page.getByRole('button',{name:'Atualizar painel',exact:true}).click());await acao('painel-tv','alternarTelaCheia()',()=>page.getByRole('button',{name:'Entrar em tela cheia',exact:true}).click());await capture('painel-tv','tela-cheia');await page.getByRole('button',{name:'Sair da tela cheia',exact:true}).click();});
  await scenario('autenticacao-alternar-senha',async()=>{await page.getByRole('link',{name:'Voltar para o dashboard',exact:true}).click();await page.getByRole('button',{name:'Sair do Caisora',exact:true}).click();await settle();const el=page.locator('input[formcontrolname=senha]');const toggle=page.getByRole('button',{name:/Mostrar senha|Exibir senha/});if(await toggle.count()){await acao('autenticacao','alternarSenha()',()=>toggle.click());assert.equal(await el.getAttribute('type'),'text');}else evidence.acoes.push({modulo:'autenticacao',expressao:'alternarSenha()',executada:false,motivo:'Controle acessível não localizado'});});
  await ad.close();

 }else if(process.env.CAISORA_APF_SESSAO==='true'){
  const cx=await browser.newContext({viewport:{width:390,height:844},isMobile:true,hasTouch:true,locale:'pt-BR'});page=await cx.newPage();page.setDefaultTimeout(6500);viewportName='mobile';await login('ADMINISTRADOR_MARINA');
  await scenario('sessao',async()=>{await capture('sessao','identidade');await page.getByRole('button',{name:'Abrir menu',exact:true}).click();await capture('sessao','menu-aberto');await page.getByRole('link',{name:'Clientes',exact:true}).click();await settle();await page.getByRole('button',{name:'Sair do Caisora',exact:true}).click();await settle();assert.equal(await page.evaluate(()=>localStorage.getItem('caisora.token')),null);await capture('sessao','apos-sair');});await cx.close();
 }else if(!complementar){
 for(const [name,viewport] of [['desktop',{width:1440,height:1100}],['mobile',{width:390,height:844}]]){
  viewportName=name;const context=await browser.newContext({viewport,isMobile:name==='mobile',hasTouch:name==='mobile',locale:'pt-BR'});page=await context.newPage();page.setDefaultTimeout(6500);token=null;
  page.on('pageerror',e=>evidence.falhas.push({id:'javascript',viewport:name,motivo:e.message}));
  if(name==='desktop'){
   await scenario('autenticacao',async()=>{await go('/login');await capture('autenticacao','inicial');await fill('codigoOrganizacao','se06-a');await fill('email','ausente@example.com');await fill('senha','Invalida-teste');await btn('Entrar').click();await settle();await capture('autenticacao','credenciais-invalidas');});
   await login('ADMINISTRADOR_MARINA');
   await scenario('modulos-bloqueados',async()=>{await go('/configuracoes/variabilidade');await capture('modulos-bloqueados','marina');await go('/relatorios');await capture('modulos-bloqueados','acesso-negado');try{await api('PUT','/variabilidade/modulos/RELATORIOS',{ativo:true});throw Error('Elevação inesperada');}catch(e){assert.match(e.message,/403/);evidence.rede.push({id:'marina-nao-libera-modulo',status:403});}});
   const orgId=await page.evaluate(()=>JSON.parse(localStorage.getItem('caisora.usuario')).organizacaoId);const platform=await browser.newContext({viewport});const previous=page;page=await platform.newPage();token=null;await login('ADMINISTRADOR_PLATAFORMA');
   await scenario('liberar-modulos',async()=>{await go('/configuracoes/variabilidade');await page.getByLabel('Organização (UUID)',{exact:true}).fill(orgId);await btn('Consultar organização').click();await settle();await capture('liberar-modulos','antes');for(const label of ['Condições ambientais','Relatórios','Referências externas']){const toggle=page.locator('.modulo').filter({hasText:label}).getByRole('switch');await effect('liberar-'+label,async()=>{await toggle.click();await settle();});}await capture('liberar-modulos','depois');});
   await scenario('organizacoes-apenas-api',async()=>{const o=await effect('organizacoes-criar',()=>api('POST','/organizacoes',{nome:'Organização descartável auditoria',slug:'auditoria-descartavel',email:'org-auditoria@example.com'}));await effect('organizacoes-alterar',()=>api('PUT','/organizacoes/'+o.id,{nome:'Organização alterada',email:'org-auditoria@example.com',ativa:true}));await effect('organizacoes-inativar',()=>api('DELETE','/organizacoes/'+o.id));evidence.rede.push({id:'organizacoes-apenas-api',consultas:await api('GET','/organizacoes?size=100')});});
   await platform.close();page=previous;token=null;await go('/dashboard');
  }else await login('ADMINISTRADOR_MARINA');
  if(name==='desktop')await scenario('usuarios-apenas-api',async()=>{const u=await effect('usuarios-criar',()=>api('POST','/usuarios',{nome:'Usuário descartável auditoria',email:'usuario-auditoria@example.com',senha:'Auditoria-Teste-2026',perfil:'ATENDENTE'}));await effect('usuarios-alterar',()=>api('PUT','/usuarios/'+u.id,{nome:'Usuário alterado',perfil:'FINANCEIRO'}));await effect('usuarios-inativar',()=>api('PATCH','/usuarios/'+u.id+'/status',{ativo:false}));await effect('usuarios-ativar',()=>api('PATCH','/usuarios/'+u.id+'/status',{ativo:true}));evidence.rede.push({id:'usuarios-apenas-api',quantidade:(await list('usuarios')).length});});
  const boats=await list('embarcacoes'),clients=await list('clientes'),spots=await list('vagas');
  const routes=[['dashboard','/dashboard'],['painel-tv','/painel-tv'],['clientes-consultar','/clientes'],['clientes-cadastrar-pf','/clientes/novo'],['clientes-alterar',`/clientes/${clients[0].id}/editar`],['embarcacoes-consultar','/embarcacoes'],['embarcacoes-cadastrar','/embarcacoes/nova'],['embarcacoes-alterar',`/embarcacoes/${boats[0].id}/editar`],['vagas-consultar','/vagas'],['vagas-cadastrar','/vagas/nova'],['vagas-alterar',`/vagas/${spots[0].id}/editar`],['ocupacoes-consultar','/ocupacoes'],['ocupacoes-cadastrar','/ocupacoes/nova'],['movimentacoes-consultar','/movimentacoes'],['movimentacoes-cadastrar','/movimentacoes/nova'],['contratos-consultar','/contratos'],['contratos-cadastrar','/contratos/novo'],['checklist','/checklist-saida'],['localizacao','/configuracoes/localizacao'],['variabilidade','/configuracoes/variabilidade'],['mares-desabilitada','/referencias/mares']];
  const occ=await list('ocupacoes');if(occ.length)routes.push(['ocupacoes-alterar',`/ocupacoes/${occ.find(o=>o.status==='ATIVA').id}/editar`]);
  const movements=await list('movimentacoes');if(movements.length)routes.push(['movimentacoes-alterar',`/movimentacoes/${movements[0].id}/editar`]);
  const contracts=await list('contratos');if(contracts.length){routes.push(['contratos-detalhe',`/contratos/${contracts[0].id}`]);routes.push(['contratos-alterar',`/contratos/${contracts[0].id}/editar`]);}
  for(const [id,path] of routes)await scenario(id,async()=>{await effect(id+'-abrir',()=>go(path));await capture(id,'inicial');if(path.endsWith('/novo')||path.endsWith('/nova')){const submit=page.locator('button[type=submit]');if(await submit.count() && await submit.isEnabled()){await submit.click();await capture(id,'validacao-obrigatorios');}}});
  for(const [group,label,value] of [['clientes','Buscar por nome','Cliente'],['embarcacoes','Buscar por nome','Barco'],['vagas','Buscar por codigo','S-01']])await scenario(group+'-pesquisar',async()=>{await go('/'+group);const input=page.getByLabel(label,{exact:true});await input.fill(value);await capture(group+'-consultar','entrada');await effect(group+'-pesquisar',async()=>{await btn('Pesquisar').click();await settle();});await capture(group+'-consultar','saida');await input.fill('SEM-RESULTADOS-AUDITORIA');await btn('Pesquisar').click();await settle();await capture(group+'-consultar','vazio');});
  for(const [type,label] of [['ocupacao','Ocupação de vagas'],['movimentacoes','Desempenho das movimentações'],['contratos','Carteira de contratos'],['permanencia-vagas','Permanência histórica nas vagas']])await scenario('relatorio-'+type,async()=>{await go('/relatorios');await select('relatorio',label);if(type==='movimentacoes'||type==='permanencia-vagas'){await fill('inicio','2026-10-10');await fill('fim','2026-10-10');}await capture('relatorio-'+type,'entrada');await effect('relatorio-'+type,async()=>{await btn('Gerar relatório').click();await settle();});await capture('relatorio-'+type,'saida');if(type==='ocupacao')await fill('setor','INEXISTENTE');else {await fill('inicio','2099-01-01');await fill('fim','2099-01-01');}await btn('Gerar relatório').click();await settle();await capture('relatorio-'+type,'vazio');});
  for(const [type,label] of [['meteorologia','Meteorologia'],['maritimo','Condições marítimas']])await scenario('ambiente-'+type,async()=>{await go('/ambiente');await select('tipo',label);await capture('ambiente-'+type,'entrada-controlada');await effect('ambiente-'+type,async()=>{await btn('Consultar').click();await settle();});await capture('ambiente-'+type,'saida-controlada');await page.route('**/api/v1/ambiente/**',r=>r.fulfill({status:503,contentType:'application/json',body:JSON.stringify({mensagem:'Provedor indisponível — resposta controlada de teste'})}),{times:1});await btn('Consultar').click();await settle();await capture('ambiente-'+type,'indisponivel-controlado');});
  await scenario('checklist-concluir',async()=>{await go('/checklist-saida');await effect('checklist-concluir',async()=>{for(const c of await page.getByRole('checkbox').all())await c.check();await capture('checklist-concluir','entrada');await btn('Concluir checklist').click();});await capture('checklist-concluir','saida');await page.reload();await settle();assert.equal(await page.getByRole('checkbox',{checked:true}).count(),0);await capture('checklist-concluir','apos-recarregar');});
  if(name==='desktop'){
   await scenario('clientes-cadastrar-pf',async()=>{await go('/clientes/novo');await fill('nome','Cliente PF Auditoria');await fill('cpfCnpj','12345678909');await fill('email','pf-auditoria@example.com');await fill('observacoes','Dado sintético de teste');await submit('clientes-cadastrar-pf','Cadastrar cliente');assert.ok((await list('clientes')).some(c=>c.nome==='Cliente PF Auditoria'));});
   await scenario('clientes-cadastrar-pj',async()=>{await go('/clientes/novo');await select('tipoPessoa','Pessoa jurídica');await fill('nome','Empresa Auditoria');await fill('razaoSocial','Empresa Sintética de Teste');await fill('cpfCnpj','11222333000181');await submit('clientes-cadastrar-pj','Cadastrar cliente');assert.ok((await list('clientes')).some(c=>c.nome==='Empresa Auditoria'));});
   await scenario('clientes-alterar',async()=>{const c=(await list('clientes')).find(c=>c.nome==='Cliente PF Auditoria');await go(`/clientes/${c.id}/editar`);await fill('observacoes','Alteração comprovada em banco descartável');await submit('clientes-alterar','Salvar alterações');});
   await scenario('embarcacoes-cadastrar',async()=>{await go('/embarcacoes/nova');const owner=page.getByLabel('Pesquise o proprietário',{exact:true});console.log('CONTROLES BARCO',await page.locator('input,mat-select').evaluateAll(es=>es.map(e=>({tag:e.tagName,form:e.getAttribute('formcontrolname'),placeholder:e.getAttribute('placeholder')}))));if(await owner.count()){await owner.fill('Cliente PF');await page.getByRole('option').first().click();}else await firstOption('proprietarioId');await fill('nome','Barco Auditoria');await select('tipo','Lancha');await fill('numeroInscricao','APF-TESTE-01');await fill('comprimentoTotalMetros','8');await fill('bocaMetros','2');await submit('embarcacoes-cadastrar','Cadastrar embarcação');});
   await scenario('vagas-cadastrar',async()=>{await go('/vagas/nova');await fill('codigo','AUD-01');await select('tipo','Seca');await fill('setor','Auditoria');await fill('comprimentoMaximoMetros','20');await fill('bocaMaximaMetros','5');await submit('vagas-cadastrar','Cadastrar vaga');});
   await scenario('localizacao-salvar',async()=>{await go('/configuracoes/localizacao');for(const [label,value] of [['Latitude','-27.6'],['Longitude','-48.56']]){const el=page.getByLabel(label,{exact:true});await el.fill('');await el.fill(value);await el.press('Tab');await settle();}await submit('localizacao-salvar','Salvar localização');});
   const barcoAuditoria=async()=>(await list('embarcacoes')).find(b=>b.nome==='Barco Auditoria');
   const vagaAuditoria=async()=>(await list('vagas')).find(v=>v.codigo==='AUD-01');
   await scenario('posicao-primeira-consulta',async()=>{const b=await barcoAuditoria();const p=await effect('posicao-primeira-consulta',()=>api('GET',`/embarcacoes/${b.id}/posicao`));evidence.rede.push({id:'posicao-primeira-consulta',resposta:p});});
   for(const [group,idField,title] of [['embarcacoes','nome','Barco Auditoria'],['vagas','codigo','AUD-01']])await scenario(group+'-alterar',async()=>{const x=(await list(group)).find(b=>b[idField]===title);await go(`/${group}/${x.id}/editar`);await fill('observacoes','Alteração de auditoria em ambiente descartável');await submit(group+'-alterar',group==='vagas'?'Salvar alteracoes':'Salvar alterações');});
   await scenario('ocupacoes-cadastrar',async()=>{await go('/ocupacoes/nova');await page.locator('mat-select[formcontrolname=embarcacaoId]').click();await page.getByRole('option').filter({hasText:'Barco Auditoria'}).click();await page.locator('mat-select[formcontrolname=vagaId]').click();await page.getByRole('option').filter({hasText:'AUD-01'}).click();await fill('inicioEm','2026-10-22T08:00');await fill('fimPrevistoEm','2026-10-23T18:00');await submit('ocupacoes-cadastrar','Registrar ocupação');});
   await scenario('ocupacoes-alterar',async()=>{const b=await barcoAuditoria();const o=(await list('ocupacoes')).find(o=>o.embarcacaoId===b.id&&o.status==='ATIVA');await go(`/ocupacoes/${o.id}/editar`);await fill('observacoes','Previsão revisada por auditoria');await submit('ocupacoes-alterar','Salvar alterações');});
   await scenario('ocupacoes-conflito',async()=>{await go('/ocupacoes/nova');await page.locator('mat-select[formcontrolname=embarcacaoId]').click();await capture('ocupacoes-conflito','opcoes-bloqueadas');await page.keyboard.press('Escape');});
   await scenario('contratos-cadastrar',async()=>{await go('/contratos/novo');await page.locator('mat-select[formcontrolname=clienteId]').click();await page.getByRole('option').filter({hasText:'Cliente de demonstração SE06'}).first().click();await firstOption('embarcacaoId');await select('tipoVagaContratada','Vaga molhada');await select('periodicidade','Mensal');await fill('dataInicio','2026-10-01');await fill('valorBase','450');await fill('diaVencimento','10');await submit('contratos-cadastrar','Criar contrato');});
   let contractId=(await list('contratos'))[0]?.id;
   await scenario('contratos-alterar',async()=>{await go(`/contratos/${contractId}/editar`);await fill('valorBase','500');await submit('contratos-alterar','Salvar alterações');});
   await scenario('contratos-enviar',async()=>{await go(`/contratos/${contractId}`);await dialog('contratos-enviar','Enviar para assinatura','Enviar para assinatura');});
   await scenario('contratos-ativar',async()=>{await go(`/contratos/${contractId}`);await dialog('contratos-ativar','Ativar','Ativar contrato');});
   await scenario('contratos-vincular',async()=>{await go(`/contratos/${contractId}`);await page.getByLabel('Ocupação ativa da embarcação',{exact:true}).focus();await page.getByLabel('Ocupação ativa da embarcação',{exact:true}).press('Space');await page.getByRole('option').first().click();await submit('contratos-vincular','Vincular');});
   await scenario('contratos-desvincular',async()=>{await page.getByRole('button',{name:'Desvincular ocupação',exact:true}).click();await capture('contratos-desvincular','confirmacao');await page.getByRole('dialog').getByRole('button',{name:'Desvincular',exact:true}).click();await capture('contratos-desvincular','motivo-obrigatorio');await page.locator('[formcontrolname=descricao]').fill('Desvinculação de auditoria');await effect('contratos-desvincular',async()=>{await page.getByRole('dialog').getByRole('button',{name:'Desvincular',exact:true}).click();await settle();});await capture('contratos-desvincular','saida');});
   for(const [id,open,confirm] of [['contratos-suspender','Suspender','Suspender'],['contratos-reativar','Reativar','Reativar'],['contratos-solicitar-encerramento','Solicitar encerramento','Iniciar encerramento'],['contratos-encerrar','Encerrar contrato','Encerrar contrato']])await scenario(id,async()=>{await go(`/contratos/${contractId}`);await dialog(id,open,confirm);});
   await scenario('contratos-cancelar',async()=>{const c=(await list('clientes'))[0],b=(await list('embarcacoes')).find(b=>b.proprietarioId===c.id);const r=await api('POST','/contratos',{clienteId:c.id,embarcacaoId:b.id,tipoVagaContratada:'MOLHADA',periodicidade:'MENSAL',dataInicio:'2026-10-01',valorBase:300,diaVencimento:10,renovacaoAutomatica:false});await go(`/contratos/${r.id}`);await dialog('contratos-cancelar','Cancelar','Cancelar contrato');});
   await scenario('ocupacoes-encerrar',async()=>{await go('/ocupacoes');const row=page.locator('tr').filter({hasText:'Barco Auditoria'}).first();await row.getByRole('button').click();await page.getByRole('menuitem').filter({hasText:'Encerrar'}).click();await capture('ocupacoes-encerrar','confirmacao');await effect('ocupacoes-encerrar',async()=>{await page.getByRole('dialog').getByRole('button',{name:'Encerrar',exact:true}).click();await settle();});await capture('ocupacoes-encerrar','saida');});
   await scenario('movimentacoes-cadastrar',async()=>{await go('/movimentacoes/nova');await page.locator('mat-select[formcontrolname=embarcacaoId]').click();await page.getByRole('option').filter({hasText:'Barco Auditoria'}).click();await select('tipo','Deslocamento interno');await firstOption('tipoPosicaoDestino');if(await page.locator('[formcontrolname=descricaoDestino]').count())await fill('descricaoDestino','Área de teste');await fill('agendadaPara','2026-10-22T17:00');await submit('movimentacoes-cadastrar','Agendar movimentação');});
   await scenario('movimentacoes-alterar',async()=>{const m=(await list('movimentacoes'))[0];await go(`/movimentacoes/${m.id}/editar`);await fill('observacoes','Revisão de agendamento de auditoria');await submit('movimentacoes-alterar','Salvar alterações');});
   for(const [id,open,confirm] of [['movimentacoes-iniciar','Iniciar','Iniciar'],['movimentacoes-concluir','Concluir','Concluir']])await scenario(id,async()=>{await go('/movimentacoes');await dialog(id,open,confirm);});
   await scenario('movimentacoes-cancelar',async()=>{const b=await barcoAuditoria();await api('POST','/movimentacoes',{embarcacaoId:b.id,tipo:'DESLOCAMENTO_INTERNO',prioridade:'NORMAL',tipoPosicaoDestino:'AREA_SERVICO',descricaoDestino:'Cenário de cancelamento',agendadaPara:'2026-10-23T18:00:00Z'});await go('/movimentacoes');await page.locator('tr').filter({hasText:'Barco Auditoria'}).filter({hasText:'Agendada'}).first().getByRole('button').click();await page.getByRole('menuitem').filter({hasText:'Cancelar'}).click();await capture('movimentacoes-cancelar','confirmacao');const reason=page.locator('textarea');await reason.fill('Cancelamento de auditoria');await effect('movimentacoes-cancelar',async()=>{await page.getByRole('dialog').getByRole('button',{name:'Cancelar movimentação',exact:true}).click();await settle();});await capture('movimentacoes-cancelar','saida');});
   for(const group of ['clientes','embarcacoes','vagas'])for(const action of ['Inativar','Ativar'])await scenario(group+'-'+action.toLowerCase(),async()=>{await go('/'+group);const row=page.locator('tr').filter({hasText:group==='clientes'?'Empresa Auditoria':group==='embarcacoes'?'Barco Auditoria':'AUD-01'}).first();await row.getByRole('button').click();await page.getByRole('menuitem').filter({hasText:action}).click();await capture(group+'-'+action.toLowerCase(),'confirmacao');await effect(group+'-'+action.toLowerCase(),async()=>{await page.getByRole('dialog').getByRole('button',{name:action,exact:true}).click();await settle();});await capture(group+'-'+action.toLowerCase(),'saida');});
   for(const [type,label] of [['movimentacoes','Desempenho das movimentações'],['contratos','Carteira de contratos']])await scenario('relatorio-'+type+'-preenchido',async()=>{await go('/relatorios');await select('relatorio',label);if(type==='movimentacoes'){await fill('inicio','2026-10-01');await fill('fim','2026-10-31');}await capture('relatorio-'+type,'entrada-preenchida');await btn('Gerar relatório').click();await settle();await capture('relatorio-'+type,'saida-preenchida');evidence.rede.push({id:'relatorio-'+type,resposta:respostas.get(type)?.data});});
   for(const route of ['dashboard','painel-tv'])await scenario(route+'-preenchido',async()=>{await go('/'+route);await capture(route,'saida-preenchida');});

  }
  await context.close();
 }
 // Distinções de perfil verificadas em sessões separadas, sem gravar JWT.
 for(const p of ['ADMINISTRADOR_PLATAFORMA','ADMINISTRADOR_MARINA','GERENTE','ATENDENTE','FINANCEIRO']){
  const ctx=await browser.newContext({viewport:{width:1440,height:1100}});page=await ctx.newPage();token=null;profile=p;viewportName='desktop';await scenario('perfil-'+p.toLowerCase(),async()=>{await login(p);await capture('perfil-'+p.toLowerCase(),'menu');const result={perfil:p};for(const path of ['/clientes','/configuracoes/variabilidade','/relatorios']){await go(path);result[path]=new URL(page.url()).pathname;}evidence.perfis.push(result);});await ctx.close();
 }
 }else{
 const ctx=await browser.newContext({viewport:{width:1440,height:1100},locale:'pt-BR'});page=await ctx.newPage();page.setDefaultTimeout(7000);viewportName='desktop';token=null;
 await login('ADMINISTRADOR_PLATAFORMA');
 const o=await page.evaluate(()=>JSON.parse(localStorage.getItem('caisora.usuario')).organizacaoId);
 for(const m of ['AMBIENTE','RELATORIOS','REFERENCIAS'])await api('PUT',`/organizacoes/${o}/modulos/${m}`,{ativo:true});
 await ctx.close();
 const admin=await browser.newContext({viewport:{width:1440,height:1100},locale:'pt-BR'});page=await admin.newPage();page.setDefaultTimeout(7000);token=null;await login('ADMINISTRADOR_MARINA');
 await scenario('localizacao-salvar',async()=>{await go('/configuracoes/localizacao');for(const [label,v] of [['Latitude','-27.6'],['Longitude','-48.56']]){const el=page.getByLabel(label,{exact:true});await el.fill('');await el.fill(v);await el.press('Tab');await settle();}await submit('localizacao-salvar','Salvar localização');assert.equal((await api('GET','/minha-organizacao/coordenadas')).latitude,-27.6);});
 const b=(await list('embarcacoes'))[0],c=(await list('clientes'))[0];
 let contrato=await api('POST','/contratos',{clienteId:c.id,embarcacaoId:b.id,tipoVagaContratada:'MOLHADA',periodicidade:'MENSAL',dataInicio:'2026-10-01',valorBase:600,diaVencimento:10,renovacaoAutomatica:false});
 await api('POST',`/contratos/${contrato.id}/enviar-para-assinatura`,{});await api('POST',`/contratos/${contrato.id}/ativar`,{dataAssinatura:'2026-10-22'});
 await scenario('contratos-vincular',async()=>{await go(`/contratos/${contrato.id}`);const s=page.getByLabel('Ocupação ativa da embarcação',{exact:true});await s.focus();await s.press('Space');await page.getByRole('option').first().click();await submit('contratos-vincular','Vincular');assert.equal((await api('GET',`/contratos/${contrato.id}/ocupacoes`)).filter(x=>x.aberto).length,1);});
 await scenario('contratos-desvincular',async()=>{await go(`/contratos/${contrato.id}`);const button=page.getByRole('button',{name:'Desvincular ocupação',exact:true});await button.focus();await button.press('Enter');await capture('contratos-desvincular','confirmacao');await page.getByRole('dialog').getByRole('button',{name:'Desvincular',exact:true}).click();await capture('contratos-desvincular','motivo-obrigatorio');await page.locator('[formcontrolname=descricao]').fill('Desvinculação em teste descartável');await effect('contratos-desvincular',async()=>{await page.getByRole('dialog').getByRole('button',{name:'Desvincular',exact:true}).click();});await capture('contratos-desvincular','saida');assert.equal((await api('GET',`/contratos/${contrato.id}/ocupacoes`)).filter(x=>x.aberto).length,0);});
 await scenario('permissoes-salvar',async()=>{await go('/configuracoes/variabilidade');await page.getByLabel('Perfil',{exact:true}).click();await page.getByRole('option',{name:'Gerente',exact:true}).click();const row=page.locator('.linha-permissao').filter({hasText:'Referências externas'});await row.getByRole('checkbox').uncheck();await submit('permissoes-salvar','Salvar permissões');});
 await scenario('movimentacoes-transferencia',async()=>{
  const v=await api('POST','/vagas',{codigo:'TRANSFER-TESTE',tipo:'SECA',setor:'Auditoria',comprimentoMaximoMetros:20,bocaMaximaMetros:5,possuiAgua:true,possuiEnergia:true});
  await api('GET',`/embarcacoes/${b.id}/posicao`);
  const m=await api('POST','/movimentacoes',{embarcacaoId:b.id,tipo:'TRANSFERENCIA',prioridade:'NORMAL',tipoPosicaoDestino:'VAGA',vagaDestinoId:v.id,agendadaPara:new Date(Date.now()+3600000).toISOString()});
  await go('/movimentacoes');await capture('movimentacoes-transferencia','antes');for(const r of ['dashboard','painel-tv']){await go('/'+r);await capture(r,'transferencia-agendada');evidence.rede.push({id:r+'-transferencia-agendada',resposta:respostas.get(r==='dashboard'?'painel-operacional':'painel-tv')?.data});}await go('/movimentacoes');
  for(const action of ['Iniciar','Concluir']){await page.locator('tr').filter({hasText:'Barco SE06'}).first().getByRole('button').click();await page.getByRole('menuitem').filter({hasText:action}).click();await capture('movimentacoes-transferencia',action.toLowerCase()+'-confirmacao');await effect('movimentacoes-transferencia-'+action.toLowerCase(),async()=>{await page.getByRole('dialog').getByRole('button',{name:action,exact:true}).click();});}
  const pos=await api('GET',`/embarcacoes/${b.id}/posicao`);assert.equal(pos.vagaId,v.id);
  evidence.rede.push({id:'movimentacoes-transferencia',posicao:pos,ocupacoes:(await list('ocupacoes')).filter(z=>z.embarcacaoId===b.id)});
  await capture('movimentacoes-transferencia','saida');await go('/ocupacoes');await capture('movimentacoes-transferencia','ocupacoes-apos-conclusao');
  await effect('movimentacoes-historico',async()=>{evidence.rede.push({id:'movimentacoes-historico',resposta:await api('GET',`/movimentacoes/${m.id}/historico`)});});
  await effect('posicoes-listar',async()=>{evidence.rede.push({id:'posicoes-listar',resposta:await api('GET','/posicoes-embarcacoes')});});
 });
 await scenario('sessao',async()=>{await capture('sessao','identidade');await page.getByRole('button',{name:'Sair do Caisora',exact:true}).click();await capture('sessao','apos-sair');});
 await admin.close();
 for(const p of ['ADMINISTRADOR_PLATAFORMA','ADMINISTRADOR_MARINA','GERENTE','ATENDENTE','FINANCEIRO']){
  const cx=await browser.newContext({viewport:{width:1440,height:1100},locale:'pt-BR'});page=await cx.newPage();page.setDefaultTimeout(6500);token=null;profile=p;
  await login(p);
  for(const path of ['/clientes','/contratos/'+contrato.id,'/relatorios','/referencias/paises','/configuracoes/variabilidade'])await scenario('perfil-'+p.toLowerCase(),async()=>{await go(path);await capture('perfil-'+p.toLowerCase(),path.split('/').filter(Boolean)[0]+'-acesso');});
  if(p==='GERENTE')await scenario('permissao-interna-negada',async()=>{try{await api('GET','/referencias/paises');throw Error('Permissão indevida');}catch(e){assert.match(e.message,/403/);evidence.rede.push({id:'referencias-modulo-habilitado-perfil-negado',status:403});}await capture('permissao-interna-negada','gerente');});
  if(p==='ATENDENTE')await scenario('contrato-acao-sem-permissao',async()=>{await go('/contratos/'+contrato.id);await btn('Suspender').click();await page.locator('[formcontrolname=descricao]').fill('Tentativa sem permissão');await page.getByRole('dialog').getByRole('button',{name:'Suspender',exact:true}).click();await page.waitForTimeout(700);await capture('contrato-acao-sem-permissao','erro-backend');evidence.rede.push({id:'atendente-contrato-status',status:respostas.get('suspender')?.status});});
  await cx.close();
 }

 }
 await flush();
}finally{await flush();await browser.close();await new Promise(r=>server.close(r));}
