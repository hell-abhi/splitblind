// QR Code helpers
function generateQrDataUrl(text){
    const qr=qrcode(0,'M');qr.addData(text);qr.make();
    return qr.createDataURL(4,0);
}
let qrModalLink='',qrModalGroupName='';
function showQrModal(link,groupName){
    qrModalLink=link;qrModalGroupName=groupName||'SplitBlind Group';
    document.getElementById('qr-modal-img').src=generateQrDataUrl(link);
    document.getElementById('qr-modal-title').textContent=qrModalGroupName;
    document.getElementById('qr-modal').style.display='flex';
    document.getElementById('qr-share-btn').onclick=()=>shareQrImage();
}
function closeQrModal(){document.getElementById('qr-modal').style.display='none'}
async function shareQrImage(){
    const img=document.getElementById('qr-modal-img');
    // Try sharing the QR image as a file
    try{
        const resp=await fetch(img.src);const blob=await resp.blob();
        const file=new File([blob],qrModalGroupName.replace(/\s+/g,'-')+'-qr.png',{type:'image/png'});
        if(navigator.canShare&&navigator.canShare({files:[file]})){
            await navigator.share({title:'Join "'+qrModalGroupName+'" on SplitBlind',text:'Scan this QR code or use the link to join my SplitBlind group "'+qrModalGroupName+'":\n'+qrModalLink,files:[file]});
        }else{
            // Fallback: share text only
            if(navigator.share){await navigator.share({title:'Join "'+qrModalGroupName+'" on SplitBlind',text:'Join my SplitBlind group "'+qrModalGroupName+'"!\n\nLink: '+qrModalLink+'\n\nOr scan the QR code in the app.'})}
            else{navigator.clipboard.writeText('Join "'+qrModalGroupName+'" on SplitBlind:\n'+qrModalLink);toast('Link copied!')}
        }
    }catch(e){
        // Fallback
        if(navigator.share){navigator.share({title:'Join "'+qrModalGroupName+'"',text:'Join my SplitBlind group "'+qrModalGroupName+'":\n'+qrModalLink}).catch(()=>{})}
        else{navigator.clipboard.writeText(qrModalLink);toast('Link copied!')}
    }
}

let qrScanner=null;
function setupQrFileInput(inputId){
    const inp=document.getElementById(inputId);
    if(!inp||inp._bound)return;
    inp._bound=true;
    inp.addEventListener('change',async(e)=>{
        const file=e.target.files[0];
        if(!file)return;
        try{
            const scanner=new Html5Qrcode('qr-reader-file');
            const result=await scanner.scanFile(file,false);
            stopQrScanner();
            await handleScannedUrl(result);
        }catch(err){
            toast('No QR code found in image');
        }
        inp.value='';
    });
}
function openQrScanner(){
    stopQrScanner();
    // Clear any leftover content from previous scans
    document.getElementById('qr-reader').innerHTML='';
    // Reset upload sections
    const uploadSec=document.getElementById('qr-upload-section');
    const uploadPri=document.getElementById('qr-upload-primary');
    if(uploadSec)uploadSec.style.display='';
    if(uploadPri)uploadPri.style.display='none';
    show('scan-qr');
    // Wire file inputs
    setTimeout(()=>{
        setupQrFileInput('qr-file-input');
        setupQrFileInput('qr-file-input-primary');
    },100);
    setTimeout(()=>{
        const readerEl=document.getElementById('qr-reader');
        const w=readerEl.clientWidth;
        const qrSize=Math.min(w-40,250);
        qrScanner=new Html5Qrcode('qr-reader');
        qrScanner.start(
            {facingMode:'environment'},
            {fps:10,qrbox:{width:qrSize,height:qrSize},aspectRatio:1},
            async(decodedText)=>{
                qrScanner.stop().catch(()=>{});qrScanner=null;
                await handleScannedUrl(decodedText);
            },
            ()=>{}
        ).catch(err=>{
            console.error('QR scan error:',err);
            // Camera failed — hide camera section, show upload as primary
            if(uploadSec)uploadSec.style.display='none';
            if(uploadPri)uploadPri.style.display='';
            document.getElementById('qr-reader').style.display='none';
        });
    },500);
}
function stopQrScanner(){
    if(qrScanner){try{qrScanner.stop().catch(()=>{})}catch(e){}qrScanner=null}
}
async function handleScannedUrl(url){
    try{
        const u=new URL(url);
        // Build search params as if we landed on the page
        const params=new URLSearchParams(u.search);
        const sc=params.get('c'),gid=params.get('g'),hash=u.hash.substring(1);
        let g,k,n;
        if(sc){
            const d=await resolveShort(sc);if(!d){toast('Invalid QR code');goBack();return}
            g=d.g;n=d.n||'Group';
            k=hash?decodeURIComponent(hash):(d.k||null);
            if(!k){toast('Invalid QR code — key missing');goBack();return}
        }else if(gid&&hash){
            g=gid;const p=hash.split('|');k=decodeURIComponent(p[0]);n=p[1]?decodeURIComponent(p[1]):'Group';
        }else{toast('Not a valid SplitBlind QR code');goBack();return}
        const ex=await iG('groups',g);
        if(ex){toast('Already in this group!');switchNavTab('home');await openGroup(g);return}
        const now=Date.now();
        await iP('groups',{groupId:g,name:n,createdBy:'',createdAt:now,inviteToken:'',groupKeyBase64:k,hlcTimestamp:now,hlcNodeId:myId});
        await iP('members',{_key:g+'_'+myId,groupId:g,memberId:myId,displayName:myName,joinedAt:now,isDeleted:false,hlcTimestamp:now});
        await pushOp(g,k,{id:uid(),type:'member_join',data:{memberId:myId,displayName:myName,joinedAt:now},hlc:now,author:myId});
        toast('Joined group!');switchNavTab('home');await openGroup(g);
    }catch(e){console.error('QR scan handle error:',e);toast('Invalid QR code');goBack()}
}
