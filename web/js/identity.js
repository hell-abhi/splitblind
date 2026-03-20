// Identity
let myId='',myName='',myDefaultCurrency='INR';
function uid(){return crypto.randomUUID?crypto.randomUUID():'xxxxxxxx-xxxx-4xxx-yxxx'.replace(/[xy]/g,c=>{const r=Math.random()*16|0;return(c==='x'?r:(r&3|8)).toString(16)})}
function shortId(){return uid().replace(/-/g,'').substring(0,8)}
async function loadId(){myId=await iG('identity','id');myName=await iG('identity','name');const dc=await iG('identity','defaultCurrency');if(dc)myDefaultCurrency=dc;return!!(myId&&myName)}
async function saveId(n){myId=uid().substring(0,16);myName=n;await iPK('identity','id',myId);await iPK('identity','name',myName)}
