package local.ivand.pomodoro;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.UUID;

/** Local preset browser/editor. Selecting or saving never changes the running timer. */
final class PresetPanel {
    interface Apply {void preset(String id);}
    private final Activity activity;
    private final Apply apply;
    private final PresetStore store;
    private final int ink=0xfff3f1e2,muted=0xffa0b1a8,accent=0xfff18770;
    private Dialog browser;
    private LinearLayout list,details,manageActions;
    private String selected;
    private boolean deleting;
    PresetPanel(Activity activity,Apply apply){this.activity=activity;this.apply=apply;store=new PresetStore(activity);}
    private int dp(int n){return (int)(n*activity.getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;}
    private LinearLayout col(){LinearLayout v=new LinearLayout(activity);v.setOrientation(1);return v;}
    private TextView text(String label,int size,int color){TextView v=new TextView(activity);v.setText(label);v.setTextSize(size);v.setTextColor(color);return v;}
    private void gap(LinearLayout v,int n){v.addView(new View(activity),new LinearLayout.LayoutParams(1,dp(n)));}
    private Button button(String label,boolean primary,Runnable action){Button b=new Button(activity);b.setText(label);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(primary?0xff141b1c:ink);b.setPadding(dp(8),0,dp(8),0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setBackground(shape(primary?accent:0xff34443c));b.setOnClickListener(v->action.run());return b;}
    private Dialog dialog(String title,LinearLayout content){
        Dialog d=new FullscreenDialog(activity);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=col();root.setPadding(dp(20),dp(14),dp(20),dp(16));root.setBackground(shape(0xff202b2a));
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=text(title,23,ink);name.setTypeface(null,Typeface.BOLD);header.addView(name,new LinearLayout.LayoutParams(0,dp(42),1));
        Button close=button("×",false,()->d.dismiss());close.setTextSize(25);close.setContentDescription("Закрыть");header.addView(close,new LinearLayout.LayoutParams(dp(42),dp(38)));root.addView(header);gap(root,10);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));d.setContentView(root);
        Window w=d.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND|WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);w.setDimAmount(.65f);w.getDecorView().setSystemUiVisibility(5894);
        d.setOnDismissListener(x->activity.getWindow().getDecorView().setSystemUiVisibility(5894));return d;
    }
    private void open(Dialog d){d.show();d.getWindow().setLayout(Math.min(dp(596),activity.getResources().getDisplayMetrics().widthPixels-dp(24)),Math.min(dp(328),activity.getResources().getDisplayMetrics().heightPixels-dp(24)));d.getWindow().getDecorView().setSystemUiVisibility(5894);}
    void show(){
        android.content.SharedPreferences p=activity.getSharedPreferences("timer",0);selected=p.getString("presetId","builtin-classic");if(store.find(selected)==null)selected="builtin-classic";
        LinearLayout body=new LinearLayout(activity);list=col();details=col();
        LinearLayout sidebar=col();ScrollView scroll=new ScrollView(activity);scroll.addView(list);sidebar.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));gap(sidebar,8);sidebar.addView(button("+ Свой режим",false,()->edit(null)),new LinearLayout.LayoutParams(-1,dp(38)));body.addView(sidebar,new LinearLayout.LayoutParams(dp(228),-1));
        View divider=new View(activity);divider.setBackgroundColor(0xff3a4941);LinearLayout.LayoutParams split=new LinearLayout.LayoutParams(dp(1),-1);split.setMargins(dp(18),0,dp(18),0);body.addView(divider,split);
        LinearLayout right=col();ScrollView detailScroll=new ScrollView(activity);detailScroll.addView(details);right.addView(detailScroll,new LinearLayout.LayoutParams(-1,0,1));gap(right,8);manageActions=new LinearLayout(activity);right.addView(manageActions,new LinearLayout.LayoutParams(-1,dp(38)));body.addView(right,new LinearLayout.LayoutParams(0,-1,1));
        browser=dialog("Режимы помодоро",body);render();open(browser);
    }
    private void render(){
        list.removeAllViews();details.removeAllViews();manageActions.removeAllViews();
        java.util.List<PomodoroPreset> all=store.all();PomodoroPreset preset=store.find(selected);if(preset==null){selected=all.get(0).id;preset=all.get(0);}
        for(PomodoroPreset row:all){
            LinearLayout item=col();item.setPadding(dp(12),dp(9),dp(12),dp(9));item.setBackground(shape(row.id.equals(selected)?0xff405448:0xff293631));
            TextView name=text(row.name,15,ink);name.setMaxLines(2);item.addView(name);TextView summary=text(row.work+" / "+row.rest+" / "+row.longRest+" · цикл "+row.every,11,muted);item.addView(summary);
            item.setContentDescription(row.name+". "+row.summary());item.setOnClickListener(v->{selected=row.id;deleting=false;render();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(6);list.addView(item,lp);
        }
        final PomodoroPreset chosen=preset;
        TextView title=text(preset.name,21,ink);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(2);details.addView(title);gap(details,5);
        TextView summary=text("Работа: "+preset.work+" мин · отдых: "+preset.rest+" мин\nДлинный: "+preset.longRest+" мин после "+preset.every+" работ",13,muted);summary.setLineSpacing(dp(2),1);details.addView(summary);gap(details,9);
        details.addView(button("Применить режим",true,()->{apply.preset(chosen.id);browser.dismiss();}),new LinearLayout.LayoutParams(-1,dp(40)));
        TextView note=text("Начнёт новый цикл и сбросит текущий интервал.",10,muted);LinearLayout.LayoutParams noteLp=new LinearLayout.LayoutParams(-1,dp(34));noteLp.topMargin=dp(5);details.addView(note,noteLp);
        details.addView(button("Создать на основе",false,()->edit(new PomodoroPreset("custom-"+UUID.randomUUID(),newName(),chosen.work,chosen.rest,chosen.longRest,chosen.every,false))),new LinearLayout.LayoutParams(-1,dp(33)));
        if(!preset.builtIn){
            manageActions.addView(button("Изменить",false,()->edit(chosen)),new LinearLayout.LayoutParams(0,dp(38),1));
            Button delete=button(deleting?"Точно удалить?":"Удалить",false,()->{if(!deleting){deleting=true;render();return;}try{store.delete(chosen.id);deleting=false;render();}catch(IllegalArgumentException error){Toast.makeText(activity,error.getMessage(),Toast.LENGTH_LONG).show();}});
            delete.setTextColor(accent);LinearLayout.LayoutParams delLp=new LinearLayout.LayoutParams(0,dp(38),1);delLp.leftMargin=dp(8);manageActions.addView(delete,delLp);
        }else{TextView noteBuiltIn=text("Встроенный режим · можно создать копию",11,muted);noteBuiltIn.setGravity(Gravity.CENTER_VERTICAL);manageActions.addView(noteBuiltIn,new LinearLayout.LayoutParams(-1,-1));}
    }
    private String newName(){for(int n=1;;n++){String candidate=n==1?"Мой режим":"Мой режим "+n;boolean found=false;for(PomodoroPreset p:store.all())if(p.name.equalsIgnoreCase(candidate))found=true;if(!found)return candidate;}}
    private int[] stepper(LinearLayout parent,String label,int initial,int max){
        int[] value={initial};LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(text(label,14,ink),new LinearLayout.LayoutParams(0,dp(46),1));
        TextView number=text(Integer.toString(initial),19,ink);number.setGravity(Gravity.CENTER);
        Button minus=button("−",false,()->{value[0]=Math.max(1,value[0]-1);number.setText(Integer.toString(value[0]));});minus.setContentDescription("Уменьшить: "+label);
        Button plus=button("+",false,()->{value[0]=Math.min(max,value[0]+1);number.setText(Integer.toString(value[0]));});plus.setContentDescription("Увеличить: "+label);
        row.addView(minus,new LinearLayout.LayoutParams(dp(36),dp(36)));row.addView(number,new LinearLayout.LayoutParams(dp(38),dp(40)));row.addView(plus,new LinearLayout.LayoutParams(dp(36),dp(36)));parent.addView(row);return value;
    }
    private void edit(PomodoroPreset existing){
        android.content.SharedPreferences p=activity.getSharedPreferences("timer",0);
        PomodoroPreset base=existing==null?new PomodoroPreset("custom-"+UUID.randomUUID(),newName(),p.getInt("work",25),p.getInt("short",5),p.getInt("long",15),p.getInt("longEvery",4),false):existing;
        LinearLayout body=new LinearLayout(activity),left=col(),right=col();body.addView(left,new LinearLayout.LayoutParams(dp(230),-1));LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(0,-1,1);rlp.leftMargin=dp(24);body.addView(right,rlp);
        left.addView(text("НАЗВАНИЕ РЕЖИМА",10,muted));gap(left,8);final String[] name={base.name};TextView nameView=text(name[0],22,ink);nameView.setMaxLines(2);left.addView(nameView,new LinearLayout.LayoutParams(-1,dp(56)));
        left.addView(button("Изменить название",false,()->rename(name,nameView)),new LinearLayout.LayoutParams(-1,dp(34)));gap(left,9);
        TextView hint=text("Сохранение не меняет таймер.\nПрименить режим можно отдельно.",12,muted);left.addView(hint,new LinearLayout.LayoutParams(-1,dp(52)));
        int[] work=stepper(right,"Работа, мин",base.work,120),rest=stepper(right,"Отдых, мин",base.rest,60),longRest=stepper(right,"Длинный, мин",base.longRest,60),every=stepper(right,"Подходов",base.every,8);
        TextView note=text("Длинный отдых после выбранного числа работ",10,muted);right.addView(note);
        Dialog editor=dialog("Свой режим",body);
        left.addView(button("Сохранить режим",true,()->{try{PomodoroPreset saved=new PomodoroPreset(base.id,name[0],work[0],rest[0],longRest[0],every[0],false);store.put(saved);selected=saved.id;deleting=false;editor.dismiss();render();}catch(IllegalArgumentException error){Toast.makeText(activity,error.getMessage(),Toast.LENGTH_LONG).show();}}),new LinearLayout.LayoutParams(-1,dp(40)));
        open(editor);
    }
    private void rename(String[] value,TextView nameView){
        Dialog d=new Dialog(activity);d.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout panel=col();panel.setPadding(dp(12),dp(6),dp(12),dp(6));panel.setBackground(shape(0xff202b2a));panel.addView(text("Название режима",14,ink));
        EditText input=new EditText(activity);input.setSingleLine(true);input.setTextColor(ink);input.setTextSize(18);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});input.setText(value[0]);input.selectAll();input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_ACTION_DONE);panel.addView(input,new LinearLayout.LayoutParams(-1,dp(40)));
        TextView error=text("",12,accent);error.setVisibility(View.GONE);panel.addView(error);
        Runnable save=()->{String name=input.getText().toString().trim();if(name.isEmpty()){error.setVisibility(View.VISIBLE);error.setText("Введите название");return;}value[0]=name;nameView.setText(name);((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);d.dismiss();};
        LinearLayout row=new LinearLayout(activity);row.addView(button("Готово",true,save),new LinearLayout.LayoutParams(0,dp(34),1));Button cancel=button("Отмена",false,()->d.dismiss());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(34),1);lp.leftMargin=dp(8);row.addView(cancel,lp);panel.addView(row);input.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE||(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER)){save.run();return true;}return false;});
        d.setContentView(panel);d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_FULLSCREEN);d.getWindow().getDecorView().setSystemUiVisibility(5894);d.getWindow().setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);WindowManager.LayoutParams params=d.getWindow().getAttributes();params.y=dp(8);d.getWindow().setAttributes(params);d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);d.show();d.getWindow().setLayout(dp(480),-2);input.requestFocus();
    }
}
