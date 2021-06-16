package com.caverock.bplist;

public class Uid
{
    private final long  uid;

    public Uid(Long uid)
    {
        if (uid == null)
            throw new NullPointerException("uid is null");
        this.uid = uid;
    }

    public long getUid()
    {
        return uid;
    }
    public int intValue()
    {
        return ((Long) uid).intValue();
    }
}
