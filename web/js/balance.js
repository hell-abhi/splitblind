// Balance
function calcBal(exps,sets){const b={};for(const e of exps){if(e.isDeleted)continue;
    // Use converted amounts (base currency) when available
    const effectiveAmt=e.convertedAmountCents||e.amountCents;
    // CREDIT: multi-payer or single payer
    if(e.convertedPaidByMap){for(const[m,a] of Object.entries(e.convertedPaidByMap))b[m]=(b[m]||0)+a}
    else if(e.paidByMap&&!e.convertedAmountCents){for(const[m,a] of Object.entries(e.paidByMap))b[m]=(b[m]||0)+a}
    else if(e.paidByMap&&e.convertedAmountCents){
        // Multi-payer with conversion: scale each payer's share by the conversion rate
        const origTotal=e.amountCents;
        for(const[m,a] of Object.entries(e.paidByMap)){
            const scaled=Math.round(a/origTotal*effectiveAmt);
            b[m]=(b[m]||0)+scaled;
        }
    }
    else{b[e.paidBy]=(b[e.paidBy]||0)+effectiveAmt}
    // DEBIT: splitDetails (already in base currency if converted) or equal split
    if(e.convertedSplitDetails){for(const[m,a] of Object.entries(e.convertedSplitDetails))b[m]=(b[m]||0)-a}
    else if(e.splitDetails&&!e.convertedAmountCents){for(const[m,a] of Object.entries(e.splitDetails))b[m]=(b[m]||0)-a}
    else if(e.splitDetails&&e.convertedAmountCents){
        // Split details exist but in original currency; scale them
        const origTotal=e.amountCents;
        for(const[m,a] of Object.entries(e.splitDetails)){
            const scaled=Math.round(a/origTotal*effectiveAmt);
            b[m]=(b[m]||0)-scaled;
        }
    }
    else{const s=e.splitAmong,sh=Math.floor(effectiveAmt/s.length),rem=effectiveAmt%s.length;s.forEach((m,i)=>{b[m]=(b[m]||0)-sh-(i<rem?1:0)})}
}for(const s of sets){if(s.isDeleted)continue;b[s.fromMember]=(b[s.fromMember]||0)+s.amountCents;b[s.toMember]=(b[s.toMember]||0)-s.amountCents}return b}
function simplify(bal){const cr=[],dr=[];for(const[m,b] of Object.entries(bal)){if(b>0)cr.push({id:m,a:b});else if(b<0)dr.push({id:m,a:-b})}cr.sort((a,b)=>b.a-a.a);dr.sort((a,b)=>b.a-a.a);const debts=[];let i=0,j=0;while(i<cr.length&&j<dr.length){const a=Math.min(cr[i].a,dr[j].a);if(a>0)debts.push({from:dr[j].id,to:cr[i].id,amountCents:a});cr[i].a-=a;dr[j].a-=a;if(!cr[i].a)i++;if(!dr[j].a)j++}return debts}
// Currency data
const CURRENCIES=[
    {code:'INR',symbol:'\u20B9',name:'Indian Rupee',top:true},
    {code:'USD',symbol:'$',name:'US Dollar',top:true},
    {code:'EUR',symbol:'\u20AC',name:'Euro',top:true},
    {code:'GBP',symbol:'\u00A3',name:'British Pound',top:true},
    {code:'AED',symbol:'\u062F.\u0625',name:'UAE Dirham',top:true},
    {code:'SGD',symbol:'S$',name:'Singapore Dollar'},
    {code:'CAD',symbol:'C$',name:'Canadian Dollar'},
    {code:'AUD',symbol:'A$',name:'Australian Dollar'},
    {code:'JPY',symbol:'\u00A5',name:'Japanese Yen'},
    {code:'THB',symbol:'\u0E3F',name:'Thai Baht'},
    {code:'MYR',symbol:'RM',name:'Malaysian Ringgit'},
    {code:'IDR',symbol:'Rp',name:'Indonesian Rupiah'},
    {code:'KRW',symbol:'\u20A9',name:'South Korean Won'},
    {code:'CHF',symbol:'Fr',name:'Swiss Franc'},
    {code:'SEK',symbol:'kr',name:'Swedish Krona'},
    {code:'NOK',symbol:'kr',name:'Norwegian Krone'},
    {code:'DKK',symbol:'kr',name:'Danish Krone'},
    {code:'NZD',symbol:'NZ$',name:'New Zealand Dollar'},
    {code:'HKD',symbol:'HK$',name:'Hong Kong Dollar'},
    {code:'PHP',symbol:'\u20B1',name:'Philippine Peso'},
    {code:'SAR',symbol:'\uFDFC',name:'Saudi Riyal'},
    {code:'QAR',symbol:'\uFDFC',name:'Qatari Riyal'},
    {code:'BHD',symbol:'BD',name:'Bahraini Dinar'},
    {code:'OMR',symbol:'OMR',name:'Omani Rial'},
    {code:'KWD',symbol:'KD',name:'Kuwaiti Dinar'},
    {code:'ZAR',symbol:'R',name:'South African Rand'},
    {code:'BRL',symbol:'R$',name:'Brazilian Real'},
    {code:'MXN',symbol:'MX$',name:'Mexican Peso'},
    {code:'TRY',symbol:'\u20BA',name:'Turkish Lira'},
    {code:'RUB',symbol:'\u20BD',name:'Russian Ruble'}
];
const CURRENCY_MAP={};CURRENCIES.forEach(c=>CURRENCY_MAP[c.code]=c);
function getCurrencySymbol(code){const c=CURRENCY_MAP[code||'INR'];return c?c.symbol:'\u20B9'}
function fmt(c,currencyCode){const sym=getCurrencySymbol(currencyCode);const w=Math.floor(Math.abs(c)/100),f=Math.abs(c)%100;return(c<0?'-':'')+sym+w+'.'+String(f).padStart(2,'0')}
function esc(s){const d=document.createElement('div');d.textContent=s;return d.innerHTML}

// Colors for avatars
const avatarColors=['#7C6FE0','#F2A0C4','#8DDCC5','#F8C4A4','#89C4F4','#F7DC6F','#BB8FCE','#F1948A'];
function getColor(id){let h=0;for(let i=0;i<id.length;i++)h=id.charCodeAt(i)+((h<<5)-h);return avatarColors[Math.abs(h)%avatarColors.length]}
function getInitial(name){return(name||'?')[0].toUpperCase()}
const expIcons=['&#x1F37D;','&#x1F697;','&#x1F6D2;','&#x1F3AC;','&#x2708;','&#x1F3E0;','&#x1F4B3;','&#x1F381;'];
const expBgs=['#EDE9FC','#FCE4EC','#E0F2F1','#FFF3E0','#E8EAF6','#F3E5F5','#E0F7FA','#FBE9E7'];
function getExpStyle(desc){let h=0;for(let i=0;i<desc.length;i++)h=desc.charCodeAt(i)+((h<<5)-h);const idx=Math.abs(h)%expIcons.length;return{icon:expIcons[idx],bg:expBgs[idx]}}

// Tags
const TAGS={
    food:{icon:'\u{1F37D}\uFE0F',color:'#EDE9FC',label:'Food'},
    transport:{icon:'\u{1F697}',color:'#E0F7FA',label:'Transport'},
    shopping:{icon:'\u{1F6CD}\uFE0F',color:'#FCE4EC',label:'Shopping'},
    entertainment:{icon:'\u{1F3AC}',color:'#FFF3E0',label:'Entertainment'},
    travel:{icon:'\u2708\uFE0F',color:'#E8EAF6',label:'Travel'},
    bills:{icon:'\u{1F4C4}',color:'#F3E5F5',label:'Bills'},
    groceries:{icon:'\u{1F6D2}',color:'#E0F2F1',label:'Groceries'},
    health:{icon:'\u{1F48A}',color:'#FBE9E7',label:'Health'},
    rent:{icon:'\u{1F3E0}',color:'#FFF8E1',label:'Rent'},
    other:{icon:'\u{1F4CC}',color:'#F5F5F5',label:'Other'}
};
function getTagStyle(tag){if(tag&&TAGS[tag])return{icon:TAGS[tag].icon,bg:TAGS[tag].color,label:TAGS[tag].label};return null}

// Helper: calculate user's share of an expense (uses converted amounts when available)
function getMyShare(expense, userId) {
    const effectiveAmt=expense.convertedAmountCents||expense.amountCents;
    // Use converted split details if available
    const sd=expense.convertedSplitDetails||expense.splitDetails;
    if (sd) {
        const details = typeof sd === 'string' ? JSON.parse(sd) : sd;
        return details[userId] || 0;
    }
    const splitAmong = typeof expense.splitAmong === 'string' ? JSON.parse(expense.splitAmong) : expense.splitAmong;
    if (Array.isArray(splitAmong) && splitAmong.includes(userId)) {
        return Math.floor(effectiveAmt / splitAmong.length);
    }
    return 0;
}

// History helpers
async function saveHistory(entry){
    await iP('history',entry);
    // Push as Firebase op so it syncs across devices
    if(curGroup&&curGroupKey){
        await pushOp(curGroup,curGroupKey,{id:uid(),type:'history',data:entry,hlc:Date.now(),author:myId});
    }
}
function makeHistoryEntry(opts){
    return{
        historyId:uid(),
        expenseId:opts.expenseId||null,
        settlementId:opts.settlementId||null,
        entityType:opts.entityType,
        action:opts.action,
        previousData:opts.previousData||null,
        newData:opts.newData||null,
        changedBy:myId,
        changedByName:myName,
        changedAt:Date.now(),
        groupId:curGroup
    };
}
function computeChanges(prev,next){
    if(!prev||!next)return[];
    const changes=[];
    if(prev.description!==next.description)changes.push({field:'Description',from:prev.description,to:next.description});
    if(prev.amountCents!==next.amountCents)changes.push({field:'Amount',from:fmt(prev.amountCents),to:fmt(next.amountCents)});
    if(prev.tag!==next.tag){const pt=prev.tag&&TAGS[prev.tag]?TAGS[prev.tag].label:(prev.tag||'None');const nt=next.tag&&TAGS[next.tag]?TAGS[next.tag].label:(next.tag||'None');changes.push({field:'Category',from:pt,to:nt})}
    if(prev.paidBy!==next.paidBy)changes.push({field:'Paid by',from:prev.paidBy,to:next.paidBy});
    // Track paidByMap changes (multi-payer add/remove/update)
    const pMap=prev.paidByMap||null,nMap=next.paidByMap||null;
    if(JSON.stringify(pMap)!==JSON.stringify(nMap)){
        if(!pMap&&nMap)changes.push({field:'Payers',from:'Single payer',to:Object.keys(nMap).length+' payers'});
        else if(pMap&&!nMap)changes.push({field:'Payers',from:Object.keys(pMap).length+' payers',to:'Single payer'});
        else if(pMap&&nMap){const pk=Object.keys(pMap),nk=Object.keys(nMap);const added=nk.filter(k=>!pk.includes(k)).length;const removed=pk.filter(k=>!nk.includes(k)).length;const updated=pk.filter(k=>nk.includes(k)&&pMap[k]!==nMap[k]).length;const desc=[];if(added)desc.push('+'+added+' added');if(removed)desc.push(removed+' removed');if(updated)desc.push(updated+' amount'+(updated>1?'s':'')+' changed');if(desc.length)changes.push({field:'Payers',from:pk.length+' payers',to:nk.length+' payers ('+desc.join(', ')+')'});}
    }
    if(JSON.stringify(prev.splitAmong||[])!==JSON.stringify(next.splitAmong||[])){const pArr=prev.splitAmong||[];const nArr=next.splitAmong||[];const added=nArr.filter(m=>!pArr.includes(m));const removed=pArr.filter(m=>!nArr.includes(m));let desc=[];if(added.length)desc.push('+'+added.length+' added');if(removed.length)desc.push(removed.length+' removed');changes.push({field:'Split among',from:pArr.length+' people',to:nArr.length+' people'+(desc.length?' ('+desc.join(', ')+')':'')})}
    if((prev.splitMode||'equal')!==(next.splitMode||'equal'))changes.push({field:'Split mode',from:prev.splitMode||'equal',to:next.splitMode||'equal'});
    // Track splitDetails value changes (unequal split amounts changed)
    if(prev.splitDetails&&next.splitDetails&&JSON.stringify(prev.splitDetails)!==JSON.stringify(next.splitDetails)){
        const diffs=[];
        const allKeys=new Set([...Object.keys(prev.splitDetails),...Object.keys(next.splitDetails)]);
        for(const k of allKeys){const pv=prev.splitDetails[k]||0;const nv=next.splitDetails[k]||0;if(pv!==nv)diffs.push(k)}
        if(diffs.length)changes.push({field:'Split amounts',from:diffs.length+' share'+(diffs.length>1?'s':'')+' changed',to:'updated'})
    }
    if((prev.notes||'')!==(next.notes||''))changes.push({field:'Notes',from:prev.notes||'(none)',to:next.notes||'(none)'});
    if((prev.currency||'INR')!==(next.currency||'INR'))changes.push({field:'Currency',from:prev.currency||'INR',to:next.currency||'INR'});
    if((prev.recurringFrequency||'')!==(next.recurringFrequency||''))changes.push({field:'Recurring',from:prev.recurringFrequency||'none',to:next.recurringFrequency||'none'});
    return changes;
}

// Exchange rate cache and fetcher
const rateCache={};
async function fetchExchangeRate(from, to){
    if(from===to) return 1;
    const key=from+'_'+to;
    if(rateCache[key]) return rateCache[key];
    try{
        const resp=await fetch(`https://api.frankfurter.app/latest?from=${from}&to=${to}`);
        const data=await resp.json();
        const rate=data.rates[to]||null;
        if(rate) rateCache[key]=rate;
        return rate;
    }catch(e){
        console.error('Rate fetch failed:',e);
        return null;
    }
}

// Recurring expense helper
function getNextRecurringDate(timestamp, frequency){
    const d=new Date(timestamp);
    if(frequency==='weekly') d.setDate(d.getDate()+7);
    else if(frequency==='monthly') d.setMonth(d.getMonth()+1);
    else if(frequency==='yearly') d.setFullYear(d.getFullYear()+1);
    return d.getTime();
}

async function processRecurringExpenses(groupId, groupKey){
    const exps=(await iA('expenses')).filter(e=>e.groupId===groupId&&!e.isDeleted&&e.recurring);
    const now=Date.now();
    let created=0;
    for(const e of exps){
        if(!e.recurring||!e.recurring.frequency) continue;
        // Find the latest occurrence of this recurring expense (by recurringParentId or itself)
        const parentId=e.recurringParentId||e.expenseId;
        const allOccurrences=(await iA('expenses')).filter(x=>x.groupId===groupId&&!x.isDeleted&&(x.expenseId===parentId||x.recurringParentId===parentId));
        const latestTs=Math.max(...allOccurrences.map(x=>x.createdAt));
        const nextDue=getNextRecurringDate(latestTs, e.recurring.frequency);
        if(nextDue<=now){
            // Create new occurrence
            const eid=uid();
            const data={
                expenseId:eid, groupId:groupId, description:e.description,
                amountCents:e.amountCents, currency:e.currency||'INR',
                paidBy:e.paidBy, splitAmong:e.splitAmong, createdAt:nextDue,
                isDeleted:false, tag:e.tag, paidByMap:e.paidByMap||null,
                splitMode:e.splitMode||null, splitDetails:e.splitDetails||null,
                notes:e.notes||null, recurring:e.recurring,
                recurringParentId:parentId, splitItems:e.splitItems||null
            };
            await iP('expenses',{...data,hlcTimestamp:now});
            await pushOp(groupId,groupKey,{id:uid(),type:'expense',data,hlc:now,author:myId});
            created++;
        }
    }
    return created;
}
