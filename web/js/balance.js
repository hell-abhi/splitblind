// Balance
function calcBal(exps,sets){const b={};for(const e of exps){if(e.isDeleted)continue;
    // CREDIT: multi-payer or single payer
    if(e.paidByMap){for(const[m,a] of Object.entries(e.paidByMap))b[m]=(b[m]||0)+a}
    else{b[e.paidBy]=(b[e.paidBy]||0)+e.amountCents}
    // DEBIT: splitDetails or equal split
    if(e.splitDetails){for(const[m,a] of Object.entries(e.splitDetails))b[m]=(b[m]||0)-a}
    else{const s=e.splitAmong,sh=Math.floor(e.amountCents/s.length),rem=e.amountCents%s.length;s.forEach((m,i)=>{b[m]=(b[m]||0)-sh-(i<rem?1:0)})}
}for(const s of sets){if(s.isDeleted)continue;b[s.fromMember]=(b[s.fromMember]||0)+s.amountCents;b[s.toMember]=(b[s.toMember]||0)-s.amountCents}return b}
function simplify(bal){const cr=[],dr=[];for(const[m,b] of Object.entries(bal)){if(b>0)cr.push({id:m,a:b});else if(b<0)dr.push({id:m,a:-b})}cr.sort((a,b)=>b.a-a.a);dr.sort((a,b)=>b.a-a.a);const debts=[];let i=0,j=0;while(i<cr.length&&j<dr.length){const a=Math.min(cr[i].a,dr[j].a);if(a>0)debts.push({from:dr[j].id,to:cr[i].id,amountCents:a});cr[i].a-=a;dr[j].a-=a;if(!cr[i].a)i++;if(!dr[j].a)j++}return debts}
function fmt(c){const w=Math.floor(Math.abs(c)/100),f=Math.abs(c)%100;return(c<0?'-':'')+'\u20B9'+w+'.'+String(f).padStart(2,'0')}
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

// Helper: calculate user's share of an expense
function getMyShare(expense, userId) {
    if (expense.splitDetails) {
        const details = typeof expense.splitDetails === 'string' ? JSON.parse(expense.splitDetails) : expense.splitDetails;
        return details[userId] || 0;
    }
    const splitAmong = typeof expense.splitAmong === 'string' ? JSON.parse(expense.splitAmong) : expense.splitAmong;
    if (Array.isArray(splitAmong) && splitAmong.includes(userId)) {
        return Math.floor(expense.amountCents / splitAmong.length);
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
    if(JSON.stringify(prev.splitAmong||[])!==JSON.stringify(next.splitAmong||[]))changes.push({field:'Split among',from:'changed',to:'updated'});
    if((prev.splitMode||'equal')!==(next.splitMode||'equal'))changes.push({field:'Split mode',from:prev.splitMode||'equal',to:next.splitMode||'equal'});
    if((prev.notes||'')!==(next.notes||''))changes.push({field:'Notes',from:prev.notes||'(none)',to:next.notes||'(none)'});
    return changes;
}
