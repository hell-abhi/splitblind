// Render
async function renderGroups(){
    const groups=(await iA('groups')).sort((a,b)=>b.createdAt-a.createdAt);
    const el=document.getElementById('grp-list');
    if(!groups.length){el.innerHTML='<div class="empty"><div class="ic">&#x1F465;</div><h3>No groups yet</h3><p>Tap + to create your first split group</p></div>';return}
    const allM=await iA('members');
    const allExps=(await iA('expenses')).filter(e=>!e.isDeleted);
    const allSets=(await iA('settlements')).filter(s=>!s.isDeleted);

    // Compute overall balances across all groups
    let totalOwed=0,totalOwe=0;
    const groupBalances={};
    for(const g of groups){
        const gExps=allExps.filter(e=>e.groupId===g.groupId);
        const gSets=allSets.filter(s=>s.groupId===g.groupId);
        const bal=calcBal(gExps,gSets);
        const myBal=bal[myId]||0;
        groupBalances[g.groupId]=myBal;
        if(myBal>0)totalOwed+=myBal;
        else if(myBal<0)totalOwe+=Math.abs(myBal);
    }
    const net=totalOwed-totalOwe;

    // Summary card
    let html=`<div class="dash-summary">
        <h2>Your Overview</h2>
        <div class="dash-summary-row">
            <div class="dash-summary-item ds-owed"><div class="ds-amount">${fmt(totalOwed)}</div><div class="ds-label">You are owed</div></div>
            <div class="dash-divider"></div>
            <div class="dash-summary-item ds-owe"><div class="ds-amount">${fmt(totalOwe)}</div><div class="ds-label">You owe</div></div>
            <div class="dash-divider"></div>
            <div class="dash-summary-item ds-net"><div class="ds-amount">${net>=0?'+':''}${fmt(net)}</div><div class="ds-label">Net</div></div>
        </div>
    </div>`;

    // Analytics button
    html+=`<button class="analytics-btn" id="dash-analytics-btn">&#x1F4CA; Spending Analytics</button>`;

    // Personal Tracker summary
    const personalGroup=groups.find(g=>g.groupType==='personal'||g.isPersonal);
    if(personalGroup){
        const pgExps=allExps.filter(e=>e.groupId===personalGroup.groupId);
        const now=new Date();const monthStart=new Date(now.getFullYear(),now.getMonth(),1).getTime();
        const thisMonthTotal=pgExps.filter(e=>e.createdAt>=monthStart).reduce((s,e)=>s+e.amountCents,0);
        html+=`<div class="personal-tracker-card" data-g="${personalGroup.groupId}"><div class="pt-header"><div class="pt-icon">&#x1F4DD;</div><div><div class="pt-title">Personal Tracker</div><div class="pt-subtitle">This month's spending</div></div></div><div class="pt-amount">${fmt(thisMonthTotal)}</div></div>`;
    }

    // Recent activity feed (last 5 expenses across all groups)
    const recentExps=allExps.sort((a,b)=>b.createdAt-a.createdAt).slice(0,5);
    if(recentExps.length){
        // Build name map for all members
        const allMembersArr=await iA('members');
        const globalNm={};allMembersArr.forEach(m=>globalNm[m.memberId]=m.displayName);
        const groupNameMap={};groups.forEach(g=>groupNameMap[g.groupId]=g.name);

        html+=`<div class="dash-activity"><div class="dash-activity-header"><h3>Recent Activity</h3></div>`;
        recentExps.forEach(e=>{
            const ts=getTagStyle(e.tag);const st=ts?{icon:ts.icon,bg:ts.bg}:getExpStyle(e.description);
            const gName=groupNameMap[e.groupId]||'';
            const payer=globalNm[e.paidBy]||e.paidBy.slice(0,8);
            html+=`<div class="dash-activity-item" data-g="${e.groupId}"><div class="dai-icon" style="background:${st.bg}">${st.icon}</div><div class="dai-info"><div class="dai-desc">${esc(e.description)}</div><div class="dai-meta">${esc(payer)} &middot; ${esc(gName)} &middot; ${new Date(e.createdAt).toLocaleDateString('en',{month:'short',day:'numeric'})}</div></div><div class="dai-amount">${fmt(e.amountCents)}</div></div>`;
        });
        html+=`</div>`;
    }

    // Group cards
    html+=`<div class="dash-activity-header"><h3>Your Groups</h3></div>`;
    html+=groups.map((g,i)=>{
        const c=allM.filter(m=>m.groupId===g.groupId&&!m.isDeleted).length;
        const myBal=groupBalances[g.groupId]||0;
        let balText,balClass;
        if(myBal>0){balText='You are owed '+fmt(myBal);balClass='bp'}
        else if(myBal<0){balText='You owe '+fmt(Math.abs(myBal));balClass='bn'}
        else{balText='Settled up';balClass='settled'}
        return`<div class="dash-group-card" data-g="${g.groupId}"><div class="group-icon gi-${(i%5)+1}">&#x1F46B;</div><div style="flex:1"><div class="card-t">${esc(g.name)}</div><div class="card-s">${c} member${c!==1?'s':''} &middot; ${new Date(g.createdAt).toLocaleDateString()}</div><div class="dgc-balance ${balClass}">${balText}</div></div></div>`
    }).join('');

    el.innerHTML=html;
    el.querySelectorAll('[data-g]').forEach(c=>c.addEventListener('click',()=>openGroup(c.dataset.g)));
    const analyticsBtn=document.getElementById('dash-analytics-btn');
    if(analyticsBtn)analyticsBtn.addEventListener('click',()=>{show('analytics');renderAnalytics()});
}

// Home tab render
let homeSearchQuery='';
async function renderHome(){
    const groups=(await iA('groups')).sort((a,b)=>b.createdAt-a.createdAt);
    const allExps=(await iA('expenses')).filter(e=>!e.isDeleted);
    const allSets=(await iA('settlements')).filter(s=>!s.isDeleted);
    const el=document.getElementById('home-content');

    const allMembersArr=await iA('members');
    const globalNm={};allMembersArr.forEach(m=>globalNm[m.memberId]=m.displayName);
    const groupNameMap={};groups.forEach(g=>groupNameMap[g.groupId]=g.name);
    const allM=allMembersArr;

    // Search bar always at top
    let html=`<div class="fg" style="margin-bottom:12px">
        <input type="text" id="home-search" placeholder="Search groups, expenses..." value="${esc(homeSearchQuery)}" style="width:100%;padding:12px 16px;border-radius:14px;border:1.5px solid var(--border);background:var(--card);font-size:14px;font-family:Inter,sans-serif;color:var(--text);outline:none;transition:border .2s">
    </div>`;

    // If searching, show search results instead of normal home content
    if(homeSearchQuery.trim()){
        const q=homeSearchQuery.trim().toLowerCase();
        const matchedGroups=groups.filter(g=>g.groupType!=='personal'&&!g.isPersonal&&g.groupType!=='iou'&&!g.isIOU&&g.name.toLowerCase().includes(q));
        const matchedExps=allExps.filter(e=>e.description.toLowerCase().includes(q));

        if(matchedGroups.length){
            html+=`<div class="sec-t" style="margin-top:8px">Groups</div>`;
            matchedGroups.forEach((g,i)=>{
                const mc=allM.filter(m=>m.groupId===g.groupId&&!m.isDeleted).length;
                const gExps=allExps.filter(e=>e.groupId===g.groupId);
                const gSets=allSets.filter(s=>s.groupId===g.groupId);
                const bal=calcBal(gExps,gSets);
                const myBal=bal[myId]||0;
                const balText=myBal>0?'<span style="color:var(--positive)">+'+fmt(myBal)+'</span>':myBal<0?'<span style="color:var(--negative)">'+fmt(myBal)+'</span>':'<span style="color:var(--text-secondary)">Settled</span>';
                html+=`<div class="card group-card" data-g="${g.groupId}" style="cursor:pointer"><div class="group-icon gi-${(i%5)+1}">&#x1F46B;</div><div style="flex:1"><div class="card-t">${esc(g.name)}</div><div class="card-s">${mc} member${mc!==1?'s':''}</div></div><div style="text-align:right;font-weight:700;font-size:14px">${balText}</div></div>`;
            });
        }

        if(matchedExps.length){
            html+=`<div class="sec-t" style="margin-top:16px">Transactions</div>`;
            matchedExps.sort((a,b)=>b.createdAt-a.createdAt).slice(0,20).forEach(e=>{
                const ts=getTagStyle(e.tag);const st=ts?{icon:ts.icon,bg:ts.bg}:getExpStyle(e.description);
                const gName=groupNameMap[e.groupId]||'';
                const payer=e.paidBy===myId?'You':(globalNm[e.paidBy]||e.paidBy.slice(0,8));
                html+=`<div class="card exp-c" data-g="${e.groupId}" style="cursor:pointer"><div class="exp-left"><div class="exp-icon" style="background:${st.bg}">${st.icon}</div><div><div class="card-t">${esc(e.description)}</div><div class="card-s">${esc(payer)} &middot; ${esc(gName)} &middot; ${new Date(e.createdAt).toLocaleDateString('en',{month:'short',day:'numeric'})}</div></div></div><div><div class="exp-a">${fmt(e.amountCents)}</div></div></div>`;
            });
        }

        if(!matchedGroups.length&&!matchedExps.length){
            html+=`<div class="empty" style="margin-top:32px"><div class="ic">&#x1F50D;</div><h3>No results</h3><p>Nothing matched "${esc(homeSearchQuery)}"</p></div>`;
        }

        el.innerHTML=html;
        el.querySelectorAll('[data-g]').forEach(c=>c.addEventListener('click',()=>openGroup(c.dataset.g)));
        const searchInput=document.getElementById('home-search');
        if(searchInput){
            searchInput.addEventListener('input',(ev)=>{homeSearchQuery=ev.target.value;renderHome()});
            searchInput.focus();searchInput.setSelectionRange(searchInput.value.length,searchInput.value.length);
        }
        return;
    }

    // Compute overall balances
    let totalOwed=0,totalOwe=0;
    for(const g of groups){
        const gExps=allExps.filter(e=>e.groupId===g.groupId);
        const gSets=allSets.filter(s=>s.groupId===g.groupId);
        const bal=calcBal(gExps,gSets);
        const myBal=bal[myId]||0;
        if(myBal>0)totalOwed+=myBal;
        else if(myBal<0)totalOwe+=Math.abs(myBal);
    }
    const net=totalOwed-totalOwe;

    // Summary card
    html+=`<div class="dash-summary">
        <h2>Your Overview</h2>
        <div class="dash-summary-row">
            <div class="dash-summary-item ds-owed"><div class="ds-amount">${fmt(totalOwed)}</div><div class="ds-label">You are owed</div></div>
            <div class="dash-divider"></div>
            <div class="dash-summary-item ds-owe"><div class="ds-amount">${fmt(totalOwe)}</div><div class="ds-label">You owe</div></div>
            <div class="dash-divider"></div>
            <div class="dash-summary-item ds-net"><div class="ds-amount">${net>=0?'+':''}${fmt(net)}</div><div class="ds-label">Net</div></div>
        </div>
    </div>`;

    // Personal Tracker summary
    const personalGroup=groups.find(g=>g.groupType==='personal'||g.isPersonal);
    if(personalGroup){
        const pgExps=allExps.filter(e=>e.groupId===personalGroup.groupId);
        const now=new Date();const monthStart=new Date(now.getFullYear(),now.getMonth(),1).getTime();
        const thisMonthTotal=pgExps.filter(e=>e.createdAt>=monthStart).reduce((s,e)=>s+e.amountCents,0);
        html+=`<div class="personal-tracker-card" id="home-personal-tracker" data-g="${personalGroup.groupId}"><div class="pt-header"><div class="pt-icon">&#x1F4DD;</div><div><div class="pt-title">Personal Tracker</div><div class="pt-subtitle">This month's spending</div></div></div><div class="pt-amount">${fmt(thisMonthTotal)}</div></div>`;
    }else{
        html+=`<div class="personal-tracker-card pt-empty" id="home-personal-create"><div class="pt-header"><div class="pt-icon">&#x1F4DD;</div><div><div class="pt-title">Personal Tracker</div><div class="pt-subtitle">Track your personal expenses</div></div></div><button class="pt-create-btn" id="home-pt-create-btn">Start Tracking</button></div>`;
    }

    // Latest Groups (3 most recent non-personal, non-IOU)
    const recentGroups=groups.filter(g=>g.groupType!=='personal'&&!g.isPersonal&&g.groupType!=='iou'&&!g.isIOU).slice(0,3);
    if(recentGroups.length){
        html+=`<div class="sec-t" style="margin-top:20px">Latest Groups</div>`;
        recentGroups.forEach((g,i)=>{
            const mc=allM.filter(m=>m.groupId===g.groupId&&!m.isDeleted).length;
            const gExps=allExps.filter(e=>e.groupId===g.groupId);
            const gSets=allSets.filter(s=>s.groupId===g.groupId);
            const bal=calcBal(gExps,gSets);
            const myBal=bal[myId]||0;
            const balText=myBal>0?'<span style="color:var(--positive)">+'+fmt(myBal)+'</span>':myBal<0?'<span style="color:var(--negative)">'+fmt(myBal)+'</span>':'<span style="color:var(--text-secondary)">Settled</span>';
            html+=`<div class="card group-card" data-g="${g.groupId}" style="cursor:pointer"><div class="group-icon gi-${(i%5)+1}">&#x1F46B;</div><div style="flex:1"><div class="card-t">${esc(g.name)}</div><div class="card-s">${mc} member${mc!==1?'s':''}</div></div><div style="text-align:right;font-weight:700;font-size:14px">${balText}</div></div>`;
        });
    }

    // Latest Transactions (3 most recent expenses across all groups)
    const recentExps=allExps.sort((a,b)=>b.createdAt-a.createdAt).slice(0,3);
    if(recentExps.length){
        html+=`<div class="sec-t" style="margin-top:20px">Latest Transactions</div>`;
        recentExps.forEach(e=>{
            const ts=getTagStyle(e.tag);const st=ts?{icon:ts.icon,bg:ts.bg}:getExpStyle(e.description);
            const gName=groupNameMap[e.groupId]||'';
            const payer=e.paidBy===myId?'You':(globalNm[e.paidBy]||e.paidBy.slice(0,8));
            html+=`<div class="card exp-c" data-g="${e.groupId}" style="cursor:pointer"><div class="exp-left"><div class="exp-icon" style="background:${st.bg}">${st.icon}</div><div><div class="card-t">${esc(e.description)}</div><div class="card-s">${esc(payer)} &middot; ${esc(gName)} &middot; ${new Date(e.createdAt).toLocaleDateString('en',{month:'short',day:'numeric'})}</div></div></div><div><div class="exp-a">${fmt(e.amountCents)}</div></div></div>`;
        });
    }

    if(!groups.length&&!recentExps.length){
        html+='<div class="empty"><div class="ic">&#x1F44B;</div><h3>Welcome to SplitBlind</h3><p>Create a group or log a debt to get started</p></div>';
    }

    el.innerHTML=html;
    el.querySelectorAll('[data-g]').forEach(c=>c.addEventListener('click',()=>openGroup(c.dataset.g)));
    const homePtBtn=document.getElementById('home-pt-create-btn');
    if(homePtBtn)homePtBtn.addEventListener('click',async(e)=>{e.stopPropagation();await createPersonalGroup();renderHome()});
    const homeSearchInput=document.getElementById('home-search');
    if(homeSearchInput)homeSearchInput.addEventListener('input',(ev)=>{homeSearchQuery=ev.target.value;renderHome()});
}

// Groups tab render
async function renderGroupsTab(){
    const groups=(await iA('groups')).sort((a,b)=>b.createdAt-a.createdAt);
    const el=document.getElementById('groups-tab-list');
    const allM=await iA('members');
    const allExps=(await iA('expenses')).filter(e=>!e.isDeleted);
    const allSets=(await iA('settlements')).filter(s=>!s.isDeleted);

    const groupBalances={};
    for(const g of groups){
        const gExps=allExps.filter(e=>e.groupId===g.groupId);
        const gSets=allSets.filter(s=>s.groupId===g.groupId);
        const bal=calcBal(gExps,gSets);
        groupBalances[g.groupId]=bal[myId]||0;
    }

    let html='';

    // Personal Tracker card at top
    const personalGroup=groups.find(g=>g.groupType==='personal'||g.isPersonal);
    if(personalGroup){
        const pgExps=allExps.filter(e=>e.groupId===personalGroup.groupId);
        const now=new Date();const monthStart=new Date(now.getFullYear(),now.getMonth(),1).getTime();
        const thisMonthTotal=pgExps.filter(e=>e.createdAt>=monthStart).reduce((s,e)=>s+e.amountCents,0);
        const totalAll=pgExps.reduce((s,e)=>s+e.amountCents,0);
        html+=`<div class="personal-tracker-card" id="gt-personal-tracker" data-g="${personalGroup.groupId}"><div class="pt-header"><div class="pt-icon">&#x1F4DD;</div><div><div class="pt-title">Personal Tracker</div><div class="pt-subtitle">${pgExps.length} expense${pgExps.length!==1?'s':''} tracked</div></div></div><div class="pt-amount">${fmt(thisMonthTotal)} <span style="font-size:13px;opacity:0.8;font-weight:500">this month</span></div></div>`;
    }else{
        html+=`<div class="personal-tracker-card pt-empty" id="gt-personal-create"><div class="pt-header"><div class="pt-icon">&#x1F4DD;</div><div><div class="pt-title">Personal Tracker</div><div class="pt-subtitle">Track your personal expenses</div></div></div><button class="pt-create-btn" id="pt-create-btn">Start Tracking</button></div>`;
    }

    // Split into regular groups and personal/iou reminders
    const regularGroups=groups.filter(g=>!g.isIOU&&g.groupType!=='personal'&&!g.isPersonal);
    const iouGroups=groups.filter(g=>g.isIOU);

    if(regularGroups.length){
        html+=`<div class="sec-t">Groups</div>`;
        html+=regularGroups.map((g,i)=>{
            const c=allM.filter(m=>m.groupId===g.groupId&&!m.isDeleted).length;
            const myBal=groupBalances[g.groupId]||0;
            let balText,balClass;
            if(myBal>0){balText='You are owed '+fmt(myBal);balClass='bp'}
            else if(myBal<0){balText='You owe '+fmt(Math.abs(myBal));balClass='bn'}
            else{balText='Settled up';balClass='settled'}
            return`<div class="dash-group-card" data-g="${g.groupId}"><div class="group-icon gi-${(i%5)+1}">&#x1F46B;</div><div style="flex:1"><div class="card-t">${esc(g.name)}</div><div class="card-s">${c} member${c!==1?'s':''} &middot; ${new Date(g.createdAt).toLocaleDateString()}</div><div class="dgc-balance ${balClass}">${balText}</div></div></div>`;
        }).join('');
    }

    if(iouGroups.length){
        html+=`<div class="sec-t">Personal Notes</div>`;
        html+=iouGroups.map((g,i)=>{
            const c=allM.filter(m=>m.groupId===g.groupId&&!m.isDeleted).length;
            const myBal=groupBalances[g.groupId]||0;
            let balText,balClass;
            if(myBal>0){balText='You are owed '+fmt(myBal);balClass='bp'}
            else if(myBal<0){balText='You owe '+fmt(Math.abs(myBal));balClass='bn'}
            else{balText='Settled up';balClass='settled'}
            return`<div class="dash-group-card" data-g="${g.groupId}"><div class="group-icon gi-${((regularGroups.length+i)%5)+1}">&#x1F4B8;</div><div style="flex:1"><div class="card-t">${esc(g.name)}</div><div class="card-s">${c} member${c!==1?'s':''} &middot; ${new Date(g.createdAt).toLocaleDateString()}</div><div class="dgc-balance ${balClass}">${balText}</div></div></div>`;
        }).join('');
    }

    if(!regularGroups.length&&!iouGroups.length&&!personalGroup){
        html+='<div class="empty"><div class="ic">&#x1F465;</div><h3>No groups yet</h3><p>Tap + to create your first split group</p></div>';
    }

    el.innerHTML=html;
    el.querySelectorAll('[data-g]').forEach(c=>c.addEventListener('click',()=>openGroup(c.dataset.g)));
    const ptCreateBtn=document.getElementById('pt-create-btn');
    if(ptCreateBtn)ptCreateBtn.addEventListener('click',async(e)=>{e.stopPropagation();await createPersonalGroup();renderGroupsTab()});
}

// Create personal tracker group
async function createPersonalGroup(){
    const now=Date.now();
    const gid=uid();const key=await genKey();
    await iP('groups',{groupId:gid,name:'Personal',createdBy:myId,createdAt:now,inviteToken:uid(),groupKeyBase64:key,hlcTimestamp:now,hlcNodeId:myId,groupType:'personal',isPersonal:true});
    await iP('members',{_key:gid+'_'+myId,groupId:gid,memberId:myId,displayName:myName,joinedAt:now,isDeleted:false,hlcTimestamp:now});
    await pushOp(gid,key,{id:uid(),type:'member_join',data:{memberId:myId,displayName:myName,joinedAt:now},hlc:now,author:myId});
    await iPK('identity','personalGroupId',gid);
    startSync(gid,key);
    toast('Personal Tracker created!');
    autoBackup().catch(()=>{});
}

// Analytics tab render
let analyticsGroupFilter='';
let analyticsViewMode='group';
let analyticsCatView='donut';
let analyticsMonthView='chart';
let analyticsMemberView='donut';
let groupAnalyticsCatView='donut';
let groupAnalyticsMonthView='chart';
let groupAnalyticsMemberView='donut';

function buildDonutGradient(entries,colorFn){
    let cumPct=0;const stops=[];
    entries.forEach(([key,amt],i)=>{
        const color=colorFn(key);
        const startPct=cumPct;
        cumPct+=amt;
        stops.push(`${color} ${startPct}% ${cumPct}%`);
    });
    if(!stops.length) return 'var(--border)';
    return `conic-gradient(${stops.join(', ')})`;
}

function buildAnalyticsSummaryCard(opts){
    const {isYours,totalGroupSpend,totalYourShare,displayTotal,totalLabel,allExps,tagAmts,tagSorted,monthAmts,
           settledTotal,outstandingTotal,monthKeys}=opts;
    const tagColors=opts.tagColors;
    let h=`<div class="analytics-card"><div style="text-align:center">`;
    h+=`<div style="font-size:13px;color:var(--text-secondary);font-weight:600;margin-bottom:4px">${totalLabel}</div>`;
    h+=`<div style="font-size:32px;font-weight:800;color:${isYours?'var(--primary)':'var(--text)'}; letter-spacing:-1px">${fmt(displayTotal)}</div>`;
    // Sub row: complementary amount
    if(isYours){
        h+=`<div class="summary-sub">Group Total: ${fmt(totalGroupSpend)}</div>`;
    } else {
        h+=`<div class="summary-sub">Your Share: ${fmt(totalYourShare)}</div>`;
    }
    // Stats row: avg/mo and expense count
    const numMonthsWithData=monthKeys?monthKeys.filter(k=>monthAmts[k]>0).length:1;
    const avgPerMonth=numMonthsWithData>0?Math.round(displayTotal/numMonthsWithData):0;
    const expCount=allExps.length;
    h+=`<div class="summary-stat-row">${fmt(avgPerMonth)}/mo avg &middot; ${expCount} expense${expCount!==1?'s':''}</div>`;
    // Top category
    if(tagSorted.length){
        const [topTag,topAmt]=tagSorted[0];
        const t=TAGS[topTag]||TAGS.other;
        const topPct=displayTotal>0?Math.round(topAmt/displayTotal*100):0;
        h+=`<div class="summary-detail">${t.icon} ${t.label} is your top category (${topPct}%)</div>`;
    }
    // Trend: compare current month to previous month
    if(monthKeys&&monthKeys.length>=2){
        const curKey=monthKeys[monthKeys.length-1];
        const prevKey=monthKeys[monthKeys.length-2];
        const curAmt=monthAmts[curKey]||0;
        const prevAmt=monthAmts[prevKey]||0;
        if(prevAmt>0){
            const changePct=Math.round((curAmt-prevAmt)/prevAmt*100);
            if(changePct>0){
                h+=`<div class="summary-detail trend-up">&#x1F4C8; +${changePct}% vs last month</div>`;
            } else if(changePct<0){
                h+=`<div class="summary-detail trend-down">&#x1F4C9; ${changePct}% vs last month</div>`;
            } else {
                h+=`<div class="summary-detail">&#x1F4CA; Same as last month</div>`;
            }
        }
    }
    // Settlement summary
    if(settledTotal>0||outstandingTotal>0){
        h+=`<div class="settlement-line">`;
        h+=`<span class="settlement-settled">${fmt(settledTotal)} settled</span>`;
        h+=`<span class="settlement-outstanding">${fmt(outstandingTotal)} outstanding</span>`;
        h+=`</div>`;
    }
    h+=`</div></div>`;
    return h;
}

function buildCategorySection(tagSorted,tagAmts,displayTotal,tagColors,prefix,catView){
    let h=`<div class="analytics-card"><h3>&#x1F3F7; By Category</h3>`;
    h+=`<div class="chart-icon-toggle"><button class="chart-icon-btn${catView==='donut'?' active':''}" data-catview="donut" data-prefix="${prefix}" title="Donut"><div class="mini-donut"></div></button><button class="chart-icon-btn${catView==='bars'?' active':''}" data-catview="bars" data-prefix="${prefix}" title="Bars"><div class="mini-bars"><span style="width:12px"></span><span style="width:8px"></span><span style="width:10px"></span></div></button></div>`;
    if(catView==='donut'){
        // Donut view
        const totalAmt=tagSorted.reduce((a,[,v])=>a+v,0)||1;
        const pctEntries=tagSorted.map(([tag,amt])=>[tag,amt/totalAmt*100]);
        const gradient=buildDonutGradient(pctEntries,tag=>tagColors[tag]||'#BDC3C7');
        const donutTotal=tagSorted.reduce((a,[,v])=>a+v,0);
        h+=`<div class="donut-container"><div class="donut" style="background:${gradient}"><div class="donut-hole"><div style="font-size:16px;font-weight:800">${fmt(donutTotal)}</div><div style="font-size:10px;color:var(--text-secondary);font-weight:600">Total</div></div></div>`;
        h+=`<div class="donut-legend">`;
        tagSorted.forEach(([tag,amt])=>{
            const t=TAGS[tag]||TAGS.other;
            const color=tagColors[tag]||'#BDC3C7';
            const pct=displayTotal>0?Math.round(amt/displayTotal*100):0;
            h+=`<div class="donut-legend-item"><span class="donut-legend-dot" style="background:${color}"></span><span style="flex:1">${t.label}</span><span style="color:var(--text-secondary)">${pct}%</span><span style="font-weight:700">${fmt(amt)}</span></div>`;
        });
        h+=`</div></div>`;
    } else {
        // Bars view
        const catMax=tagSorted.length?tagSorted[0][1]:1;
        tagSorted.forEach(([tag,amt])=>{
            const t=TAGS[tag]||TAGS.other;
            const color=tagColors[tag]||'#BDC3C7';
            const pct=catMax>0?(amt/catMax*100):0;
            const pctOfTotal=displayTotal>0?Math.round(amt/displayTotal*100):0;
            h+=`<div style="display:flex;align-items:center;gap:8px;margin-bottom:10px"><span class="donut-legend-dot" style="background:${color}"></span><span style="font-size:13px;font-weight:600;width:70px;flex-shrink:0">${t.label}</span><div class="chart-bar-track" style="flex:1"><div class="chart-bar-fill" style="width:${Math.max(pct,3)}%;background:${color}"><span></span></div></div><span style="font-size:12px;font-weight:700;width:65px;text-align:right;flex-shrink:0">${fmt(amt)}</span><span style="font-size:11px;color:var(--text-secondary);width:32px;text-align:right;flex-shrink:0">${pctOfTotal}%</span></div>`;
        });
    }
    h+=`</div>`;
    return h;
}

function buildMonthSection(monthAmts,monthKeys,monthNames,monthExpCounts,prefix,monthView){
    const monthMax=Math.max(...Object.values(monthAmts),1);
    let h=`<div class="analytics-card"><h3>&#x1F4C5; By Month</h3>`;
    h+=`<div class="chart-icon-toggle"><button class="chart-icon-btn${monthView==='chart'?' active':''}" data-monthview="chart" data-prefix="${prefix}" title="Bar chart"><div class="mini-bars"><span style="width:12px"></span><span style="width:8px"></span><span style="width:10px"></span></div></button><button class="chart-icon-btn${monthView==='line'?' active':''}" data-monthview="line" data-prefix="${prefix}" title="Line chart"><div class="mini-line"><span style="height:4px"></span><span style="height:8px"></span><span style="height:6px"></span><span style="height:10px"></span><span style="height:7px"></span></div></button></div>`;
    if(monthView==='line'){
        // Line chart SVG
        const vals=monthKeys.map(k=>monthAmts[k]);
        const maxVal=Math.max(...vals,1);
        const svgW=400,svgH=150,padTop=25,padBot=20,padX=30;
        const chartW=svgW-padX*2,chartH=svgH-padTop-padBot;
        const points=vals.map((v,i)=>{
            const x=padX+(vals.length>1?(i/(vals.length-1))*chartW:chartW/2);
            const y=padTop+(maxVal>0?(1-v/maxVal)*chartH:chartH);
            return {x,y,v};
        });
        const pointsStr=points.map(p=>`${p.x},${p.y}`).join(' ');
        const areaPoints=`${points[0].x},${padTop+chartH} ${pointsStr} ${points[points.length-1].x},${padTop+chartH}`;
        h+=`<svg viewBox="0 0 ${svgW} ${svgH}" class="line-chart" preserveAspectRatio="xMidYMid meet">`;
        h+=`<defs><linearGradient id="lineGrad-${prefix}" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="var(--primary)" stop-opacity="0.25"/><stop offset="100%" stop-color="var(--primary)" stop-opacity="0"/></linearGradient></defs>`;
        h+=`<polygon points="${areaPoints}" fill="url(#lineGrad-${prefix})"/>`;
        h+=`<polyline points="${pointsStr}" fill="none" stroke="var(--primary)" stroke-width="2.5"/>`;
        points.forEach((p,i)=>{
            h+=`<circle cx="${p.x}" cy="${p.y}" r="4" fill="var(--primary)"/>`;
            const m=parseInt(monthKeys[i].split('-')[1])-1;
            const yr=monthKeys[i].split('-')[0].slice(2);
            h+=`<text x="${p.x}" y="${padTop+chartH+14}">${monthNames[m]}'${yr}</text>`;
            h+=`<text x="${p.x}" y="${p.y-10}" class="amount-label">${fmt(p.v)}</text>`;
        });
        h+=`</svg>`;
    } else {
        // Bar chart (original)
        const monthVals=monthKeys.map(k=>monthAmts[k]);
        monthKeys.forEach((key,idx)=>{
            const amt=monthAmts[key];
            const pct=monthMax>0?(amt/monthMax*100):0;
            const m=parseInt(key.split('-')[1])-1;
            const yr=key.split('-')[0].slice(2);
            let changeHtml='';
            if(idx>0){
                const prevAmt=monthVals[idx-1];
                if(prevAmt>0){
                    const chg=Math.round((amt-prevAmt)/prevAmt*100);
                    if(chg>0) changeHtml=` <span class="trend-up" style="font-size:11px;font-weight:700">&uarr;${chg}%</span>`;
                    else if(chg<0) changeHtml=` <span class="trend-down" style="font-size:11px;font-weight:700">&darr;${Math.abs(chg)}%</span>`;
                }
            }
            h+=`<div style="margin-bottom:10px"><div style="display:flex;justify-content:space-between;margin-bottom:4px"><span style="font-size:13px;font-weight:600">${monthNames[m]} '${yr}${changeHtml}</span><span style="font-size:12px;color:var(--text-secondary)">${fmt(amt)}</span></div>`;
            h+=`<div class="chart-bar-track"><div class="chart-bar-fill" style="width:${Math.max(pct,3)}%;background:var(--primary)"><span></span></div></div>`;
            h+=`</div>`;
        });
    }
    h+=`</div>`;
    return h;
}

function buildMemberSection(memberPaid,nameMap,prefix,memberView){
    memberView=memberView||'donut';
    const paidSorted=Object.entries(memberPaid).sort((a,b)=>b[1]-a[1]);
    const totalPaid=paidSorted.reduce((a,[,v])=>a+v,0)||1;
    const memberCount=paidSorted.length;
    let h=`<div class="analytics-card"><h3>&#x1F4B3; By Member</h3>`;
    h+=`<div class="chart-icon-toggle"><button class="chart-icon-btn${memberView==='donut'?' active':''}" data-memberview="donut" data-prefix="${prefix}" title="Donut"><div class="mini-donut"></div></button><button class="chart-icon-btn${memberView==='bars'?' active':''}" data-memberview="bars" data-prefix="${prefix}" title="Bars"><div class="mini-bars"><span style="width:12px"></span><span style="width:8px"></span><span style="width:10px"></span></div></button></div>`;
    if(memberView==='donut'){
        const pctEntries=paidSorted.map(([id,amt])=>[id,amt/totalPaid*100]);
        const gradient=buildDonutGradient(pctEntries,id=>getColor(id));
        h+=`<div class="donut-container"><div class="donut" style="background:${gradient}"><div class="donut-hole"><div style="font-size:22px;font-weight:800">${memberCount}</div><div style="font-size:10px;color:var(--text-secondary);font-weight:600">member${memberCount!==1?'s':''}</div></div></div>`;
        h+=`<div class="donut-legend">`;
        paidSorted.forEach(([id,amt])=>{
            const col=getColor(id);
            const pct=Math.round(amt/totalPaid*100);
            h+=`<div class="donut-legend-item"><span class="donut-legend-dot" style="background:${col}"></span><span style="flex:1">${esc(nameMap[id]||id.slice(0,8))}</span><span style="font-weight:700">${fmt(amt)}</span><span style="color:var(--text-secondary)">${pct}%</span></div>`;
        });
        h+=`</div></div>`;
    } else {
        // Bars only
        const maxPaid=paidSorted.length>0?paidSorted[0][1]:1;
        paidSorted.forEach(([id,amt])=>{
            const col=getColor(id);
            const pct=maxPaid>0?(amt/maxPaid*100):0;
            const pctOfTotal=Math.round(amt/totalPaid*100);
            h+=`<div style="margin-bottom:10px"><div style="display:flex;justify-content:space-between;margin-bottom:4px"><span style="font-size:13px;font-weight:600">${esc(nameMap[id]||id.slice(0,8))}</span><span style="font-size:12px;color:var(--text-secondary)">${pctOfTotal}% &middot; ${fmt(amt)}</span></div>`;
            h+=`<div class="chart-bar-track"><div class="chart-bar-fill" style="width:${Math.max(pct,3)}%;background:${col}"><span></span></div></div>`;
            h+=`</div>`;
        });
    }
    h+=`</div>`;
    return h;
}

async function renderAnalyticsTab(){
    const allExpsRaw=(await iA('expenses')).filter(e=>!e.isDeleted);
    const allSetsRaw=(await iA('settlements')).filter(s=>!s.isDeleted);
    const allMems=await iA('members');
    const groups=await iA('groups');
    const globalNm={};allMems.forEach(m=>globalNm[m.memberId]=m.displayName);
    const el=document.getElementById('analytics-tab-content');
    const isYours=analyticsViewMode==='yours';

    // Filter by group if selected
    const allExps=analyticsGroupFilter?allExpsRaw.filter(e=>e.groupId===analyticsGroupFilter):allExpsRaw;
    const allSets=analyticsGroupFilter?allSetsRaw.filter(s=>s.groupId===analyticsGroupFilter):allSetsRaw;

    // Group selector
    let html='<div style="margin-bottom:16px"><select id="analytics-group-select" style="width:100%;padding:12px 16px;border:2px solid var(--border);border-radius:var(--rs);font-size:14px;font-weight:600;background:var(--card);color:var(--text);font-family:Inter,sans-serif;cursor:pointer">';
    html+=`<option value=""${!analyticsGroupFilter?' selected':''}>Overall (All Groups)</option>`;
    groups.forEach(g=>{html+=`<option value="${g.groupId}"${analyticsGroupFilter===g.groupId?' selected':''}>${esc(g.name)}</option>`});
    html+='</select></div>';

    // View toggle: Group / Yours
    html+=`<div class="seg-control" id="analytics-view-toggle"><button class="seg-btn${!isYours?' active':''}" data-mode="group">Group</button><button class="seg-btn${isYours?' active':''}" data-mode="yours">Yours</button></div>`;

    if(!allExps.length){html+='<div class="empty"><div class="ic">&#x1F4CA;</div><h3>No data yet</h3><p>Add expenses to see analytics</p></div>';el.innerHTML=html;el.querySelector('#analytics-group-select').addEventListener('change',e=>{analyticsGroupFilter=e.target.value;renderAnalyticsTab()});el.querySelectorAll('#analytics-view-toggle .seg-btn').forEach(b=>b.addEventListener('click',()=>{analyticsViewMode=b.dataset.mode;renderAnalyticsTab()}));return}

    const tagColors={'food':'#7C6FE0','transport':'#89C4F4','shopping':'#F2A0C4','entertainment':'#F8C4A4',
        'travel':'#BB8FCE','bills':'#F7DC6F','groceries':'#8DDCC5','health':'#F1948A','rent':'#F8C4A4','other':'#BDC3C7'};

    // Total spending
    const totalGroupSpend=allExps.reduce((a,e)=>a+e.amountCents,0);
    const totalYourShare=allExps.reduce((a,e)=>a+getMyShare(e,myId),0);
    const displayTotal=isYours?totalYourShare:totalGroupSpend;
    const totalLabel=isYours?'Your Spending':'Total Group Spending';

    // By Category
    const tagTotals={};
    const tagMyTotals={};
    allExps.forEach(e=>{const t=e.tag||'other';tagTotals[t]=(tagTotals[t]||0)+e.amountCents;tagMyTotals[t]=(tagMyTotals[t]||0)+getMyShare(e,myId)});
    const tagAmts=isYours?tagMyTotals:tagTotals;
    const tagSorted=Object.entries(tagAmts).sort((a,b)=>b[1]-a[1]);

    // By Month
    const monthTotals={};
    const monthMyTotals={};
    const monthExpCounts={};
    const now=new Date();
    const monthNames=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    for(let i=5;i>=0;i--){
        const d=new Date(now.getFullYear(),now.getMonth()-i,1);
        const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
        monthTotals[key]=0;
        monthMyTotals[key]=0;
        monthExpCounts[key]=0;
    }
    allExps.forEach(e=>{
        const d=new Date(e.createdAt);
        const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
        if(key in monthTotals){monthTotals[key]+=e.amountCents;monthMyTotals[key]+=getMyShare(e,myId);monthExpCounts[key]++}
    });
    const monthAmts=isYours?monthMyTotals:monthTotals;
    const monthKeys=Object.keys(monthAmts);

    // Settlement totals
    const settledTotal=allSets.reduce((a,s)=>a+s.amountCents,0);
    const grpsToCheck=analyticsGroupFilter?groups.filter(g=>g.groupId===analyticsGroupFilter):groups;
    let outstandingTotal=0;
    for(const g of grpsToCheck){
        const gExps=allExps.filter(e=>e.groupId===g.groupId);
        const gSets=allSets.filter(s=>s.groupId===g.groupId);
        const bal=calcBal(gExps,gSets);
        for(const[id,b] of Object.entries(bal)){
            if(b<0) outstandingTotal+=Math.abs(b);
        }
    }

    // 1. Rich Summary Card
    html+=buildAnalyticsSummaryCard({isYours,totalGroupSpend,totalYourShare,displayTotal,totalLabel,allExps,tagAmts,tagSorted,monthAmts,settledTotal,outstandingTotal,tagColors,monthKeys});

    // 2. By Category (Donut/Bars toggle)
    html+=buildCategorySection(tagSorted,tagAmts,displayTotal,tagColors,'at',analyticsCatView);

    // 3. By Month (Chart/Table toggle)
    html+=buildMonthSection(monthAmts,monthKeys,monthNames,monthExpCounts,'at',analyticsMonthView);

    // 4. Category by Month (stacked bars with legend)
    html+=`<div class="analytics-card"><h3>&#x1F4CA; Category by Month</h3>`;
    const allTags=[...new Set(allExps.map(e=>e.tag||'other'))];
    html+=`<div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px">`;
    allTags.forEach(tag=>{const t=TAGS[tag]||TAGS.other;const color=tagColors[tag]||'#BDC3C7';html+=`<span style="display:inline-flex;align-items:center;gap:4px;font-size:11px;color:var(--text-secondary)"><span class="donut-legend-dot" style="background:${color};border-radius:2px"></span>${t.label}</span>`});
    html+=`</div>`;
    const monthCatData={};
    Object.keys(monthTotals).forEach(key=>{monthCatData[key]={}});
    allExps.forEach(e=>{
        const d=new Date(e.createdAt);
        const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
        const tag=e.tag||'other';
        if(key in monthCatData){
            const amt=isYours?getMyShare(e,myId):e.amountCents;
            monthCatData[key][tag]=(monthCatData[key][tag]||0)+amt;
        }
    });
    const monthCatTotals={};
    Object.entries(monthCatData).forEach(([key,cats])=>{monthCatTotals[key]=Object.values(cats).reduce((a,b)=>a+b,0)});
    const monthCatMax=Math.max(...Object.values(monthCatTotals),1);
    Object.entries(monthCatData).forEach(([key,cats])=>{
        const m=parseInt(key.split('-')[1])-1;
        const yr=key.split('-')[0].slice(2);
        const total=monthCatTotals[key]||0;
        html+=`<div style="margin-bottom:10px"><div style="display:flex;justify-content:space-between;margin-bottom:4px"><span style="font-size:13px;font-weight:600">${monthNames[m]} '${yr}</span><span style="font-size:12px;color:var(--text-secondary)">${fmt(total)}</span></div>`;
        html+=`<div style="display:flex;height:24px;border-radius:6px;overflow:hidden;background:var(--bg-secondary)">`;
        if(total>0){
            const catEntries=Object.entries(cats).sort((a,b)=>b[1]-a[1]);
            catEntries.forEach(([tag,amt])=>{
                const color=tagColors[tag]||'#BDC3C7';
                const segPct=(amt/monthCatMax*100);
                html+=`<div style="width:${segPct}%;background:${color};min-width:${segPct>2?0:2}px" title="${(TAGS[tag]||TAGS.other).label}: ${fmt(amt)}"></div>`;
            });
        }
        html+=`</div></div>`;
    });
    html+=`</div>`;

    // 5. By Member — Donut chart (hidden in "yours" mode)
    if(!isYours){
        const memberPaid={};
        allExps.forEach(e=>{
            if(e.paidByMap){for(const[id,a] of Object.entries(e.paidByMap))memberPaid[id]=(memberPaid[id]||0)+a}
            else{memberPaid[e.paidBy]=(memberPaid[e.paidBy]||0)+e.amountCents}
        });
        if(Object.keys(memberPaid).length){
            html+=buildMemberSection(memberPaid,globalNm,'at',analyticsMemberView);
        }
    }

    el.innerHTML=html;

    // Event listeners
    el.querySelector('#analytics-group-select').addEventListener('change',e=>{analyticsGroupFilter=e.target.value;renderAnalyticsTab()});
    el.querySelectorAll('#analytics-view-toggle .seg-btn').forEach(b=>b.addEventListener('click',()=>{analyticsViewMode=b.dataset.mode;renderAnalyticsTab()}));
    el.querySelectorAll('.chart-icon-btn[data-catview][data-prefix="at"]').forEach(b=>b.addEventListener('click',()=>{analyticsCatView=b.dataset.catview;renderAnalyticsTab()}));
    el.querySelectorAll('.chart-icon-btn[data-monthview][data-prefix="at"]').forEach(b=>b.addEventListener('click',()=>{analyticsMonthView=b.dataset.monthview;renderAnalyticsTab()}));
    el.querySelectorAll('.chart-icon-btn[data-memberview][data-prefix="at"]').forEach(b=>b.addEventListener('click',()=>{analyticsMemberView=b.dataset.memberview;renderAnalyticsTab()}));
}

// Profile tab render
async function renderProfile(){
    const el=document.getElementById('profile-content');
    const pp=await iG('identity','pp');
    const ppStatus=pp?'Set':'Not set';
    const ppStatusColor=pp?'var(--positive)':'var(--negative)';
    const col=getColor(myId);
    const savedTheme=localStorage.getItem('theme')||'system';

    let html=`
        <div class="profile-header">
            <div class="profile-avatar" style="background:${col}">${getInitial(myName)}</div>
            <div class="profile-name-edit">
                <span class="profile-name" id="profile-display-name">${esc(myName)}</span>
                <button class="profile-name-edit-btn" id="profile-edit-name-btn" title="Edit name">&#x270F;&#xFE0F;</button>
            </div>
        </div>
        <div id="profile-edit-name-form" style="display:none;padding:0 0 16px">
            <div class="fg" style="margin-bottom:10px">
                <input type="text" id="profile-name-input" placeholder="Your name" value="${esc(myName)}">
            </div>
            <div style="display:flex;gap:8px">
                <button class="btn btn-p" id="profile-name-save" style="font-size:14px;padding:12px">Save</button>
                <button class="btn btn-o" id="profile-name-cancel" style="font-size:14px;padding:12px">Cancel</button>
            </div>
        </div>

        <div class="profile-section">
            <div class="profile-section-title">Recovery</div>
            <div class="profile-row">
                <div class="profile-row-left">
                    <span class="profile-row-icon">&#x1F511;</span>
                    <div>
                        <div class="profile-row-label">Passphrase</div>
                        <div class="profile-row-sublabel" style="color:${ppStatusColor}">${ppStatus}</div>
                    </div>
                </div>
                <button class="profile-row-action" id="profile-pp-btn">${pp?'View':'Set Up'}</button>
            </div>
        </div>

        <div class="profile-section">
            <div class="profile-section-title">Devices</div>
            <div class="profile-row">
                <div class="profile-row-left">
                    <span class="profile-row-icon">&#x1F4F1;</span>
                    <div>
                        <div class="profile-row-label">Sync to Another Device</div>
                        <div class="profile-row-sublabel">Transfer your data</div>
                    </div>
                </div>
                <button class="profile-row-action" id="profile-sync-btn">Sync</button>
            </div>
        </div>

        <div class="profile-section">
            <div class="profile-section-title">Security</div>
            <div class="profile-row">
                <div class="profile-row-left">
                    <span class="profile-row-icon">&#x1F512;</span>
                    <div>
                        <div class="profile-row-label">How Your Data is Protected</div>
                        <div class="profile-row-sublabel">End-to-end encrypted</div>
                    </div>
                </div>
                <button class="profile-row-action" id="profile-security-btn">View</button>
            </div>
        </div>

        <div class="profile-section">
            <div class="profile-section-title">Appearance</div>
            <div class="seg-control" id="profile-theme-seg">
                <button class="seg-btn${savedTheme==='light'?' active':''}" data-theme="light">&#x2600;&#xFE0F; Light</button>
                <button class="seg-btn${!savedTheme||savedTheme==='system'?' active':''}" data-theme="system">&#x1F4F1; System</button>
                <button class="seg-btn${savedTheme==='dark'?' active':''}" data-theme="dark">&#x1F319; Dark</button>
            </div>
        </div>

        <div class="profile-about">
            SplitBlind v0.1.0 &middot; Open Source<br>
            <a href="https://github.com/hell-abhi/splitblind" target="_blank" rel="noopener">View on GitHub &#x2192;</a>
        </div>
    `;

    el.innerHTML=html;

    // Wire profile events
    document.getElementById('profile-edit-name-btn').onclick=()=>{
        document.getElementById('profile-edit-name-form').style.display='';
        document.getElementById('profile-name-input').focus();
    };
    document.getElementById('profile-name-cancel').onclick=()=>{
        document.getElementById('profile-edit-name-form').style.display='none';
        document.getElementById('profile-name-input').value=myName;
    };
    document.getElementById('profile-name-save').onclick=async()=>{
        const newName=document.getElementById('profile-name-input').value.trim();
        if(!newName){toast('Name cannot be empty');return}
        myName=newName;
        await iPK('identity','name',myName);
        document.getElementById('profile-display-name').textContent=myName;
        document.getElementById('profile-edit-name-form').style.display='none';
        toast('Name updated');
        autoBackup().catch(()=>{});
    };
    document.getElementById('profile-pp-btn').onclick=async()=>{
        const pp=await iG('identity','pp');
        if(pp){
            // Show current passphrase
            openModal(`
                <div style="text-align:center;padding:12px 0 8px">
                    <div style="font-size:40px;margin-bottom:12px">&#x1F511;</div>
                    <h3 style="font-size:17px;font-weight:800">Your Recovery Passphrase</h3>
                    <div style="font-size:20px;font-weight:800;color:var(--primary);padding:16px;background:var(--primary-bg);border-radius:14px;margin:16px 0;word-spacing:6px">${esc(pp)}</div>
                    <p style="color:var(--text-secondary);font-size:13px;font-weight:500;line-height:1.6">Keep this safe. It's the only way to recover your data.</p>
                    <div style="text-align:left;margin-top:12px;padding:12px;background:var(--surface);border-radius:12px;font-size:11px;color:var(--text-secondary);line-height:1.6">
                        &#x1F512; <strong>Your passphrase is hashed (SHA-256)</strong> before lookup — the server never sees the actual words.<br>
                        &#x1F4E6; <strong>Your backup is AES-256 encrypted</strong> with a key derived from your passphrase + unique salt.<br>
                        &#x1F6E1; <strong>No one can read it</strong> — not us, not Firebase, not anyone without your exact passphrase.
                    </div>
                </div>
                <div style="display:flex;gap:8px;margin-bottom:10px">
                    <button class="modal-btn modal-btn-edit" style="flex:1" onclick="downloadPassphrase('${esc(pp)}');closeModal()">&#x1F4E5; Download</button>
                    <button class="modal-btn modal-btn-edit" style="flex:1" onclick="sharePassphrase('${esc(pp)}');closeModal()">&#x1F4E4; Share</button>
                </div>
                <button class="modal-btn modal-btn-cancel" onclick="closeModal()">Close</button>
            `);
        }else{
            // Go to passphrase setup
            obPassphrase=generatePassphrase();showObPp();
            document.getElementById('ob-pp-confirm').checked=false;
            document.getElementById('ob-pp-continue').disabled=true;
            document.getElementById('ob-pp-custom-input').style.display='none';
            show('ob-recovery');
        }
    };
    document.getElementById('profile-sync-btn').onclick=()=>{
        document.getElementById('sg-pin').value='';
        document.getElementById('sg-btn').disabled=true;
        document.getElementById('sg-result').style.display='none';
        document.getElementById('sg-btn').style.display='';
        show('sync-gen');
    };
    document.getElementById('profile-security-btn').onclick=()=>show('security');
    document.querySelectorAll('#profile-theme-seg .seg-btn').forEach(btn=>{
        btn.onclick=()=>{
            const mode=btn.dataset.theme;
            localStorage.setItem('theme',mode==='system'?'system':mode);
            document.documentElement.classList.remove('dark','light');
            if(mode==='dark')document.documentElement.classList.add('dark');
            else if(mode==='light')document.documentElement.classList.add('light');
            // system: no class = falls through to @media query
            renderProfile();
        };
    });
}

async function openGroup(gid){curGroup=gid;const g=await iG('groups',gid);if(!g)return;curGroupKey=g.groupKeyBase64;curGroupType=g.groupType||'';document.getElementById('gd-title').textContent=g.name;const isPersonal=g.groupType==='personal'||g.isPersonal;const pplTab=document.querySelector('.tab[data-tab="members"]');if(pplTab)pplTab.style.display=isPersonal?'none':'';const analyticsTab=document.querySelector('.tab[data-tab="analytics"]');if(analyticsTab)analyticsTab.style.display='';show('gd');startSync(gid,curGroupKey);switchGroupTab('exp')}

async function restoreExpense(eid){
    const expense=await iG('expenses',eid);if(!expense)return;
    const now=Date.now();
    const data={...expense,isDeleted:false};delete data.hlcTimestamp;
    await saveHistory(makeHistoryEntry({expenseId:eid,entityType:'expense',action:'created',previousData:null,newData:data}));
    await iP('expenses',{...data,hlcTimestamp:now});
    await pushOp(curGroup,curGroupKey,{id:uid(),type:'expense',data,hlc:now,author:myId});
    refreshTab();toast('Expense restored');
}
async function restoreSettlement(sid){
    const settlement=await iG('settlements',sid);if(!settlement)return;
    const now=Date.now();
    const data={...settlement,isDeleted:false};delete data.hlcTimestamp;
    await saveHistory(makeHistoryEntry({settlementId:sid,entityType:'settlement',action:'created',previousData:null,newData:data}));
    await iP('settlements',{...data,hlcTimestamp:now});
    await pushOp(curGroup,curGroupKey,{id:uid(),type:'settlement',data,hlc:now,author:myId});
    refreshTab();toast('Settlement restored');
}
function renderHistoryHtml(entries,nm){
    if(!entries.length)return'';
    const sorted=entries.sort((a,b)=>b.changedAt-a.changedAt);
    const actionIcons={created:'\u{1F4DD}',edited:'\u270F\uFE0F',deleted:'\u{1F5D1}\uFE0F'};
    const actionLabels={created:'Created',edited:'Edited',deleted:'Deleted'};
    let html='<div class="history-log">';
    sorted.forEach(h=>{
        const icon=actionIcons[h.action]||'\u{1F4CB}';
        const label=actionLabels[h.action]||h.action;
        const byName=esc(h.changedByName||nm[h.changedBy]||h.changedBy.slice(0,8));
        const dateStr=new Date(h.changedAt).toLocaleDateString('en',{month:'short',day:'numeric'});
        html+=`<div class="history-entry">${icon} ${label} by ${byName} \u00B7 ${dateStr}</div>`;
        if(h.action==='edited'&&h.previousData&&h.newData){
            const changes=computeChanges(h.previousData,h.newData);
            changes.forEach(c=>{
                if(c.field==='Split among'||c.field==='Split mode'){
                    html+=`<span class="history-detail">${esc(c.field)}: ${esc(c.to)}</span>`;
                }else{
                    html+=`<span class="history-detail">${esc(c.field)}: ${esc(c.from)} \u2192 ${esc(c.to)}</span>`;
                }
            });
        }
    });
    html+='</div>';
    return html;
}
let expSearchQuery='',expTagFilter=null,expDateFilter='all';
async function renderExpenses(){
    const allExps=(await iA('expenses')).filter(e=>e.groupId===curGroup);
    const allSets=(await iA('settlements')).filter(s=>s.groupId===curGroup);
    const allHistory=(await iA('history')).filter(h=>h.groupId===curGroup);
    const mems=(await iA('members')).filter(m=>m.groupId===curGroup);const nm={};mems.forEach(m=>nm[m.memberId]=m.displayName);
    // Build history lookup by expenseId/settlementId
    const histByExpense={},histBySettlement={};
    allHistory.forEach(h=>{
        if(h.expenseId){if(!histByExpense[h.expenseId])histByExpense[h.expenseId]=[];histByExpense[h.expenseId].push(h)}
        if(h.settlementId){if(!histBySettlement[h.settlementId])histBySettlement[h.settlementId]=[];histBySettlement[h.settlementId].push(h)}
    });
    // Active items
    const exps=allExps.filter(e=>!e.isDeleted);
    const sets=allSets.filter(s=>!s.isDeleted);
    // Deleted items that have history (so users can see what was deleted)
    const deletedExps=allExps.filter(e=>e.isDeleted&&histByExpense[e.expenseId]);
    const deletedSets=allSets.filter(s=>s.isDeleted&&histBySettlement[s.settlementId]);
    // Build lookup maps
    expenseLookup={};exps.forEach(e=>expenseLookup[e.expenseId]=e);
    settlementLookup={};sets.forEach(s=>settlementLookup[s.settlementId]=s);
    // Merge expenses and settlements into one timeline
    let items=[];
    exps.forEach(e=>items.push({type:'expense',data:e,ts:e.createdAt,deleted:false}));
    sets.forEach(s=>items.push({type:'settlement',data:s,ts:s.createdAt,deleted:false}));
    deletedExps.forEach(e=>items.push({type:'expense',data:e,ts:e.createdAt,deleted:true}));
    deletedSets.forEach(s=>items.push({type:'settlement',data:s,ts:s.createdAt,deleted:true}));

    // Apply search filter
    if(expSearchQuery){
        const q=expSearchQuery.toLowerCase();
        items=items.filter(item=>{
            if(item.type==='expense')return item.data.description.toLowerCase().includes(q);
            if(item.type==='settlement'){
                const fn=nm[item.data.fromMember]||'';const tn=nm[item.data.toMember]||'';
                return fn.toLowerCase().includes(q)||tn.toLowerCase().includes(q)||'settlement'.includes(q);
            }
            return true;
        });
    }
    // Apply tag filter
    if(expTagFilter){
        items=items.filter(item=>{
            if(item.type==='expense')return item.data.tag===expTagFilter;
            return true; // keep settlements
        });
    }
    // Apply date filter
    if(expDateFilter!=='all'){
        const now=new Date();let startDate;
        if(expDateFilter==='this-month'){startDate=new Date(now.getFullYear(),now.getMonth(),1)}
        else if(expDateFilter==='last-month'){startDate=new Date(now.getFullYear(),now.getMonth()-1,1);const endDate=new Date(now.getFullYear(),now.getMonth(),0);items=items.filter(i=>i.ts>=startDate.getTime()&&i.ts<=endDate.getTime()+86400000)}
        if(expDateFilter==='this-month')items=items.filter(i=>i.ts>=startDate.getTime());
    }

    items.sort((a,b)=>b.ts-a.ts);
    const el=document.getElementById('t-exp');

    // Build search/filter bar
    let filterHtml=`<div class="search-bar"><span class="search-icon">&#x1F50D;</span><input type="text" id="exp-search" placeholder="Search expenses..." value="${esc(expSearchQuery)}"></div>`;
    // Tag filter pills
    const usedTags={};exps.forEach(e=>{if(e.tag)usedTags[e.tag]=true});
    if(Object.keys(usedTags).length){
        filterHtml+=`<div class="filter-bar">`;
        for(const[k,v] of Object.entries(TAGS)){
            if(!usedTags[k])continue;
            filterHtml+=`<button class="filter-pill${expTagFilter===k?' active':''}" data-tag="${k}" style="background:${v.color}">${v.icon} ${v.label}</button>`;
        }
        filterHtml+=`</div>`;
    }
    // Date filter
    filterHtml+=`<select class="date-filter" id="exp-date-filter"><option value="all"${expDateFilter==='all'?' selected':''}>All time</option><option value="this-month"${expDateFilter==='this-month'?' selected':''}>This month</option><option value="last-month"${expDateFilter==='last-month'?' selected':''}>Last month</option></select>`;

    if(!items.length&&!expSearchQuery&&!expTagFilter&&expDateFilter==='all'){el.innerHTML=filterHtml+'<div class="empty"><div class="ic">&#x1F9FE;</div><h3>No activity yet</h3><p>Tap + to add your first expense</p></div>';wireExpFilters();return}
    if(!items.length){el.innerHTML=filterHtml+'<div class="empty"><div class="ic">&#x1F50D;</div><h3>No results</h3><p>Try a different search or filter</p></div>';wireExpFilters();return}

    el.innerHTML=filterHtml+items.map(item=>{
        const isDeleted=item.deleted;
        const delClass=isDeleted?' deleted-card':'';
        const delBadge=isDeleted?'<span class="deleted-badge">Deleted</span>':'';
        if(item.type==='expense'){
            const e=item.data;
            const hist=histByExpense[e.expenseId]||[];
            const ts=getTagStyle(e.tag);const st=ts?{icon:ts.icon,bg:ts.bg}:getExpStyle(e.description);
            let paidStr;
            if(e.paidByMap){const names=Object.keys(e.paidByMap).map(id=>esc(nm[id]||id.slice(0,8)));paidStr='Paid by '+names.join(', ')}
            else{paidStr='Paid by '+esc(nm[e.paidBy]||e.paidBy.slice(0,8))}
            const tagHtml=ts?`<div class="tag-pill-sm" style="background:${ts.bg};color:var(--text)">${ts.icon} ${ts.label}</div>`:'';
            const splitHtml=e.splitMode&&e.splitMode!=='equal'?`<div class="exp-split-info">${e.splitMode==='amount'?'By amount':e.splitMode==='percentage'?'By %':'By ratio'}</div>`:'';
            // Notes indicator
            let noteHtml='';
            if(e.notes){
                noteHtml=`<div class="exp-note-indicator" data-note-target="note-${e.expenseId}">\u{1F4DD} Note</div><div id="note-${e.expenseId}" class="exp-note-text" style="display:none">${esc(e.notes)}</div>`;
            }
            const histToggle=hist.length?`<div class="history-toggle" data-hist-target="hist-${e.expenseId}">\u{1F4CB} History (${hist.length})</div><div id="hist-${e.expenseId}" style="display:none">${renderHistoryHtml(hist,nm)}${isDeleted?`<button class="restore-btn" data-restore-eid="${e.expenseId}">\u21A9 Restore</button>`:''}</div>`:(isDeleted?`<button class="restore-btn" data-restore-eid="${e.expenseId}">\u21A9 Restore</button>`:'');
            return`<div class="card${delClass}" data-eid="${e.expenseId}"><div class="exp-c"><div class="exp-left"><div class="exp-icon" style="background:${st.bg}">${st.icon}</div><div><div class="card-t">${esc(e.description)}${delBadge}</div><div class="card-s">${paidStr}</div>${tagHtml}${splitHtml}</div></div><div><div class="exp-a">${fmt(e.amountCents)}</div><div class="exp-d">${new Date(e.createdAt).toLocaleDateString('en',{month:'short',day:'numeric'})}</div></div></div>${noteHtml}${histToggle}</div>`;
        }else{
            const s=item.data;
            const hist=histBySettlement[s.settlementId]||[];
            const fromName=esc(nm[s.fromMember]||s.fromMember.slice(0,8));
            const toName=esc(nm[s.toMember]||s.toMember.slice(0,8));
            const histToggle=hist.length?`<div class="history-toggle" data-hist-target="hist-${s.settlementId}">\u{1F4CB} History (${hist.length})</div><div id="hist-${s.settlementId}" style="display:none">${renderHistoryHtml(hist,nm)}${isDeleted?`<button class="restore-btn" data-restore-sid="${s.settlementId}">\u21A9 Restore</button>`:''}</div>`:(isDeleted?`<button class="restore-btn" data-restore-sid="${s.settlementId}">\u21A9 Restore</button>`:'');
            return`<div class="card${delClass}" data-sid="${s.settlementId}" style="border-left:3px solid var(--positive)"><div class="exp-c"><div class="exp-left"><div class="exp-icon" style="background:rgba(107,203,119,0.12)">&#x1F91D;</div><div><div class="card-t">Settlement${delBadge}</div><div class="card-s">${fromName} paid ${toName}</div></div></div><div><div class="exp-a" style="color:var(--positive)">${fmt(s.amountCents)}</div><div class="exp-d">${new Date(s.createdAt).toLocaleDateString('en',{month:'short',day:'numeric'})}</div></div></div>${histToggle}</div>`;
        }
    }).join('');

    wireExpFilters();

    // Attach click handlers for non-deleted expense/settlement cards (click on the exp-c row)
    el.querySelectorAll('[data-eid]:not(.deleted-card)').forEach(c=>{
        const expC=c.querySelector('.exp-c');
        if(expC)expC.addEventListener('click',()=>{const e=expenseLookup[c.dataset.eid];if(e)showExpenseActions(e,nm)});
    });
    el.querySelectorAll('[data-sid]:not(.deleted-card)').forEach(c=>{
        const expC=c.querySelector('.exp-c');
        if(expC)expC.addEventListener('click',()=>{const s=settlementLookup[c.dataset.sid];if(s)showSettlementActions(s,nm)});
    });
    // Note toggle handlers
    el.querySelectorAll('.exp-note-indicator').forEach(t=>{
        t.addEventListener('click',ev=>{
            ev.stopPropagation();
            const target=document.getElementById(t.dataset.noteTarget);
            if(target)target.style.display=target.style.display==='none'?'':'none';
        });
    });
    // History toggle handlers
    el.querySelectorAll('.history-toggle').forEach(t=>{
        t.addEventListener('click',ev=>{
            ev.stopPropagation();
            const target=document.getElementById(t.dataset.histTarget);
            if(target){target.style.display=target.style.display==='none'?'':'none';t.textContent=target.style.display==='none'?t.textContent.replace('\u25B2','\u25BC'):t.textContent.replace('\u25BC','\u25B2')}
        });
    });
    // Restore button handlers
    el.querySelectorAll('[data-restore-eid]').forEach(b=>{
        b.addEventListener('click',ev=>{ev.stopPropagation();restoreExpense(b.dataset.restoreEid)});
    });
    el.querySelectorAll('[data-restore-sid]').forEach(b=>{
        b.addEventListener('click',ev=>{ev.stopPropagation();restoreSettlement(b.dataset.restoreSid)});
    });
}
function wireExpFilters(){
    const searchEl=document.getElementById('exp-search');
    if(searchEl)searchEl.addEventListener('input',e=>{expSearchQuery=e.target.value;renderExpenses()});
    document.querySelectorAll('.filter-pill[data-tag]').forEach(p=>{
        p.addEventListener('click',()=>{expTagFilter=expTagFilter===p.dataset.tag?null:p.dataset.tag;renderExpenses()});
    });
    const dateEl=document.getElementById('exp-date-filter');
    if(dateEl)dateEl.addEventListener('change',e=>{expDateFilter=e.target.value;renderExpenses()});
}

async function renderBalances(){
    const exps=(await iA('expenses')).filter(e=>e.groupId===curGroup&&!e.isDeleted);
    const sets=(await iA('settlements')).filter(s=>s.groupId===curGroup&&!s.isDeleted);
    const mems=(await iA('members')).filter(m=>m.groupId===curGroup);const nm={};mems.forEach(m=>nm[m.memberId]=m.displayName);
    const bal=calcBal(exps,sets),debts=simplify(bal);
    const el=document.getElementById('t-bal');
    let h='<div class="sec-t">Net Balances</div><div class="card" style="cursor:default">';
    const ent=Object.entries(bal);
    if(!ent.length)h+='<p style="text-align:center;padding:24px;color:var(--text-secondary);font-weight:600">All settled up! &#x1F389;</p>';
    else h+=ent.map(([id,a])=>{const col=getColor(id);return`<div class="bal-r"><div class="bal-name"><div class="bal-avatar" style="background:${col}">${getInitial(nm[id]||id)}</div><span style="font-weight:600">${esc(nm[id]||id.slice(0,8))}${id===myId?' <span class="pill" style="background:var(--primary-bg);color:var(--primary)">you</span>':''}</span></div><span class="${a>=0?'bp':'bn'}">${a>0?'+':''}${fmt(a)}</span></div>`}).join('');
    h+='</div>';
    if(debts.length){h+='<div class="sec-t">Settlements Needed</div>';h+=debts.map(d=>{const fn=d.from===myId?'You':esc(nm[d.from]||d.from.slice(0,8)),tn=d.to===myId?'You':esc(nm[d.to]||d.to.slice(0,8)),mine=d.from===myId||d.to===myId;return`<div class="card debt-c" style="${mine?'border:2px solid var(--primary);background:var(--primary-bg)':''}"><div><div class="card-t" style="font-size:15px">${fn} &#x2192; ${tn}${mine?' <span class="pill" style="background:var(--primary);color:#fff">you</span>':''}</div><div style="color:var(--primary);font-weight:800;font-size:18px;margin-top:4px">${fmt(d.amountCents)}</div></div><button class="settle-b" data-f="${d.from}" data-t="${d.to}" data-a="${d.amountCents}" style="${mine?'':'background:var(--text-secondary);opacity:0.7'}">${mine?'Settle':'Settle'}</button></div>`}).join('')}
    el.innerHTML=h;
    el.querySelectorAll('.settle-b').forEach(b=>b.addEventListener('click',()=>openSettle(b.dataset.f,b.dataset.t,+b.dataset.a)));
}

async function renderMembers(){
    const mems=(await iA('members')).filter(m=>m.groupId===curGroup&&!m.isDeleted).sort((a,b)=>a.joinedAt-b.joinedAt);
    const g=await iG('groups',curGroup);const el=document.getElementById('t-mem');
    let h='<div class="sec-t">Members ('+mems.length+')</div>';
    h+=mems.map(m=>{const col=getColor(m.memberId);return`<div class="card member-card"><div class="member-avatar" style="background:${col}">${getInitial(m.displayName)}</div><div><div class="card-t">${esc(m.displayName)}${m.memberId===myId?' <span class="pill" style="background:var(--primary-bg);color:var(--primary)">you</span>':''}</div><div class="card-s">Joined ${new Date(m.joinedAt).toLocaleDateString()}</div></div></div>`}).join('');
    h+='<div style="margin-top:24px"><button class="btn btn-o" id="invite-btn">&#x1F4E8; Invite Friends</button></div>';
    el.innerHTML=h;
    document.getElementById('invite-btn').addEventListener('click',async()=>{const link=await makeShortLink(curGroup,curGroupKey,g.name);document.getElementById('inv-display2').textContent=link;document.getElementById('inv-qr-img').src=generateQrDataUrl(link);document.getElementById('share-btn2').onclick=()=>shareLink(link,g.name);document.getElementById('qr-btn-inv').onclick=()=>showQrModal(link,g.name);show('invite')});
}

let groupAnalyticsViewMode='group';
async function renderGroupAnalytics(){
    const isPersonal=curGroupType==='personal';
    const exps=(await iA('expenses')).filter(e=>e.groupId===curGroup&&!e.isDeleted);
    const sets=(await iA('settlements')).filter(s=>s.groupId===curGroup&&!s.isDeleted);
    const mems=(await iA('members')).filter(m=>m.groupId===curGroup);
    const nm={};mems.forEach(m=>nm[m.memberId]=m.displayName);
    const el=document.getElementById('t-analytics');
    const isYours=groupAnalyticsViewMode==='yours';

    // View toggle: Group / Yours
    let html=`<div class="seg-control" id="group-analytics-view-toggle" style="margin:16px 16px 0"><button class="seg-btn${!isYours?' active':''}" data-mode="group">Group</button><button class="seg-btn${isYours?' active':''}" data-mode="yours">Yours</button></div>`;

    if(!exps.length){html+='<div class="empty"><div class="ic">&#x1F4CA;</div><h3>No data yet</h3><p>Add expenses to see analytics</p></div>';el.innerHTML=html;el.querySelectorAll('#group-analytics-view-toggle .seg-btn').forEach(b=>b.addEventListener('click',()=>{groupAnalyticsViewMode=b.dataset.mode;renderGroupAnalytics()}));return}

    const tagColors={'food':'#7C6FE0','transport':'#89C4F4','shopping':'#F2A0C4','entertainment':'#F8C4A4',
        'travel':'#BB8FCE','bills':'#F7DC6F','groceries':'#8DDCC5','health':'#F1948A','rent':'#F8C4A4','other':'#BDC3C7'};

    // Total spending
    const totalGroupSpend=exps.reduce((a,e)=>a+e.amountCents,0);
    const totalYourShare=exps.reduce((a,e)=>a+getMyShare(e,myId),0);
    const displayTotal=isYours?totalYourShare:totalGroupSpend;
    const totalLabel=isYours?'Your Spending':'Total Group Spending';

    // By Category
    const tagTotals={};
    const tagMyTotals={};
    exps.forEach(e=>{const t=e.tag||'other';tagTotals[t]=(tagTotals[t]||0)+e.amountCents;tagMyTotals[t]=(tagMyTotals[t]||0)+getMyShare(e,myId)});
    const tagAmts=isYours?tagMyTotals:tagTotals;
    const tagSorted=Object.entries(tagAmts).sort((a,b)=>b[1]-a[1]);

    // By Month
    const monthTotals={};
    const monthMyTotals={};
    const monthExpCounts={};
    const now=new Date();
    const monthNames=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    for(let i=5;i>=0;i--){
        const d=new Date(now.getFullYear(),now.getMonth()-i,1);
        const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
        monthTotals[key]=0;
        monthMyTotals[key]=0;
        monthExpCounts[key]=0;
    }
    exps.forEach(e=>{
        const d=new Date(e.createdAt);
        const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
        if(key in monthTotals){monthTotals[key]+=e.amountCents;monthMyTotals[key]+=getMyShare(e,myId);monthExpCounts[key]++}
    });
    const monthAmts=isYours?monthMyTotals:monthTotals;
    const monthKeys=Object.keys(monthAmts);

    // Settlement totals
    const settledTotal=sets.reduce((a,s)=>a+s.amountCents,0);
    const bal=calcBal(exps,sets);
    let outstandingTotal=0;
    for(const[id,b] of Object.entries(bal)){
        if(b<0) outstandingTotal+=Math.abs(b);
    }

    // 1. Rich Summary Card
    html+=buildAnalyticsSummaryCard({isYours,totalGroupSpend,totalYourShare,displayTotal,totalLabel,allExps:exps,tagAmts,tagSorted,monthAmts,settledTotal,outstandingTotal,tagColors,monthKeys});

    // 2. By Category (Donut/Bars toggle)
    html+=buildCategorySection(tagSorted,tagAmts,displayTotal,tagColors,'ga',groupAnalyticsCatView);

    // 3. By Month (Chart/Table toggle)
    html+=buildMonthSection(monthAmts,monthKeys,monthNames,monthExpCounts,'ga',groupAnalyticsMonthView);

    // 4. Category by Month (stacked bars with legend)
    html+=`<div class="analytics-card"><h3>&#x1F4CA; Category by Month</h3>`;
    const allTags=[...new Set(exps.map(e=>e.tag||'other'))];
    html+=`<div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px">`;
    allTags.forEach(tag=>{const t=TAGS[tag]||TAGS.other;const color=tagColors[tag]||'#BDC3C7';html+=`<span style="display:inline-flex;align-items:center;gap:4px;font-size:11px;color:var(--text-secondary)"><span class="donut-legend-dot" style="background:${color};border-radius:2px"></span>${t.label}</span>`});
    html+=`</div>`;
    const monthCatData={};
    Object.keys(monthTotals).forEach(key=>{monthCatData[key]={}});
    exps.forEach(e=>{
        const d=new Date(e.createdAt);
        const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
        const tag=e.tag||'other';
        if(key in monthCatData){
            const amt=isYours?getMyShare(e,myId):e.amountCents;
            monthCatData[key][tag]=(monthCatData[key][tag]||0)+amt;
        }
    });
    const monthCatTotals={};
    Object.entries(monthCatData).forEach(([key,cats])=>{monthCatTotals[key]=Object.values(cats).reduce((a,b)=>a+b,0)});
    const monthCatMax=Math.max(...Object.values(monthCatTotals),1);
    Object.entries(monthCatData).forEach(([key,cats])=>{
        const m=parseInt(key.split('-')[1])-1;
        const yr=key.split('-')[0].slice(2);
        const total=monthCatTotals[key]||0;
        html+=`<div style="margin-bottom:10px"><div style="display:flex;justify-content:space-between;margin-bottom:4px"><span style="font-size:13px;font-weight:600">${monthNames[m]} '${yr}</span><span style="font-size:12px;color:var(--text-secondary)">${fmt(total)}</span></div>`;
        html+=`<div style="display:flex;height:24px;border-radius:6px;overflow:hidden;background:var(--bg-secondary)">`;
        if(total>0){
            const catEntries=Object.entries(cats).sort((a,b)=>b[1]-a[1]);
            catEntries.forEach(([tag,amt])=>{
                const color=tagColors[tag]||'#BDC3C7';
                const segPct=(amt/monthCatMax*100);
                html+=`<div style="width:${segPct}%;background:${color};min-width:${segPct>2?0:2}px" title="${(TAGS[tag]||TAGS.other).label}: ${fmt(amt)}"></div>`;
            });
        }
        html+=`</div></div>`;
    });
    html+=`</div>`;

    // 5. By Member — Donut chart (skip for personal groups, hidden in "yours" mode)
    if(!isPersonal&&!isYours){
        const memberPaid={};
        exps.forEach(e=>{
            if(e.paidByMap){for(const[id,a] of Object.entries(e.paidByMap))memberPaid[id]=(memberPaid[id]||0)+a}
            else{memberPaid[e.paidBy]=(memberPaid[e.paidBy]||0)+e.amountCents}
        });
        if(Object.keys(memberPaid).length){
            html+=buildMemberSection(memberPaid,nm,'ga',groupAnalyticsMemberView);
        }
    }

    el.innerHTML=html;

    // Event listeners
    el.querySelectorAll('#group-analytics-view-toggle .seg-btn').forEach(b=>b.addEventListener('click',()=>{groupAnalyticsViewMode=b.dataset.mode;renderGroupAnalytics()}));
    el.querySelectorAll('.chart-icon-btn[data-catview][data-prefix="ga"]').forEach(b=>b.addEventListener('click',()=>{groupAnalyticsCatView=b.dataset.catview;renderGroupAnalytics()}));
    el.querySelectorAll('.chart-icon-btn[data-monthview][data-prefix="ga"]').forEach(b=>b.addEventListener('click',()=>{groupAnalyticsMonthView=b.dataset.monthview;renderGroupAnalytics()}));
    el.querySelectorAll('.chart-icon-btn[data-memberview][data-prefix="ga"]').forEach(b=>b.addEventListener('click',()=>{groupAnalyticsMemberView=b.dataset.memberview;renderGroupAnalytics()}));
}

let aePayerMode='single',aeSplitMode='equal',aeMembers=[];
async function renderAddExpense(){
    editingExpense=null;
    aePayerMode='single';aeSplitMode='equal';
    document.getElementById('ae-btn').textContent='Add Expense';
    document.querySelector('#s-ae .topbar h1').textContent='Add Expense';
    aeMembers=(await iA('members')).filter(m=>m.groupId===curGroup&&!m.isDeleted);
    // Tags
    document.getElementById('ae-tags').innerHTML=Object.entries(TAGS).map(([k,v])=>`<button class="tag-pill${k==='other'?' active':''}" data-tag="${k}" style="background:${v.color}"><span class="tag-icon">${v.icon}</span>${v.label}</button>`).join('');
    document.getElementById('ae-tags').querySelectorAll('.tag-pill').forEach(p=>p.addEventListener('click',()=>{document.querySelectorAll('#ae-tags .tag-pill').forEach(t=>t.classList.remove('active'));p.classList.add('active')}));
    // Payer toggle
    document.querySelectorAll('.payer-toggle .seg-btn').forEach(b=>{b.classList.toggle('active',b.dataset.mode==='single');b.onclick=()=>{aePayerMode=b.dataset.mode;document.querySelectorAll('.payer-toggle .seg-btn').forEach(x=>x.classList.toggle('active',x===b));document.getElementById('ae-paidby').style.display=aePayerMode==='single'?'':'none';document.getElementById('ae-paidby-multi').style.display=aePayerMode==='multi'?'':'none';renderMultiPayer();updAe()}});
    // Single payer radios
    document.getElementById('ae-paidby').innerHTML=aeMembers.map(m=>`<div class="chk-r"><input type="radio" name="pb" value="${m.memberId}" ${m.memberId===myId?'checked':''}><span>${esc(m.displayName)}${m.memberId===myId?' (you)':''}</span></div>`).join('');
    document.getElementById('ae-paidby').style.display='';
    document.getElementById('ae-paidby-multi').style.display='none';
    document.getElementById('ae-paidby-validation').style.display='none';
    // Split mode buttons
    document.querySelectorAll('#ae-split-modes .seg-btn').forEach(b=>{b.classList.toggle('active',b.dataset.mode==='equal');b.onclick=()=>{aeSplitMode=b.dataset.mode;document.querySelectorAll('#ae-split-modes .seg-btn').forEach(x=>x.classList.toggle('active',x===b));renderSplitDetail();updAe()}});
    // Split checkboxes
    document.getElementById('ae-split').innerHTML=aeMembers.map(m=>`<div class="chk-r"><input type="checkbox" class="sc" value="${m.memberId}" checked><span>${esc(m.displayName)}${m.memberId===myId?' (you)':''}</span></div>`).join('');
    document.getElementById('ae-split-detail').style.display='none';
    document.getElementById('ae-split-detail').innerHTML='';
    document.getElementById('ae-split-validation').style.display='none';
    document.getElementById('ae-desc').value='';document.getElementById('ae-amt').value='';document.getElementById('ae-notes').value='';updAe();
}
function renderMultiPayer(){
    if(aePayerMode!=='multi')return;
    const el=document.getElementById('ae-paidby-multi');
    el.innerHTML=aeMembers.map(m=>{const col=getColor(m.memberId);return`<div class="member-input-row"><div class="mi-avatar" style="background:${col}">${getInitial(m.displayName)}</div><span class="mi-name">${esc(m.displayName)}${m.memberId===myId?' (you)':''}</span><span style="font-size:14px;color:var(--text-secondary);font-weight:700">&#x20B9;</span><input type="number" class="mp-amt" data-mid="${m.memberId}" placeholder="0.00" step="0.01" min="0" inputmode="decimal"></div>`}).join('')+'<div id="mp-remaining" class="split-validation" style="display:none"></div>';
    el.querySelectorAll('.mp-amt').forEach(i=>i.addEventListener('input',()=>{updateMultiPayerValidation();updAe()}));
}
function updateMultiPayerValidation(){
    const total=Math.round((parseFloat(document.getElementById('ae-amt').value)||0)*100);
    const inputs=document.querySelectorAll('.mp-amt');
    let sum=0;inputs.forEach(i=>sum+=Math.round((parseFloat(i.value)||0)*100));
    const rem=total-sum;const el=document.getElementById('mp-remaining');const valEl=document.getElementById('ae-paidby-validation');
    if(total<=0){if(el)el.style.display='none';valEl.style.display='none';return}
    if(el){el.style.display='';el.className='split-validation'+(rem===0?' ok':' error');el.textContent=rem===0?'\u2713 Fully allocated':'Remaining: '+fmt(rem)}
    valEl.style.display=rem!==0?'':'none';valEl.className='split-validation error';valEl.textContent=rem!==0?'Amounts don\'t add up to total':'';
}
function renderSplitDetail(){
    const detailEl=document.getElementById('ae-split-detail');const valEl=document.getElementById('ae-split-validation');
    if(aeSplitMode==='equal'){detailEl.style.display='none';detailEl.innerHTML='';valEl.style.display='none';document.getElementById('ae-split').style.display='';return}
    document.getElementById('ae-split').style.display='';
    detailEl.style.display='';
    const checked=getCheckedSplitMembers();
    const suffix=aeSplitMode==='percentage'?'%':aeSplitMode==='ratio'?'x':'';
    const placeholder=aeSplitMode==='percentage'?'0':aeSplitMode==='ratio'?'1':'0.00';
    const step=aeSplitMode==='amount'?'0.01':'1';
    const pre=aeSplitMode==='amount'?'<span style="font-size:14px;color:var(--text-secondary);font-weight:700">&#x20B9;</span>':'';
    detailEl.innerHTML=checked.map(m=>{const col=getColor(m.id);return`<div class="member-input-row"><div class="mi-avatar" style="background:${col}">${getInitial(m.name)}</div><span class="mi-name">${esc(m.name)}</span>${pre}<input type="number" class="sd-val" data-mid="${m.id}" placeholder="${placeholder}" step="${step}" min="0" inputmode="decimal"><span class="mi-suffix">${suffix}</span></div>`}).join('');
    detailEl.querySelectorAll('.sd-val').forEach(i=>i.addEventListener('input',()=>{updateSplitValidation();updAe()}));
    updateSplitValidation();
}
function getCheckedSplitMembers(){
    const checked=[...document.querySelectorAll('.sc:checked')].map(c=>c.value);
    return aeMembers.filter(m=>checked.includes(m.memberId)).map(m=>({id:m.memberId,name:m.displayName+(m.memberId===myId?' (you)':'')}));
}
function updateSplitValidation(){
    const valEl=document.getElementById('ae-split-validation');
    if(aeSplitMode==='equal'){valEl.style.display='none';return}
    const total=Math.round((parseFloat(document.getElementById('ae-amt').value)||0)*100);
    const inputs=document.querySelectorAll('.sd-val');
    if(aeSplitMode==='amount'){
        let sum=0;inputs.forEach(i=>sum+=Math.round((parseFloat(i.value)||0)*100));
        const rem=total-sum;
        if(total<=0){valEl.style.display='none';return}
        valEl.style.display='';valEl.className='split-validation'+(rem===0?' ok':' error');
        valEl.textContent=rem===0?'\u2713 Fully allocated':'Remaining: '+fmt(rem);
    }else if(aeSplitMode==='percentage'){
        let sum=0;inputs.forEach(i=>sum+=(parseFloat(i.value)||0));
        valEl.style.display='';valEl.className='split-validation'+(Math.abs(sum-100)<0.01?' ok':' error');
        valEl.textContent=Math.abs(sum-100)<0.01?'\u2713 Total 100%':'Total: '+sum.toFixed(1)+'% (need 100%)';
    }else if(aeSplitMode==='ratio'){
        let any=false;inputs.forEach(i=>{if(parseFloat(i.value)>0)any=true});
        valEl.style.display='';valEl.className='split-validation'+(any?' ok':' error');
        valEl.textContent=any?'\u2713 Ratio set':'Enter at least one ratio';
    }
}
function updAe(){
    const d=document.getElementById('ae-desc').value.trim();
    const a=parseFloat(document.getElementById('ae-amt').value)||0;
    const amtC=Math.round(a*100);
    const c=document.querySelectorAll('.sc:checked').length;
    let valid=!!d&&a>0&&c>0;
    // Multi-payer validation
    if(aePayerMode==='multi'&&amtC>0){
        let sum=0;document.querySelectorAll('.mp-amt').forEach(i=>sum+=Math.round((parseFloat(i.value)||0)*100));
        if(sum!==amtC)valid=false;
    }
    // Split validation
    if(aeSplitMode==='amount'&&amtC>0){
        let sum=0;document.querySelectorAll('.sd-val').forEach(i=>sum+=Math.round((parseFloat(i.value)||0)*100));
        if(sum!==amtC)valid=false;
    }else if(aeSplitMode==='percentage'){
        let sum=0;document.querySelectorAll('.sd-val').forEach(i=>sum+=(parseFloat(i.value)||0));
        if(Math.abs(sum-100)>=0.01)valid=false;
    }else if(aeSplitMode==='ratio'){
        const inputs=document.querySelectorAll('.sd-val');
        if(inputs.length>0){let any=false;inputs.forEach(i=>{if(parseFloat(i.value)>0)any=true});if(!any)valid=false}
    }
    document.getElementById('ae-btn').disabled=!valid;
    // Re-validate displays when amount changes
    if(aePayerMode==='multi')updateMultiPayerValidation();
    if(aeSplitMode!=='equal')updateSplitValidation();
}

function showExpenseActions(expense,nm){
    const ts=getTagStyle(expense.tag);const st=ts?{icon:ts.icon,bg:ts.bg}:getExpStyle(expense.description);
    let paidStr;
    if(expense.paidByMap){const names=Object.keys(expense.paidByMap).map(id=>esc(nm[id]||id.slice(0,8)));paidStr='Paid by '+names.join(', ')}
    else{paidStr='Paid by '+esc(nm[expense.paidBy]||expense.paidBy.slice(0,8))}
    const noteHtml=expense.notes?`<div class="exp-note-text" style="margin-bottom:14px">${esc(expense.notes)}</div>`:'';
    openModal(`
        <div class="modal-summary">
            <div class="exp-icon" style="background:${st.bg}">${st.icon}</div>
            <div style="flex:1">
                <div style="font-weight:700;font-size:15px">${esc(expense.description)}</div>
                <div style="font-size:13px;color:var(--text-secondary);margin-top:2px">${paidStr}</div>
            </div>
            <div class="modal-amt">${fmt(expense.amountCents)}</div>
        </div>
        ${noteHtml}
        <button class="modal-btn modal-btn-edit" onclick="editExpense('${expense.expenseId}')">&#x270F;&#xFE0F; Edit Expense</button>
        <button class="modal-btn modal-btn-delete" onclick="confirmDeleteExpense('${expense.expenseId}')">&#x1F5D1;&#xFE0F; Delete Expense</button>
        <button class="modal-btn modal-btn-cancel" onclick="closeModal()">Cancel</button>
    `);
}

function confirmDeleteExpense(eid){
    openModal(`
        <div style="text-align:center;padding:12px 0 8px">
            <div style="font-size:40px;margin-bottom:12px">&#x1F5D1;&#xFE0F;</div>
            <h3 style="font-size:17px;font-weight:800;letter-spacing:-0.3px">Delete this expense?</h3>
            <p style="color:var(--text-secondary);font-size:14px;margin-top:8px;font-weight:500">Balances will be recalculated.</p>
        </div>
        <button class="modal-btn modal-btn-delete" onclick="deleteExpense('${eid}')">Delete Expense</button>
        <button class="modal-btn modal-btn-cancel" onclick="closeModal()">Cancel</button>
    `);
}

async function deleteExpense(eid){
    const expense=expenseLookup[eid];if(!expense)return;
    const now=Date.now();
    // Save history before deleting
    const prevData={...expense};delete prevData.hlcTimestamp;
    await saveHistory(makeHistoryEntry({expenseId:eid,entityType:'expense',action:'deleted',previousData:prevData,newData:null}));
    const data={...expense,isDeleted:true};
    delete data.hlcTimestamp;
    await iP('expenses',{...data,hlcTimestamp:now});
    await pushOp(curGroup,curGroupKey,{id:uid(),type:'expense',data,hlc:now,author:myId});
    closeModal();refreshTab();toast('Expense deleted');
}

function editExpense(eid){
    const expense=expenseLookup[eid];if(!expense)return;
    editingExpense=expense;
    closeModal();
    show('ae');
    renderAddExpense().then(()=>prefillExpense(expense));
}

function prefillExpense(expense){
    document.getElementById('ae-desc').value=expense.description;
    document.getElementById('ae-amt').value=(expense.amountCents/100).toFixed(2);
    document.getElementById('ae-notes').value=expense.notes||'';
    // Tag
    if(expense.tag){
        document.querySelectorAll('#ae-tags .tag-pill').forEach(t=>{t.classList.toggle('active',t.dataset.tag===expense.tag)});
    }
    // Payer mode
    if(expense.paidByMap){
        aePayerMode='multi';
        document.querySelectorAll('.payer-toggle .seg-btn').forEach(b=>{b.classList.toggle('active',b.dataset.mode==='multi')});
        document.getElementById('ae-paidby').style.display='none';
        document.getElementById('ae-paidby-multi').style.display='';
        renderMultiPayer();
        // Fill multi-payer amounts
        for(const[mid,amt] of Object.entries(expense.paidByMap)){
            const inp=document.querySelector(`.mp-amt[data-mid="${mid}"]`);
            if(inp)inp.value=(amt/100).toFixed(2);
        }
        updateMultiPayerValidation();
    }else{
        // Single payer
        const radio=document.querySelector(`input[name=pb][value="${expense.paidBy}"]`);
        if(radio)radio.checked=true;
    }
    // Split among
    document.querySelectorAll('.sc').forEach(cb=>{cb.checked=expense.splitAmong.includes(cb.value)});
    // Split mode
    if(expense.splitMode&&expense.splitMode!=='equal'){
        aeSplitMode=expense.splitMode;
        document.querySelectorAll('#ae-split-modes .seg-btn').forEach(b=>{b.classList.toggle('active',b.dataset.mode===expense.splitMode)});
        renderSplitDetail();
        // Fill split detail values
        if(expense.splitDetails){
            if(expense.splitMode==='amount'){
                for(const[mid,amt] of Object.entries(expense.splitDetails)){
                    const inp=document.querySelector(`.sd-val[data-mid="${mid}"]`);
                    if(inp)inp.value=(amt/100).toFixed(2);
                }
            }else if(expense.splitMode==='percentage'){
                // Reverse percentage: splitDetails are in cents, compute %
                const total=expense.amountCents;
                for(const[mid,amt] of Object.entries(expense.splitDetails)){
                    const inp=document.querySelector(`.sd-val[data-mid="${mid}"]`);
                    if(inp)inp.value=total>0?(amt/total*100).toFixed(1):'0';
                }
            }else if(expense.splitMode==='ratio'){
                // Ratio: we stored cents, try to reconstruct ratios
                // Find GCD of all values to simplify
                const vals=Object.values(expense.splitDetails);
                const gcd=(a,b)=>b?gcd(b,a%b):a;
                const g=vals.reduce((a,b)=>gcd(a,b));
                for(const[mid,amt] of Object.entries(expense.splitDetails)){
                    const inp=document.querySelector(`.sd-val[data-mid="${mid}"]`);
                    if(inp)inp.value=g>0?Math.round(amt/g):'1';
                }
            }
        }
        updateSplitValidation();
    }
    // Change button text
    document.getElementById('ae-btn').textContent='Save Changes';
    // Update title
    document.querySelector('#s-ae .topbar h1').textContent='Edit Expense';
    updAe();
}

function showSettlementActions(settlement,nm){
    const fromName=esc(nm[settlement.fromMember]||settlement.fromMember.slice(0,8));
    const toName=esc(nm[settlement.toMember]||settlement.toMember.slice(0,8));
    openModal(`
        <div style="text-align:center;padding:12px 0 8px">
            <div style="font-size:40px;margin-bottom:12px">&#x1F91D;</div>
            <h3 style="font-size:17px;font-weight:800;letter-spacing:-0.3px">Undo this settlement?</h3>
            <p style="color:var(--text-secondary);font-size:14px;margin-top:8px;font-weight:500">${fromName} paid ${toName} &mdash; ${fmt(settlement.amountCents)}</p>
            <p style="color:var(--text-secondary);font-size:13px;margin-top:4px;font-weight:500">The debt will reappear.</p>
        </div>
        <button class="modal-btn modal-btn-undo" onclick="undoSettlement('${settlement.settlementId}')">Undo Settlement</button>
        <button class="modal-btn modal-btn-cancel" onclick="closeModal()">Cancel</button>
    `);
}

async function undoSettlement(sid){
    const settlement=settlementLookup[sid];if(!settlement)return;
    const now=Date.now();
    // Save history before undoing
    const prevData={...settlement};delete prevData.hlcTimestamp;
    await saveHistory(makeHistoryEntry({settlementId:sid,entityType:'settlement',action:'deleted',previousData:prevData,newData:null}));
    const data={...settlement,isDeleted:true};
    delete data.hlcTimestamp;
    await iP('settlements',{...data,hlcTimestamp:now});
    await pushOp(curGroup,curGroupKey,{id:uid(),type:'settlement',data,hlc:now,author:myId});
    closeModal();refreshTab();toast('Settlement undone');
}

let settleData={};
async function openSettle(from,to,amt){
    settleData={from,to,amountCents:amt};
    const mems=(await iA('members')).filter(m=>m.groupId===curGroup);
    const nm={};mems.forEach(m=>nm[m.memberId]=m.displayName);
    document.getElementById('stl-who').textContent=`${nm[from]||from.slice(0,8)} pays ${nm[to]||to.slice(0,8)}`;
    const input=document.getElementById('stl-input');
    input.value=(amt/100).toFixed(2);
    input.max=(amt/100).toFixed(2);
    updateSettleHint(amt);
    show('settle');
}
function updateSettleHint(totalCents){
    const input=document.getElementById('stl-input');
    const val=Math.round((parseFloat(input.value)||0)*100);
    const hint=document.getElementById('stl-partial-hint');
    if(val<totalCents&&val>0){
        hint.textContent='Settling '+fmt(val)+' of '+fmt(totalCents)+' total';
        hint.style.display='';
    }else if(val===totalCents){
        hint.textContent='Full settlement';
        hint.style.display='';
    }else{
        hint.textContent='';
        hint.style.display='none';
    }
    document.getElementById('stl-btn').disabled=val<=0||val>totalCents;
}

// Analytics (legacy stack screen)
let legacyAnalyticsCatView='donut';
let legacyAnalyticsMonthView='chart';
let legacyAnalyticsMemberView='donut';
async function renderAnalytics(){
    const allExps=(await iA('expenses')).filter(e=>!e.isDeleted);
    const allSets=(await iA('settlements')).filter(s=>!s.isDeleted);
    const allMems=await iA('members');
    const groups=await iA('groups');
    const globalNm={};allMems.forEach(m=>globalNm[m.memberId]=m.displayName);
    const el=document.getElementById('analytics-content');

    if(!allExps.length){el.innerHTML='<div class="empty"><div class="ic">&#x1F4CA;</div><h3>No data yet</h3><p>Add expenses to see analytics</p></div>';return}

    const tagColors={'food':'#7C6FE0','transport':'#89C4F4','shopping':'#F2A0C4','entertainment':'#F8C4A4',
        'travel':'#BB8FCE','bills':'#F7DC6F','groceries':'#8DDCC5','health':'#F1948A','rent':'#F8C4A4','other':'#BDC3C7'};

    let html='';

    // Totals
    const tagTotals={};
    allExps.forEach(e=>{const t=e.tag||'other';tagTotals[t]=(tagTotals[t]||0)+e.amountCents});
    const totalSpend=Object.values(tagTotals).reduce((a,b)=>a+b,0);
    const totalYourShare=allExps.reduce((a,e)=>a+getMyShare(e,myId),0);
    const tagSorted=Object.entries(tagTotals).sort((a,b)=>b[1]-a[1]);

    // Month data
    const monthTotals={};
    const monthExpCounts={};
    const now=new Date();
    const monthNames=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    for(let i=5;i>=0;i--){
        const d=new Date(now.getFullYear(),now.getMonth()-i,1);
        const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
        monthTotals[key]=0;
        monthExpCounts[key]=0;
    }
    allExps.forEach(e=>{
        const d=new Date(e.createdAt);
        const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
        if(key in monthTotals){monthTotals[key]+=e.amountCents;monthExpCounts[key]++}
    });
    const monthKeys=Object.keys(monthTotals);

    // Settlement totals
    const settledTotal=allSets.reduce((a,s)=>a+s.amountCents,0);
    let outstandingTotal=0;
    for(const g of groups){
        const gExps=allExps.filter(e=>e.groupId===g.groupId);
        const gSets=allSets.filter(s=>s.groupId===g.groupId);
        const bal=calcBal(gExps,gSets);
        for(const[id,b] of Object.entries(bal)){
            if(b<0) outstandingTotal+=Math.abs(b);
        }
    }

    // 1. Rich Summary Card
    html+=buildAnalyticsSummaryCard({isYours:false,totalGroupSpend:totalSpend,totalYourShare,displayTotal:totalSpend,totalLabel:'Total Group Spending',allExps,tagAmts:tagTotals,tagSorted,monthAmts:monthTotals,settledTotal,outstandingTotal,tagColors,monthKeys});

    // 2. By Category (Donut/Bars toggle)
    html+=buildCategorySection(tagSorted,tagTotals,totalSpend,tagColors,'la',legacyAnalyticsCatView);

    // 3. By Month (Chart/Table toggle)
    html+=buildMonthSection(monthTotals,monthKeys,monthNames,monthExpCounts,'la',legacyAnalyticsMonthView);

    // 4. By Member — Donut chart
    const memberPaid={};
    allExps.forEach(e=>{
        if(e.paidByMap){for(const[id,a] of Object.entries(e.paidByMap))memberPaid[id]=(memberPaid[id]||0)+a}
        else{memberPaid[e.paidBy]=(memberPaid[e.paidBy]||0)+e.amountCents}
    });
    if(Object.keys(memberPaid).length){
        html+=buildMemberSection(memberPaid,globalNm,'la',legacyAnalyticsMemberView);
    }

    el.innerHTML=html;

    // Event listeners
    el.querySelectorAll('.chart-icon-btn[data-catview][data-prefix="la"]').forEach(b=>b.addEventListener('click',()=>{legacyAnalyticsCatView=b.dataset.catview;renderAnalytics()}));
    el.querySelectorAll('.chart-icon-btn[data-monthview][data-prefix="la"]').forEach(b=>b.addEventListener('click',()=>{legacyAnalyticsMonthView=b.dataset.monthview;renderAnalytics()}));
    el.querySelectorAll('.chart-icon-btn[data-memberview][data-prefix="la"]').forEach(b=>b.addEventListener('click',()=>{legacyAnalyticsMemberView=b.dataset.memberview;renderAnalytics()}));
}
