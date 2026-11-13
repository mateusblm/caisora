// Interface manual. Persistência pelas classes e SQL gerados pelo MyBatis Generator.
const $ = id => document.getElementById(id);
const modules = {
  CLIENTES: ['Clientes', 'Cadastro e acompanhamento de clientes', ['VISUALIZAR', 'CRIAR', 'EDITAR', 'ALTERAR_STATUS']],
  EMBARCACOES: ['Embarcações', 'Cadastro e informações das embarcações', ['VISUALIZAR', 'CRIAR', 'EDITAR', 'ALTERAR_STATUS']],
  VAGAS: ['Vagas', 'Organização das vagas da marina', ['VISUALIZAR', 'CRIAR', 'EDITAR', 'ALTERAR_STATUS']],
  OCUPACOES: ['Ocupações', 'Controle de entrada e permanência nas vagas', ['VISUALIZAR', 'CRIAR', 'EDITAR', 'CONCLUIR']],
  MOVIMENTACOES: ['Movimentações', 'Acompanhamento das operações de pátio', ['VISUALIZAR', 'CRIAR', 'EDITAR', 'INICIAR', 'CONCLUIR', 'CANCELAR']],
  CONTRATOS: ['Contratos', 'Gestão dos contratos de serviços', ['VISUALIZAR', 'CRIAR', 'EDITAR', 'ALTERAR_STATUS']],
  PAINEL_TV: ['Painel TV', 'Visualização das operações da marina', ['VISUALIZAR']],
  USUARIOS: ['Usuários', 'Cadastro de usuários da organização', ['VISUALIZAR', 'CRIAR', 'EDITAR', 'ALTERAR_STATUS']],
  CHECKLIST_SAIDA: ['Checklist de saída', 'Conferência da embarcação antes da saída', ['VISUALIZAR', 'EDITAR', 'CONCLUIR']],
  AMBIENTE: ['Condições ambientais', 'Consulta das condições na região da marina', ['VISUALIZAR']],
  REFERENCIAS: ['Referências externas', 'Consulta a catálogos e serviços externos', ['VISUALIZAR']],
  RELATORIOS: ['Relatórios', 'Consulta de indicadores e relatórios operacionais', ['VISUALIZAR']],
  DASHBOARD: ['Dashboard', 'Visão geral da operação', ['VISUALIZAR']],
  CONFIGURACOES: ['Configurações', 'Preferências e controle de acesso da marina', ['CONFIGURAR']]
};
const profiles = { ADMINISTRADOR_MARINA: 'Administrador da marina', GERENTE: 'Gerente', ATENDENTE: 'Atendente', FINANCEIRO: 'Financeiro', ADMINISTRADOR_PLATAFORMA: 'Administrador da plataforma' };
const actions = { VISUALIZAR: 'Visualizar', CRIAR: 'Criar', EDITAR: 'Editar', ALTERAR_STATUS: 'Alterar situação', INICIAR: 'Iniciar', CONCLUIR: 'Concluir', CANCELAR: 'Cancelar', CONFIGURAR: 'Configurar' };
const moduleName = code => modules[code]?.[0] || code;
let auth = sessionStorage.getItem('dbf-auth') || '';
let table = 'modulos', editing = null, pendingDelete = null, records = [], busy = false, loadVersion = 0;

function message(text, error = false) {
  $('message').textContent = text;
  $('message').classList.toggle('error', error);
  $('message').hidden = !text;
}
async function api(path = '', method = 'GET', body, resource = table) {
  const response = await fetch('/api/' + resource + path, {
    method, headers: { Authorization: auth, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined
  });
  if (response.status === 204) return;
  const result = await response.json();
  if (!response.ok) {
    if (response.status === 401) logout(false);
    throw new Error(result.erro || 'Não foi possível concluir a operação. Tente novamente.');
  }
  return result;
}
function options(id, values, selected) {
  $(id).replaceChildren();
  for (const [value, label] of values) $(id).add(new Option(label, value));
  if (selected && !Array.from($(id).options).some(option => option.value === selected)) $(id).add(new Option(selected, selected));
  if (selected) $(id).value = selected;
}
function actionOptions(selected) {
  options('acao', (modules[$('modulo').value]?.[2] || Object.keys(actions)).map(code => [code, actions[code]]), selected);
}
function form(row = null) {
  editing = row?.id || null;
  $('record-form').reset(); $('form-error').hidden = true;
  const permission = table === 'permissoes';
  options('modulo', Object.entries(modules).map(([code, value]) => [code, value[0]]), row?.modulo);
  options('perfil', Object.entries(profiles), row?.perfil);
  actionOptions(row?.acao);
  $('permission-fields').hidden = !permission;
  $('perfil').required = permission; $('acao').required = permission;
  $('form-title').textContent = (row ? 'Editar ' : 'Adicionar ') + (permission ? 'permissão' : 'módulo');
  $('form-description').textContent = permission ? 'Defina o acesso do perfil às funções do módulo.' : 'Defina a disponibilidade do módulo nesta marina.';
  $('enabled-label').textContent = permission ? 'Acesso' : 'Situação';
  options('enabled', permission ? [['true', 'Permitido'], ['false', 'Bloqueado']] : [['true', 'Ativo'], ['false', 'Desativado']], String(row ? (permission ? row.permitido : row.ativo) : true));
  $('save').textContent = row ? 'Salvar alterações' : 'Adicionar ' + (permission ? 'permissão' : 'módulo');
  $('record-dialog').showModal(); $('modulo').focus();
}
function logout(clearMessage = true) {
  auth = ''; sessionStorage.removeItem('dbf-auth');
  $('workspace').hidden = true; $('login-section').hidden = false; $('logout').hidden = true;
  $('password').value = '';
  if ($('record-dialog').open) $('record-dialog').close();
  if ($('delete-dialog').open) $('delete-dialog').close();
  if (clearMessage) message('Você saiu do Caisora.');
}
function cell(row, value) {
  const element = document.createElement('td');
  if (value instanceof Node) element.append(value); else element.textContent = value;
  row.append(element); return element;
}
function iconButton(label, path, onClick) {
  const button = document.createElement('button'); button.type = 'button'; button.className = 'icon-button';
  button.ariaLabel = label; button.title = label; button.onclick = onClick;
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg'); svg.setAttribute('viewBox', '0 0 24 24'); svg.setAttribute('aria-hidden', 'true');
  const shape = document.createElementNS('http://www.w3.org/2000/svg', 'path'); shape.setAttribute('d', path); svg.append(shape); button.append(svg);
  return button;
}
function render() {
  const permission = table === 'permissoes';
  const query = $('search').value.toLocaleLowerCase('pt-BR').trim();
  const visible = records.filter(row => [moduleName(row.modulo), profiles[row.perfil], actions[row.acao]].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR').includes(query))
    .sort((a, b) => moduleName(a.modulo).localeCompare(moduleName(b.modulo), 'pt-BR'));
  $('rows').replaceChildren(); $('head').replaceChildren();
  const heading = document.createElement('tr');
  (permission ? ['Perfil', 'Módulo', 'Ação', 'Acesso', 'Ações'] : ['Módulo', 'Situação', 'Ações']).forEach(title => {
    const th = document.createElement('th'); th.scope = 'col'; th.textContent = title; heading.append(th);
  });
  $('head').append(heading);
  $('count').textContent = visible.length + ' ' + (permission ? (visible.length === 1 ? 'permissão' : 'permissões') : (visible.length === 1 ? 'módulo' : 'módulos'));
  $('list-summary').textContent = visible.length ? 'Exibindo ' + visible.length + ' de ' + records.length : '';
  $('empty').hidden = visible.length > 0;
  $('empty').querySelector('h2').textContent = query ? 'Nenhum resultado encontrado' : (permission ? 'Nenhuma permissão cadastrada' : 'Nenhum módulo cadastrado');
  $('empty').querySelector('p').textContent = query ? 'Tente buscar por outro termo.' : 'Adicione ' + (permission ? 'uma permissão para definir o acesso de um perfil.' : 'um módulo para definir sua disponibilidade nesta marina.');
  for (const row of visible) {
    const tr = document.createElement('tr');
    if (permission) cell(tr, profiles[row.perfil] || row.perfil);
    const title = document.createElement('strong'); title.textContent = moduleName(row.modulo);
    const name = cell(tr, title);
    if (!permission && modules[row.modulo]) { const description = document.createElement('small'); description.textContent = modules[row.modulo][1]; name.append(description); }
    if (permission) cell(tr, actions[row.acao] || row.acao);
    const enabled = permission ? row.permitido : row.ativo;
    const badge = document.createElement('span'); badge.className = enabled ? 'badge' : 'badge off';
    badge.textContent = permission ? (enabled ? 'Permitido' : 'Bloqueado') : (enabled ? 'Ativo' : 'Desativado'); cell(tr, badge);
    const controls = document.createElement('div'); controls.className = 'row-actions';
    controls.append(iconButton('Editar ' + moduleName(row.modulo), 'm16 3 5 5-12 12H4v-5ZM14 5l5 5', () => form(row)));
    controls.append(iconButton('Remover ' + moduleName(row.modulo), 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7', () => {
      pendingDelete = row.id; $('delete-error').hidden = true;
      $('delete-title').textContent = 'Remover ' + (permission ? 'permissão?' : 'módulo?');
      $('confirm-delete').textContent = 'Remover ' + (permission ? 'permissão' : 'módulo');
      $('delete-detail').textContent = permission ? 'A permissão de ' + (actions[row.acao]?.toLocaleLowerCase('pt-BR') || row.acao) + ' em ' + moduleName(row.modulo) + ' será removida do perfil ' + (profiles[row.perfil] || row.perfil) + '.' : 'A configuração do módulo ' + moduleName(row.modulo) + ' será removida desta marina.';
      $('delete-dialog').showModal(); $('cancel-delete').focus();
    }));
    cell(tr, controls); $('rows').append(tr);
  }
}
async function load() {
  const version = ++loadVersion, resource = table;
  $('refresh').disabled = true; $('count').textContent = 'Carregando…';
  try { const data = await api('', 'GET', undefined, resource); if (version === loadVersion) { records = data; render(); } }
  finally { if (version === loadVersion) $('refresh').disabled = false; }
}
async function navigate(resource) {
  if (busy) return;
  table = resource; $('search').value = ''; records = []; render(); message('');
  const permission = table === 'permissoes';
  $('page-title').textContent = permission ? 'Permissões por perfil' : 'Módulos da marina';
  $('page-description').textContent = permission ? 'Defina as ações que cada perfil pode executar nos módulos da marina.' : 'Gerencie os módulos disponíveis para a operação da marina.';
  $('new-record').querySelector('span').textContent = 'Adicionar ' + (permission ? 'permissão' : 'módulo');
  $('search').placeholder = permission ? 'Buscar perfil, módulo ou ação' : 'Buscar módulo';
  for (const name of ['modulos', 'permissoes']) {
    $('nav-' + name).classList.toggle('active', name === table);
    if (name === table) $('nav-' + name).setAttribute('aria-current', 'page'); else $('nav-' + name).removeAttribute('aria-current');
  }
  try { await load(); } catch (error) { message(error.message, true); }
}
function setBusy(value) {
  busy = value;
  for (const id of ['save', 'cancel', 'close-form', 'confirm-delete', 'cancel-delete']) $(id).disabled = value;
}
$('login-form').onsubmit = async event => {
  event.preventDefault();
  auth = 'Basic ' + btoa(String.fromCharCode(...new TextEncoder().encode('demo:' + $('password').value)));
  try {
    await load(); sessionStorage.setItem('dbf-auth', auth); $('password').value = '';
    $('login-section').hidden = true; $('workspace').hidden = false; $('logout').hidden = false; message('');
  } catch (error) { message(error.message, true); }
};
$('logout').onclick = () => logout();
$('nav-modulos').onclick = () => navigate('modulos');
$('nav-permissoes').onclick = () => navigate('permissoes');
$('refresh').onclick = () => load().then(() => message('Listagem atualizada.')).catch(error => message(error.message, true));
$('search').oninput = render;
$('new-record').onclick = () => form();
$('modulo').onchange = () => actionOptions();
$('cancel').onclick = $('close-form').onclick = () => $('record-dialog').close();
for (const id of ['record-dialog', 'delete-dialog']) $(id).addEventListener('cancel', event => { if (busy) event.preventDefault(); });
$('record-form').onsubmit = async event => {
  event.preventDefault(); setBusy(true); $('form-error').hidden = true;
  const permission = table === 'permissoes';
  const body = { modulo: $('modulo').value, ...(permission ? { perfil: $('perfil').value, acao: $('acao').value, permitido: $('enabled').value === 'true' } : { ativo: $('enabled').value === 'true' }) };
  try {
    const wasEdit = Boolean(editing);
    await api(editing ? '/' + editing : '', wasEdit ? 'PUT' : 'POST', body);
    $('record-dialog').close(); await load();
    message(wasEdit ? 'Alterações salvas com sucesso.' : (permission ? 'Permissão adicionada' : 'Módulo adicionado') + ' com sucesso.');
  } catch (error) { $('form-error').textContent = error.message; $('form-error').hidden = false; }
  finally { setBusy(false); }
};
$('cancel-delete').onclick = () => $('delete-dialog').close();
$('confirm-delete').onclick = async () => {
  setBusy(true);
  try {
    await api('/' + pendingDelete, 'DELETE'); $('delete-dialog').close(); await load();
    message((table === 'permissoes' ? 'Permissão removida' : 'Módulo removido') + ' com sucesso.');
  } catch (error) { $('delete-error').textContent = error.message; $('delete-error').hidden = false; }
  finally { setBusy(false); }
};
if (auth) load().then(() => { $('login-section').hidden = true; $('workspace').hidden = false; $('logout').hidden = false; }).catch(error => message(error.message, true));
