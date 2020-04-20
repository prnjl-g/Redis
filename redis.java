import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
public class Redis {
    private static AtomicInteger sortedElementsSize = new AtomicInteger(0);
    private static Map<String,String> elements = Collections.synchronizedMap(new HashMap<String, String>());
    private static Map<String,Long> expires = Collections.synchronizedMap(new HashMap<String, Long>());
    private static Map<String,SortedMap<Long,SortedSet<String>>> sortedElements = Collections.synchronizedMap(new HashMap<String,SortedMap<Long,SortedSet<String>>>());
    private static Map<String,Map<String,Long>> refElements = Collections.synchronizedMap(new HashMap<String,Map<String,Long>>());
    public static synchronized String GET(String key){
        if(expires.containsKey(key)){
            if(expires.get(key) <= System.currentTimeMillis()){
                expires.remove(key);
                elements.remove(key);
            }

        }
        if(elements.containsKey(key)){
            return elements.get(key);
        }
        else return null;
    }
    public static synchronized String SET(String key, String value){
        if(expires.containsKey(key)){
            if(expires.get(key) <= System.currentTimeMillis()){
                expires.remove(key);
                elements.remove(key);
            }

        }
        elements.put(key, value);
        if(expires.containsKey(key)) expires.remove(key);
        return "OK";
    }
    public static synchronized int Expire(String key, int time){
        if(time <= 0 && expires.containsKey(key)){
            expires.remove(key);
            elements.remove(key);
        }
        else{
            expires.put(key,System.currentTimeMillis() + time);
        }
        return 1;
    }
    public static synchronized int ZADD(String data){
        String[] key_value = data.split(" ");
        if(refElements.containsKey(key_value[0])){
            for(int i = 1;i < key_value.length;i += 2){
                long value = Long.parseLong(key_value[i]);
                if(refElements.get(key_value[0]).containsKey(key_value[i+1])){
                    sortedElements.get(key_value[0]).get(value).remove(key_value[i+1]);
                    sortedElementsSize.getAndAdd(-1);
                }
                refElements.get(key_value[0]).put(key_value[i+1], value);
                sortedElementsSize.getAndAdd(1);
                if(sortedElements.get(key_value[0]).containsKey(value)){
                    sortedElements.get(key_value[0]).get(value).add(key_value[i+1]);
                }
                else{
                    SortedSet<String> stringData = Collections.synchronizedSortedSet(new TreeSet<String>());
                    stringData.add(key_value[i+1]);
                    sortedElements.get(key_value[0]).put(value, stringData);
                }
            }
        }
        else{
            SortedMap<String,Long> values = Collections.synchronizedSortedMap(new TreeMap<String, Long>());
            SortedMap<Long,SortedSet<String>> sortedValue = Collections.synchronizedSortedMap(new TreeMap<Long, SortedSet<String>>());
            for(int i = 1;i < key_value.length;i += 2){
                sortedElementsSize.getAndAdd(1);
                long value = Long.parseLong(key_value[i]);
                values.put(key_value[i+1], Long.parseLong(key_value[i]));
                if(sortedValue.containsKey(value)){
                    sortedValue.get(value).add(key_value[i+1]);
                }
                else{
                    SortedSet<String> stringData = Collections.synchronizedSortedSet(new TreeSet<String>());
                    stringData.add(key_value[i+1]);
                    sortedValue.put(value, stringData);
                }
            }
            refElements.put(key_value[0], values);
            sortedElements.put(key_value[0], sortedValue);
        }
        return 1;
    }
    public static synchronized Integer ZRANK(String key, String value){
        if(refElements.containsKey(key)){
            if(refElements.get(key).containsKey(value)){
                long score = refElements.get(key).get(value);
                int index = 0;
                for(long i:sortedElements.get(key).keySet()){
                    if(i == score){
                        for(String s:sortedElements.get(key).get(i)){
                            if(s == value) break;
                            index++;
                        }
                        break;
                    }
                    index += sortedElements.get(key).get(i).size();
                }
                return index;
            }
            return null;
        }
        return null;
    }
    public static synchronized List<String> ZRANGE(String key, int start, int stop){
        List<String> element = Collections.synchronizedList(new ArrayList<String>());
        if(start < 0) {
            start += sortedElementsSize.get();
        }
        if(stop < 0) {
            stop += sortedElementsSize.get();
        }
        if(start > stop) {
            return element;
        }
        int index = 0;
        for(long i:sortedElements.get(key).keySet()){
            if(index + sortedElements.get(key).get(i).size() <= start){
                index += sortedElements.get(key).get(i).size();
                continue;
            }
            for(String s:sortedElements.get(key).get(i)){
                if(index <= start){
                    index++;
                    continue;
                }
                element.add(s);
                index++;
                if(index > stop) break;
            }
            if(index > stop) break;
        }
        return element;
    }
}
