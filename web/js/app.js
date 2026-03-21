// Join
async function handleJoin(){
    const params=new URLSearchParams(location.search);const sc=params.get('c'),gid=params.get('g'),hash=location.hash.substring(1);
    if(sc||gid)history.replaceState({},'',location.pathname);
    let g,k,n;
    if(sc){
        // Short link: ?c=code#key (key might be missing if # was stripped)
        const d=await resolveShort(sc);if(!d)return null;
        g=d.g;n=d.n||'Group';
        k=hash?decodeURIComponent(hash):null;
        if(!k){toast('This invite link is incomplete. Ask the sender to share it again, or scan the QR code.');return null}
    }else if(gid&&hash){
        // Full link: ?g=uuid#key|name
        g=gid;const p=hash.split('|');k=decodeURIComponent(p[0]);n=p[1]?decodeURIComponent(p[1]):'Group';
    }else if(gid&&!hash){
        // Full link but # stripped — can't recover key
        toast('Invalid invite link — ask for a new one');return null;
    }else{return null}

    const ex=await iG('groups',g);if(ex)return{groupId:g,action:'existing'};
    const now=Date.now();
    try{
        await iP('groups',{groupId:g,name:n,createdBy:'',createdAt:now,inviteToken:'',groupKeyBase64:k,hlcTimestamp:now,hlcNodeId:myId});
        await iP('members',{_key:g+'_'+myId,groupId:g,memberId:myId,displayName:myName,joinedAt:now,isDeleted:false,hlcTimestamp:now});
        await pushOp(g,k,{id:uid(),type:'member_join',data:{memberId:myId,displayName:myName,joinedAt:now},hlc:now,author:myId});
        return{groupId:g,action:'joined'};
    }catch(e){console.error('Join failed:',e);toast('Failed to join group');return null}
}

let obPassphrase='';
let aeRecurringFreq='monthly';
function showObPp(){document.getElementById('ob-pp-display').textContent=obPassphrase}
function closeExportModal(){document.getElementById('export-modal').style.display='none'}

// Add Reminder
let ldDirection='they-owe';
function initLogDebt(){
    ldDirection='they-owe';
    document.getElementById('ld-friend').value='';
    document.getElementById('ld-amount').value='';
    document.getElementById('ld-desc').value='';
    document.getElementById('ld-btn').disabled=true;
    document.querySelectorAll('#ld-direction .seg-btn').forEach(b=>{
        b.classList.toggle('active',b.dataset.dir==='they-owe');
    });
}
function updLd(){
    const friend=document.getElementById('ld-friend').value.trim();
    const amount=parseFloat(document.getElementById('ld-amount').value)||0;
    document.getElementById('ld-btn').disabled=!friend||amount<=0;
}

// Wire events
function wireEvents(){
    document.getElementById('cg-back').onclick=goBack;
    document.getElementById('gd-back').onclick=goHome;
    document.getElementById('btn-refresh').onclick=async()=>{if(curGroup&&curGroupKey){const btn=document.getElementById('btn-refresh');btn.classList.add('syncing');await fullSync(curGroup,curGroupKey);setTimeout(()=>btn.classList.remove('syncing'),600);toast('Synced!')}};
    document.getElementById('ae-back').onclick=()=>{editingExpense=null;goBack()};
    document.getElementById('stl-back').onclick=goBack;
    document.getElementById('inv-back').onclick=goBack;
    document.getElementById('sec-back').onclick=goBack;
    document.getElementById('inv-done').onclick=goBack;
    document.getElementById('scan-back').onclick=()=>{stopQrScanner();goBack()};
    document.getElementById('groups-scan-btn').onclick=()=>openQrScanner();
    document.getElementById('home-sync').onclick=()=>{document.getElementById('sg-pin').value='';document.getElementById('sg-btn').disabled=true;document.getElementById('sg-result').style.display='none';document.getElementById('sg-btn').style.display='';show('sync-gen')};
    document.getElementById('home-security').onclick=()=>show('security');
    document.getElementById('home-fab-scan').onclick=()=>openQrScanner();
    document.getElementById('home-fab-create').onclick=()=>{document.getElementById('cg-form').style.display='';document.getElementById('cg-done').style.display='none';document.getElementById('cg-name').value='';document.getElementById('cg-btn').disabled=true;show('cg')};
    document.getElementById('analytics-back').onclick=goBack;
    document.getElementById('ld-back').onclick=goBack;

    // Groups tab sync & security buttons
    document.getElementById('groups-sync-btn').onclick=()=>{
        document.getElementById('sg-pin').value='';
        document.getElementById('sg-btn').disabled=true;
        document.getElementById('sg-result').style.display='none';
        document.getElementById('sg-btn').style.display='';
        show('sync-gen');
    };
    document.getElementById('groups-security-btn').onclick=()=>show('security');

    // Add Reminder wiring (legacy)
    document.querySelectorAll('#ld-direction .seg-btn').forEach(b=>{
        b.addEventListener('click',()=>{
            ldDirection=b.dataset.dir;
            document.querySelectorAll('#ld-direction .seg-btn').forEach(x=>x.classList.toggle('active',x===b));
        });
    });
    document.getElementById('ld-friend').addEventListener('input',updLd);
    document.getElementById('ld-amount').addEventListener('input',updLd);
    document.getElementById('ld-btn').addEventListener('click',async()=>{
        const friendName=document.getElementById('ld-friend').value.trim();
        const amtC=Math.round((parseFloat(document.getElementById('ld-amount').value)||0)*100);
        const desc=document.getElementById('ld-desc').value.trim()||'Reminder';
        if(!friendName||amtC<=0)return;
        // Create or find a 2-person IOU group
        const allGroups=await iA('groups');
        let iouGroup=null;
        for(const g of allGroups){
            if(g.isIOU){
                const gMems=(await iA('members')).filter(m=>m.groupId===g.groupId&&!m.isDeleted);
                const names=gMems.map(m=>m.displayName.toLowerCase());
                if(names.includes(friendName.toLowerCase())&&gMems.some(m=>m.memberId===myId)){iouGroup=g;break}
            }
        }
        const now=Date.now();
        let gid,key;
        if(iouGroup){
            gid=iouGroup.groupId;key=iouGroup.groupKeyBase64;
        }else{
            // Create new IOU group
            gid=uid();key=await genKey();
            await iP('groups',{groupId:gid,name:friendName,createdBy:myId,createdAt:now,inviteToken:uid(),groupKeyBase64:key,hlcTimestamp:now,hlcNodeId:myId,isIOU:true,baseCurrency:myDefaultCurrency});
            await iP('members',{_key:gid+'_'+myId,groupId:gid,memberId:myId,displayName:myName,joinedAt:now,isDeleted:false,hlcTimestamp:now});
            await pushOp(gid,key,{id:uid(),type:'member_join',data:{memberId:myId,displayName:myName,joinedAt:now},hlc:now,author:myId});
            // Add friend as a virtual member
            const friendId='iou_'+shortId();
            await iP('members',{_key:gid+'_'+friendId,groupId:gid,memberId:friendId,displayName:friendName,joinedAt:now,isDeleted:false,hlcTimestamp:now});
            await pushOp(gid,key,{id:uid(),type:'member_join',data:{memberId:friendId,displayName:friendName,joinedAt:now},hlc:now,author:myId});
        }
        // Get friend's member id
        const gMems=(await iA('members')).filter(m=>m.groupId===gid&&!m.isDeleted);
        const friendMem=gMems.find(m=>m.memberId!==myId);
        if(!friendMem){toast('Error creating debt');return}
        // Create expense
        const eid=uid();
        const paidBy=ldDirection==='they-owe'?myId:friendMem.memberId;
        const splitAmong=[myId,friendMem.memberId];
        const splitDetails={};
        // The person who didn't pay owes the full amount
        if(ldDirection==='they-owe'){
            splitDetails[friendMem.memberId]=amtC;splitDetails[myId]=0;
        }else{
            splitDetails[myId]=amtC;splitDetails[friendMem.memberId]=0;
        }
        const data={expenseId:eid,groupId:gid,description:desc,amountCents:amtC,currency:'INR',paidBy,splitAmong,createdAt:now,isDeleted:false,tag:'other',paidByMap:null,splitMode:'amount',splitDetails,notes:null};
        await iP('expenses',{...data,hlcTimestamp:now});
        await pushOp(gid,key,{id:uid(),type:'expense',data,hlc:now,author:myId});
        goBack();toast('Debt logged!');if(TAB_SCREENS.includes(curScreen))switchNavTab(curScreen);
        autoBackup().catch(()=>{});
    });

    document.getElementById('sg-back').onclick=goBack;
    document.getElementById('rec-back').onclick=goBack;

    // Recovery passphrase setup (onboarding step 2)
    obPassphrase=generatePassphrase();
    document.getElementById('ob-pp-regen').onclick=()=>{obPassphrase=generatePassphrase();showObPp();document.getElementById('ob-pp-custom-input').style.display='none'};
    document.getElementById('ob-pp-custom').onclick=()=>{document.getElementById('ob-pp-custom-input').style.display='';document.getElementById('ob-pp-input').value='';document.getElementById('ob-pp-input').focus()};
    document.getElementById('ob-pp-input').addEventListener('input',e=>{
        const v=e.target.value;const errEl=document.getElementById('ob-pp-error');
        if(v.trim()&&!validatePassphrase(v)){errEl.textContent='Need at least 3 words';errEl.style.display=''}
        else{errEl.style.display='none';if(v.trim())obPassphrase=v.trim()}
        document.getElementById('ob-pp-display').textContent=validatePassphrase(v)?v.trim():obPassphrase;
    });
    document.getElementById('ob-pp-download').onclick=()=>downloadPassphrase(obPassphrase);
    document.getElementById('ob-pp-share').onclick=()=>sharePassphrase(obPassphrase);
    document.getElementById('ob-pp-confirm').addEventListener('change',e=>{document.getElementById('ob-pp-continue').disabled=!e.target.checked});
    document.getElementById('ob-pp-continue').onclick=async()=>{
        if(!validatePassphrase(obPassphrase)){toast('Passphrase needs at least 3 words');return}
        await backupToRecovery(obPassphrase);
        toast('Backed up!');
        // If coming from profile (already has identity), just go back
        if(myId&&hist.length){goBack();return}
        const jr=await handleJoin();
        switchNavTab('home');
        if(jr){await openGroup(jr.groupId);if(jr.action==='joined')toast('Joined group!')}
    };
    document.getElementById('ob-pp-skip').onclick=async()=>{
        // If coming from profile, just go back
        if(myId&&hist.length){goBack();return}
        const jr=await handleJoin();
        switchNavTab('home');
        if(jr){await openGroup(jr.groupId);if(jr.action==='joined')toast('Joined group!')}
    };

    // Recovery from passphrase
    document.getElementById('rec-phrase').addEventListener('input',e=>{document.getElementById('rec-btn').disabled=!validatePassphrase(e.target.value)});
    document.getElementById('rec-btn').onclick=async()=>{
        const pp=document.getElementById('rec-phrase').value;
        const statusEl=document.getElementById('rec-status');
        statusEl.style.display='';statusEl.style.color='#7F8C8D';statusEl.textContent='Searching...';
        const bundle=await recoverFromPassphrase(pp);
        if(!bundle){statusEl.style.color='var(--negative)';statusEl.textContent='No backup found for this passphrase';return}
        // Apply restore
        myId=bundle.id;myName=bundle.name;
        await iPK('identity','id',myId);await iPK('identity','name',myName);
        await iPK('identity','pp',pp.trim().toLowerCase());
        const now=Date.now();
        for(const g of bundle.groups){
            const ex=await iG('groups',g.i);
            if(!ex){
                await iP('groups',{groupId:g.i,name:g.n,createdBy:'',createdAt:now,inviteToken:'',groupKeyBase64:g.k,hlcTimestamp:now,hlcNodeId:myId});
                await iP('members',{_key:g.i+'_'+myId,groupId:g.i,memberId:myId,displayName:myName,joinedAt:now,isDeleted:false,hlcTimestamp:now});
            }
        }
        hist=[];switchNavTab('home');
        toast('Recovered '+bundle.groups.length+' group'+(bundle.groups.length!==1?'s':'')+'!');
    };
    document.getElementById('sr-back').onclick=goBack;

    // Sync generate
    const sgPin=document.getElementById('sg-pin');
    sgPin.addEventListener('input',()=>{const v=sgPin.value.replace(/\D/g,'').slice(0,6);sgPin.value=v;document.getElementById('sg-btn').disabled=v.length!==6});
    document.getElementById('sg-btn').addEventListener('click',async()=>{
        const pin=sgPin.value;
        try{
            const encBundle=await generateSyncBundle(pin);
            const code=genSyncCode();
            await uploadSyncBundle(code,encBundle);
            document.getElementById('sg-code').textContent=code;
            document.getElementById('sg-result').style.display='';
            document.getElementById('sg-btn').style.display='none';
            // Timer
            let sec=300;
            const timerEl=document.getElementById('sg-timer');
            const iv=setInterval(()=>{sec--;if(sec<=0){clearInterval(iv);timerEl.textContent='Expired';return}timerEl.textContent=Math.floor(sec/60)+':'+String(sec%60).padStart(2,'0')},1000);
        }catch(e){toast('Failed to generate code');console.error(e)}
    });

    // Sync restore
    const srCode=document.getElementById('sr-code'),srPin=document.getElementById('sr-pin');
    function updSrBtn(){const c=srCode.value.replace(/[^A-Za-z0-9]/g,'').length>=8,p=srPin.value.replace(/\D/g,'').length===6;document.getElementById('sr-btn').disabled=!(c&&p)}
    srCode.addEventListener('input',()=>{let v=srCode.value.toUpperCase().replace(/[^A-Z0-9]/g,'');if(v.length>4)v=v.slice(0,4)+'-'+v.slice(4,8);srCode.value=v;updSrBtn()});
    srPin.addEventListener('input',()=>{srPin.value=srPin.value.replace(/\D/g,'').slice(0,6);updSrBtn()});
    document.getElementById('sr-btn').addEventListener('click',async()=>{
        const errEl=document.getElementById('sr-error');errEl.style.display='none';
        const code=srCode.value,pin=srPin.value;
        try{
            const encBundle=await downloadSyncBundle(code);
            if(!encBundle){errEl.textContent='Code not found or expired';errEl.style.display='';return}
            const bundle=await restoreFromBundle(encBundle,pin);
            await applyRestore(bundle);
            // Clean up the sync code from Firebase
            try{await fbDb.ref('sync/'+code.replace('-','')).remove()}catch(e){}
            hist=[];switchNavTab('home');toast('Restored '+bundle.groups.length+' group'+(bundle.groups.length!==1?'s':'')+'!');
        }catch(e){errEl.textContent='Wrong PIN or corrupted data';errEl.style.display='';console.error(e)}
    });
    document.querySelectorAll('.tab').forEach(t=>t.addEventListener('click',()=>switchGroupTab(t.dataset.tab)));
    // Bottom nav tab buttons
    document.querySelectorAll('#bottom-nav .nav-tab').forEach(t=>t.addEventListener('click',()=>switchNavTab(t.dataset.tab)));
    document.getElementById('fab-cg').addEventListener('click',()=>{document.getElementById('cg-form').style.display='';document.getElementById('cg-done').style.display='none';document.getElementById('cg-name').value='';document.getElementById('cg-btn').disabled=true;show('cg')});
    document.getElementById('cg-name').addEventListener('input',e=>{document.getElementById('cg-btn').disabled=!e.target.value.trim()});
    let newGid='';
    document.getElementById('cg-btn').addEventListener('click',async()=>{const name=document.getElementById('cg-name').value.trim(),gid=uid(),now=Date.now(),key=await genKey();await iP('groups',{groupId:gid,name,createdBy:myId,createdAt:now,inviteToken:uid(),groupKeyBase64:key,hlcTimestamp:now,hlcNodeId:myId,baseCurrency:myDefaultCurrency});await iP('members',{_key:gid+'_'+myId,groupId:gid,memberId:myId,displayName:myName,joinedAt:now,isDeleted:false,hlcTimestamp:now});await pushOp(gid,key,{id:uid(),type:'member_join',data:{memberId:myId,displayName:myName,joinedAt:now},hlc:now,author:myId});newGid=gid;const link=await makeShortLink(gid,key,name);document.getElementById('inv-display').textContent=link;document.getElementById('cg-form').style.display='none';document.getElementById('cg-done').style.display='';document.getElementById('share-btn').onclick=()=>shareLink(link,name);document.getElementById('qr-btn-cg').onclick=()=>showQrModal(link,name);document.getElementById('goto-btn').onclick=async()=>{openGroup(newGid)};autoBackup().catch(()=>{})});
    // Export button
    document.getElementById('btn-export').addEventListener('click',()=>{
        document.getElementById('export-modal').style.display='flex';
    });
    document.getElementById('export-csv-btn').addEventListener('click',()=>{closeExportModal();exportCSV()});
    document.getElementById('export-pdf-btn').addEventListener('click',()=>{closeExportModal();exportPDF()});
    // Calculator
    document.getElementById('ae-calc-btn').addEventListener('click',openCalcModal);
    document.querySelectorAll('.calc-key').forEach(k=>k.addEventListener('click',()=>calcKeyPress(k.dataset.key)));
    document.getElementById('calc-done').addEventListener('click',()=>{
        const val=document.getElementById('calc-result').textContent;
        if(val&&val!=='0'){document.getElementById('ae-amt').value=val;updAe()}
        closeCalcModal();
    });
    // Currency picker
    document.getElementById('ae-currency-btn').addEventListener('click',openCurrencyModal);
    document.getElementById('fab-ae').addEventListener('click',()=>{show('ae');renderAddExpense()});
    document.getElementById('ae-desc').addEventListener('input',updAe);
    document.getElementById('ae-amt').addEventListener('input',updAe);
    document.addEventListener('change',e=>{if(e.target.classList.contains('sc')){if(aeSplitMode!=='equal')renderSplitDetail();updAe()}});
    document.getElementById('ae-btn').addEventListener('click',async()=>{
        const desc=document.getElementById('ae-desc').value.trim();
        const amtC=Math.round((parseFloat(document.getElementById('ae-amt').value)||0)*100);
        const split=aeSplitMode==='items'?[...new Set(aeItemRows.flatMap(r=>r.members))]:[...document.querySelectorAll('.sc:checked')].map(c=>c.value);
        const now=Date.now(),eid=uid();
        // Tag
        const activeTag=document.querySelector('#ae-tags .tag-pill.active');
        const tag=activeTag?activeTag.dataset.tag:null;
        // Paid by — always populate paidBy for backward compat
        let paidBy,paidByMap=null;
        if(aePayerMode==='multi'){
            paidByMap={};document.querySelectorAll('.mp-amt').forEach(i=>{const v=Math.round((parseFloat(i.value)||0)*100);if(v>0)paidByMap[i.dataset.mid]=v});
            paidBy=Object.keys(paidByMap)[0]||myId;
        }else{paidBy=document.querySelector('input[name=pb]:checked')?.value||myId}
        // Split — always populate splitAmong for backward compat; compute splitDetails in cents
        let splitMode=aeSplitMode!=='equal'?aeSplitMode:null;
        let splitDetails=null;
        let splitItems=null;
        if(aeSplitMode==='items'){
            const itemsData=getItemsSplitData();
            splitDetails=itemsData.splitDetails;
            splitItems=itemsData.splitItems;
        }else if(aeSplitMode==='amount'){
            splitDetails={};document.querySelectorAll('.sd-val').forEach(i=>{const v=Math.round((parseFloat(i.value)||0)*100);if(v>0)splitDetails[i.dataset.mid]=v});
        }else if(aeSplitMode==='percentage'){
            splitDetails={};const inputs=[...document.querySelectorAll('.sd-val')];
            let allocated=0;
            inputs.forEach((i,idx)=>{const pct=parseFloat(i.value)||0;let cents=Math.floor(amtC*pct/100);if(idx===inputs.length-1)cents=amtC-allocated;splitDetails[i.dataset.mid]=cents;allocated+=cents});
        }else if(aeSplitMode==='ratio'){
            splitDetails={};const inputs=[...document.querySelectorAll('.sd-val')];
            let totalR=0;inputs.forEach(i=>totalR+=(parseFloat(i.value)||0));
            if(totalR>0){let allocated=0;inputs.forEach((i,idx)=>{const r=parseFloat(i.value)||0;let cents=Math.floor(amtC*r/totalR);if(idx===inputs.length-1)cents=amtC-allocated;splitDetails[i.dataset.mid]=cents;allocated+=cents})}
        }
        const isEdit=!!editingExpense;
        const finalEid=isEdit?editingExpense.expenseId:eid;
        const createdAt=isEdit?editingExpense.createdAt:now;
        const notes=document.getElementById('ae-notes').value.trim()||null;
        // Recurring
        const recurringEnabled=document.getElementById('ae-recurring-toggle').checked;
        const recurring=recurringEnabled?{frequency:aeRecurringFreq}:null;
        // Currency
        const currency=aeCurrency||'INR';
        // Currency conversion
        let convertedAmountCents=null,conversionRate=null,convertedCurrency=null;
        let convertedSplitDetails=null,convertedPaidByMap=null;
        const groupObj=await iG('groups',curGroup);
        const groupBaseCurrency=groupObj?.baseCurrency||myDefaultCurrency;
        if(currency!==groupBaseCurrency){
            const rate=aeConversionRate||await fetchExchangeRate(currency,groupBaseCurrency);
            if(rate){
                conversionRate=rate;
                convertedCurrency=groupBaseCurrency;
                convertedAmountCents=Math.round(amtC*rate);
                // Convert splitDetails to base currency
                if(splitDetails){
                    convertedSplitDetails={};
                    for(const[mid,amt] of Object.entries(splitDetails)){
                        convertedSplitDetails[mid]=Math.round(amt*rate);
                    }
                }
                // Convert paidByMap to base currency
                if(paidByMap){
                    convertedPaidByMap={};
                    for(const[mid,amt] of Object.entries(paidByMap)){
                        convertedPaidByMap[mid]=Math.round(amt*rate);
                    }
                }
            }
        }
        const data={expenseId:finalEid,groupId:curGroup,description:desc,amountCents:amtC,currency,paidBy,splitAmong:split,createdAt,isDeleted:false,tag,paidByMap,splitMode,splitDetails,notes,recurring,splitItems,convertedAmountCents,conversionRate,convertedCurrency,convertedSplitDetails,convertedPaidByMap};
        // Save history
        if(isEdit){
            const prevData={...editingExpense};delete prevData.hlcTimestamp;
            await saveHistory(makeHistoryEntry({expenseId:finalEid,entityType:'expense',action:'edited',previousData:prevData,newData:data}));
        }else{
            await saveHistory(makeHistoryEntry({expenseId:finalEid,entityType:'expense',action:'created',previousData:null,newData:data}));
        }
        await iP('expenses',{...data,hlcTimestamp:now});await pushOp(curGroup,curGroupKey,{id:uid(),type:'expense',data,hlc:now,author:myId});
        editingExpense=null;
        goBack();switchGroupTab('exp');toast(isEdit?'Expense updated!':'Expense added!')
    });
    document.getElementById('stl-input').addEventListener('input',()=>updateSettleHint(settleData.amountCents));
    document.getElementById('stl-btn').addEventListener('click',async()=>{const settleAmt=Math.round((parseFloat(document.getElementById('stl-input').value)||0)*100);if(settleAmt<=0||settleAmt>settleData.amountCents)return;const now=Date.now(),sid=uid(),data={settlementId:sid,groupId:curGroup,fromMember:settleData.from,toMember:settleData.to,amountCents:settleAmt,createdAt:now,isDeleted:false};await saveHistory(makeHistoryEntry({settlementId:sid,entityType:'settlement',action:'created',previousData:null,newData:data}));await iP('settlements',{...data,hlcTimestamp:now});await pushOp(curGroup,curGroupKey,{id:uid(),type:'settlement',data,hlc:now,author:myId});goBack();switchGroupTab('bal');toast(settleAmt<settleData.amountCents?'Partially settled!':'Settled!')});
}

// Init
async function init(){
    await openDB();wireEvents();
    const ni=document.getElementById('ob-name'),nb=document.getElementById('ob-btn');
    ni.addEventListener('input',()=>{nb.disabled=!ni.value.trim()});
    nb.addEventListener('click',async()=>{
        await saveId(ni.value.trim());
        // Go to passphrase setup step
        obPassphrase=generatePassphrase();showObPp();
        document.getElementById('ob-pp-confirm').checked=false;
        document.getElementById('ob-pp-continue').disabled=true;
        document.getElementById('ob-pp-custom-input').style.display='none';
        show('ob-recovery',false);
    });
    document.getElementById('ob-recover-pp').addEventListener('click',()=>{document.getElementById('rec-phrase').value='';document.getElementById('rec-btn').disabled=true;document.getElementById('rec-status').style.display='none';show('recover')});
    document.getElementById('ob-restore').addEventListener('click',()=>{document.getElementById('sr-code').value='';document.getElementById('sr-pin').value='';document.getElementById('sr-btn').disabled=true;document.getElementById('sr-error').style.display='none';show('sync-restore')});
    if(!(await loadId())){show('onboard',false);return}
    const jr=await handleJoin();switchNavTab('home');
    if(jr){await openGroup(jr.groupId);if(jr.action==='joined')toast('Joined group!')}
}
init().catch(console.error);
