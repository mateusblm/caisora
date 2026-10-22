from pathlib import Path
import re,json,csv,hashlib,subprocess
ROOT=Path.cwd(); OUT=ROOT/'docs/evidencias/apf'; OUT.mkdir(parents=True,exist_ok=True)
readme_path=OUT/'README.md'
final_anterior=readme_path.read_text().split('## Conferência final',1)[1] if readme_path.exists() and '## Conferência final' in readme_path.read_text() else ''
sha=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
# Operações nomeadas pelo objetivo funcional; não se atribui tipo APF por verbo HTTP.
processos=[]
def add(id,mod,obj,route,group='',write='',template=None,notes='',api=''):
 processos.append(dict(id=id,mod=mod,obj=obj,route=route,group=group,write=write,template=template,notes=notes,api=api))
def tpl(group,page):
 ps=list(Path('frontend/src/app/features',group).glob(f'pages/{page}/*component.html'));return str(ps[0]) if ps else ''
add('autenticacao','Autenticação','Iniciar sessão e rejeitar credenciais inválidas','/login',template=tpl('autenticacao','login'),notes='Senha e JWT nunca compõem o inventário; captura de senha mascarada.')
add('sessao','Autenticação','Consultar identidade e encerrar sessão','Layout / botão Sair',notes='Dados da sessão e logout local; /autenticacao/me consulta permissões atualizadas.',api='GET /api/v1/autenticacao/me')
for group,singular,new,logical in [('clientes','cliente','novo','Clientes'),('embarcacoes','embarcacao','nova','Embarcações'),('vagas','vaga','nova','Vagas'),('ocupacoes','ocupacao','nova','Ocupações'),('movimentacoes','movimentacao','nova','Movimentações'),('contratos','contrato','novo','Contratos')]:
 refs={'clientes':'Clientes','embarcacoes':'Embarcações; Clientes (seleção do proprietário)','vagas':'Vagas','ocupacoes':'Ocupações; Vagas; Embarcações','movimentacoes':'Movimentações; Embarcações; Posições; Vagas; Usuários (responsável)','contratos':'Contratos; Clientes; Embarcações; Ocupações' }[group]
 add(group+'-consultar',logical,'Pesquisar e listar '+group,'/'+group,refs,template=tpl(group,singular+'-listagem'),api=f'GET /api/v1/{group}')
 variants=['-pf','-pj'] if group=='clientes' else ['']
 for v in variants:add(group+'-cadastrar'+v,logical,'Cadastrar '+singular+({'-pf':' pessoa física','-pj':' pessoa jurídica'}.get(v,'')),f'/{group}/{new}',refs,logical,template=tpl(group,singular+'-formulario'),api=f'POST /api/v1/{group}')
 add(group+'-alterar',logical,'Alterar '+singular,f'/{group}/:id/editar',refs,logical,template=tpl(group,singular+'-formulario'),api=f'PUT /api/v1/{group}/{{id}}')
 if group in ['clientes','embarcacoes','vagas']:
  for act in ['inativar','ativar']:add(group+'-'+act,logical,act.capitalize()+' '+singular,'/'+group+' / ações da linha',refs,logical,template=tpl(group,singular+'-listagem'),notes='Alternância de situação; preserva histórico. Não é exclusão física.',api=f'PATCH /api/v1/{group}/{{id}}/status')
for action,label in [('encerrar','Encerrar ocupação')]:add('ocupacoes-'+action,'Ocupações',label,'/ocupacoes / ações da linha','Ocupações; Vagas; Embarcações','Ocupações',template=tpl('ocupacoes','ocupacao-listagem'),api='PATCH /api/v1/ocupacoes/{id}/encerramento')
for action in ['iniciar','concluir','cancelar']:add('movimentacoes-'+action,'Movimentações',action.capitalize()+' movimentação','/movimentacoes / ações da linha','Movimentações; Posições; Ocupações; Vagas; Embarcações','Movimentações; histórico; Posições/Ocupações conforme destino' if action=='concluir' else 'Movimentações; histórico',template=tpl('movimentacoes','movimentacao-listagem'),api='PATCH /api/v1/movimentacoes/{id}/'+dict(iniciar='inicio',concluir='conclusao',cancelar='cancelamento')[action])
add('movimentacoes-historico','Movimentações','Consultar eventos de uma movimentação','Sem tela implementada','Movimentações e seu histórico',api='GET /api/v1/movimentacoes/{id}/historico',notes='Controller existente; listagem não apresenta botão de histórico.')
add('posicao-primeira-consulta','Posições','Obter ou inicializar posição da embarcação','Exibida ao selecionar embarcação em movimentações','Embarcações; Ocupações; Posições','Posições na primeira consulta',api='GET /api/v1/embarcacoes/{id}/posicao',notes='Efeito colateral verificado: pode persistir posição inicial. Não considerar consulta sem manutenção.')
add('posicoes-listar','Posições','Consultar posições atuais','Sem tela própria; painel operacional','Posições; Embarcações; Vagas',api='GET /api/v1/posicoes-embarcacoes')
add('contratos-detalhe','Contratos','Consultar condições, vínculos e histórico do contrato','/contratos/:id','Contratos; Clientes; Embarcações; vínculos/histórico; Ocupações',template=tpl('contratos','contrato-detalhe'),api='GET /api/v1/contratos/{id}; /ocupacoes; /historico')
for action,label in [('enviar','Enviar para assinatura'),('ativar','Ativar contrato'),('suspender','Suspender contrato'),('reativar','Reativar contrato'),('solicitar-encerramento','Solicitar encerramento'),('encerrar','Encerrar contrato'),('cancelar','Cancelar contrato'),('vincular','Vincular ocupação'),('desvincular','Desvincular ocupação')]:
 add('contratos-'+action,'Contratos',label,'/contratos/:id / diálogo ou ação','Contratos; histórico; Ocupações; vínculos','Contratos e histórico; vínculos conforme ação',template=tpl('contratos','contrato-detalhe'),notes='Condição por status; botões de status no detalhe são condicionados por status, sem verificação explícita da permissão nessa classe; backend verifica ação.',api=('DELETE /api/v1/contratos/{id}/ocupacoes/{vinculoId}' if action=='desvincular' else 'POST /api/v1/contratos/{id}/'+('ocupacoes' if action=='vincular' else 'enviar-para-assinatura' if action=='enviar' else action)))
for id,label,route in [('dashboard','Dashboard operacional','/dashboard'),('painel-tv','Painel operacional TV','/painel-tv')]:add(id,'Operação',label,route,'Movimentações; Ocupações; Vagas; Embarcações; Posições',template=tpl(id,id),notes='Indicadores derivados candidatos; classificação pendente da equipe.')
add('checklist','Checklist','Conferir itens antes da saída','/checklist-saida',template=tpl('checklist-saida','checklist-saida'),notes='Protótipo local com retirada/embarcação fixas no template; não lê nem mantém dados de negócio.')
add('checklist-concluir','Checklist','Marcar itens e concluir conferência','/checklist-saida',template=tpl('checklist-saida','checklist-saida'),notes='Marcações perdidas ao recarregar; confirmação de liberação não persiste nem conclui movimentação.')
for id,obj,logical in [('ocupacao','Analisar ocupação instantânea','Vagas; Ocupações'),('movimentacoes','Analisar desempenho operacional','Movimentações'),('contratos','Analisar carteira de contratos','Contratos'),('permanencia-vagas','Analisar permanência histórica','Ocupações; Vagas')]:add('relatorio-'+id,'Relatórios',obj,'/relatorios',logical,template='frontend/src/app/features/relatorios/relatorios.component.html',api='GET /api/v1/relatorios/'+id)
for id,obj in [('meteorologia','Consultar previsão meteorológica'),('maritimo','Consultar condições marítimas')]:add('ambiente-'+id,'Ambiente',obj,'/ambiente',template='frontend/src/app/features/ambiente/ambiente.component.html',api='GET /api/v1/ambiente/'+id,notes='Capturas com upstream Open-Meteo controlado; nenhuma consulta real adicional nesta auditoria.')
for id,obj in [('paises','Pesquisar países'),('municipios','Pesquisar municípios brasileiros'),('feriados','Consultar calendário de feriados'),('mares','Consultar previsões de marés')]:add('referencias-'+id,'Referências',obj,'/referencias/'+id,template='frontend/src/app/features/referencias/referencias.component.html',api='GET /api/v1/referencias/'+id,notes='Reutiliza evidências de apf-aie; WorldTides desabilitada e teste controlado separados.')
for action in ['consultar','criar','alterar','inativar']:
 add('organizacoes-'+action,'Organizações',action.capitalize()+' organização','Sem tela implementada','Organizações','Organizações' if action!='consultar' else '',api={'consultar':'GET /api/v1/organizacoes','criar':'POST /api/v1/organizacoes','alterar':'PUT /api/v1/organizacoes/{id}','inativar':'DELETE /api/v1/organizacoes/{id}'}[action],notes='Somente Administrador da Plataforma; UCP não disponibilizado para verificar vínculo.')
for action in ['consultar','criar','alterar','inativar','ativar']:
 add('usuarios-'+action,'Usuários',action.capitalize()+' usuário','Sem tela implementada','Usuários; Organização','Usuários' if action!='consultar' else '',api={'consultar':'GET /api/v1/usuarios','criar':'POST /api/v1/usuarios','alterar':'PUT /api/v1/usuarios/{id}','inativar':'PATCH /api/v1/usuarios/{id}/status','ativar':'PATCH /api/v1/usuarios/{id}/status'}[action],notes='Ator segundo política de permissões; perfis Operador/Técnico não implementados. UCP indisponível.')
add('localizacao','Configurações','Consultar coordenadas da marina','/configuracoes/localizacao','Organizações',template='frontend/src/app/features/ambiente/localizacao.component.html',api='GET /api/v1/minha-organizacao/coordenadas')
add('localizacao-salvar','Configurações','Configurar coordenadas da marina','/configuracoes/localizacao','Organizações','Organizações',template='frontend/src/app/features/ambiente/localizacao.component.html',api='PUT /api/v1/minha-organizacao/coordenadas')
add('variabilidade','Configurações','Consultar módulos e permissões','/configuracoes/variabilidade','Disponibilidade de módulos; permissões por perfil',template=tpl('variabilidade','configuracao-variabilidade'))
add('liberar-modulos','Configurações','Liberar módulos comerciais pela Plataforma','/configuracoes/variabilidade','Organizações; Disponibilidade de módulos','Disponibilidade de módulos',template=tpl('variabilidade','configuracao-variabilidade'),api='PUT /api/v1/organizacoes/{organizacaoId}/modulos/{modulo}')
add('permissoes-salvar','Configurações','Gerenciar permissões internas pela Marina','/configuracoes/variabilidade','Disponibilidade de módulos; permissões por perfil','Permissões por perfil',template=tpl('variabilidade','configuracao-variabilidade'),api='PUT /api/v1/variabilidade/perfis/{perfil}/permissoes')
# Metadados brutos da fonte não são validações vistas no navegador.
metadata={}
for f in sorted(Path('frontend/src/app').rglob('*.component.html')):
 s=f.read_text();clean=lambda t:re.sub(r'\s+',' ',t).strip()
 metadata[str(f)]={'rotulosFonte':[clean(t) for t in re.findall(r'<mat-label[^>]*>(.*?)</mat-label>',s,re.S)],'validacoesFonteNaoPresumidasComoVistas':[clean(t) for t in re.findall(r'<mat-error[^>]*>(.*?)</mat-error>',s,re.S)],'acoes':[{'evento':e,'expressao':clean(v)} for e,v in re.findall(r'\((click|ngSubmit|selectionChange|change|ngModelChange)\)="([^"]+)"',s,re.S)],'condicoes':[clean(x) for x in re.findall(r'@if\s*\((.*?)\)\s*{',s,re.S)]}
(OUT/'rastreio-fontes.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
x=json.loads((OUT/'execucao-browser.json').read_text())
calc={
 'relatorio-ocupacao':'Taxa = ocupadas ativas / total de vagas ativas × 100; total zero → 0%; grupos por tipo/setor; disponíveis = ativas − ocupadas.',
 'relatorio-movimentacoes':'Duração = conclusão − início; média somente amostras completas e cronológicas; pontual = conclusão <= agendadaPara; percentual pontuais / concluídas × 100; zero concluídas → 0%.',
 'relatorio-contratos':'Contagens por status e periodicidade; percentual status/total × 100; soma valorBase por periodicidade separada; não receita recebida.',
 'relatorio-permanencia-vagas':'Somente ENCERRADAS por encerradaEm em datas inclusivas UTC; duração encerradaEm−inicioEm; média, mínimo, máximo em segundos; soma/3600 em horas; vazio: quantidade e soma zero, média/mín/máx nulos; tipo/setor atuais.',
 'dashboard':'Indicadores operacionais calculados pelo backend; examinar campos efetivamente renderizados e fórmulas em PainelOperacionalService.',
 'painel-tv':'Fila/indicadores operacionais calculados pelo backend; tempos relativos também apresentados pelo frontend; não equivale a listagem simples.'}
external={'paises':'IBGE Países — identificação M49, nome, ISO2/ISO3','municipios':'IBGE Localidades — município/código IBGE e UF/código/nome','feriados':'BrasilAPI — data/nome/tipo/UF/dia da semana conforme disponibilidade','mares':'WorldTides — dt/date/type/height/copyright/responseDatum (teste controlado); origem e m conforme contrato','meteorologia':'Open-Meteo Weather — time, temperature_2m, wind_speed_10m, wind_direction_10m, wind_gusts_10m, precipitation_probability, hourly_units','maritimo':'Open-Meteo Marine — time, wave_height, wave_direction, wave_period, swell_wave_height, swell_wave_direction, hourly_units'}
headers='id_provisorio modulo objetivo_do_usuario ator_perfil rota_ou_dialogo acao_do_usuario situacao campos_entrada_visiveis campos_condicionais parametros_indiretos conjunto_botoes_acao mensagens_erro_distintas mensagens_sucesso confirmacoes_modal campos_saida_visiveis campos_saida_derivados tabelas_listagens grupos_logicos_lidos grupos_logicos_alterados dados_externos_referenciados houve_escrita_no_banco arquivos_fisicos_consultados screenshot_entrada screenshot_saida screenshot_erro screenshot_mobile evidencias_teste requisito_UCP_candidato notas_ambiguidade sha_commit'.split()
rows=[]
for p in processos:
 id=p['id']; states=[z for z in x['cenarios'] if z['id']==id];effects=[e for e in x['efeitos'] if e['id']==id or e['id']==id+'-abrir' or e['id']==id.replace('-consultar','-pesquisar')];src=metadata.get(p['template'],{})
 aliases={'organizacoes-consultar':'organizacoes-apenas-api','usuarios-consultar':'usuarios-apenas-api','referencias-mares':'mares-desabilitada'}
 if not states and id in aliases:states=[z for z in x['cenarios'] if z['id']==aliases[id]]
 if id.startswith('referencias-'):
  g=id.split('-')[1]; reuse=list(Path('docs/evidencias/apf-aie').glob(g+'-*.png'))
 else:reuse=[]
 inputs=list(dict.fromkeys(c['rotulo'] for z in states for c in z['entradas'] if c['rotulo']))
 errors=list(dict.fromkeys(f"{m} [visto: {z['estado']}; {z['captura']}]" for z in states for m in z['erros']))
 buttons=list(dict.fromkeys((c['aria'] or c['texto']) for z in states for c in z['botoes']))
 modals=list(dict.fromkeys(m for z in states for m in z['dialogos']))
 changed=sorted(set(t for e in effects for t in e['tabelasAlteradas']))
 proof=('sim: '+', '.join(changed) if changed else 'não: hashes e quantidades idênticos')+'; execucao-browser.json/efeitos: '+', '.join(e['id'] for e in effects) if effects else 'indeterminado: sem comparação de banco específica nesta auditoria'
 def pics(pred):return '; '.join(z['captura'] for z in states if pred(z))
 r=dict.fromkeys(headers,'');r.update(id_provisorio=id,modulo=p['mod'],objetivo_do_usuario=p['obj'],ator_perfil='; '.join(sorted(set(z['perfil'] for z in states))) or ('ADMINISTRADOR_PLATAFORMA' if p['mod']=='Organizações' else 'Conforme política backend; sem sessão capturada específica'),rota_ou_dialogo=p['route'],acao_do_usuario=p['obj'],situacao='apenas API' if p['route'].startswith('Sem tela') else 'implementada',campos_entrada_visiveis='; '.join(inputs) or ('Sem tela implementada; parâmetros da API em '+p['api'] if p['route'].startswith('Sem tela') else 'Não observados nesta passagem; rótulos condicionais no rastreio-fontes.json'),campos_condicionais='; '.join(src.get('condicoes',[])),parametros_indiretos='Usuário/perfil e organização do JWT; configuração comercial e permissões. '+('Coordenadas da organização; configuração segura do provedor.' if p['mod'] in ['Ambiente','Referências'] else ''),conjunto_botoes_acao='; '.join(buttons) or 'Não observado; ações da fonte em rastreio-fontes.json',mensagens_erro_distintas='; '.join(errors) or 'Nenhuma mensagem efetivamente vista neste processo; catálogo da fonte separado',mensagens_sucesso='Ver texto renderizado dos estados de saída em execucao-browser.json; não presumir sucesso apenas por HTTP',confirmacoes_modal='; '.join(modals) or 'Não observada',campos_saida_visiveis='; '.join(sorted(set(c for z in states for c in z['colunas']))) or ('Sem tela' if p['route'].startswith('Sem tela') else 'Texto/indicadores renderizados no manifesto; verificar capturas e rastreio'),campos_saida_derivados=calc.get(id,'Nenhum cálculo derivado identificado no escopo deste processo'),tabelas_listagens='; '.join(sorted(set(c for z in states for c in z['colunas']))) or 'Não há cabeçalhos observados',grupos_logicos_lidos=p['group']+'; proposta funcional a conferir, não deduzida como quantidade de ALIs',grupos_logicos_alterados=p['write'] or 'Nenhum proposto; conferir prova de banco',dados_externos_referenciados=next((v for k,v in external.items() if id.endswith('-'+k)),''),houve_escrita_no_banco=proof,arquivos_fisicos_consultados='; '.join(changed) if changed else 'Ver hashes por tabela no manifesto; nomes físicos não definem ALI',screenshot_entrada=pics(lambda z:'entrada' in z['estado'] or z['estado']=='inicial'),screenshot_saida=pics(lambda z:z['estado'].startswith('saida')),screenshot_erro=pics(lambda z:bool(z['erros'])),screenshot_mobile=pics(lambda z:z['viewport']=='mobile'),evidencias_teste='execucao-browser.json; '+(p['template'] or p['api'])+'; '+p['api'],requisito_UCP_candidato='Pendente: UCP e slides não fornecidos no repositório; sem vínculo verificável',notas_ambiguidade=p['notes']+' '+('Sem execução visual específica; implementação inspecionada estaticamente.' if not states else '')+' '+('Campos/fórmulas da tela compartilhada podem depender da seleção; não unir DETs automaticamente.' if p['mod'] in ['Relatórios','Referências','Ambiente'] else ''),sha_commit=sha)
 if reuse:
  r['screenshot_entrada']='; '.join('../apf-aie/'+f.name for f in reuse if 'resultado.png' in f.name)
  r['screenshot_saida']='; '.join('../apf-aie/'+f.name for f in reuse if 'resultado-completo' in f.name)
  r['screenshot_mobile']='; '.join('../apf-aie/'+f.name for f in reuse if 'mobile' in f.name)
  r['evidencias_teste']+='; ../apf-aie/README.md; ../apf-aie/consultas-reais.json'
 outputs={
 'dashboard':'Em execução; Atrasadas; Próxima hora; Urgentes abertas; Sem operador; Concluídas hoje; filas por janela de tempo e concluídas recentemente; embarcação/proprietário, origem/destino, horário, prioridade, operador e ações nos cartões.',
 'painel-tv':'Descidas para água; Retiradas da água; Transferências de vaga; Deslocamentos internos; Em execução; Alertas; cartões com embarcação, operação, prioridade, origem/destino, operador e tempo relativo; atualização e relógio.',
 'relatorio-ocupacao':'Total; Ocupadas; Disponíveis; Taxa de ocupação (%); Setor; Tipo; Ativas; Ocupadas; Disponíveis; Ocupação (%).',
 'relatorio-movimentacoes':'Total; Concluídas; Canceladas; Tempo médio (segundos); Amostras de duração; No prazo (quantidade e %); regra de pontualidade; Situação; Quantidade.',
 'relatorio-contratos':'Total de contratos; Situação; Quantidade; Percentual (%); Periodicidade; Quantidade; Valor-base (R$); aviso de que valores não são pagamentos ou faturamento realizado.',
 'relatorio-permanencia-vagas':'Ocupações encerradas; Permanência média (segundos); Menor permanência (segundos); Maior permanência (segundos); Total de horas de ocupação; Setor; Tipo; quantidade/média/mínimo/máximo/horas por grupo; sem dados no vazio.'}
 sucessos_vistos={'clientes-cadastrar-pf':'Cliente cadastrado com sucesso.','embarcacoes-cadastrar':'Embarcação cadastrada com sucesso.','vagas-cadastrar':'Vaga cadastrada com sucesso.','ocupacoes-cadastrar':'Ocupação registrada com sucesso.','movimentacoes-concluir':'Movimentação concluída com sucesso.'}
 if id in sucessos_vistos:r['mensagens_sucesso']=sucessos_vistos[id]+' [texto inspecionado na captura de saída desktop]'
 if id in outputs:r['campos_saida_visiveis']=outputs[id]
 if id=='dashboard':r['campos_saida_derivados']='Contagem de abertas em execução/atrasadas; agendadas entre agora e agora+1h; abertas urgentes e sem operador; concluídas no dia local; agrupamentos até 30min/2h/restante do dia e recentes.'
 if id=='painel-tv':r['campos_saida_derivados']='Contagem por tipo e alertas; atraso/tempo até início/duração em execução em minutos arredondados para cima: (segundos+59)/60; próximo até 15min; critérios de alerta do PainelTvOperacionalService.'
 rows.append(r)
with (OUT/'processos-funcionais.csv').open('w',newline='') as f:
 w=csv.DictWriter(f,fieldnames=headers,lineterminator="\n");w.writeheader();w.writerows(rows)
md=['# Processos funcionais observados — inventário técnico','',f'SHA de produção inspecionado: `{sha}`. {len(rows)} operações/estados funcionais candidatos, sem classificação ou pontuação APF definitiva. Estados de impedimento e checklist não constituem automaticamente processos contáveis.','', 'Campos vistos são obtidos do DOM real; mensagens do código que não foram vistas ficam somente em `rastreio-fontes.json`. Evidência de banco significa comparação imediatamente antes/depois, não mera ausência de chamadas POST. A execução do roteiro não implica que todo cenário foi aprovado; conferir falhas no manifesto.','']
for r in rows:
 md += ['## '+r['id_provisorio']+' — '+r['objetivo_do_usuario'],'']
 for k,v in r.items():
  if k not in ['id_provisorio','objetivo_do_usuario']:md+=['- **'+k+'**: '+str(v).replace('\n',' ')]
 md+=['']
(OUT/'processos-funcionais.md').write_text('\n'.join(md)+'\n')
# Cobertura de cada rota e de cada binding, incluindo botões internos sem URL própria.
coverage=[]
for p in processos:
 rs=[r for r in rows if r['id_provisorio']==p['id']][0]
 ss=[z for z in x['cenarios'] if z['id']==p['id']]
 status='sem tela' if p['route'].startswith('Sem tela') else 'falhou' if any(f['id']==p['id'] and not f.get('resolvida') for f in x['falhas']) else 'coberta' if ss or any(e['id']==p['id'] for e in x['efeitos']) or p['id'].startswith('referencias-') else 'falhou'
 coverage.append(dict(rota=p['route'],acao=p['obj'],processo=p['id'],estado=status,evidencias=rs['screenshot_entrada'] or rs['screenshot_saida'] or rs['evidencias_teste'],observacao='Reutilizada em apf-aie' if p['id'].startswith('referencias-') else 'Sem execução visual específica' if not ss else 'Conferir estados e falhas; cobertura de rota não prova todas as ações'))
for path,meta in metadata.items():
 for a in meta['acoes']:
  matches=[p for p in processos if p['template']==path]
  if not matches:
   if 'dialogo-acao-contrato' in path:matches=[p for p in processos if p['id'].startswith('contratos-')]
   elif 'cancelamento-movimentacao' in path:matches=[p for p in processos if p['id']=='movimentacoes-cancelar']
   elif '/layout/' in path:matches=[p for p in processos if p['id']=='sessao']
   elif '/cartao-movimentacao-operacional/' in path:matches=[p for p in processos if p['id']=='dashboard']
   elif '/dialogo-confirmacao/' in path:matches=[p for p in processos if p['id'] in ['movimentacoes-iniciar','movimentacoes-concluir','ocupacoes-encerrar']]
   if not matches:continue
  expr=a['expressao'];target=[]
  # Mapeamento explícito de controles exercitados; nunca atribui classe APF.
  grupo=matches[0]['id'].split('-')[0]
  if expr.startswith('salvar()'):target=[p['id'] for p in matches if any(t in p['id'] for t in ['cadastrar','alterar','salvar'])]
  elif expr.startswith('pesquisar()'):target=[grupo+'-pesquisar']
  elif 'solicitarAlteracaoStatus' in expr:target=[grupo+'-ativar',grupo+'-inativar']
  elif expr.startswith('consultar()'):target=[p['id'] for p in matches]
  elif 'solicitarInicio(' in expr:target=['movimentacoes-iniciar'] if grupo=='movimentacoes' else []
  elif 'solicitarConclusao(' in expr:target=['movimentacoes-concluir'] if grupo=='movimentacoes' else []
  elif 'solicitarEncerramento(' in expr:target=['ocupacoes-encerrar']
  elif 'solicitarCancelamento(' in expr:target=[grupo+'-cancelar']
  elif expr.startswith('solicitarEnvio'):target=['contratos-enviar']
  elif expr.startswith('solicitarAtivacao'):target=['contratos-ativar']
  elif expr.startswith('solicitarSuspensao'):target=['contratos-suspender']
  elif expr.startswith('solicitarReativacao'):target=['contratos-reativar']
  elif expr.startswith('solicitarInicioEncerramento'):target=['contratos-solicitar-encerramento']
  elif expr.startswith('solicitarConclusaoEncerramento'):target=['contratos-encerrar']
  elif expr.startswith('vincularOcupacao'):target=['contratos-vincular']
  elif expr.startswith('solicitarDesvinculo'):target=['contratos-desvincular']
  elif expr.startswith('alterarModulo'):target=['liberar-Condições ambientais','liberar-Relatórios','liberar-Referências externas']
  elif expr.startswith('salvarPermissoes'):target=['permissoes-salvar']
  elif expr.startswith('concluir()') or expr.startswith('alterarItem('):target=['checklist-concluir']
  proved=[e for e in x['efeitos'] if e['id'] in target]
  direct=[z for z in x.get('acoes',[]) if z.get('executada') and '/features/'+z['modulo']+'/' in path and z['expressao']==expr]
  contexto=('referencias/referencias.component' in path and expr=='consultar()') or ('cancelamento-movimentacao-dialog' in path and expr=='confirmar()') or ('relatorios/relatorios.component' in path and expr=='limpar()') or ('ambiente/ambiente.component' in path and expr=='resultado.set(null)') or ('configuracao-variabilidade' in path and (expr.startswith('selecionarPerfil') or expr.startswith('alternarPermissao') or 'organizacaoId =' in expr)) or ('/login/' in path and expr=='entrar()') or ('/layout/' in path and expr in ['sair()','menu.toggle()','telaPequena() && menu.close()']) or ('dialogo' in path and expr=='confirmar()')
  estado='coberta' if proved or direct or contexto else 'não aplicável' if expr.startswith(('paginaAnterior','proximaPagina')) else 'falhou'
  obs=('Controle exercitado na passagem complementar: execucao-browser.json/acoes' if direct else 'Controle exercitado na jornada registrada (seleção, autenticação, menu ou confirmação); evidência no manifesto e capturas.' if contexto else 'Execução rastreada nos efeitos: '+', '.join(e['id'] for e in proved) if proved else 'Paginação desabilitada: conjunto de demonstração cabe em uma página; não testado avanço entre páginas.' if estado=='não aplicável' else 'Controle identificado na fonte; execução individual não comprovada neste levantamento. Não é falha de negócio presumida.')
  coverage.append(dict(rota=matches[0]['route'],acao=a['evento']+': '+expr,processo='; '.join(p['id'] for p in matches),estado=estado,evidencias='execucao-browser.json' if proved else path,observacao=obs))
for route,obs in [('/','Redireciona para dashboard'),('/referencias','Redireciona para /referencias/paises'),('/**','Redireciona rota desconhecida para dashboard')]:coverage.append(dict(rota=route,acao='Redirecionar',processo='',estado='não aplicável',evidencias='frontend/src/app/app.routes.ts',observacao=obs))
with (OUT/'rotas-cobertura.csv').open('w',newline='') as f:
 w=csv.DictWriter(f,fieldnames=['rota','acao','processo','estado','evidencias','observacao'],lineterminator="\n");w.writeheader();w.writerows(coverage)
# Índice com uma linha por imagem e sem réplicas desnecessárias.
md=['# Auditoria visual funcional do Caisora','',f'SHA inspecionado: `{sha}`. Branch atual: main. Código de produção preservado; os novos arquivos são exclusivamente documentação e instrumentos de teste. Alterações anteriores em frontend/angular.json e docs/padroes-de-projeto.md não pertencem à auditoria.','', '## Documentos','', '- [Inventário CSV](processos-funcionais.csv) e [versão detalhada](processos-funcionais.md).','- [Cobertura de rotas e ações](rotas-cobertura.csv).','- [Grupos de dados candidatos](grupos-de-dados.md).','- [Lacunas e divergências](lacunas-e-divergencias.md).','- [Cálculos observados nas seis saídas candidatas](calculos-observados.md).','- [Mensagens efetivamente vistas, deduplicadas](mensagens-observadas.json).','- [Manifesto do navegador e comparação de banco](execucao-browser.json).','- [Rastreio estático de campos, condições e ações](rastreio-fontes.json).','', '## Método e reprodução','', 'Aplicação Angular de produção, API Spring Boot real, autenticação JWT real, PostgreSQL descartável Testcontainers. Dados exclusivamente sintéticos criados por código em src/test. Observador HTTP de hashes existe apenas no teste, ligado a 127.0.0.1, encerrado no finally. Nenhum token, senha real ou linha de banco é retornado pelo observador. O manifesto contém somente hashes/quantidades e campos públicos de teste. A fixture de histórico é reaproveitada; data corrente influencia transições realizadas agora.','', '```bash','npm --prefix frontend run build','CAISORA_APF_VISUAL=true PLAYWRIGHT_BROWSERS_PATH=/tmp/caisora-browsers \\','  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \\','  ./mvnw -Dtest=AuditoriaVisualApfTest test','CAISORA_APF_VISUAL=true CAISORA_APF_COMPLEMENTAR=true PLAYWRIGHT_BROWSERS_PATH=/tmp/caisora-browsers \\', '  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -Dtest=AuditoriaVisualApfTest test', '# Passagens adicionais: manter as variáveis acima e acrescentar CAISORA_APF_SESSAO=true ou CAISORA_APF_CONTROLES=true.', 'python3 scripts/gerar-inventario-apf.py','```','', 'Requer Docker, JDK21 e Playwright temporário instalado conforme ../apf-aie/README.md. Roteiro aceita somente host 127.0.0.1. Não consome serviços públicos nem WorldTides. WorldTides permanece desabilitada. Teste JUnit verifica execução do coletor; não declara todos os cenários aprovados. Falhas de seletores ou regras ficam explicitadas no manifesto.','', 'Desktop base 1440×1100; mobile emulado em Chromium, base 390×844. Para contornar scroll interno do layout, altura ampliada preservando largura; dimensão exata por captura no manifesto. Imagens espelhadas durante ações desktop apenas alteram viewport, sem afirmar aparelho físico/touch. A passagem mobile separada usa isMobile e hasTouch.','', '## Capturas novas realmente obtidas no Chromium','',f'Data UTC da execução: {x["data"]}; Chromium {x["navegador"]}. Todas abaixo usam dados fictícios de teste; Open-Meteo é resposta controlada upstream local. Não apresentam condições ambientais reais. SHA em todas: `{sha}`. Fonte: `AuditoriaVisualApfTest` + `scripts/auditar-apf-browser.mjs`.','', '| Processo / estado | Perfil | Viewport base → captura | Imagem real do navegador |','|---|---|---|---|']
for z in x['cenarios']:md.append(f'| {z["id"]} / {z["estado"]} | {z["perfil"]} | {z["largura"]}×{z["alturaBase"]} → {z["largura"]}×{z["alturaCaptura"]} | [{z["viewport"]}]({z["captura"]}) |')
md+=['','## Reutilização identificada','', '61 imagens anteriores: 15 em [SE06](../se06/README.md), 46 em [AIE03–AIE06](../apf-aie/README.md). São artefatos herdados, presentes no SHA inspecionado. As pastas antigas não registram inequivocamente o SHA de captura original; não atribuir o SHA atual como data de criação dessas imagens. A adequação é sustentada pelos scripts/fontes presentes e pela ausência de alterações de produção nesta auditoria. Essa limitação de proveniência fica registrada.','', 'SE06: resultado/vazio com API e banco reais de teste; loading/erro induzidos. AIE: telas com upstream controlado; consulta real IBGE/BrasilAPI em consultas-reais.json, separada dos screenshots; sondagem real posterior indisponível preservada. WorldTides nunca consultada de verdade. Não recriamos essas imagens e não afirmamos consulta real Open-Meteo.','',f'Novas capturas indexadas: {len(x["cenarios"])}. Processos/estados candidatos no inventário: {len(rows)}. Linhas de cobertura (inclui bindings e redirecionamentos, não quantidade de funções APF): {len(coverage)}.','', '## Testes/build','', 'Ver resultado-validacao.json, produzido a partir dos relatórios efetivos. Ausência de execução não equivale a aprovação. A classificação, agrupamentos e contagem acadêmica dependem da equipe e dos slides não fornecidos.']
(OUT/'README.md').write_text('\n'.join(md)+'\n')
print('Processos',len(rows),'capturas',len(x['cenarios']),'cobertura',len(coverage))

mensagens={}
for z in x['cenarios']:
 for msg in z['erros']:
  mensagens.setdefault(msg,[]).append({'processo':z['id'],'gatilho_estado':z['estado'],'captura':z['captura'],'perfil':z['perfil']})
(OUT/'mensagens-observadas.json').write_text(json.dumps(mensagens,ensure_ascii=False,indent=2)+'\n')

if final_anterior:
 with readme_path.open('a') as f:f.write('\n## Conferência final'+final_anterior)

# Padroniza finais de linha e espaços finais dos artefatos textuais gerados.
for artefato in OUT.iterdir():
 if artefato.suffix in {".md", ".csv"}:
  artefato.write_text("\n".join(linha.rstrip() for linha in artefato.read_text().splitlines())+"\n")
