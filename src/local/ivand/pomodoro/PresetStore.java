package local.ivand.pomodoro;

import android.content.*;
import android.util.Log;
import org.json.*;
import java.util.*;

/** Built-ins are immutable. Custom definitions are persisted independently from the active timer snapshot. */
final class PresetStore {
    private final SharedPreferences data;
    PresetStore(Context context){data=context.getSharedPreferences("presets",0);}
    java.util.List<PomodoroPreset> all(){
        ArrayList<PomodoroPreset> result=new ArrayList<>(Arrays.asList(PomodoroPreset.defaults()));
        try{JSONArray array=new JSONArray(data.getString("custom","[]"));for(int i=0;i<array.length();i++){
            try{JSONObject v=array.getJSONObject(i);String id=v.getString("id");if(!id.startsWith("custom-"))continue;
                PomodoroPreset p=new PomodoroPreset(id,v.getString("name"),v.getInt("work"),v.getInt("rest"),v.getInt("longRest"),v.getInt("every"),false);
                boolean duplicate=false;for(PomodoroPreset existing:result)if(existing.id.equals(id)||existing.name.equalsIgnoreCase(p.name))duplicate=true;
                if(!duplicate)result.add(p);
            }catch(Exception error){Log.w("HonorPomo","Skipped invalid preset",error);}
        }}catch(JSONException error){Log.w("HonorPomo","Cannot read custom presets",error);}return result;
    }
    PomodoroPreset find(String id){for(PomodoroPreset p:all())if(p.id.equals(id))return p;return null;}
    void put(PomodoroPreset preset){
        if(preset.builtIn||!preset.id.startsWith("custom-"))throw new IllegalArgumentException("Встроенный режим нельзя заменить");
        java.util.List<PomodoroPreset> list=all();
        for(PomodoroPreset p:list)if(!p.id.equals(preset.id)&&p.name.equalsIgnoreCase(preset.name))throw new IllegalArgumentException("Такое название уже есть");
        for(int i=list.size()-1;i>=0;i--)if(list.get(i).id.equals(preset.id))list.remove(i);list.add(preset);write(list);
    }
    void delete(String id){java.util.List<PomodoroPreset> list=all();for(int i=list.size()-1;i>=0;i--)if(list.get(i).id.equals(id)&&!list.get(i).builtIn)list.remove(i);write(list);}
    private void write(java.util.List<PomodoroPreset> list){
        JSONArray array=new JSONArray();try{for(PomodoroPreset p:list)if(!p.builtIn)array.put(new JSONObject().put("id",p.id).put("name",p.name).put("work",p.work).put("rest",p.rest).put("longRest",p.longRest).put("every",p.every));}
        catch(JSONException error){throw new IllegalArgumentException("Не удалось сохранить режим",error);}
        if(!data.edit().putString("custom",array.toString()).commit())throw new IllegalArgumentException("Не удалось сохранить режим");
    }
}
