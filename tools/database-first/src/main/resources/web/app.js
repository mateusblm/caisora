// Tela manual; apenas os mappers Java/XML de persistencia sao gerados pelo MBG.
const $ = id => document.getElementById(id);
let auth = sessionStorage.getItem('dbf-auth') || '';
let table = 'modulos', editing = null, pendingDelete = null, records = [];
function message(text, error = false) { $('message').textContent = text; $('message').classList.toggle('error', error); $('message').hidden = !text; }
async function api(path = '', method = 'GET', body) {
  const response = await fetch(`/api/${table}${path}`, { method, headers: { Authorization: auth, ...(body ? { 'Content-Type': 'application/json' } : {}) }, body: body ? JSON.stringify(body) : undefined });
  if (response.status === 204) return;
  const result = await response.json();
  if (!response.ok) {
    if (response.status === 401) logout(false);
    throw new Error(result.erro || 'Não foi possível completar a operação. Tente atualizar a listagem.');
  }
  return result;
}
function reset() {
  editing = null; $('record-form').reset(); $('form-title').textContent = 'Cadastrar registro';
  $('save').textContent = 'Cadastrar'; $('cancel').hidden = true; $('editing').hidden = true;
  const permission = table === 'permissoes';
  $('permission-fields').hidden = !permission; $('perfil').required = permission; $('acao').required = permission;
  $('enabled-label').textContent = permission ? 'Permitido' : 'Ativo';
}
function logout(clearMessage = true) {
  auth = ''; sessionStorage.removeItem('dbf-auth'); $('workspace').hidden = true; $('login-section').hidden = false;
  $('password').value = ''; if (clearMessage) message('Sessão local encerrada.');
}
function edit(row) {
  editing = row.id; $('form-title').textContent = 'Editar registro'; $('save').textContent = 'Salvar alteração'; $('cancel').hidden = false;
  $('editing').textContent = row.id; $('editing').hidden = false;
  for (const field of ['modulo', 'perfil', 'acao']) $(field).value = row[field] || '';
  $('enabled').value = String(table === 'modulos' ? row.ativo : row.permitido); $('modulo').focus();
}
function cell(row, value) { const element = document.createElement('td'); element.textContent = value; row.append(element); return element; }
function render() {
  $('rows').replaceChildren(); $('head').replaceChildren(); const heading = document.createElement('tr');
  (table === 'modulos' ? ['Módulo', 'Ativo', 'Operações'] : ['Módulo / Perfil', 'Ação / Permitido', 'Operações']).forEach(title => { const th = document.createElement('th'); th.scope = 'col'; th.textContent = title; heading.append(th); });
  $('head').append(heading); $('count').textContent = `${records.length} registro(s) nesta organização`; $('empty').hidden = records.length > 0;
  for (const row of records) {
    const tr = document.createElement('tr');
    const title = cell(tr, table === 'modulos' ? row.modulo : `${row.modulo} / ${row.perfil}`);
    const id = document.createElement('small'); id.textContent = row.id; title.append(id);
    cell(tr, table === 'modulos' ? (row.ativo ? 'Sim' : 'Não') : `${row.acao} / ${row.permitido ? 'Sim' : 'Não'}`);
    const actions = cell(tr, '');
    const editButton = document.createElement('button'); editButton.type = 'button'; editButton.className = 'secondary'; editButton.textContent = 'Editar'; editButton.ariaLabel = `Editar ${row.modulo}`; editButton.onclick = () => edit(row);
    const deleteButton = document.createElement('button'); deleteButton.type = 'button'; deleteButton.className = 'secondary'; deleteButton.textContent = 'Excluir'; deleteButton.ariaLabel = `Excluir ${row.modulo}`; deleteButton.onclick = () => { pendingDelete = row.id; $('delete-detail').textContent = `${row.modulo} · ${row.id}`; $('delete-dialog').showModal(); $('cancel-delete').focus(); };
    actions.append(editButton, deleteButton); $('rows').append(tr);
  }
}
async function load() {
  $('refresh').disabled = true; $('count').textContent = 'Consultando PostgreSQL…';
  try { records = await api(); render(); } finally { $('refresh').disabled = false; }
}
$('login-form').onsubmit = async event => {
  event.preventDefault(); auth = 'Basic ' + btoa('demo:' + $('password').value);
  try { await load(); sessionStorage.setItem('dbf-auth', auth); $('password').value = ''; $('login-section').hidden = true; $('workspace').hidden = false; reset(); message('Conectado ao banco exclusivo da demonstração.'); } catch (error) { message(error.message, true); }
};
$('logout').onclick = () => logout();
$('refresh').onclick = () => load().then(() => message('Listagem consultada novamente no banco.')).catch(error => message(error.message, true));
$('table').onchange = async () => { table = $('table').value; reset(); message(''); try { await load(); } catch (error) { message(error.message, true); } };
$('cancel').onclick = reset;
$('record-form').onsubmit = async event => {
  event.preventDefault(); $('save').disabled = true;
  const body = { modulo: $('modulo').value, ...(table === 'modulos' ? { ativo: $('enabled').value === 'true' } : { perfil: $('perfil').value, acao: $('acao').value, permitido: $('enabled').value === 'true' }) };
  try { const wasEdit = Boolean(editing); await api(editing ? `/${editing}` : '', editing ? 'PUT' : 'POST', body); reset(); await load(); message(wasEdit ? 'Alteração salva e consultada novamente no PostgreSQL.' : 'Registro inserido e consultado novamente no PostgreSQL.'); } catch (error) { message(error.message, true); } finally { $('save').disabled = false; }
};
$('cancel-delete').onclick = () => $('delete-dialog').close();
$('confirm-delete').onclick = async () => {
  $('confirm-delete').disabled = true;
  try { await api(`/${pendingDelete}`, 'DELETE'); if (editing === pendingDelete) reset(); $('delete-dialog').close(); await load(); message('Registro excluído. A listagem foi consultada novamente no banco.'); } catch (error) { $('delete-dialog').close(); message(error.message, true); } finally { $('confirm-delete').disabled = false; }
};
reset();
if (auth) { load().then(() => { $('login-section').hidden = true; $('workspace').hidden = false; }).catch(error => message(error.message, true)); }
