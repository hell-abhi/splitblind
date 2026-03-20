// IDB
let db;
function openDB(){return new Promise((res,rej)=>{const r=indexedDB.open('splitblind',4);r.onupgradeneeded=e=>{const d=e.target.result;['identity','processedOps','shortcodes'].forEach(n=>{if(!d.objectStoreNames.contains(n))d.createObjectStore(n)});[['groups','groupId'],['expenses','expenseId'],['settlements','settlementId']].forEach(([n,k])=>{if(!d.objectStoreNames.contains(n))d.createObjectStore(n,{keyPath:k})});if(!d.objectStoreNames.contains('members'))d.createObjectStore('members',{keyPath:'_key'});if(!d.objectStoreNames.contains('history'))d.createObjectStore('history',{keyPath:'historyId'})};r.onsuccess=()=>{db=r.result;res(db)};r.onerror=()=>rej(r.error)})}
function iP(s,d){return new Promise((r,j)=>{const t=db.transaction(s,'readwrite');t.objectStore(s).put(d);t.oncomplete=()=>r();t.onerror=()=>j(t.error)})}
function iPK(s,k,v){return new Promise((r,j)=>{const t=db.transaction(s,'readwrite');t.objectStore(s).put(v,k);t.oncomplete=()=>r();t.onerror=()=>j(t.error)})}
function iG(s,k){return new Promise((r,j)=>{const t=db.transaction(s,'readonly');const q=t.objectStore(s).get(k);q.onsuccess=()=>r(q.result);q.onerror=()=>j(q.error)})}
function iA(s){return new Promise((r,j)=>{const t=db.transaction(s,'readonly');const q=t.objectStore(s).getAll();q.onsuccess=()=>r(q.result);q.onerror=()=>j(q.error)})}
