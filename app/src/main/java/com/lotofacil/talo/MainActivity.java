package com.lotofacil.talo;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final int PICK=8101, PDF=8102;
    final ExecutorService exec=Executors.newSingleThreadExecutor();
    TextView status,out; ProgressBar bar; Button imp,go,pdf; RadioGroup repsGroup;
    List<Draw> hist=new ArrayList<>(); Result result;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        try{ui();}catch(Throwable t){TextView v=new TextView(this);v.setPadding(30,30,30,30);v.setText("Falha ao iniciar:\n"+t);setContentView(v);}
    }

    void ui(){
        ScrollView sv=new ScrollView(this);
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(22,22,22,38);
        r.setBackgroundColor(Color.rgb(252,248,255));
        sv.addView(r);

        TextView h=new TextView(this);
        h.setText("☘  LOTOFÁCIL\nENGROSSANDO O TALO");
        h.setTextColor(Color.WHITE);
        h.setTextSize(26);
        h.setGravity(Gravity.CENTER);
        h.setPadding(18,30,18,30);
        h.setBackgroundColor(Color.rgb(123,31,162));
        r.addView(h,new LinearLayout.LayoutParams(-1,-2));

        TextView sub=new TextView(this);
        sub.setText("Movimento para cima • repetidas 8/9/10 • espelho • P01→P15 • estrutura histórica • mesmo conceito do PyDroid");
        sub.setTextSize(15);
        sub.setPadding(2,16,2,12);
        r.addView(sub);

        imp=button("IMPORTAR HISTÓRICO TXT");
        r.addView(imp);

        TextView label=new TextView(this);
        label.setText("QUANTAS REPETIDAS DO ÚLTIMO?");
        label.setTextSize(16);
        label.setPadding(0,12,0,4);
        r.addView(label);

        repsGroup=new RadioGroup(this);
        repsGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton r8=radio("8",8);
        RadioButton r9=radio("9",9);
        RadioButton r10=radio("10",10);
        repsGroup.addView(r8); repsGroup.addView(r9); repsGroup.addView(r10);
        r9.setChecked(true);
        r.addView(repsGroup);

        go=button("GERAR MELHOR TALO");
        pdf=button("GERAR PDF RESUMO");
        go.setEnabled(false); pdf.setEnabled(false);
        r.addView(go);

        bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        r.addView(bar,new LinearLayout.LayoutParams(-1,18));

        status=new TextView(this);
        status.setText("Aguardando histórico.");
        status.setTextSize(16);
        status.setPadding(0,12,0,12);
        r.addView(status);
        r.addView(pdf);

        out=new TextView(this);
        out.setTextSize(14);
        out.setTextIsSelectable(true);
        out.setPadding(0,18,0,30);
        r.addView(out);

        imp.setOnClickListener(v->pick());
        go.setOnClickListener(v->analyze());
        pdf.setOnClickListener(v->createPdf());
        setContentView(sv);
    }

    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(17);return b;}
    RadioButton radio(String s,int id){RadioButton b=new RadioButton(this);b.setText(s);b.setTextSize(17);b.setId(id);return b;}

    void pick(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        startActivityForResult(i,PICK);
    }

    @Override protected void onActivityResult(int q,int rc,Intent data){
        super.onActivityResult(q,rc,data);
        if(rc!=RESULT_OK||data==null||data.getData()==null)return;
        if(q==PICK)load(data.getData()); else if(q==PDF)writePdf(data.getData());
    }

    void load(Uri u){
        busy(true);
        status.setText("Lendo histórico da Lotofácil...");
        exec.execute(()->{
            try{
                List<Draw>x=Parser.parse(getContentResolver().openInputStream(u));
                if(x.size()<30)throw new Exception("Histórico insuficiente: "+x.size());
                hist=x;
                runOnUiThread(()->{
                    busy(false);
                    go.setEnabled(true);
                    bar.setProgress(100);
                    Draw last=hist.get(hist.size()-1);
                    status.setText("Histórico carregado: "+hist.size()+" concursos. Último: "+last.contest);
                    out.setText("Último resultado:\n"+fmt(last.nums));
                });
            }catch(Throwable t){runOnUiThread(()->fail(t));}
        });
    }

    int repetidas(){
        int id=repsGroup.getCheckedRadioButtonId();
        if(id==8||id==9||id==10)return id;
        return 9;
    }

    void analyze(){
        if(hist.isEmpty())return;
        int rep=repetidas();
        busy(true); out.setText("");
        exec.execute(()->{
            try{
                Result rr=new Engine(hist,rep,(p,m)->runOnUiThread(()->{bar.setProgress(p);status.setText(m);})).run();
                result=rr;
                runOnUiThread(()->{
                    busy(false);
                    pdf.setEnabled(true);
                    bar.setProgress(100);
                    status.setText("Análise concluída.");
                    out.setText(rr.summary());
                });
            }catch(Throwable t){runOnUiThread(()->fail(t));}
        });
    }

    void createPdf(){
        if(result==null)return;
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE,"Lotofacil_Engrossando_o_Talo.pdf");
        startActivityForResult(i,PDF);
    }

    void writePdf(Uri u){
        try(OutputStream os=getContentResolver().openOutputStream(u)){
            PdfDocument doc=new PdfDocument();
            PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
            Canvas c=page.getCanvas();
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.rgb(123,31,162)); p.setTextSize(22); p.setFakeBoldText(true);
            c.drawText("LOTOFÁCIL — ENGROSSANDO O TALO",28,40,p);
            p.setFakeBoldText(false); p.setColor(Color.DKGRAY); p.setTextSize(10);
            c.drawText("Verde = jogo sugerido | Vermelho = falha",28,58,p);

            Set<Integer> sel=new HashSet<>(); for(int n:result.bestGame)sel.add(n);
            int sx=72,sy=100,dx=85,dy=50,r=18;
            for(int n=1;n<=25;n++){
                int idx=n-1,row=idx/5,col=idx%5;
                float x=sx+col*dx,y=sy+row*dy;
                p.setColor(sel.contains(n)?Color.rgb(25,145,70):Color.rgb(198,40,40));
                c.drawCircle(x,y,r,p);
                p.setColor(Color.WHITE);p.setTextSize(11);p.setFakeBoldText(true);
                String s=String.format(Locale.US,"%02d",n);
                c.drawText(s,x-p.measureText(s)/2,y+4,p);
            }

            p.setFakeBoldText(false);p.setColor(Color.BLACK);p.setTextSize(10.5f);
            int y=390;
            for(String line:result.summary().split("\n")){
                if(y>812)break;
                c.drawText(line,28,y,p); y+=14;
            }
            doc.finishPage(page); doc.writeTo(os); doc.close();
            status.setText("PDF gerado.");
        }catch(Throwable t){fail(t);}
    }

    void busy(boolean b){imp.setEnabled(!b);go.setEnabled(!b&&!hist.isEmpty());repsGroup.setEnabled(!b);if(b){pdf.setEnabled(false);bar.setProgress(0);}}
    void fail(Throwable t){busy(false);status.setText("Erro: "+t.getMessage());new AlertDialog.Builder(this).setTitle("Erro").setMessage(String.valueOf(t)).setPositiveButton("OK",null).show();}
    static String fmt(int[]a){StringBuilder s=new StringBuilder();for(int n:a){if(s.length()>0)s.append(" ");s.append(String.format(Locale.US,"%02d",n));}return s.toString();}
}

class Draw{int contest;int[]nums;Draw(int c,int[]n){contest=c;nums=n;}}

class Parser{
    static List<Draw>parse(InputStream in)throws Exception{
        List<Draw>out=new ArrayList<>();
        BufferedReader br=new BufferedReader(new InputStreamReader(in));
        String line; int auto=1;
        while((line=br.readLine())!=null){
            String[] raw=line.trim().split("[^0-9]+");
            List<Integer> vals=new ArrayList<>();
            for(String s:raw)if(!s.isEmpty())try{vals.add(Integer.parseInt(s));}catch(Exception ignored){}
            if(vals.size()<15)continue;
            int contest=vals.size()>=16?vals.get(0):auto;
            int start=vals.size()>=16?1:0;
            int[] nums=new int[15]; Set<Integer>seen=new HashSet<>(); boolean ok=true; int j=0;
            for(int i=start;i<vals.size()&&j<15;i++){
                int n=vals.get(i);
                if(n>=1&&n<=25&&!seen.contains(n)){seen.add(n);nums[j++]=n;}
            }
            if(j==15){Arrays.sort(nums);out.add(new Draw(contest,nums));}
            auto++;
        }
        out.sort(Comparator.comparingInt(d->d.contest));
        return out;
    }
}

interface ProgressCb{void update(int p,String m);}

class GroupScore{
    int[] group, serie18, serie6; int mask;
    double score,slope18,slope6,growth,persist,ups,move;
}

class GameScore{
    int[] game; GroupScore rep,esp; double score,positional,structure,talo;
}

class Result{
    int repetidas; int[] last,mirror,bestGame; List<GameScore> games;
    String summary(){
        StringBuilder s=new StringBuilder();
        s.append("MELHOR JOGO\n").append(MainActivity.fmt(bestGame)).append("\n\n");
        s.append("Repetidas escolhidas: ").append(repetidas).append("\n");
        s.append("Último: ").append(MainActivity.fmt(last)).append("\n");
        s.append("Espelho: ").append(MainActivity.fmt(mirror)).append("\n\n");
        int limit=Math.min(20,games.size());
        for(int i=0;i<limit;i++){
            GameScore g=games.get(i);
            s.append("JOGO ").append(i+1).append("\n").append(MainActivity.fmt(g.game)).append("\n");
            s.append(String.format(Locale.US,"Score %.2f | Talo %.2f | Pos %.2f | Estr %.2f\n",g.score,g.talo,g.positional,g.structure));
            s.append("Rep: ").append(MainActivity.fmt(g.rep.group)).append(" série6 ").append(Arrays.toString(g.rep.serie6)).append("\n");
            s.append("Esp: ").append(MainActivity.fmt(g.esp.group)).append(" série6 ").append(Arrays.toString(g.esp.serie6)).append("\n\n");
        }
        return s.toString();
    }
}

class Engine{
    static final int JANELA_TALO=18,JANELA_CURTA=6,JANELA_POS=24,TOP_REP=120,TOP_ESP=120,TOP_JOGOS=20;
    static final Set<Integer> PRIMOS=new HashSet<>(Arrays.asList(2,3,5,7,11,13,17,19,23));
    static final Set<Integer> FIB=new HashSet<>(Arrays.asList(1,2,3,5,8,13,21));
    static final Set<Integer> MIOLO=new HashSet<>(Arrays.asList(7,8,9,12,13,14,17,18,19));
    static final Set<Integer> MOLDURA=new HashSet<>(Arrays.asList(1,2,3,4,5,6,10,11,15,16,20,21,22,23,24,25));
    static final Set<Integer> CRUZ=new HashSet<>(Arrays.asList(3,8,11,12,13,14,15,18,23));
    final List<Draw> hist; final int repQtd; final ProgressCb cb; int[] masks; Map<String,double[]> limits; double[] prev,dev;

    Engine(List<Draw>h,int rep,ProgressCb cb){hist=h;repQtd=rep;this.cb=cb;}

    Result run(){
        cb.update(5,"Preparando histórico e máscaras...");
        masks=new int[hist.size()];
        for(int i=0;i<hist.size();i++)masks[i]=mask(hist.get(i).nums);

        cb.update(15,"Aprendendo padrões históricos...");
        limits=learnLimits();

        cb.update(25,"Preparando P01→P15...");
        preparePositional();

        int[] last=hist.get(hist.size()-1).nums;
        Set<Integer> ls=new HashSet<>();for(int n:last)ls.add(n);
        int[] mirror=new int[10];int k=0;for(int n=1;n<=25;n++)if(!ls.contains(n))mirror[k++]=n;

        cb.update(35,"Ranqueando talos das repetidas...");
        List<GroupScore> reps=rankGroups(last,repQtd,TOP_REP,"Repetidas",35,55);

        cb.update(58,"Ranqueando talos do espelho...");
        List<GroupScore> esps=rankGroups(mirror,15-repQtd,TOP_ESP,"Espelho",58,75);

        cb.update(78,"Cruzando repetidas + espelho...");
        List<GameScore> games=cross(reps,esps);

        Result r=new Result();
        r.repetidas=repQtd;r.last=last;r.mirror=mirror;r.games=games;r.bestGame=games.get(0).game;
        cb.update(100,"Análise concluída.");
        return r;
    }

    List<GroupScore> rankGroups(int[] universe,int qtd,int top,String title,int p0,int p1){
        PriorityQueue<GroupScore> pq=new PriorityQueue<>(Comparator.comparingDouble(g->g.score));
        int total=comb(universe.length,qtd); int[] cur=new int[qtd]; int[] count={0};
        gen(universe,0,0,qtd,cur,g->{
            count[0]++;
            GroupScore gs=talo(g);
            if(pq.size()<top)pq.add(gs); else if(gs.score>pq.peek().score){pq.poll();pq.add(gs);}
            if(count[0]%500==0||count[0]==total){
                int pct=p0+(int)((p1-p0)*count[0]/(double)total);
                cb.update(pct,title+": "+count[0]+"/"+total);
            }
        });
        List<GroupScore> out=new ArrayList<>(pq);
        out.sort((a,b)->Double.compare(b.score,a.score));
        return out;
    }

    GroupScore talo(int[] group){
        GroupScore gs=new GroupScore(); gs.group=group.clone(); Arrays.sort(gs.group); gs.mask=mask(gs.group);
        gs.serie18=series(gs.mask,JANELA_TALO); gs.serie6=Arrays.copyOfRange(gs.serie18,Math.max(0,gs.serie18.length-JANELA_CURTA),gs.serie18.length);
        double weighted=0,sw=0; for(int i=0;i<gs.serie18.length;i++){double w=i+1;weighted+=gs.serie18[i]*w;sw+=w;}
        double media=weighted/sw/group.length*100.0;
        gs.slope18=slope(gs.serie18); gs.slope6=slope(gs.serie6);
        int mid=gs.serie18.length/2; double old=avg(gs.serie18,0,mid), neu=avg(gs.serie18,mid,gs.serie18.length); gs.growth=neu-old;
        int lim=(int)Math.ceil(group.length*.60), per=0; for(int x:gs.serie18)if(x>=lim)per++; gs.persist=100.0*per/gs.serie18.length;
        int ups=0; for(int i=1;i<gs.serie6.length;i++)if(gs.serie6[i]>gs.serie6[i-1])ups++; gs.ups=100.0*ups/Math.max(1,gs.serie6.length-1);
        gs.move=gs.serie6.length>=2?gs.serie6[gs.serie6.length-1]-gs.serie6[gs.serie6.length-2]:0;
        double b18=clamp(gs.slope18*15,-15,15), b6=clamp(gs.slope6*18,-20,20), bg=clamp(gs.growth*8,-15,15), bm=clamp(gs.move*5,-10,10);
        gs.score=media*.34+gs.persist*.22+gs.ups*.12+b18+b6+bg+bm;
        return gs;
    }

    List<GameScore> cross(List<GroupScore> reps,List<GroupScore> esps){
        PriorityQueue<GameScore> pq=new PriorityQueue<>(Comparator.comparingDouble(g->g.score));
        int total=reps.size()*esps.size(),c=0;
        for(GroupScore r:reps)for(GroupScore e:esps){
            c++;
            int[] game=merge(r.group,e.group);
            if(game.length!=15)continue;
            GameScore gs=new GameScore(); gs.game=game; gs.rep=r; gs.esp=e;
            gs.talo=(r.score+e.score)/2.0; gs.positional=scorePos(game); gs.structure=scoreStructure(game);
            gs.score=gs.talo*.55+gs.positional*.27+gs.structure*.18;
            if(pq.size()<TOP_JOGOS)pq.add(gs); else if(gs.score>pq.peek().score){pq.poll();pq.add(gs);}
            if(c%2000==0||c==total)cb.update(78+(int)(18*c/(double)total),"Cruzamento: "+c+"/"+total);
        }
        List<GameScore> out=new ArrayList<>(pq);
        out.sort((a,b)->Double.compare(b.score,a.score));
        return out;
    }

    Map<String,double[]> learnLimits(){
        Map<String,List<Double>> vals=new HashMap<>();
        String[] keys={"soma","pares","primos","fib","miolo","moldura","cruz","seq","L1","L2","L3","L4","L5","C1","C2","C3","C4","C5"};
        for(String k:keys)vals.put(k,new ArrayList<>());
        for(Draw d:hist){
            Map<String,Double> f=features(d.nums);
            for(String k:keys)vals.get(k).add(f.get(k));
        }
        Map<String,double[]> out=new HashMap<>();
        for(String k:keys)out.put(k,new double[]{quant(vals.get(k),.02),quant(vals.get(k),.98)});
        return out;
    }

    double scoreStructure(int[] game){
        Map<String,Double> f=features(game); int ok=0,total=0;
        for(String k:limits.keySet()){double[] l=limits.get(k);total++;if(f.get(k)>=l[0]&&f.get(k)<=l[1])ok++;}
        return 100.0*ok/total;
    }

    void preparePositional(){
        int start=Math.max(0,hist.size()-JANELA_POS); int n=hist.size()-start;
        prev=new double[15];dev=new double[15];
        for(int p=0;p<15;p++){
            int[] serie=new int[n]; for(int i=0;i<n;i++)serie[i]=hist.get(start+i).nums[p];
            double sw=0,s=0; for(int i=0;i<n;i++){double w=i+1;s+=serie[i]*w;sw+=w;}
            double media=s/sw; double sl=slope(serie); prev[p]=media+sl*1.5;
            double av=avg(serie,0,n),var=0; for(int x:serie)var+=(x-av)*(x-av); dev[p]=Math.max(.75,Math.sqrt(var/n));
        }
    }

    double scorePos(int[] game){
        double s=0; for(int i=0;i<15;i++){double z=(game[i]-prev[i])/dev[i];s+=Math.exp(-.5*z*z);}
        return s/15.0*100.0;
    }

    Map<String,Double> features(int[] a){
        Set<Integer>s=new HashSet<>();for(int n:a)s.add(n);Map<String,Double> f=new HashMap<>();
        int sum=0,even=0;for(int n:a){sum+=n;if(n%2==0)even++;}
        f.put("soma",(double)sum);f.put("pares",(double)even);f.put("primos",(double)countSet(s,PRIMOS));f.put("fib",(double)countSet(s,FIB));
        f.put("miolo",(double)countSet(s,MIOLO));f.put("moldura",(double)countSet(s,MOLDURA));f.put("cruz",(double)countSet(s,CRUZ));f.put("seq",(double)seq(a));
        for(int l=0;l<5;l++){int c=0;for(int n:a)if((n-1)/5==l)c++;f.put("L"+(l+1),(double)c);}
        for(int col=0;col<5;col++){int c=0;for(int n:a)if((n-1)%5==col)c++;f.put("C"+(col+1),(double)c);}
        return f;
    }

    int[] series(int m,int janela){int start=Math.max(0,masks.length-janela);int[] out=new int[masks.length-start];for(int i=start;i<masks.length;i++)out[i-start]=Integer.bitCount(m&masks[i]);return out;}
    static int mask(int[]a){int m=0;for(int n:a)m|=1<<(n-1);return m;}
    static int[] merge(int[]a,int[]b){TreeSet<Integer>s=new TreeSet<>();for(int x:a)s.add(x);for(int x:b)s.add(x);int[]r=new int[s.size()];int i=0;for(int x:s)r[i++]=x;return r;}
    interface Sink{void add(int[]g);}
    static void gen(int[]u,int st,int dep,int q,int[]cur,Sink sink){if(dep==q){sink.add(cur.clone());return;}for(int i=st;i<=u.length-(q-dep);i++){cur[dep]=u[i];gen(u,i+1,dep+1,q,cur,sink);}}
    static int comb(int n,int k){long r=1;for(int i=1;i<=k;i++)r=r*(n-k+i)/i;return (int)r;}
    static double slope(int[]v){if(v.length<2)return 0;double mx=(v.length-1)/2.0,my=avg(v,0,v.length),num=0,den=0;for(int i=0;i<v.length;i++){double dx=i-mx;num+=dx*(v[i]-my);den+=dx*dx;}return den==0?0:num/den;}
    static double avg(int[]a,int s,int e){double x=0;for(int i=s;i<e;i++)x+=a[i];return x/Math.max(1,e-s);}
    static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
    static int countSet(Set<Integer>a,Set<Integer>b){int c=0;for(int x:b)if(a.contains(x))c++;return c;}
    static int seq(int[]a){int best=1,cur=1;for(int i=1;i<a.length;i++){if(a[i]==a[i-1]+1){cur++;best=Math.max(best,cur);}else cur=1;}return best;}
    static double quant(List<Double> vals,double q){Collections.sort(vals);double p=(vals.size()-1)*q;int lo=(int)Math.floor(p),hi=(int)Math.ceil(p);if(lo==hi)return vals.get(lo);double w=p-lo;return vals.get(lo)*(1-w)+vals.get(hi)*w;}
}
