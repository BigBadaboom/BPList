package com.caverock.bplist;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;

public class Dict extends HashMap<String, Object>
{
    public Dict()
    {
        super();
    }

    public Dict(int initialCapacity)
    {
        super(initialCapacity);
    }


    //------------------------------------------------------------------------------------------------------------------


    public Integer  getInt(String key)
    {
        return (Integer) get(key);
    }

    public Long  getLong(String key)
    {
        return (Long) get(key);
    }

    public Float  getFloat(String key)
    {
        return (Float) get(key);
    }

    public Double  getDouble(String key)
    {
        return (Double) get(key);
    }

    public Boolean  getBoolean(String key)
    {
        return (Boolean) get(key);
    }

    public String  getString(String key)
    {
        return (String) get(key);
    }

    public Dict  getDict(String key)
    {
        return (Dict) get(key);
    }

    public Uid  getUid(String key)
    {
        return (Uid) get(key);
    }

    public Object[] getObjectArray(String key)
    {
        return (Object[]) get(key);
    }

    public Uid[] getUidArray(String key)
    {
        Object[]  objs = (Object[]) get(key);
        return Arrays.copyOf(objs, objs.length, Uid[].class);
    }



}
