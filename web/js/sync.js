// Firebase sync
let syncRef=null;
async function pushOp(gid,key,op){const e=await enc(key,op);e.t=firebase.database.ServerValue.TIMESTAMP;return fbDb.ref('groups/'+gid+'/ops').push(e)}
async function processOp(op,gid){if(await iG('processedOps',op.id))return false;if(op.type==='member_join'){const k=gid+'_'+op.data.memberId;const ex=await iG('members',k);if(!ex||op.hlc>ex.hlcTimestamp)await iP('members',{_key:k,groupId:gid,memberId:op.data.memberId,displayName:op.data.displayName,joinedAt:op.data.joinedAt,isDeleted:false,hlcTimestamp:op.hlc})}else if(op.type==='expense'){const ex=await iG('expenses',op.data.expenseId);if(!ex||op.hlc>ex.hlcTimestamp)await iP('expenses',{...op.data,hlcTimestamp:op.hlc})}else if(op.type==='settlement'){const ex=await iG('settlements',op.data.settlementId);if(!ex||op.hlc>ex.hlcTimestamp)await iP('settlements',{...op.data,hlcTimestamp:op.hlc})}else if(op.type==='history'){const ex=await iG('history',op.data.historyId);if(!ex)await iP('history',op.data)}await iPK('processedOps',op.id,true);return true}
async function fullSync(gid,key){
    // Pull ALL ops from Firebase and process them (ignoring processedOps cache)
    try{
        const snap=await fbDb.ref('groups/'+gid+'/ops').orderByChild('t').once('value');
        const ops=snap.val();if(!ops)return;
        let changed=false;
        for(const[,enc] of Object.entries(ops)){
            try{
                if(!enc||!enc.d||!enc.n)continue;
                const op=await dec(key,enc);
                // Force-process: skip processedOps check
                if(op.type==='member_join'){
                    const k=gid+'_'+op.data.memberId;
                    const ex=await iG('members',k);
                    if(!ex||op.hlc>ex.hlcTimestamp){await iP('members',{_key:k,groupId:gid,memberId:op.data.memberId,displayName:op.data.displayName,joinedAt:op.data.joinedAt,isDeleted:false,hlcTimestamp:op.hlc});changed=true}
                }else if(op.type==='expense'){
                    const ex=await iG('expenses',op.data.expenseId);
                    if(!ex||op.hlc>ex.hlcTimestamp){await iP('expenses',{...op.data,hlcTimestamp:op.hlc});changed=true}
                }else if(op.type==='settlement'){
                    const ex=await iG('settlements',op.data.settlementId);
                    if(!ex||op.hlc>ex.hlcTimestamp){await iP('settlements',{...op.data,hlcTimestamp:op.hlc});changed=true}
                }else if(op.type==='history'){
                    const ex=await iG('history',op.data.historyId);
                    if(!ex){await iP('history',op.data);changed=true}
                }
                await iPK('processedOps',op.id,true);
            }catch(e){console.warn('Op decrypt failed:',e)}
        }
        if(changed)refreshTab();
    }catch(e){console.error('Full sync failed:',e)}
}

function startSync(gid,key){
    stopSync();
    document.getElementById('sync-dot').className='sync-dot sync-on';
    // Do a full pull first
    fullSync(gid,key);
    // Then listen for new ops
    syncRef=fbDb.ref('groups/'+gid+'/ops').orderByChild('t');
    syncRef.on('child_added',async snap=>{
        try{
            const e=snap.val();if(!e||!e.d||!e.n)return;
            const op=await dec(key,e);
            const changed=await processOp(op,gid);
            if(changed&&curScreen==='gd'&&curGroup===gid)refreshTab();
        }catch(err){console.warn('Sync err:',err)}
    });
}
function stopSync(){if(syncRef){syncRef.off();syncRef=null}const d=document.getElementById('sync-dot');if(d)d.className='sync-dot sync-off'}
function refreshTab(){const a=document.querySelector('.tab.active');if(!a)return;const t=a.dataset.tab;if(t==='exp')renderExpenses();else if(t==='bal')renderBalances();else if(t==='analytics')renderGroupAnalytics();else renderMembers()}

// Recovery passphrase
const WORDS='apple mango banana cherry grape lemon melon peach plum orange tiger eagle falcon panda koala whale shark dolphin kitten puppy river ocean sunset cloud mountain forest garden bridge castle island valley breeze thunder crystal diamond golden silver velvet marble puzzle rocket planet comet galaxy nebula compass anchor hammer shield lantern beacon mirror shadow flame frost ember coral maple bamboo lotus tulip daisy violet orchid pepper cinnamon ginger vanilla cocoa espresso latte mocha pillow blanket candle ribbon arrow feather acorn pebble meadow harbor lighthouse'.split(' ');

function generatePassphrase(){return[WORDS[Math.floor(Math.random()*WORDS.length)],WORDS[Math.floor(Math.random()*WORDS.length)],WORDS[Math.floor(Math.random()*WORDS.length)]].join(' ')}

function validatePassphrase(pp){const words=pp.trim().split(/\s+/);return words.length>=3&&pp.trim().length>=8}

async function derivePpKey(passphrase,salt){
    const raw=new TextEncoder().encode(passphrase.trim().toLowerCase()+salt);
    const hash=await crypto.subtle.digest('SHA-256',raw);
    return crypto.subtle.importKey('raw',hash,{name:'AES-GCM'},false,['encrypt','decrypt']);
}

async function ppHash(passphrase){
    const raw=new TextEncoder().encode(passphrase.trim().toLowerCase()+'splitblind-recovery');
    const hash=await crypto.subtle.digest('SHA-256',raw);
    return bufB64(new Uint8Array(hash)).replace(/[+/=]/g,'').substring(0,24);
}

async function backupToRecovery(passphrase){
    const groups=await iA('groups');
    const bundle={id:myId,name:myName,groups:groups.map(g=>({i:g.groupId,k:g.groupKeyBase64,n:g.name})),ts:Date.now()};
    const salt=bufB64(crypto.getRandomValues(new Uint8Array(16)));
    const key=await derivePpKey(passphrase,salt);
    const iv=crypto.getRandomValues(new Uint8Array(12));
    const ct=await crypto.subtle.encrypt({name:'AES-GCM',iv},key,new TextEncoder().encode(JSON.stringify(bundle)));
    const entry={s:salt,d:bufB64(new Uint8Array(ct)),n:bufB64(iv),ts:Date.now()};
    const hash=await ppHash(passphrase);
    // Store under recovery/{hash}/{unique_id}
    await fbDb.ref('recovery/'+hash).push(entry);
    // Save passphrase hash locally so we can auto-update
    await iPK('identity','ppHash',hash);
    await iPK('identity','pp',passphrase.trim().toLowerCase());
}

async function autoBackup(){
    const pp=await iG('identity','pp');
    if(!pp)return;
    const hash=await ppHash(pp);
    // Remove old entries for this user, push new one
    const snap=await fbDb.ref('recovery/'+hash).once('value');
    const entries=snap.val();
    if(entries){
        for(const[k,v] of Object.entries(entries)){
            try{
                const key=await derivePpKey(pp,v.s);
                const pt=await crypto.subtle.decrypt({name:'AES-GCM',iv:b64Buf(v.n)},key,b64Buf(v.d));
                const data=JSON.parse(new TextDecoder().decode(pt));
                if(data.id===myId){await fbDb.ref('recovery/'+hash+'/'+k).remove()}
            }catch(e){}
        }
    }
    await backupToRecovery(pp);
}

async function recoverFromPassphrase(passphrase){
    const hash=await ppHash(passphrase);
    const snap=await fbDb.ref('recovery/'+hash).once('value');
    const entries=snap.val();
    if(!entries)return null;
    for(const[,v] of Object.entries(entries)){
        try{
            const key=await derivePpKey(passphrase.trim().toLowerCase(),v.s);
            const pt=await crypto.subtle.decrypt({name:'AES-GCM',iv:b64Buf(v.n)},key,b64Buf(v.d));
            const data=JSON.parse(new TextDecoder().decode(pt));
            if(data.id&&data.groups)return data;
        }catch(e){/* wrong entry, try next */}
    }
    return null;
}

function downloadPassphrase(pp){
    const text='SplitBlind Recovery Passphrase\n================================\n\nYour passphrase: '+pp+'\n\nKeep this safe! This is the only way to recover your\ngroups and data if you lose your device.\n\nTo recover: Open SplitBlind > "Recover with Passphrase" > Enter the words above.\n\nHow is this safe?\n- Your passphrase is hashed (SHA-256) before any server lookup\n- Your backup is encrypted with AES-256-GCM using your passphrase\n- Each backup uses a unique random salt (no collisions)\n- The server stores only encrypted gibberish — it cannot read your data\n- No one can recover your data without knowing your exact passphrase\n';
    const blob=new Blob([text],{type:'text/plain'});
    const a=document.createElement('a');
    a.href=URL.createObjectURL(blob);
    a.download='splitblind-recovery.txt';
    a.click();
    URL.revokeObjectURL(a.href);
}

function sharePassphrase(pp){
    const text='My SplitBlind Recovery Passphrase: '+pp+'\n\nKeep this safe! Use it to recover your data on any device.';
    if(navigator.share){navigator.share({title:'SplitBlind Recovery',text}).catch(()=>{})}
    else{navigator.clipboard.writeText(text).then(()=>toast('Copied!'))}
}

// Device sync
async function generateSyncBundle(pin){
    const groups=await iA('groups');
    const bundle={id:myId,name:myName,groups:groups.map(g=>({i:g.groupId,k:g.groupKeyBase64,n:g.name}))};
    // Encrypt bundle with PIN-derived key
    const pinKey=await derivePinKey(pin);
    const iv=crypto.getRandomValues(new Uint8Array(12));
    const ct=await crypto.subtle.encrypt({name:'AES-GCM',iv},pinKey,new TextEncoder().encode(JSON.stringify(bundle)));
    return{d:bufB64(new Uint8Array(ct)),n:bufB64(iv)};
}

async function restoreFromBundle(encBundle,pin){
    const pinKey=await derivePinKey(pin);
    const pt=await crypto.subtle.decrypt({name:'AES-GCM',iv:b64Buf(encBundle.n)},pinKey,b64Buf(encBundle.d));
    return JSON.parse(new TextDecoder().decode(pt));
}

async function derivePinKey(pin){
    const raw=new TextEncoder().encode(pin+'splitblind-salt-2026');
    const hash=await crypto.subtle.digest('SHA-256',raw);
    return crypto.subtle.importKey('raw',hash,{name:'AES-GCM'},false,['encrypt','decrypt']);
}

function genSyncCode(){
    const chars='ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code='';for(let i=0;i<8;i++)code+=chars[Math.floor(Math.random()*chars.length)];
    return code.slice(0,4)+'-'+code.slice(4);
}

async function uploadSyncBundle(code,encBundle){
    const ref=fbDb.ref('sync/'+code.replace('-',''));
    await ref.set({...encBundle,ts:firebase.database.ServerValue.TIMESTAMP});
    // Auto-delete after 5 minutes
    setTimeout(async()=>{try{await ref.remove()}catch(e){}},5*60*1000);
}

async function downloadSyncBundle(code){
    const snap=await fbDb.ref('sync/'+code.replace('-','')).once('value');
    const data=snap.val();
    if(!data||!data.d||!data.n)return null;
    return{d:data.d,n:data.n};
}

async function applyRestore(bundle){
    // Restore identity
    myId=bundle.id;myName=bundle.name;
    await iPK('identity','id',myId);
    await iPK('identity','name',myName);
    // Restore groups
    const now=Date.now();
    for(const g of bundle.groups){
        const existing=await iG('groups',g.i);
        if(!existing){
            await iP('groups',{groupId:g.i,name:g.n,createdBy:'',createdAt:now,inviteToken:'',groupKeyBase64:g.k,hlcTimestamp:now,hlcNodeId:myId});
            await iP('members',{_key:g.i+'_'+myId,groupId:g.i,memberId:myId,displayName:myName,joinedAt:now,isDeleted:false,hlcTimestamp:now});
        }
    }
}

// Short links — key ONLY in URL fragment (never stored on Firebase)
async function makeShortLink(gid,key,name){const code=shortId();await fbDb.ref('links/'+code).set({g:gid,n:name});return location.origin+location.pathname+'?c='+code+'#'+encodeURIComponent(key)}
async function resolveShort(code){const s=await fbDb.ref('links/'+code).once('value');return s.val()}
function shareLink(link,name){if(navigator.share){navigator.share({title:'Join "'+name+'" on SplitBlind',text:link}).catch(()=>{})}else{navigator.clipboard.writeText(link).then(()=>toast('Link copied!'))}}
