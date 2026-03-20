// Nav
let curScreen='',curGroup='',curGroupKey='',curGroupType='',hist=[];
const TAB_SCREENS=['home','groups-tab','analytics-tab','profile'];
function show(id,push=true){
    const isTab=TAB_SCREENS.includes(id);
    const nav=document.getElementById('bottom-nav');
    if(isTab){
        nav.classList.remove('hidden');
        document.querySelectorAll('.screen').forEach(s=>s.classList.remove('active'));
        document.getElementById('s-'+id).classList.add('active');
        curScreen=id;
    }else{
        nav.classList.add('hidden');
        if(curScreen&&push)hist.push(curScreen);
        document.querySelectorAll('.screen').forEach(s=>s.classList.remove('active'));
        document.getElementById('s-'+id).classList.add('active');
        curScreen=id;
    }
    window.scrollTo(0,0);
}
function goBack(){const p=hist.pop();if(p){if(TAB_SCREENS.includes(p)){switchNavTab(p)}else{show(p,false)}}}
function goHome(){stopSync();hist=[];switchNavTab('home')}
function toast(m){const t=document.getElementById('toast');t.textContent=m;t.classList.add('show');setTimeout(()=>t.classList.remove('show'),2500)}
function switchNavTab(tabId){
    // Update active tab styling
    document.querySelectorAll('#bottom-nav .nav-tab').forEach(t=>t.classList.toggle('active',t.dataset.tab===tabId));
    // Show the corresponding screen
    show(tabId,false);
    // Call the render function
    if(tabId==='home')renderHome();
    else if(tabId==='groups-tab')renderGroupsTab();
    else if(tabId==='analytics-tab')renderAnalyticsTab();
    else if(tabId==='profile')renderProfile();
}
function switchGroupTab(tab){expSearchQuery='';expTagFilter=null;expDateFilter='all';document.querySelectorAll('.tab').forEach(t=>t.classList.toggle('active',t.dataset.tab===tab));document.getElementById('t-exp').style.display=tab==='exp'?'':'none';document.getElementById('t-bal').style.display=tab==='bal'?'':'none';document.getElementById('t-mem').style.display=tab==='members'?'':'none';document.getElementById('t-analytics').style.display=tab==='analytics'?'':'none';document.getElementById('fab-ae').style.display=tab==='exp'?'':'none';if(tab==='exp')renderExpenses();else if(tab==='bal')renderBalances();else if(tab==='analytics')renderGroupAnalytics();else renderMembers()}

// Modal
let expenseLookup={}, settlementLookup={}, editingExpense=null;
function openModal(html){document.getElementById('modal-body').innerHTML=html;document.getElementById('modal').style.display=''}
function closeModal(){document.getElementById('modal').style.display='none'}
